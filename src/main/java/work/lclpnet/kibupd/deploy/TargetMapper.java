package work.lclpnet.kibupd.deploy;

import java.nio.file.Path;

public interface TargetMapper {

    Path map(Path path);
}
