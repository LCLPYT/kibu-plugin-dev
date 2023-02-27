package work.lclpnet.kibupd.deploy;

import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class StreamFileTransfer implements FileTransfer {

    private final Path relativeDir;
    private final Logger logger;

    public StreamFileTransfer(Path relativeDir, Logger logger) {
        this.relativeDir = relativeDir;
        this.logger = logger;
    }

    @Override
    public void transfer(Path from, Path to) {
        logger.info("Deploying {} to {}...", relativeDir.relativize(from),
                to.startsWith(relativeDir) ? relativeDir.relativize(to) : to.toAbsolutePath());

        // transfer file contents to enable plugin hot-swapping using reloading
        try (InputStream input = Files.newInputStream(from.toFile().toPath());
             OutputStream output = Files.newOutputStream(to.toFile().toPath())) {

            byte[] buf = new byte[1024];
            int read;

            while ((read = input.read(buf)) > 0) {
                output.write(buf, 0, read);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
