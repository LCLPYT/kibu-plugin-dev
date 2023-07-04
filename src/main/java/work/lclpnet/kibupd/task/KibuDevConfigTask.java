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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class KibuDevConfigTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getPluginPaths();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public KibuDevConfigTask() {
        setGroup(KibuGradlePlugin.TASK_GROUP);
    }

    @TaskAction
    public void execute() {
        Set<String> paths = getPluginPaths().getFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        Map<String, Object> map = new HashMap<>();
        map.put("plugin_paths", paths);

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
