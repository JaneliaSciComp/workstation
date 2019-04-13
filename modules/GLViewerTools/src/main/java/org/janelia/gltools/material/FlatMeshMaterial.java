
package org.janelia.gltools.material;

/**
 *
 * @author brunsc
 */
public class FlatMeshMaterial
extends BasicMaterial
{
    
    public FlatMeshMaterial(float red, float green, float blue) {
        shaderProgram = new FlatMeshShader(red, green, blue);
        setShadingStyle(Shading.NONE);
    }

    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    } 

    @Override
    public boolean usesNormals() {
        return false;
    }
}
