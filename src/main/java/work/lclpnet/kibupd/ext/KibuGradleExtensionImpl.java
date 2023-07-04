package work.lclpnet.kibupd.ext;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

public class KibuGradleExtensionImpl implements KibuGradleExtension {

    private final ConfigurableFileCollection pluginPaths;
    private final Property<String> appBundleName;

    public KibuGradleExtensionImpl(Project project) {
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
}
