package work.lclpnet.kibupd.deploy;

import java.nio.file.Path;

public interface FileTransfer {

    void transfer(Path from, Path to);
}
