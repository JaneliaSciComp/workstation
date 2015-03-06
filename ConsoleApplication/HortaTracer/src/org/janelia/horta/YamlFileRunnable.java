package org.janelia.horta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JOptionPane;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.MouseLightYamlBrickSource;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.scenewindow.SceneWindow;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.StatusDisplayer;

/**
 * Convenience runnable for disposing of a YML file.
 * @author fosterl
 */
public class YamlFileRunnable implements Runnable {
    private File yamlFile;
    private SceneWindow sceneWindow;
    private NeuronTracerTopComponent nttc;
    private NeuronTraceLoader loader;
    
    public YamlFileRunnable(
            File yamlFile, 
            SceneWindow sceneWindow, 
            NeuronTraceLoader loader,
            NeuronTracerTopComponent neuronTracerTC) {
        this.yamlFile = yamlFile;
        this.sceneWindow = sceneWindow;
        this.loader = loader;
        this.nttc = neuronTracerTC;
    }
    
    @Override
    public void run() {
        ProgressHandle progress
                = ProgressHandleFactory.createHandle("Loading brain tile metadata");
        progress.start();
        progress.progress("Loading YAML tile information...");

        PerformanceTimer timer = new PerformanceTimer();

        InputStream yamlStream;
        try {
            // 1 - Load tile index
            yamlStream = new FileInputStream(yamlFile);
            StaticVolumeBrickSource volumeSource = 
                    new MouseLightYamlBrickSource(yamlStream);
            int tileCount = 0;
            for (Double res : volumeSource.getAvailableResolutions()) {
                BrickInfoSet brickInfoSet = volumeSource.getAllBrickInfoForResolution(res);
                tileCount += brickInfoSet.size();
            }
            progress.finish();
            StatusDisplayer.getDefault().setStatusText(
                    tileCount
                    + " tiles loaded in "
                    + String.format("%1$,.2f", timer.reportMsAndRestart() / 1000.0)
                    + " seconds."
            );
            // logger.info("yaml load took " + timer.reportMsAndRestart() + " ms");

            // Recenter
            Vector3 centerFocus = volumeSource.getBoundingBox().getCentroid();
            // logger.info("Center of volume is " + centerFocus.toString());
            PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
            loader.animateToFocusXyz(centerFocus, pCam.getVantage(), 150);

            BrickInfo centerBrickInfo = loader.loadTileAtCurrentFocus( volumeSource );

            Vantage v = pCam.getVantage();
            v.centerOn(centerBrickInfo.getBoundingBox());
            v.setDefaultBoundingBox(centerBrickInfo.getBoundingBox());
            v.notifyObservers();

            nttc.setVolumeSource(volumeSource);
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(
                    nttc,
                    "ERROR loading YAML file "
                    + yamlFile.getAbsolutePath(),
                    "ERROR: YAML load error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    nttc,
                    "ERROR loading Volume file",
                    "ERROR: Volume load error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            progress.finish();
        }
    }
    
    
}
