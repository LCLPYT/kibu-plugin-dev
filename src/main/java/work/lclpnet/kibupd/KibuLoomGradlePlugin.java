package work.lclpnet.kibupd;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftJarConfiguration;
import net.fabricmc.loom.task.RunGameTask;
import net.fabricmc.loom.util.gradle.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskContainer;
import work.lclpnet.kibupd.task.FixRunClasspathTask;
import work.lclpnet.kibupd.task.PluginPathsConfigTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KibuLoomGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        final SourceSetContainer sourceSets = target.getExtensions().getByType(SourceSetContainer.class);
        final TaskContainer tasks = target.getTasks();

        final var generatePluginPathsConfig = tasks.register("generatePluginPathsConfig", PluginPathsConfigTask.class, task -> {
            Path configPath = target.getProjectDir().toPath().resolve(".gradle").resolve("kibu-dev")
                    .resolve("config.json");

            task.getOutputFile().set(configPath.toFile());
        }).get();

        final var fixIdeaRunClasspath = tasks.register("fixIdeaRunClasspath", FixRunClasspathTask.class, task -> {
            task.dependsOn(generatePluginPathsConfig);

            final Task genIdeaWorkspace = tasks.named("genIdeaWorkspace").get();
            genIdeaWorkspace.finalizedBy(task);
            task.mustRunAfter(genIdeaWorkspace);
        }).get();

        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure(main -> {
            // remove all sourceSet outputs from the main runtime classpath
            // the outputs will be loaded by the plugin loader instead
            for (SourceSet sourceSet : sourceSets) {
                FileCollection compileClasspath = main.getCompileClasspath();
                SourceSetOutput output = sourceSet.getOutput();

                compileClasspath.minus(output);
            }

            // exclude main outputs in the classpath of the run config
            fixIdeaRunClasspath.getClasspathExcludes().from(main.getOutput());

            final RegularFileProperty kibuDevConfig = generatePluginPathsConfig.getOutputFile();

            // configure run game tasks to contain paths to plugin sources
            tasks.withType(RunGameTask.class).forEach(task -> {
                task.dependsOn(generatePluginPathsConfig);
                task.systemProperty("kibu-dev.config", kibuDevConfig);
            });
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

            final ConfigurableFileCollection inputFiles = fixIdeaRunClasspath.getInputFiles();

            runConfigFiles.stream()
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(inputFiles::from);
        });
    }
}
