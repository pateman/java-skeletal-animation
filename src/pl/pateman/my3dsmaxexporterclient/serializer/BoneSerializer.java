package pl.pateman.my3dsmaxexporterclient.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import pl.pateman.skeletal.mesh.Bone;

import java.lang.reflect.Type;

/**
 * Created by pateman.
 */
public class BoneSerializer implements JsonSerializer<Bone> {
    @Override
    public JsonElement serialize(final Bone src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject jsonElement = new JsonObject();

        jsonElement.addProperty("name", src.getName());
        jsonElement.addProperty("index", src.getIndex());

        final int parentIndex = src.getParent() == null ? -1 : src.getParent().getIndex();
        jsonElement.addProperty("parent", parentIndex);

        jsonElement.add("bindTranslation", context.serialize(src.getBindPosition()));
        jsonElement.add("bindRotation", context.serialize(src.getBindRotation()));
        jsonElement.add("bindScale", context.serialize(src.getBindScale()));

        jsonElement.add("weights", context.serialize(src.getVertexWeights()));

        return jsonElement;
    }
}
