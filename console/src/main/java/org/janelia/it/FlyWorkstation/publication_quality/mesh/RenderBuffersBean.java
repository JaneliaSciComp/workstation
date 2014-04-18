package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by fosterl on 4/18/14.
 */
public class RenderBuffersBean {
    private IntBuffer indexBuffer;
    private FloatBuffer attributesBuffer;

    public IntBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public void setIndexBuffer(IntBuffer indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public FloatBuffer getAttributesBuffer() {
        return attributesBuffer;
    }

    public void setAttributesBuffer(FloatBuffer attributesBuffer) {
        this.attributesBuffer = attributesBuffer;
    }
}

