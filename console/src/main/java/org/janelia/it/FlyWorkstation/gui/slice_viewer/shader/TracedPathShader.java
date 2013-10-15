package org.janelia.it.FlyWorkstation.gui.slice_viewer.shader;


public class TracedPathShader extends PassThroughTextureShader {
    @Override
    public String getVertexShader() {
        return "TracedPathVrtx.glsl";
    }

    @Override
    public String getFragmentShader() {
        return "TracedPathFrag.glsl";
    }
}
