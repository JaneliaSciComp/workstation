package org.janelia.it.workstation.gui.large_volume_viewer.shader;


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
