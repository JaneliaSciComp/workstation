package org.janelia.horta.movie;

import com.google.gson.JsonObject;

/**
 *
 * @author brunsc
 */
public interface ViewerState {
    JsonObject serialize();
    String getStateType();
    int getStateVersion();
}
