package work.lclpnet.kibupd.util;

import org.slf4j.Logger;
import work.lclpnet.plugin.manifest.JsonManifestLoader;
import work.lclpnet.plugin.manifest.PluginManifestLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class PluginDetector {

    private final Logger logger;
    private final PluginManifestLoader manifestLoader;

    public PluginDetector(Logger logger) {
        this(logger, new JsonManifestLoader());
    }

    public PluginDetector(Logger logger, PluginManifestLoader manifestLoader) {
        this.logger = logger;
        this.manifestLoader = manifestLoader;
    }

    public boolean isPlugin(Path path) {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(path))) {
            return hasPluginEntries(in);
        } catch (IOException e) {
            logger.debug("Failed to open jar file at {}", path, e);
            return false;
        }
    }

    public boolean isNonMappedPlugin(Path path) {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(path))) {

            Manifest manifest = in.getManifest();

            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                String remapValue = attributes.getValue("Fabric-Loom-Remap");

                if (Boolean.parseBoolean(remapValue)) {
                    // is mapped
                    return false;
                }
            }

            return hasPluginEntries(in);
        } catch (IOException e) {
            logger.debug("Failed to open jar file at {}", path, e);
            return false;
        }
    }

    private boolean hasPluginEntries(JarInputStream in) throws IOException {
        ZipEntry entry;

        while ((entry = in.getNextEntry()) != null) {
            if ("plugin.json".equals(entry.getName())) {
                return isValidPluginManifest(in);
            }
        }

        return false;
    }

    private boolean isValidPluginManifest(InputStream in) {
        try {
            manifestLoader.load(in);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
