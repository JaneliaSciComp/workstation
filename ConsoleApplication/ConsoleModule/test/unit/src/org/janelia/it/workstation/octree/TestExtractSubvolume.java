package org.janelia.it.workstation.octree;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;

import org.janelia.it.workstation.gui.slice_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.slice_viewer.Subvolume;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestExtractSubvolume {

    @Test
    @Category(TestCategories.PrototypeTests.class) // mark this as prototype until octree folder can be properly setup
    public void testExtractUpperRightBackValue() {
        // I see a neurite going from one of these points to another in data set AAV 4/25/2013
        org.janelia.it.workstation.octree.ZoomLevel zoomLevel = new ZoomLevel(0);
        org.janelia.it.workstation.octree.ZoomedVoxelIndex v1 = new org.janelia.it.workstation.octree.ZoomedVoxelIndex(zoomLevel, 29952, 24869, 1243); // upper right back corner
        org.janelia.it.workstation.octree.ZoomedVoxelIndex v2 = new org.janelia.it.workstation.octree.ZoomedVoxelIndex(zoomLevel, 29753, 25609, 1233); // lower left front corner
        SharedVolumeImage wholeImage = new SharedVolumeImage();
        // TODO - this only works on Windows with mousebrainmicro drive mounted as M:
        String octreeFolder = "M:/render/2013-04-25-AAV";
        try {
            wholeImage.loadURL(new File(octreeFolder).toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            fail("Error opening octree directory "+octreeFolder);
        }
        // Create some padding around the neurite ends
        org.janelia.it.workstation.octree.ZoomedVoxelIndex v1pad = new org.janelia.it.workstation.octree.ZoomedVoxelIndex(
                v1.getZoomLevel(),
                v1.getX()+10, v1.getY()-10, v1.getZ()+10);
        org.janelia.it.workstation.octree.ZoomedVoxelIndex v2pad = new org.janelia.it.workstation.octree.ZoomedVoxelIndex(
                v2.getZoomLevel(),
                v2.getX()-10, v2.getY()+10, v2.getZ()-10);
        //
        Subvolume subvolume = new Subvolume(v1pad, v2pad, wholeImage);
        assertEquals(25281, subvolume.getIntensityGlobal(v1, 0));
        assertEquals(25903, subvolume.getIntensityGlobal(v2, 0));
    }

}
