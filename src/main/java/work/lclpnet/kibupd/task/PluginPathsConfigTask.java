package work.lclpnet.kibupd.task;

import groovy.json.JsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
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

public abstract class PluginPathsConfigTask extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public PluginPathsConfigTask() {
        setGroup(KibuGradlePlugin.TASK_GROUP);
    }

    @TaskAction
    public void execute() {
        final SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSetOutput output = main.getOutput();

        Set<Set<String>> pluginPaths = new HashSet<>();
        pluginPaths.add(output.getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet()));
        // TODO add dependency plugins to pluginPaths as well

        Map<String, Object> map = new HashMap<>();
        map.put("paths", pluginPaths);

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
