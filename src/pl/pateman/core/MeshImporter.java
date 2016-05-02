package pl.pateman.core;

import pl.pateman.core.entity.MeshEntity;

import java.io.IOException;

/**
 * Created by pateman.
 */
public interface MeshImporter {
    MeshEntity load(final String meshFileResource) throws IOException;
}
