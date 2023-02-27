package work.lclpnet.kibupd;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskContainer;
import work.lclpnet.kibupd.task.DeployLocalTask;
import work.lclpnet.kibupd.task.DeployTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KibuGradlePlugin implements Plugin<Project> {

    public static final String TASK_GROUP = "kibu";

    private Properties properties;

    @Override
    public void apply(Project target) {
        properties = loadProps(target.file("mplugindev.properties").toPath(), target.getLogger());

        TaskContainer tasks = target.getTasks();

        tasks.register("deploy", DeployTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.dependsOn(tasks.named("build"));
        });

        tasks.register("deployLocal", DeployLocalTask.class, task -> {
            task.setGroup(TASK_GROUP);
            task.dependsOn(tasks.named("build"));
        });
    }

    private Properties loadProps(Path path, Logger logger) {
        Properties properties = new Properties();

        if (!Files.exists(path)) {
            logger.debug("Properties file does not exist, fallback to empty properties");
            return properties;
        }

        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            logger.error("Failed to load properties, fallback to empty properties", e);
        }

        return properties;
    }

    public static Properties getProperties(Project project) {
        KibuGradlePlugin plugin = project.getPlugins().getPlugin(KibuGradlePlugin.class);

        return plugin.properties != null ? plugin.properties : new Properties();
    }
}