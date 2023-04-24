package work.lclpnet.kibupd.util;

import org.gradle.api.Project;

public class ProjectUtils {

    public static void onEvaluationSuccess(Project target, Runnable runnable) {
        target.afterEvaluate(project -> {
            if (project.getState().getFailure() == null) {
                runnable.run();
            }
        });
    }
}
