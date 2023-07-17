package work.lclpnet.kibupd.ext;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import work.lclpnet.kibupd.util.KibuPluginConfigurations;
import work.lclpnet.kibupd.util.PluginDetector;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class KibuGradleExtensionImpl implements KibuGradleExtension {

    private final Project project;
    private final ConfigurableFileCollection pluginPaths;
    private final Property<String> appBundleName;
    private final Set<Configuration> pluginConfigurations = new HashSet<>();

    public KibuGradleExtensionImpl(Project project) {
        this.project = project;
        this.pluginPaths = project.getObjects().fileCollection();
        this.appBundleName = project.getObjects().property(String.class).convention(project.getName());
    }

    @Override
    public ConfigurableFileCollection getPluginPaths() {
        return pluginPaths;
    }

    @Override
    public Property<String> getAppBundleName() {
        return appBundleName;
    }

    @Override
    public void createPluginConfigurations(SourceSet sourceSet) {
        var configurations = KibuPluginConfigurations.apply(project, sourceSet);

        pluginConfigurations.addAll(configurations);
    }

    @Override
    public ConfigurableFileCollection getPluginDependencies() {
        var collection = project.getObjects().fileCollection();
        final PluginDetector pluginDetector = new PluginDetector(project.getLogger());

        for (Configuration configuration : pluginConfigurations) {
            Set<File> files = configuration.resolve();

            for (File file : files) {
                if (pluginDetector.isPlugin(file.toPath())) {
                    collection.from(file);
                }
            }
        }

        return collection;
    }
}
