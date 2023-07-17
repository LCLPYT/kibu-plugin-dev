package work.lclpnet.kibupd.task;

import groovy.json.JsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import work.lclpnet.kibupd.KibuGradlePlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class KibuDevConfigTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getPluginPaths();

    @InputFiles
    public abstract ConfigurableFileCollection getPluginDependencies();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public KibuDevConfigTask() {
        setGroup(KibuGradlePlugin.TASK_GROUP);
    }

    @TaskAction
    public void execute() {
        Set<String> projectPluginPaths = getPluginPaths().getFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        Set<Set<String>> pluginPaths = new HashSet<>();
        pluginPaths.add(projectPluginPaths);  // all paths of the plugin built by the consumer project

        // add each plugin dependency as an own entry
        Set<File> pluginDependencies = getPluginDependencies().getFiles();

        for (File pluginDependency : pluginDependencies) {
            pluginPaths.add(Set.of(pluginDependency.getAbsolutePath()));
        }

        Map<String, Object> map = new HashMap<>();
        map.put("plugin_paths", pluginPaths);

        final String json = new JsonBuilder(map).toPrettyString();
        final Path config = getOutputFile().get().getAsFile().toPath();

        try {
            createDirectoryIfNecessary(config.getParent());
            Files.writeString(config, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not create plugin paths config", e);
        }
    }

    private void createDirectoryIfNecessary(Path dir) throws IOException {
        if (Files.exists(dir)) return;

        Files.createDirectories(dir);
    }
}
