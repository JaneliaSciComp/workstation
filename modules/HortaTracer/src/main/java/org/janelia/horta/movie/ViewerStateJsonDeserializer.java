package org.janelia.horta.movie;

import com.google.gson.JsonObject;

/**
 *
 * @author brunsc
 */
public interface ViewerStateJsonDeserializer {
    ViewerState deserializeJson(JsonObject json);
}
