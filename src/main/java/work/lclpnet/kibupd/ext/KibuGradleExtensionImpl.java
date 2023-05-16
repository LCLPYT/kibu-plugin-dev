package work.lclpnet.kibupd.ext;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;

public class KibuGradleExtensionImpl implements KibuGradleExtension {

    private final ConfigurableFileCollection pluginPaths;

    public KibuGradleExtensionImpl(Project project) {
        this.pluginPaths = project.getObjects().fileCollection();
    }

    @Override
    public ConfigurableFileCollection getPluginPaths() {
        return pluginPaths;
    }
}
