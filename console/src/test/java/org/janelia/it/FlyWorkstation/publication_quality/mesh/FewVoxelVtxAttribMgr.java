package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This voxel manager will make vertices from mocked voxels, rather than having to marshal them from an input file.
 * Great for testing.  Please do not delete!
 *
 * Created by fosterl on 4/18/14.
 */
public class FewVoxelVtxAttribMgr implements VertexAttributeManagerI {
    private List<TriangleSource> vertexFactories;

    private Map<Long,RenderBuffersBean> renderIdToBuffers;
    private Long id;

    public FewVoxelVtxAttribMgr( Long id ) {
        this.id = id;
    }

    @Override
    public List<TriangleSource> execute() throws Exception {
        vertexFactories = new ArrayList<TriangleSource>();
        renderIdToBuffers = new HashMap<Long,RenderBuffersBean>();

        VertexFactory factory = new VertexFactory();

        VoxelInfoBean voxelInfoBean = new VoxelInfoBean();
        VoxelInfoKey key = new VoxelInfoKey(10,10,10);
        voxelInfoBean.setKey(key);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );

        factory.addEnclosure(voxelInfoBean);

        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(10,11,10);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        voxelInfoBean.setKey(key);

        factory.addEnclosure(voxelInfoBean);

        // Now have a full complement of triangles and vertices.  For this renderable, can traverse the
        // vertices, making a "composite normal" based on the normals of all entangling triangles.
        NormalCompositor normalCompositor = new NormalCompositor();
        normalCompositor.createGouraudNormals(factory);

        vertexFactories.add( factory );
        BufferPackager packager = new BufferPackager();
        RenderBuffersBean rbb = new RenderBuffersBean();
        rbb.setAttributesBuffer( packager.getVertexAttributes(factory) );
        rbb.setIndexBuffer( packager.getIndices(factory) );

        renderIdToBuffers.put( id, rbb );

        return vertexFactories;
    }

    @Override
    public Map<Long,RenderBuffersBean> getRenderIdToBuffers() {
        return renderIdToBuffers;
    }

    @Override
    public void close() {
        renderIdToBuffers.clear();
        vertexFactories.clear();
    }

}
