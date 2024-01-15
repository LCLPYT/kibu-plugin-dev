package work.lclpnet.kibupd.component;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.component.external.descriptor.MavenScope;

public class KibuSoftwareComponentHelper {

    private final Project project;
    private final SoftwareComponentFactory componentFactory;
    private final String componentName, configurationName;
    private final Configuration configuration;
    private final AdhocComponentWithVariants component;

    public KibuSoftwareComponentHelper(Project project, SoftwareComponentFactory componentFactory, String componentName, String configurationName) {
        this.project = project;
        this.componentFactory = componentFactory;
        this.componentName = componentName;
        this.configurationName = configurationName;

        this.configuration = createConfig();
        this.component = createComponent(configuration);
    }

    private Configuration createConfig() {
        ConfigurationContainer configurations = project.getConfigurations();
        return configurations.create(configurationName, this::configure);
    }

    private void configure(Configuration config) {
        config.setVisible(false);

        ObjectFactory objects = project.getObjects();

        AttributeContainer attributes = config.getAttributes();
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
    }

    private AdhocComponentWithVariants createComponent(Configuration configuration) {
        AdhocComponentWithVariants component = componentFactory.adhoc(componentName);

        component.addVariantsFromConfiguration(configuration, details ->
                details.mapToMavenScope(MavenScope.Runtime.getLowerName()));

        return component;
    }

    public void addArtifact(Object task) {
        ConfigurationPublications outgoing = configuration.getOutgoing();
        outgoing.artifact(task);
    }

    public AdhocComponentWithVariants getComponent() {
        return component;
    }
}
