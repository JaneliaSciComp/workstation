package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerObjData;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorSharedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;

/**
 * Created by murphys on 9/4/2015.
 */
public class JFRC2010CompartmentSharedResource extends ActorSharedResource {

    private static final Logger logger = LoggerFactory.getLogger(JFRC2010CompartmentSharedResource.class);

    public JFRC2010CompartmentSharedResource() {
        super("JFRC2010Compartment");
    }

    @Override
    public void load() {

        File localJaneliaMeshDir = new File("U:\\meshes");
        File[] meshFiles = localJaneliaMeshDir.listFiles();
        Random rand = new Random();
        Matrix4 vertexRotation=new Matrix4();

        // Empirically derived - compatible with results of MeshLab import/export from normalized compartment coordinates
        vertexRotation.setTranspose(-1.0f,   0.0f,   0.0f,   0.5f,
                0.0f,  -1.0f,   0.0f,   0.25f,
                0.0f,   0.0f,  -1.0f,   0.625f,
                0.0f,   0.0f,   0.0f,   1.0f);

        for (File meshFile : meshFiles) {
            if (meshFile.getName().endsWith(".obj")) {
                try {
                    VoxelViewerObjData objData = VoxelViewerObjData.createObjDataFromFile(meshFile);
                    // todo: create ArrayMeshGLActor from this objData
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(ex.toString());
                }

//                final MeshObjActor ma = new MeshObjActor(meshFile);
//                ma.setVertexRotation(vertexRotation);
//                ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.5f));
//                ma.setUpdateCallback(new GLDisplayUpdateCallback() {
//                    @Override
//                    public void update(GL4 gl) {
//                        Matrix4 actorModel = ma.getModel();
//                        drawShader.setModel(gl, actorModel);
//                        drawShader.setDrawColor(gl, ma.getColor());
//                    }
//                });
//                drawSequence.getActorSequence().add(ma);

            }
        }

    }
}
