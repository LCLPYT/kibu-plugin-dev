package work.lclpnet.kibupd.component;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.*;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.internal.component.external.descriptor.MavenScope;

public class KibuSoftwareComponentHelper {

    private final Project project;
    private final SoftwareComponentFactory componentFactory;
    private final Configuration runtimeElements, apiElements, sourcesElements;
    private final AdhocComponentWithVariants component;

    public KibuSoftwareComponentHelper(Project project, SoftwareComponentFactory componentFactory, String componentName) {
        this.project = project;
        this.componentFactory = componentFactory;

        this.runtimeElements = createRuntimeConfig(componentName);
        this.apiElements = createApiConfig(componentName);
        this.sourcesElements = createSourcesElements(componentName);
        this.component = createComponent(componentName, runtimeElements, apiElements);
    }

    private Configuration createSourcesElements(String componentName) {
        ConfigurationContainer configurations = project.getConfigurations();

        return configurations.create(componentName + "SourcesElements", config -> {
            // configure gradle attributes
            config.setVisible(false);

            ObjectFactory objects = project.getObjects();
            AttributeContainer attributes = config.getAttributes();

            attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.DOCUMENTATION));
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
            attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.class, DocsType.SOURCES));
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
        });
    }

    private Configuration createRuntimeConfig(String componentName) {
        ConfigurationContainer configurations = project.getConfigurations();

        return configurations.create(componentName + "RuntimeElements", config -> {
            // configure gradle attributes
            configureLibrary(config);

            ObjectFactory objects = project.getObjects();
            AttributeContainer attributes = config.getAttributes();

            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));

            // extend from runtime configurations, so that the artifact dependencies are known
            configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).configure(config::extendsFrom);
            configurations.named(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME).configure(config::extendsFrom);
            configurations.named("modImplementation").configure(config::extendsFrom);
            configurations.named("modRuntimeOnly").configure(config::extendsFrom);
        });
    }

    private Configuration createApiConfig(String componentName) {
        ConfigurationContainer configurations = project.getConfigurations();

        return configurations.create(componentName + "ApiElements", config -> {
            // configure gradle attributes
            configureLibrary(config);

            ObjectFactory objects = project.getObjects();
            AttributeContainer attributes = config.getAttributes();

            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));

            // extend from runtime configurations, so that the artifact dependencies are known
            configurations.named(JavaPlugin.API_CONFIGURATION_NAME).configure(config::extendsFrom);
            configurations.named("modApi").configure(config::extendsFrom);
            configurations.named("modCompileOnlyApi").configure(config::extendsFrom);
        });
    }

    private void configureLibrary(Configuration config) {
        config.setVisible(false);

        ObjectFactory objects = project.getObjects();
        AttributeContainer attributes = config.getAttributes();

        // configure gradle attributes
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));

        int javaVersion = Integer.parseInt(JavaVersion.current().getMajorVersion());
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaVersion);
    }

    private AdhocComponentWithVariants createComponent(String name, Configuration runtimeElements, Configuration apiElements) {
        AdhocComponentWithVariants component = componentFactory.adhoc(name);

        // add runtimeElements (maven runtime scope)
        component.addVariantsFromConfiguration(runtimeElements, details ->
                details.mapToMavenScope(MavenScope.Runtime.getLowerName()));

        // add apiElements (maven compile scope)
        component.addVariantsFromConfiguration(apiElements, details ->
                details.mapToMavenScope(MavenScope.Compile.getLowerName()));

        // add sources elements, if configured
        component.addVariantsFromConfiguration(sourcesElements, details -> {
            details.mapToMavenScope(MavenScope.Runtime.getLowerName());
            details.mapToOptional();
        });

        // add the component to the project
        project.getComponents().add(component);

        return component;
    }

    public void addArtifact(Object task) {
        runtimeElements.getOutgoing().artifact(task);
        apiElements.getOutgoing().artifact(task);
    }

    public void addSourceArtifact(Object task) {
        sourcesElements.getOutgoing().artifact(task);
    }

    public AdhocComponentWithVariants getComponent() {
        return component;
    }
}
