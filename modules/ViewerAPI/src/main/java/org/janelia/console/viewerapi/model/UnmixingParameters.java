package org.janelia.console.viewerapi.model;

/**
 * wrapper around unmixing parameters so we can store as global lookup
 */
public class UnmixingParameters {
    public UnmixingParameters(float[] unmixins) {
        params = unmixins;
    }

    public float[] getParams() {
        return params;
    }

    public void setParams(float[] params) {
        this.params = params;
    }

    float[] params;

}
