package work.lclpnet.kibupd.util;

import org.slf4j.Logger;
import work.lclpnet.plugin.manifest.JsonManifestLoader;
import work.lclpnet.plugin.manifest.PluginManifestLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
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

            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if ("plugin.json".equals(entry.getName())) {
                    return isValidManifest(in);
                }
            }

        } catch (IOException e) {
            logger.debug("Failed to open jar file at {}", path, e);
            return false;
        }

        return false;
    }

    private boolean isValidManifest(InputStream in) {
        try {
            manifestLoader.load(in);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
