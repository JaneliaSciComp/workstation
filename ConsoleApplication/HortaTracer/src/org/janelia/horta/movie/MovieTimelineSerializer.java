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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.janelia.horta.NeuronTracerTopComponent.HortaViewerState;

/**
 *
 * @author brunsc
 */
class MovieTimelineSerializer 
implements JsonSerializer<Timeline<HortaViewerState>>, JsonDeserializer<Timeline<HortaViewerState>>
{
    private final boolean doLoop;

    public MovieTimelineSerializer(boolean doLoop) {
        this.doLoop = doLoop;
    }

    @Override
    public JsonElement serialize(Timeline<HortaViewerState> t, Type type, JsonSerializationContext jsc) 
    {
        JsonObject result = new JsonObject();
        JsonObject timeline = new JsonObject();
        result.add("movie", timeline);
        timeline.addProperty("doLoop", doLoop);
        timeline.addProperty("totalDuration", t.getTotalDuration(doLoop));
        JsonArray frames = new JsonArray();
        timeline.add("keyFrames", frames);
        for (KeyFrame<HortaViewerState> keyFrame : t) {
            frames.add(keyFrame.serializeJson());
        }
        return result;
    }

    @Override
    public Timeline<HortaViewerState> deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException 
    {
        JsonObject movie = je.getAsJsonObject();
        JsonObject timeline = movie.get("movie").getAsJsonObject();
        boolean doLoop = timeline.getAsJsonPrimitive("doLoop").getAsBoolean();
        float totalDuration = timeline.getAsJsonPrimitive("totalDuration").getAsFloat();
        
        // TODO - serialize/deserialize interpolator
        Interpolator<HortaViewerState> defaultInterpolator = new HortaViewerStateInterpolator();
        Timeline<HortaViewerState> result = new BasicMovieTimeline<>(defaultInterpolator);
        
        JsonDeserializer<KeyFrame<HortaViewerState>> hortaFrameSerializer = new HortaFrameDeserializer();
        Type frameType = new TypeToken<KeyFrame<HortaViewerState>>(){}.getType();
        
        JsonArray frames = timeline.getAsJsonArray("keyFrames");
        for (int i = 0; i < frames.size(); ++i) {
            JsonElement f = frames.get(i);
            KeyFrame<HortaViewerState> keyFrame = hortaFrameSerializer.deserialize(f, frameType, jdc);
            result.add(keyFrame);
        }
        
        return result;
    }
    
}
