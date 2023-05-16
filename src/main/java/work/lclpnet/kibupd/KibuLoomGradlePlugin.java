package work.lclpnet.kibupd;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftJarConfiguration;
import net.fabricmc.loom.task.RunGameTask;
import net.fabricmc.loom.util.gradle.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskContainer;
import work.lclpnet.kibupd.ext.KibuGradleExtension;
import work.lclpnet.kibupd.ext.KibuGradleExtensionImpl;
import work.lclpnet.kibupd.task.FixRunClasspathTask;
import work.lclpnet.kibupd.task.KibuDevConfigTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KibuLoomGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        final KibuGradleExtension ext = target.getExtensions().create(KibuGradleExtension.class,
                "kibu", KibuGradleExtensionImpl.class, target);

        final SourceSetContainer sourceSets = target.getExtensions().getByType(SourceSetContainer.class);
        final TaskContainer tasks = target.getTasks();

        final var generateKibuDevConfig = tasks.register("generateKibuDevConfig", KibuDevConfigTask.class);
        final var fixIdeaRunClasspath = tasks.register("fixIdeaRunClasspath", FixRunClasspathTask.class);
        final var configurePluginLaunch = tasks.register("configurePluginLaunch");
        final var ideaSyncTask = tasks.named("ideaSyncTask");

        configurePluginLaunch.configure(task -> {
            task.setGroup(KibuGradlePlugin.TASK_GROUP);

            task.dependsOn(generateKibuDevConfig);
            task.dependsOn(fixIdeaRunClasspath);
        });

        // configure kibu dev config path
        generateKibuDevConfig.configure(task -> {
            Path configPath = target.getProjectDir().toPath().resolve(".gradle").resolve("kibu-dev")
                    .resolve("config.json");

            task.getOutputFile().set(configPath.toFile());
        });

        ideaSyncTask.configure(task -> {
            task.finalizedBy(configurePluginLaunch);
            configurePluginLaunch.get().mustRunAfter(task);
        });

        fixIdeaRunClasspath.configure(task -> task.dependsOn(ideaSyncTask));

        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure(main -> {
            // remove all sourceSet outputs from the main runtime classpath
            // the outputs will be loaded by the plugin loader instead
            for (SourceSet sourceSet : sourceSets) {
                FileCollection compileClasspath = main.getCompileClasspath();
                SourceSetOutput output = sourceSet.getOutput();

                compileClasspath.minus(output);
            }

            // exclude main outputs in the classpath of the run config
            Set<File> mainOutputFiles = main.getOutput().getFiles();
            fixIdeaRunClasspath.get().getClasspathExcludes().from(mainOutputFiles);

            final RegularFileProperty kibuDevConfig = generateKibuDevConfig.get().getOutputFile();

            // configure run game tasks to contain paths to plugin sources
            tasks.withType(RunGameTask.class).forEach(task -> {
                task.dependsOn(generateKibuDevConfig);
                task.systemProperty("kibu-dev.config", kibuDevConfig);
            });

            // configure plugin file paths of the target project
            ext.getPluginPaths().from(mainOutputFiles);
        });

        GradleUtils.afterSuccessfulEvaluation(target, () -> {
            // gather run configs (must be done after project evaluation, as minecraftJarConfiguration is set during it
            final LoomGradleExtension extension = LoomGradleExtension.get(target);
            final MinecraftJarConfiguration conf = extension.getMinecraftJarConfiguration().get();

            final Path runConfigDir = target.getProjectDir().toPath()
                    .resolve(".idea").resolve("runConfigurations");

            final List<Path> runConfigFiles = new ArrayList<>();

            if (conf != MinecraftJarConfiguration.CLIENT_ONLY) {
                runConfigFiles.add(runConfigDir.resolve("Minecraft_Server.xml"));
            }

            if (conf != MinecraftJarConfiguration.SERVER_ONLY) {
                runConfigFiles.add(runConfigDir.resolve("Minecraft_Client.xml"));
            }

            final ConfigurableFileCollection inputFiles = fixIdeaRunClasspath.get().getInputFiles();

            runConfigFiles.stream()
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(inputFiles::from);
        });
    }
}
