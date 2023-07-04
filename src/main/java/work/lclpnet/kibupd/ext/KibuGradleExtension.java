package work.lclpnet.kibupd.ext;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

public interface KibuGradleExtension {

    ConfigurableFileCollection getPluginPaths();

    Property<String> getAppBundleName();
}
