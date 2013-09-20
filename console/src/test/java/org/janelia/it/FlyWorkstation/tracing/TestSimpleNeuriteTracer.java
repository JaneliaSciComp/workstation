package org.janelia.it.FlyWorkstation.tracing;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;

import features.ComputeCurvatures;
import ij.ImagePlus;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Subvolume;
import org.janelia.it.FlyWorkstation.octree.ZoomLevel;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.junit.Test;

import tracing.Path;
import tracing.TracerThread;

public class TestSimpleNeuriteTracer {

    @Test
    public void testTracePath() {
        // Use the same example chunk from TestExtractSubvolume.java
        ZoomLevel zoomLevel = new ZoomLevel(0);
        ZoomedVoxelIndex v1 = new ZoomedVoxelIndex(zoomLevel, 29952, 24869, 1243); // upper right back corner
        ZoomedVoxelIndex v2 = new ZoomedVoxelIndex(zoomLevel, 29753, 25609, 1233); // lower left front corner
        SharedVolumeImage wholeImage = new SharedVolumeImage();
        // TODO - this only works on Windows with mousebrainmicro drive mounted as M:
        // We should create a local volume resource for testing.
        String octreeFolder = "M:/render/2013-04-25-AAV";
        try {
            wholeImage.loadURL(new File(octreeFolder).toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            fail("Error opening octree directory "+octreeFolder);
        }
        Subvolume subvolume = new Subvolume(v1, v2, wholeImage);
        // Run Simple Neurite Tracer
        // Adapted from 
        //  http://fiji.sc/cgi-bin/gitweb.cgi?p=fiji.git&a=blob&f=src-plugins/Simple_Neurite_Tracer/src/main/java/tracing/Albert_Test.java
        ImagePlus imagePlus = SubvolumeConverter.toImagePlus(subvolume);
        //
        int depth = subvolume.getExtent().getZ();
        boolean reciprocal = true;
        ComputeCurvatures hessian = null; // TODO
        int start_x = v1.getX() - subvolume.getOrigin().getX();
        int start_y = v1.getY() - subvolume.getOrigin().getY();
        int start_z = v1.getZ() - subvolume.getOrigin().getZ();
        int  goal_x = v2.getX() - subvolume.getOrigin().getX();
        int  goal_y = v2.getY() - subvolume.getOrigin().getY();
        int  goal_z = v2.getZ() - subvolume.getOrigin().getZ();
        long reportEveryMilliseconds = 3000; // how often to check for timeout?
        int timeoutSeconds = 5 * 60; // give up after 5 minutes
        TracerThread tracer = new TracerThread(
                imagePlus, 0, 255, timeoutSeconds, reportEveryMilliseconds, 
                start_x, start_y, start_z, goal_x, goal_y, goal_z, reciprocal, 
                depth == 1, hessian, ((hessian == null) ? 1 : 4), 
                null, hessian != null );
        tracer.run(); // run in foreground for testing
        Path path = tracer.getResult();
        assertNotNull(path); // fails TODO
        assertFalse(path.points == 0);
    }

}
