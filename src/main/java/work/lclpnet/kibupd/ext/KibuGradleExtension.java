package work.lclpnet.kibupd.ext;

import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

public interface KibuGradleExtension {

    ConfigurableFileCollection getPluginPaths();

    Property<String> getAppBundleName();

    void createPluginConfigurations(SourceSet sourceSet);

    /**
     * Collects all plugin dependencies.
     * @implNote This method will finalize plugin configurations such as "pluginImplementation".
     * @return A collection of plugin jars from all plugin configurations.
     */
    ConfigurableFileCollection getPluginDependencies();

    AdhocComponentWithVariants getSoftwareComponent();

    void addArtifact(Object artifact);

    void addSourceArtifact(Object artifact);
}
