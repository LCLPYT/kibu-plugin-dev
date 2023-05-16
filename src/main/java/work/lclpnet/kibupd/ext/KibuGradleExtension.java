package work.lclpnet.kibupd.ext;

import org.gradle.api.file.ConfigurableFileCollection;

public interface KibuGradleExtension {

    ConfigurableFileCollection getPluginPaths();
}
