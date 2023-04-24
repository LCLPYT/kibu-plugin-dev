package work.lclpnet.kibupd;

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin;
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation;
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import net.fabricmc.loom.task.RemapJarTask;
import net.fabricmc.loom.util.Constants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import work.lclpnet.kibupd.util.ProjectUtils;

import java.io.File;
import java.util.Collections;

public class KibuShadowGradlePlugin implements Plugin<Project> {

    public static final String BUNDLE_CONFIGURATION_NAME = "bundle";
    public static final String REMAP_SHADOW_JAR_TASK_NAME = "relocateDeps";

    @Override
    public void apply(Project target) {
        Configuration bundle = target.getConfigurations().create(BUNDLE_CONFIGURATION_NAME);
        TaskContainer tasks = target.getTasks();

        // add relocateDeps task
        ConfigureShadowRelocation relocateDepsTask = tasks.create(REMAP_SHADOW_JAR_TASK_NAME, ConfigureShadowRelocation.class, task -> {
            task.setGroup("shadow");

            ShadowJar shadowJar = tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, ShadowJar.class).get();
            task.setTarget(shadowJar);

            String escaped = getProjectNameAsPackage(target);
            task.setPrefix(escaped);
        });

        // configure shadowJar task
        tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, ShadowJar.class).configure(task -> {
            task.setConfigurations(Collections.singletonList(bundle));
            task.getArchiveClassifier().set("dev-bundle");
            task.getDestinationDirectory().set(new File(target.getBuildDir(), "devlibs"));
            task.dependsOn(relocateDepsTask);
        });

        // register remapShadowJar task
        RemapJarTask remapShadowJarTask = tasks.create("remapShadowJar", RemapJarTask.class, task -> {
            final AbstractArchiveTask shadowJarTask = tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME, AbstractArchiveTask.class).get();

            task.dependsOn(shadowJarTask);
            task.setDescription("Remaps the built shadow project jar to intermediary mappings.");
            task.setGroup(Constants.TaskGroup.FABRIC);
            task.getArchiveClassifier().convention("bundle");

            task.getInputFile().convention(shadowJarTask.getArchiveFile());
        });

        ProjectUtils.onEvaluationSuccess(target, () -> {
            if (bundle.isEmpty()) {
                remapShadowJarTask.setEnabled(false);
            } else {
                tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(remapShadowJarTask));
            }
        });
    }

    private static String getProjectNameAsPackage(Project target) {
        String name = target.getName();
        name = name.replaceAll("[^a-z_.0-9]", "");

        if (name.isEmpty()) return "app";

        // not perfect, as keywords are not considered

        return name;
    }
}
