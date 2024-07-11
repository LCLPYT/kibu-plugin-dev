package work.lclpnet.kibupd;

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin;
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import net.fabricmc.loom.task.RemapJarTask;
import net.fabricmc.loom.task.RemapTaskConfiguration;
import net.fabricmc.loom.util.Constants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import work.lclpnet.kibupd.ext.KibuGradleExtension;
import work.lclpnet.kibupd.util.ProjectUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;

public class KibuShadowGradlePlugin implements Plugin<Project> {

    public static final String BUNDLE_CONFIGURATION_NAME = "bundle";
    public static final String PROVIDE_CONFIGURATION_NAME = "provide";
    public static final String REMAP_SHADOW_JAR_TASK_NAME = "remapShadowJar";

    @Override
    public void apply(Project target) {
        final KibuGradleExtension ext = target.getExtensions().getByType(KibuGradleExtension.class);

        Configuration bundle = target.getConfigurations().create(BUNDLE_CONFIGURATION_NAME, config -> config.setTransitive(false));
        Configuration provide = target.getConfigurations().create(PROVIDE_CONFIGURATION_NAME, config -> config.setTransitive(false));

        TaskContainer tasks = target.getTasks();

        // configure shadowJar task
        File devlibsDir = new File(target.getBuildDir(), "devlibs");

        tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, ShadowJar.class).configure(task -> {
            task.setConfigurations(Collections.singletonList(bundle));
            task.getArchiveClassifier().set("dev-bundle");
            task.getDestinationDirectory().set(devlibsDir);

            task.from(provide);
        });

        // register remapShadowJar task
        RemapJarTask remapShadowJarTask = tasks.create(REMAP_SHADOW_JAR_TASK_NAME, RemapJarTask.class, task -> {
            final AbstractArchiveTask shadowJarTask = tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, AbstractArchiveTask.class).get();

            task.dependsOn(shadowJarTask);
            task.setDescription("Remaps the built shadow project jar to intermediary mappings.");
            task.setGroup(Constants.TaskGroup.FABRIC);
            task.getArchiveClassifier().convention("bundle");

            task.getInputFile().convention(shadowJarTask.getArchiveFile());
        });

        ext.addArtifact(remapShadowJarTask);

        ProjectUtils.onEvaluationSuccess(target, () -> {
            configureShadowJar(ext, tasks, remapShadowJarTask, bundle, provide, devlibsDir);
            configureSourceArtifacts(tasks, ext);
        });
    }

    private void configureShadowJar(KibuGradleExtension ext, TaskContainer tasks, RemapJarTask remapShadowJarTask,
                                    Configuration bundle, Configuration provide, File devlibsDir) {

        String escaped = getNameAsPackage(ext.getAppBundleName().get());

        tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, ShadowJar.class).configure(task -> {
            task.setEnableRelocation(true);
            task.setRelocationPrefix(escaped);
        });

        if (bundle.isEmpty() && provide.isEmpty()) {
            remapShadowJarTask.setEnabled(false);

            tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, AbstractArchiveTask.class)
                    .configure(task -> task.setEnabled(false));

            return;
        }

        tasks.named(BasePlugin.ASSEMBLE_TASK_NAME)
                .configure(task -> task.dependsOn(remapShadowJarTask));

        // adjust archive classifiers
        tasks.named(RemapTaskConfiguration.REMAP_JAR_TASK_NAME, AbstractArchiveTask.class)
                .configure(task -> {
                    task.getArchiveClassifier().convention("slim");
                    task.getDestinationDirectory().set(devlibsDir);
                });

        tasks.named(REMAP_SHADOW_JAR_TASK_NAME, AbstractArchiveTask.class)
                .configure(task -> task.getArchiveClassifier().convention(""));
    }

    private void configureSourceArtifacts(TaskContainer tasks, KibuGradleExtension ext) {
        Task remapSourcesJar = tasks.named("remapSourcesJar").getOrNull();

        if (remapSourcesJar != null) {
            ext.addSourceArtifact(remapSourcesJar);
        }
    }

    private static String getNameAsPackage(@Nullable String name) {
        name = (name != null ? name : "").replaceAll("[^a-z_.0-9]", "");

        if (name.isEmpty()) return "app";

        // not perfect, as keywords are not considered

        return name;
    }
}
