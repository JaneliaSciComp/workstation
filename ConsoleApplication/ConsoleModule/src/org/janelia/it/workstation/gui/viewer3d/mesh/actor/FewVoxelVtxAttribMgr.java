package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import org.janelia.it.jacs.shared.mesh_loader.*;
import org.janelia.it.jacs.shared.mesh_loader.wavefront_obj.OBJWriter;

import java.io.File;
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
@SuppressWarnings("unused")
public class FewVoxelVtxAttribMgr implements VertexAttributeSourceI, VertexExporterI {
    public enum Scenario { minimal, small, whole }
    private List<TriangleSource> triangleSources;
    private Scenario scenario = Scenario.small;

    private Map<Long,RenderBuffersBean> renderIdToBuffers;
    private Long id;

    public FewVoxelVtxAttribMgr( Long id ) {
        this.id = id;
    }

    public FewVoxelVtxAttribMgr( Long id, Scenario scenario ) {
        this.id = id;
        this.scenario = scenario;
    }

    @Override
    public List<TriangleSource> execute() throws Exception {
        triangleSources = new ArrayList<>();
        renderIdToBuffers = new HashMap<>();

        int startingX = 0;
        int startingY = 0;
        int startingZ = 0;

        VertexFactory factory = new VertexFactory();
        switch (scenario) {
            case whole:
                establishStackedScenario(startingX, startingY, startingZ, factory);
                break;
            case small:
                establishSmallScenario(startingX, startingY, startingZ, factory);
                break;
            case minimal:
                establishSimplestScenario(startingX, startingY, startingZ, factory);
                break;
        }

        // Now have a full complement of triangles and vertices.  For this renderable, can traverse the
        // vertices, making a "composite normal" based on the normals of all entangling triangles.
        NormalCompositor normalCompositor = new NormalCompositor();
        normalCompositor.createGouraudNormals(factory);

        triangleSources.add(factory);
        BufferPackager packager = new BufferPackager();
        RenderBuffersBean rbb = new RenderBuffersBean();
        rbb.setAttributesBuffer( packager.getVertexAttributes(factory) );
        rbb.setIndexBuffer( packager.getIndices(factory) );

        renderIdToBuffers.put( id, rbb );

        return triangleSources;
    }

    @Override
    public void exportVertices(File outputLocation, String filenamePrefix) throws Exception {
        OBJWriter objWriter = new OBJWriter();
        for ( TriangleSource triangleSource: triangleSources) {
            objWriter.writeVertices(outputLocation, filenamePrefix, OBJWriter.FILE_SUFFIX, id, triangleSource);
        }
    }

    @Override
    public Map<Long,RenderBuffersBean> getRenderIdToBuffers() {
        return renderIdToBuffers;
    }

    @Override
    public void close() {
        renderIdToBuffers.clear();
        triangleSources.clear();
    }

    private void establishStackedScenario(int startingX, int startingY, int startingZ, VertexFactory factory) {
        //NOTE: the definitions below appear very similar.  However, they differ in the important aspect,
        // that the exposed face list tells which are out-facing.  Attempting to call functions and add
        // single voxels will be compounded by this.

        // Making a zig-zag pattern on level Z=0.
        VoxelInfoBean voxelInfoBean;
        VoxelInfoKey key;
        
        int voxSize = 15;

        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(startingX,startingY,startingZ);
        voxelInfoBean.setKey(key);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        key = new VoxelInfoKey(startingX+voxSize, startingY+voxSize, startingZ);
        voxelInfoBean = new VoxelInfoBean();
        voxelInfoBean.setKey( key );
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        key = new VoxelInfoKey(startingX+voxSize * 2, startingY+voxSize * 2, startingZ);
        voxelInfoBean = new VoxelInfoBean();
        voxelInfoBean.setKey( key );
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        // Making zig-zag at next z-level.
        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(startingX+voxSize,startingY,startingZ+voxSize);
        voxelInfoBean.setKey(key);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        key = new VoxelInfoKey(startingX+voxSize * 2, startingY+voxSize, startingZ+voxSize);
        voxelInfoBean = new VoxelInfoBean();
        voxelInfoBean.setKey( key );
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        key = new VoxelInfoKey(startingX+voxSize * 3, startingY+voxSize * 2, startingZ+voxSize);
        voxelInfoBean = new VoxelInfoBean();
        voxelInfoBean.setKey( key );
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

        key = new VoxelInfoKey(startingX+voxSize * 4, startingY+voxSize * 3, startingZ+voxSize * 1);
        voxelInfoBean = new VoxelInfoBean();
        voxelInfoBean.setKey( key );
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        factory.addEnclosure(voxelInfoBean);

    }

    /**
     * Makes a stacked pair of boxes, joined on one line-corner to another box, and with another box in empty space
     * unattached to those three.
     */
    private void establishSmallScenario(int startingX, int startingY, int startingZ, VertexFactory factory) {
        //NOTE: the definitions below appear very similar.  However, they differ in the important aspect,
        // that the exposed face list tells which are out-facing.  Attempting to call functions and add
        // single voxels will be compounded by this.
        VoxelInfoBean voxelInfoBean = new VoxelInfoBean();
        VoxelInfoKey key = new VoxelInfoKey(startingX,startingY,startingZ);
        voxelInfoBean.setKey(key);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );

        factory.addEnclosure(voxelInfoBean);

        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(startingX,startingY+1,startingZ);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setKey(key);

        factory.addEnclosure(voxelInfoBean);

        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(startingX+1,startingY+1,startingZ+1);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        voxelInfoBean.setKey(key);

        factory.addEnclosure(voxelInfoBean);

        // Isolated
        voxelInfoBean = new VoxelInfoBean();
        key = new VoxelInfoKey(startingX,startingY,startingZ+5);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace( VoxelInfoBean.RIGHT_FACE );
        voxelInfoBean.setKey(key);

        factory.addEnclosure(voxelInfoBean);
    }

    /**
     * Makes a stacked pair of boxes, joined on one line-corner to another box, and with another box in empty space
     * unattached to those three.
     */
    private void establishSimplestScenario(int startingX, int startingY, int startingZ, VertexFactory factory) {
        //NOTE: the definitions below appear very similar.  However, they differ in the important aspect,
        // that the exposed face list tells which are out-facing.  Attempting to call functions and add
        // single voxels will be compounded by this.
        VoxelInfoBean voxelInfoBean = new VoxelInfoBean();
        VoxelInfoKey key = new VoxelInfoKey(startingX,startingY,startingZ);
        voxelInfoBean.setKey(key);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BACK_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.FRONT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.TOP_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.LEFT_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.BOTTOM_FACE);
        voxelInfoBean.setExposedFace(VoxelInfoBean.RIGHT_FACE);

        factory.addEnclosure(voxelInfoBean);
    }

}

