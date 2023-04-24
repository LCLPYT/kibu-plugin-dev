package work.lclpnet.kibupd.task;

import java.nio.file.Path;

public class DeployLocalTask extends DeployTask {

    @Override
    protected boolean shouldDeployRemapped() {
        return false;
    }

    @Override
    protected boolean shouldDeployBundled() {
        return false;
    }

    @Override
    protected Path getDeployPath() {
        return this.getProject().getProjectDir().toPath().resolve("run/plugins");
    }
}
