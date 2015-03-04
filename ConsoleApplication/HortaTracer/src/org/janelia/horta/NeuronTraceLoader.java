//Error reading included file Templates/Classes/Templates/Licenses/license-janelia.txt
package org.janelia.horta;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JOptionPane;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.geometry3d.Vantage;
import org.janelia.gltools.GL3Actor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fosterl
 */
public class NeuronTraceLoader {
    private Logger logger = LoggerFactory.getLogger(NeuronTraceLoader.class);
    
    private NeuronTracerTopComponent nttc;
    private NeuronMPRenderer neuronMPRenderer;
    private SceneWindow sceneWindow;
    private TracingInteractor tracingInteractor;
    
    public NeuronTraceLoader(NeuronTracerTopComponent nttc, NeuronMPRenderer neuronMPRenderer, SceneWindow sceneWindow, TracingInteractor tracingInteractor) {
        this.nttc = nttc;
        this.neuronMPRenderer = neuronMPRenderer;
        this.sceneWindow = sceneWindow;
        this.tracingInteractor = tracingInteractor;
    }
    
    public void loadYamlFile(File f) throws IOException {
        final File file = f;
        Runnable task = new Runnable() {
            public void run() {
                ProgressHandle progress = ProgressHandleFactory.createHandle("Loading brain tiles");
                progress.start();
                progress.progress("Loading YAML tile information...");

                PerformanceTimer timer = new PerformanceTimer();
                BrainTileInfoList tileList = new BrainTileInfoList();
                try {
                    tileList.loadYamlFile(file);
                } catch (IOException ex) {
                    handleException(ex);
                }
                progress.progress("YAML tile information loaded");
                logger.info("yaml load took " + timer.reportMsAndRestart() + " ms");
                // TODO remove this testing hack
                if (! loadExampleTile(tileList, progress)) {
                    RuntimeException re = new RuntimeException(FAILED_TO_LOAD_EXAMPLE_TILE_MSG);
                    handleException(re);
                }
                progress.progress("Example tile loaded");

                progress.finish();
            }
            public static final String FAILED_TO_LOAD_EXAMPLE_TILE_MSG = "Failed to load example tile.";
        };
        RequestProcessor.getDefault().post(task);
    }

    private boolean loadExampleTile(BrainTileInfoList tileList, ProgressHandle progress) {

        BrainTileInfo exampleTile = null;
        // Find first existing tile
        for (BrainTileInfo tile : tileList) {
            if (tile.folderExists()) {
                exampleTile = tile;
                break;
            }
        }
        
        if (exampleTile == null) {
            logger.error("No tiles found");
            StringBuilder bldr = new StringBuilder("Nonexistent Files List:");
            for (BrainTileInfo tile: tileList) {
                bldr.append(" ");
                bldr.append(
                    tile.getParentPath()).append("/").append(tile.getLocalPath()
                );
            }
            logger.info(bldr.toString());
            return false;
        }

        File tileFile = new File(exampleTile.getParentPath(), exampleTile.getLocalPath());
        logger.info(tileFile.getAbsolutePath());

        progress.progress("Loading tile file " + tileFile);
        try {
            GL3Actor brickActor = nttc.createBrickActor(exampleTile);
            // mprActor.addChild(brickActor);
            neuronMPRenderer.addVolumeActor(brickActor);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        // progress.finish();
        
        for (NeuriteActor tracingActor : tracingInteractor.createActors()) {
            sceneWindow.getRenderer().addActor(tracingActor);
            tracingActor.getModel().addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    sceneWindow.getInnerComponent().repaint();
                }
            });
        }

        Vantage v = sceneWindow.getVantage();
        v.centerOn(exampleTile.getBoundingBox());
        v.setDefaultBoundingBox(exampleTile.getBoundingBox());
        v.notifyObservers();

        return true;
    }

    private void handleException(Exception ex) {
        Exceptions.printStackTrace(ex);
        JOptionPane.showMessageDialog(nttc, ex);
    }
    

}
