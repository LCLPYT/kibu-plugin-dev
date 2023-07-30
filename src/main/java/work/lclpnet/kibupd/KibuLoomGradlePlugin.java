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
import org.gradle.api.tasks.*;
import org.gradle.jvm.tasks.Jar;
import work.lclpnet.kibupd.ext.KibuGradleExtension;
import work.lclpnet.kibupd.task.FixIdeaRunConfigsTask;
import work.lclpnet.kibupd.task.KibuDevConfigTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KibuLoomGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        final KibuGradleExtension ext = target.getExtensions().getByType(KibuGradleExtension.class);

        final SourceSetContainer sourceSets = target.getExtensions().getByType(SourceSetContainer.class);
        final TaskContainer tasks = target.getTasks();

        final var generateKibuDevConfig = tasks.register("generateKibuDevConfig", KibuDevConfigTask.class);
        final var fixIdeaRunClasspath = tasks.register("fixIdeaRunConfigs", FixIdeaRunConfigsTask.class);
        final var configurePluginLaunch = tasks.register("configurePluginLaunch");
        final var ideaSyncTask = tasks.named("ideaSyncTask");

        configurePluginLaunch.configure(task -> {
            task.setGroup(KibuGradlePlugin.TASK_GROUP);

            task.dependsOn(generateKibuDevConfig);
            task.dependsOn(fixIdeaRunClasspath);
        });

        // configure kibu dev config path
        generateKibuDevConfig.configure(task -> {
            for (SourceSet sourceSet : sourceSets) {
                Task compileJava = tasks.findByName(sourceSet.getCompileJavaTaskName());
                Task processResources = tasks.findByName(sourceSet.getProcessResourcesTaskName());

                if (compileJava != null) {
                    task.mustRunAfter(compileJava);
                }

                if (processResources != null) {
                    task.mustRunAfter(processResources);
                }
            }

            Path configPath = target.getProjectDir().toPath().resolve(".gradle").resolve("kibu-dev")
                    .resolve("config.json");

            task.getOutputFile().set(configPath.toFile());

            ConfigurableFileCollection pluginPaths = task.getPluginPaths();
            SourceSetOutput mainOutput = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get().getOutput();
            pluginPaths.from(mainOutput.getFiles());
        });

        ideaSyncTask.configure(task -> {
            task.finalizedBy(configurePluginLaunch);
            configurePluginLaunch.get().mustRunAfter(task);
        });

        fixIdeaRunClasspath.configure(task -> {
            task.dependsOn(ideaSyncTask);

            final RegularFileProperty kibuDevConfig = generateKibuDevConfig.get().getOutputFile();
            task.systemProperty("kibu-dev.config", kibuDevConfig.get().getAsFile().getAbsolutePath());
        });

        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure(ext::createPluginConfigurations);

        // configure run game tasks to contain paths to plugin sources
        tasks.withType(RunGameTask.class).forEach(task -> {
            task.dependsOn(generateKibuDevConfig);

            final File kibuDevConfig = generateKibuDevConfig.get().getOutputFile().get().getAsFile();
            task.systemProperty("kibu-dev.config", kibuDevConfig.getAbsolutePath());
        });

        GradleUtils.afterSuccessfulEvaluation(target, () -> {
            SourceSet main = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get();

            removePluginOutputFromGradleMainClasspath(sourceSets, main);
            removePluginOutputFromRunConfigs(sourceSets, ext, fixIdeaRunClasspath);

            removePluginDependenciesFromGradleMainClasspath(ext, main);
            removePluginDependenciesFromRunConfigs(ext, fixIdeaRunClasspath);

            configureKibuDevConfigTask(ext, generateKibuDevConfig);

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

            // set remap attribute to produced jars, so that they will be remapped in consumer dev environments
            tasks.withType(Jar.class).configureEach(task ->
                    task.manifest(manifest -> manifest.attributes(Map.of("Fabric-Loom-Remap", "true"))));
        });
    }

    private static void configureKibuDevConfigTask(KibuGradleExtension ext, TaskProvider<KibuDevConfigTask> generateKibuDevConfig) {
        // collect configured plugin paths and configure the kibu config task
        KibuDevConfigTask kibuDevConfigTask = generateKibuDevConfig.get();
        ConfigurableFileCollection pluginPaths = kibuDevConfigTask.getPluginPaths();

        ext.getPluginPaths().getFiles().stream()
                .map(File::getAbsolutePath)
                .forEach(pluginPaths::from);

        kibuDevConfigTask.getPluginDependencies().from(ext.getPluginDependencies());
    }

    private static void removePluginOutputFromRunConfigs(SourceSetContainer sourceSets, KibuGradleExtension ext, TaskProvider<FixIdeaRunConfigsTask> fixIdeaRunClasspath) {
        // exclude sourceSet outputs in the classpath of the run config
        Set<File> files = new HashSet<>();

        for (SourceSet sourceSet : sourceSets) {
            if (SourceSet.TEST_SOURCE_SET_NAME.equals(sourceSet.getName())) continue;

            files.addAll(sourceSet.getOutput().getFiles());
        }

        fixIdeaRunClasspath.configure(task -> task.getClasspathExcludes().from(files));

        // configure plugin file paths of the target project
        ext.getPluginPaths().from(files);
    }

    private static void removePluginOutputFromGradleMainClasspath(SourceSetContainer sourceSets, SourceSet main) {
        // remove all sourceSet outputs from the main runtime classpath
        // the outputs will be loaded by the plugin loader instead
        for (SourceSet sourceSet : sourceSets) {
            FileCollection mainRuntime = main.getRuntimeClasspath();
            SourceSetOutput output = sourceSet.getOutput();

            main.setRuntimeClasspath(mainRuntime.minus(output));
        }
    }

    private static void removePluginDependenciesFromRunConfigs(KibuGradleExtension ext, TaskProvider<FixIdeaRunConfigsTask> fixIdeaRunClasspath) {
        var pluginDependencies = ext.getPluginDependencies();

        fixIdeaRunClasspath.configure(task -> task.getClasspathExcludes().from(pluginDependencies));
    }

    private static void removePluginDependenciesFromGradleMainClasspath(KibuGradleExtension ext, SourceSet main) {
        var pluginDependencies = ext.getPluginDependencies();

        main.setRuntimeClasspath(main.getRuntimeClasspath().minus(pluginDependencies));
    }
}
