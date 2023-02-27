package work.lclpnet.kibupd.deploy;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface ArtifactCollector {

    Stream<Path> collect();
}
