package org.janelia.workstation.mesh_loader;

import java.util.ArrayList;
import java.util.List;

/**
 * This essentially tracks which vertices are in a given triangle.  It is necessary to keep track of triangles
 * so that the vertices, themselves could have including-triangle-averaged normal vectors calculated for them.
 *
 * Created by fosterl on 4/3/14.
 */
public class Triangle {
    private final List<VertexInfoBean> vertices = new ArrayList<>();
    private AxialNormalDirection normalVector = AxialNormalDirection.NOT_APPLICABLE;
    private double[] customNormal; // Only needed if the axial normal is N/A.
    private boolean normalCombinationParticant = true;
    
    public void addVertex( VertexInfoBean bean ) {
        vertices.add( bean );
        // Need to ensure all is consistent.
        bean.addIncludingTriangle( this );
    }

    public AxialNormalDirection getNormalVector() {
        return normalVector;
    }
    
    /**
     * Call this only if the
     * @see #getNormalVector() returns 'not applicable' enum value.
     * @return array of double representing normal unit vector.
     */
    public double[] getCustomNormal() {
        if (customNormal == null) {            
            customNormal = NormalCompositor.computeNormal( vertices );
        }
        return customNormal;
    }

    /**
     * Calling this is optional.  Some triangles may not adhere to an
     * axial normal direction (aligned with x, y or z).
     * 
     * @param normalVector one of the axial directions, with unit vector normal.
     */
    public void setNormalVector(AxialNormalDirection normalVector) {
        this.normalVector = normalVector;
    }
    public List<VertexInfoBean> getVertices() { return vertices; }

    /**
     * Participants contribute their surface normal values, to affect
     * the normals of all vertices which are shared by that participant.
     * In other words, if this triangle has a given vertex, the surface
     * normal of this triangle will cause that vertex's normal to shift.
     * 
     * @return the normalCombinationParticant
     */
    public boolean isNormalCombinationParticant() {
        return normalCombinationParticant;
    }

    /**
     * @param normalCombinationParticant the normalCombinationParticant to set
     */
    public void setNormalCombinationParticant(boolean normalCombinationParticant) {
        this.normalCombinationParticant = normalCombinationParticant;
    }
        
}
