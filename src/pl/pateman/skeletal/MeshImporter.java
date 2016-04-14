package pl.pateman.skeletal;

import pl.pateman.skeletal.entity.MeshEntity;

import java.io.IOException;

/**
 * Created by pateman.
 */
public interface MeshImporter {
    MeshEntity load(final String meshFileResource) throws IOException;
}
