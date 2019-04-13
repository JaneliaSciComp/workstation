package org.janelia.horta.movie;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 *
 * @author brunsc
 */
class MovieTimelineSerializer 
implements JsonSerializer<Timeline>
, JsonDeserializer<Timeline>
{
    private final boolean doLoop;
    private final MovieSource movieSource;

    public MovieTimelineSerializer(boolean doLoop, MovieSource movieSource) {
        this.doLoop = doLoop;
        this.movieSource = movieSource;
    }

    @Override
    public JsonElement serialize(Timeline t, Type type, JsonSerializationContext jsc) 
    {
        JsonObject result = new JsonObject();
        JsonObject timeline = new JsonObject();
        result.add("movie", timeline);
        timeline.addProperty("doLoop", doLoop);
        timeline.addProperty("totalDuration", t.getTotalDuration(doLoop));
        JsonArray frames = new JsonArray();
        timeline.add("keyFrames", frames);
        for (KeyFrame keyFrame : t) {
            frames.add(keyFrame.serializeJson());
        }
        return result;
    }

    @Override
    public Timeline deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException 
    {
        JsonObject timeline = je.getAsJsonObject().get("movie").getAsJsonObject();
        // boolean doLoop = timeline.getAsJsonPrimitive("doLoop").getAsBoolean(); // not used
        // float totalDuration = timeline.getAsJsonPrimitive("totalDuration").getAsFloat(); // not used
        
        // TODO - serialize/deserialize interpolator
        Timeline result = new BasicMovieTimeline(movieSource.getDefaultInterpolator());
        
        ViewerStateJsonDeserializer stateDeserializer = movieSource.getStateDeserializer();
        JsonArray frames = timeline.getAsJsonArray("keyFrames");
        for (int i = 0; i < frames.size(); ++i) {
            JsonObject frame = frames.get(i).getAsJsonObject();
            ViewerState viewerState = stateDeserializer.deserializeJson(frame);
            float interval = frame.getAsJsonPrimitive("followingInterval").getAsFloat();
            KeyFrame keyFrame = new BasicKeyFrame(viewerState, interval);
            result.add(keyFrame);
        }
        
        return result;
    }
    
}
