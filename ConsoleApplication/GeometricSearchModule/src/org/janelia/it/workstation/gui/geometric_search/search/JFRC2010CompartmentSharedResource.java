package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerObjData;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorSharedResource;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.MeshActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.Random;

/**
 * Created by murphys on 9/4/2015.
 */
public class JFRC2010CompartmentSharedResource extends ActorSharedResource {

    private static final Logger logger = LoggerFactory.getLogger(JFRC2010CompartmentSharedResource.class);

    private static JFRC2010CompartmentSharedResource singletonInstance;

    private JFRC2010CompartmentSharedResource() {
        super("JFRC2010Compartment");
    }

    public static JFRC2010CompartmentSharedResource getInstance() {
        if (singletonInstance==null) {
            singletonInstance=new JFRC2010CompartmentSharedResource();
        }
        return singletonInstance;
    }

    @Override
    public void load() {

        File localJaneliaMeshDir = new File("U:\\meshes");
        File homeMeshDir = new File("C:\\cygwin64\\home\\murphys\\meshes");
        File meshDir=localJaneliaMeshDir;
        if (!meshDir.exists()) {
            meshDir=homeMeshDir;
        }
        File[] meshFiles = meshDir.listFiles();
        Random rand = new Random();
        Matrix4 vertexRotation=new Matrix4();

        // Empirically derived - compatible with results of MeshLab import/export from normalized compartment coordinates
        vertexRotation.setTranspose( 1.0f,   0.0f,   0.0f,  -0.5f,
                                     0.0f,  -1.0f,   0.0f,   0.25f,
                                     0.0f,   0.0f,  -1.0f,   0.625f,
                                     0.0f,   0.0f,   0.0f,   1.0f);

        int mvpPrecomputeGroup= GL4SimpleActor.getNextMvpPrecomputeGroup();

        for (File meshFile : meshFiles) {
            if (meshFile.getName().endsWith(".obj")) {
                try {
                    logger.info("Creating VoxelViewerObjData from file="+meshFile.getAbsolutePath());
                    VoxelViewerObjData objData = VoxelViewerObjData.createObjDataFromFile(meshFile);
                    MeshActor ma=new MeshActor(meshFile.getName().substring(0,meshFile.getName().length()-4),objData, vertexRotation);
                    ma.setColor(new Vector4(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 0.5f));
                    ma.createAndSetGLActor();
                    ma.getGlActor().setMvpPrecomputeGroup(mvpPrecomputeGroup);
                    getSharedActorList().add(ma);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error(ex.toString());
                }

            }
        }

        isLoaded=true;
    }
}
