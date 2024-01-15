package work.lclpnet.kibupd;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import work.lclpnet.kibupd.ext.KibuGradleExtension;
import work.lclpnet.kibupd.ext.KibuGradleExtensionImpl;
import work.lclpnet.kibupd.task.DeployTask;
import work.lclpnet.kibupd.util.ProjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KibuGradlePlugin implements Plugin<Project> {

    public static final String TASK_GROUP = "kibu";
    public static final String SHADOW_PLUGIN_ID = "com.github.johnrengelman.shadow";
    public static final String LOOM_PLUGIN_ID = "fabric-loom";
    public static final String KIBU_COMPONENT_NAME = "kibu";
    public static final String KIBU_ARTIFACTS_CONFIGURATION_NAME = "kibuArtifacts";

    private Properties properties;

    @Override
    public void apply(Project target) {
        target.getExtensions().create(KibuGradleExtension.class, "kibu", KibuGradleExtensionImpl.class, target);

        properties = loadProps(target.file("mplugindev.properties").toPath(), target.getLogger());

        TaskContainer tasks = target.getTasks();

        tasks.register("deploy", DeployTask.class);

        PluginContainer plugins = target.getPlugins();
        plugins.withId("fabric-loom", loomPlugin -> loomReady(target));

        ProjectUtils.onEvaluationSuccess(target, () -> {
            if (!plugins.hasPlugin(LOOM_PLUGIN_ID)) {
                target.getLogger().warn("The 'fabric-loom' gradle plugin is not applied");
            }
        });
    }

    private void loomReady(Project target) {
        PluginContainer plugins = target.getPlugins();
        plugins.apply(KibuLoomGradlePlugin.class);

        plugins.withId(SHADOW_PLUGIN_ID, shadowPlugin -> shadowReady(target));
    }

    private void shadowReady(Project target) {
        target.getPlugins().apply(KibuShadowGradlePlugin.class);
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