package consumer;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.blackbuild.annodocimal.generator.ProjectionPolicy;
import com.blackbuild.annodocimal.generator.SourceProjector;

public final class ArtifactConsumer {
    private ArtifactConsumer() {
    }

    public static void main(String[] args) {
        if (!AnnoDoc.class.isAnnotation())
            throw new AssertionError("The annotations artifact did not expose AnnoDoc");
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation());
        if (projector == null)
            throw new AssertionError("The shaded generator was not constructible");
    }
}
