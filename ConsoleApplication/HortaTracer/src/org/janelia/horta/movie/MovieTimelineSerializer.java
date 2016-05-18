/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
