package pl.pateman.my3dsmaxexporterclient.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import pl.pateman.skeletal.mesh.AnimationTrack;

import java.lang.reflect.Type;

/**
 * Created by pateman.
 */
public class AnimationTrackSerializer implements JsonSerializer<AnimationTrack> {
    @Override
    public JsonElement serialize(AnimationTrack src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("bone", src.getBone().getIndex());
        jsonObject.add("keyframes", context.serialize(src.getKeyframes()));

        return jsonObject;
    }
}
