package work.lclpnet.kibupd.util;

import net.fabricmc.loom.util.Constants;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSet;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inspired by <a href="https://github.com/FabricMC/fabric-loom/blob/cfe72b933a35778fbac05628f7a5111b9219c6e0/src/main/java/net/fabricmc/loom/configuration/RemapConfigurations.java">Fabric Loom</a>
 */
public class KibuPluginConfigurations {

    private static final List<Option> OPTIONS = List.of(
            new Option(mainOnly(SourceSet::getApiConfigurationName)),
            new Option(SourceSet::getImplementationConfigurationName),
            new Option(SourceSet::getCompileOnlyConfigurationName),
            new Option(mainOnly(SourceSet::getCompileOnlyApiConfigurationName)),
            new Option(SourceSet::getRuntimeOnlyConfigurationName),
            new Option(mainOnly(sourceSet -> Constants.Configurations.LOCAL_RUNTIME))
    );

    private static Function<SourceSet, String> mainOnly(Function<SourceSet, String> fun) {
        return sourceSet -> SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName()) ? fun.apply(sourceSet) : null;
    }

    private static Map<String, String> getNames(SourceSet sourceSet) {
        return OPTIONS.stream().collect(Collectors.toMap(
                o -> o.name(sourceSet),
                o -> o.parentName(sourceSet)
        ));
    }

    public static Set<Configuration> apply(Project project, SourceSet sourceSet) {
        var configurations = project.getConfigurations();
        var names = getNames(sourceSet);

        Set<Configuration> pluginConfigs = new HashSet<>();

        for (var entry : names.entrySet()) {
            String pluginName = entry.getKey();
            String modName = entry.getValue();

            Configuration pluginConfig = configurations.create(pluginName);
            configurations.named(modName).configure(modConfig -> modConfig.extendsFrom(pluginConfig));

            pluginConfigs.add(pluginConfig);
        }

        return pluginConfigs;
    }

    public record Option(Function<SourceSet, String> name) {

        @Nonnull
        public String name(SourceSet sourceSet) {
            String suffix = name.apply(sourceSet);

            if (suffix == null) {
                throw new UnsupportedOperationException("Configuration not available for sourceset %s".formatted(sourceSet.getName()));
            }

            if (suffix.startsWith(sourceSet.getName())) {
                suffix = suffix.substring(sourceSet.getName().length());
            }

            final StringBuilder builder = new StringBuilder();
            builder.append("plugin");

            if (!SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
                builder.append(capitalise(sourceSet.getName()));
            }

            builder.append(capitalise(suffix));

            return builder.toString();
        }

        /**
         * Getter for the fabric parent configuration name.
         * @param sourceSet Target source set
         * @return The name of the fabric parent configuration.
         * @see <a href="https://github.com/FabricMC/fabric-loom/blob/cfe72b933a35778fbac05628f7a5111b9219c6e0/src/main/java/net/fabricmc/loom/configuration/RemapConfigurations.java#L226">Fabric Loom</a>
         */
        public String parentName(SourceSet sourceSet) {
            String targetName = name.apply(sourceSet);

            if (targetName == null) {
                throw new UnsupportedOperationException("Configuration option is not available for sourceset (%s)".formatted(sourceSet.getName()));
            }

            if (targetName.startsWith(sourceSet.getName())) {
                targetName = targetName.substring(sourceSet.getName().length());
            }

            final StringBuilder builder = new StringBuilder();
            builder.append("mod");

            if (!SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
                builder.append(capitalise(sourceSet.getName()));
            }

            builder.append(capitalise(targetName));
            return builder.toString();
        }
    }

    private static String capitalise(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
