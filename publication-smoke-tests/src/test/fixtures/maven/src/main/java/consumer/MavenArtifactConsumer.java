package consumer;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.blackbuild.annodocimal.generator.ProjectionPolicy;
import com.blackbuild.annodocimal.generator.SourceProjector;

public final class MavenArtifactConsumer {
    private MavenArtifactConsumer() {
    }

    public static void main(String[] args) {
        if (!AnnoDoc.class.isAnnotation())
            throw new AssertionError("The Maven annotations dependency did not expose AnnoDoc");
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation());
        if (projector == null)
            throw new AssertionError("The Maven generator dependency was not constructible");
    }
}
