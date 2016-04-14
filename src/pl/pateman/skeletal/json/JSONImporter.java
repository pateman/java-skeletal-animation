package pl.pateman.skeletal.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.MeshImporter;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.entity.MeshEntity;

import java.io.IOException;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class JSONImporter implements MeshImporter {
    @Override
    public MeshEntity load(String meshFileResource) throws IOException {
        final Gson gson = new Gson();

        final Map<String, JSONSceneData> importResult = gson.fromJson(Utils.readResource(meshFileResource),
                new TypeToken<Map<String, JSONSceneData>>(){}.getType());
        if (importResult.isEmpty()) {
            return null;
        }

        final JSONSceneData sceneData = importResult.entrySet().iterator().next().getValue();

        //  Prepare data from transformation.
        final TempVars vars = TempVars.get();

        final Vector3f translation = vars.vect3d1;
        final Quaternionf rotation = vars.quat1;
        final Vector3f scale = vars.vect3d2.set(1.0f, 1.0f, 1.0f);
        if (sceneData.getTranslation() != null) {
            translation.set(sceneData.getTranslation());
        }
        if (sceneData.getRotation() != null) {
            rotation.set(sceneData.getRotation());
        }
        if (sceneData.getScale() != null) {
            scale.set(sceneData.getScale());
        }

        //  Create the entity.
        final MeshEntity meshEntity = new MeshEntity();
        meshEntity.setMesh(sceneData.getMesh());
        meshEntity.setTransformation(rotation, translation, scale);
        meshEntity.forceTransformationUpdate();

        return meshEntity;
    }
}
