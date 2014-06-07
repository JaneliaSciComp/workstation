package org.janelia.it.workstation.tracing;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import org.janelia.it.workstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.slice_viewer.Subvolume;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestAStar {

    @Test
    @Category(TestCategories.PrototypeTests.class) // mark this as prototype until octree folder can be properly setup
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
        // Create some padding around the neurite ends.
        ZoomedVoxelIndex v1pad = new ZoomedVoxelIndex(
                v1.getZoomLevel(),
                v1.getX()+10, v1.getY()-10, v1.getZ()+10);
        ZoomedVoxelIndex v2pad = new ZoomedVoxelIndex(
                v2.getZoomLevel(),
                v2.getX()-10, v2.getY()+10, v2.getZ()-10);
        //
        System.out.println("Loading subvolume...");
        Subvolume subvolume = new Subvolume(v1pad, v2pad, wholeImage);
        System.out.println("Finished loading subvolume.");
        // 
        System.out.println("Initializing A*...");
        AStar astar = new AStar(subvolume);
        System.out.println("Finished initializing A*.");
        System.out.println("Tracing path...");
        List<ZoomedVoxelIndex> path = astar.trace(v1, v2, 1000.0);
        /*
                new VoxelIndex(start_x, start_y, start_z),
                new VoxelIndex(goal_x, goal_y, goal_z));
                */
        System.out.println("Finished tracing path.");
        assertNotNull(path);
        assertFalse(path.size() == 0);
        System.out.println("Number of points in path = "+path.size());
        for (ZoomedVoxelIndex p : path) {
            System.out.println(p + ": " +subvolume.getIntensityGlobal(p, 0));
        }
    }

}
