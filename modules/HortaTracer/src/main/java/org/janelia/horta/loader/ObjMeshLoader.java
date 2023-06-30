package org.janelia.horta.loader;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FilenameUtils;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.WavefrontObjLoader;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.DiffuseMaterial;
import org.janelia.gltools.material.IBLDiffuseMaterial;
import org.janelia.gltools.material.TransparentEnvelope;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.MeshCreateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.Exceptions;

/**
 *
 * @author brunsc
 */
public class ObjMeshLoader implements FileTypeLoader
{
    private final NeuronTracerTopComponent horta;

    public ObjMeshLoader(NeuronTracerTopComponent horta) {
        this.horta = horta;
    }

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("OBJ"))
            return true;
        return false;
    }

    public void saveObjectMesh (String meshName, String filename) {
        TmObjectMesh newObjMesh = new TmObjectMesh(meshName, filename);
        try {
            if(TmModelManager.getInstance().getCurrentWorkspace() != null) {
                TmModelManager.getInstance().getCurrentWorkspace().addObjectMesh(newObjMesh);
                TmModelManager.getInstance().saveCurrentWorkspace();
            }

            // fire off event for scene editor
            MeshCreateEvent meshEvent = new MeshCreateEvent(this,
                    Arrays.asList(new TmObjectMesh[]{newObjMesh}));
            ViewerEventBus.postEvent(meshEvent);
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException 
    {
        Runnable meshLoadTask = new Runnable() {
            @Override
            public void run() {
                MeshGeometry meshGeometry;
                try {
                    meshGeometry = WavefrontObjLoader.load(source.openInputStream());
                    TransparentEnvelope material = new TransparentEnvelope();
                    Color color = meshGeometry.getDefaultColor();
                    if (color != null)
                        material.setDiffuseColor(color);
                    final MeshActor meshActor = new MeshActor(
                            meshGeometry,
                            material,
                            null
                    );
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            int meshNum = horta.getMeshActors().size()+1;
                            meshActor.setMeshName("Object Mesh #" + meshNum);
                            horta.addMeshActor(meshActor);
                            if (source instanceof FileDataSource) {                                
                                saveObjectMesh(meshActor.getMeshName(),
                                        ((FileDataSource)source).getFile().getAbsolutePath());
                            }
                        }
                    });
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        
        new Thread(meshLoadTask).start();
        
        return true;
    }
    
}
