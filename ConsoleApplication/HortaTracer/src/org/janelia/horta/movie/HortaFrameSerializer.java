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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Arrays;
import org.janelia.geometry3d.Quaternion;
import org.janelia.horta.NeuronTracerTopComponent.HortaViewerState;

/**
 *
 * @author brunsc
 */
class HortaFrameSerializer 
implements JsonSerializer<KeyFrame<HortaViewerState>>, JsonDeserializer<KeyFrame<HortaViewerState>>
{

    public HortaFrameSerializer() {
    }

    @Override
    public JsonElement serialize(KeyFrame<HortaViewerState> t, Type type, JsonSerializationContext jsc) {
        JsonObject result = new JsonObject();
        
        result.addProperty("followingInterval", t.getFollowingIntervalDuration());
        HortaViewerState state = t.getViewerState();
        result.addProperty("zoom", state.getCameraSceneUnitsPerViewportHeight());
        float rot[] = state.getCameraRotation().asArray();
        JsonArray quat = new JsonArray();
        for (int i = 0; i < 4; ++i)
            quat.add(new JsonPrimitive(rot[i]));
        result.add("quaternionRotation", quat);
        JsonArray focus = new JsonArray();
        float f[] = state.getCameraFocus();
        for (int i = 0; i < 3; ++i)
            focus.add(new JsonPrimitive(f[i]));
        result.add("focusXyz", focus);
        
        return result;
    }

    @Override
    public KeyFrame<HortaViewerState> deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException 
    {
        JsonObject frame = je.getAsJsonObject();
        float interval = frame.getAsJsonPrimitive("followingInterval").getAsFloat();
        float zoom = frame.getAsJsonPrimitive("zoom").getAsFloat();
        JsonArray quat = frame.getAsJsonArray("quaternionRotation");
        JsonArray focus = frame.getAsJsonArray("focusXyz");
        float[] f = new float[] {
            focus.get(0).getAsFloat(),
            focus.get(1).getAsFloat(),
            focus.get(2).getAsFloat()};
        Quaternion q = new Quaternion();
        q.set(
            quat.get(0).getAsFloat(),
            quat.get(1).getAsFloat(),
            quat.get(2).getAsFloat(),
            quat.get(3).getAsFloat());
        HortaViewerState state = new HortaViewerState(
                f[0], f[1], f[2],
                q,
                zoom);
        KeyFrame<HortaViewerState> result = new BasicKeyFrame<>(state, interval);
        return result;
    }
    
}
