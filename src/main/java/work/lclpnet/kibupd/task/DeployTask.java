package work.lclpnet.kibupd.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import work.lclpnet.kibupd.KibuGradlePlugin;
import work.lclpnet.kibupd.KibuShadowGradlePlugin;
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

    public DeployTask() {
        super();
        dependsOn(getDeployArtifactTask());
    }

    @TaskAction
    public void execute() throws IOException {
        final Project project = this.getProject();
        final Logger logger = getLogger();
        final Path projectPath = project.getProjectDir().toPath();

        final boolean remapped = shouldDeployRemapped();
        final Path target = getDeployPath();

        if (!Files.exists(target)) Files.createDirectories(target);

        Task deployArtifactTask = getDeployArtifactTask();
        ArtifactCollector collector = () -> getTaskOutputs(deployArtifactTask).stream();
        TargetMapper targetMapper = p -> target.resolve(p.getFileName());
        FileTransfer transfer = new StreamFileTransfer(projectPath, logger);

        if (!remapped) logger.quiet("Deploying mapped plugin...");

        new Deployer(collector, targetMapper, transfer).deploy().join();
    }

    protected boolean shouldDeployRemapped() {
        final Properties props = KibuGradlePlugin.getProperties(this.getProject());

        return Boolean.parseBoolean(props.getProperty("deployRemapped", "true"));
    }

    protected boolean shouldDeployBundled() {
        final Project project = getProject();
        final Properties props = KibuGradlePlugin.getProperties(project);

        if (!Boolean.parseBoolean(props.getProperty("deployBundled", "true"))) return false;

        if (!project.getPlugins().hasPlugin(KibuGradlePlugin.SHADOW_PLUGIN_ID)) return false;

        Configuration bundle = project.getConfigurations().findByName(KibuShadowGradlePlugin.BUNDLE_CONFIGURATION_NAME);
        if (bundle == null) return false;

        return !bundle.isEmpty();
    }

    protected String getDeployArtifactTaskName() {
        boolean remap = shouldDeployRemapped();

        if (shouldDeployBundled()) {
            return remap ? "remapShadowJar" : "shadowJar";
        }

        return remap ? "remapJar" : "jar";
    }

    protected Task getDeployArtifactTask() {
        String taskName = getDeployArtifactTaskName();
        TaskProvider<Task> refTask = this.getProject().getTasks().named(taskName);

        if (!refTask.isPresent())
            throw new IllegalStateException(String.format("Task '%s' is not present", taskName));

        return refTask.get();
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

    private Set<Path> getTaskOutputs(Task task) {
        return task.getOutputs().getFiles().getFiles().stream()
                .map(File::toPath)
                .collect(Collectors.toSet());
    }
}
