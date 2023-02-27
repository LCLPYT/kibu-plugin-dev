package work.lclpnet.kibupd.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import work.lclpnet.kibupd.KibuGradlePlugin;
import work.lclpnet.kibupd.deploy.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class DeployTask extends DefaultTask {

    @TaskAction
    public void execute() throws IOException {
        final Project project = this.getProject();
        final Logger logger = getLogger();
        final Path projectPath = project.getProjectDir().toPath();

        final boolean remapped = shouldDeployRemapped();
        final Path target = getDeployPath();

        if (!Files.exists(target)) Files.createDirectories(target);

        ArtifactCollector collector = () -> getTaskOutputs(remapped ? "remapJar" : "jar").stream();
        TargetMapper targetMapper = p -> target.resolve(p.getFileName());
        FileTransfer transfer = new StreamFileTransfer(projectPath, logger);

        if (!remapped) logger.quiet("Deploying mapped plugin...");

        new Deployer(collector, targetMapper, transfer).deploy().join();
    }

    protected boolean shouldDeployRemapped() {
        final Properties props = KibuGradlePlugin.getProperties(this.getProject());

        return Boolean.parseBoolean(props.getProperty("deployRemapped", "true"));
    }

    @Internal
    protected Path getDeployPath() {
        final Project project = this.getProject();
        final Properties props = KibuGradlePlugin.getProperties(project);

        String deployPath = Optional.ofNullable(props.getProperty("deployPath"))
                .orElseThrow(() -> new IllegalStateException("deployPath is not configured in mplugindev.properties"));

        Path relDeployPath = Paths.get(deployPath);

        return project.getProjectDir().toPath().resolve(relDeployPath);
    }

    private Set<Path> getTaskOutputs(String taskName) {
        TaskProvider<Task> refTask = this.getProject().getTasks().named(taskName);

        if (!refTask.isPresent())
            throw new IllegalStateException(String.format("Task '%s' is not present", taskName));

        return refTask.get().getOutputs().getFiles().getFiles().stream()
                .map(File::toPath)
                .collect(Collectors.toSet());
    }
}
