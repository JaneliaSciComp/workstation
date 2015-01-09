/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.shared.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.janelia.it.workstation.geom.Vec3;

/**
 * Simplistic comparison: expects points in same order in both files.
 * @author fosterl
 */
public class SWCComparison {
    // These files are to be produced by a run, stored in the user's home direction, and then checked by this program.
    private static final String DEFAULT_FILE1 = System.getProperty("user.home") + "/ExportImportTest.swc";
    private static final String DEFAULT_FILE2 = System.getProperty("user.home") + "/ExportImportTest_Loop2.swc";
    public static void main( String[] args ) throws Exception {
        String filename1;
        String filename2;
        if (args.length == 0) {
            filename1 = DEFAULT_FILE1;
            filename2 = DEFAULT_FILE2;
        }
        else if (args.length < 2) {
            throw new IllegalArgumentException("USAGE: java " + SWCComparison.class.getName() + " <file1> <file2>");
        }
        else {
            filename1 = args[0];
            filename2 = args[1];
        }
        File file1 = new File(filename1);
        File file2 = new File(filename2);
        if (!file1.exists()) {
            throw new IOException("File " + filename1 + " cannot be read");
        }
        if (!file2.exists()) {
            throw new IOException("File " + filename2 + " cannot be read");
        }
        SWCData data1 = SWCData.read(file1);
        SWCData data2 = SWCData.read(file2);
        
        double[] offset1 = parseOffset(data1);
        double[] offset2 = parseOffset(data2);

        Iterator<SWCNode> data2Nodes = data2.getNodeList().iterator();
        int iteration = 1;
        int failCount = 0;
        for (SWCNode node1 : data1.getNodeList()) {
            if (!data2Nodes.hasNext()) {
                throw new Exception("Node list length differs.");
            }
            SWCNode node2 = data2Nodes.next();
            Vec3 vec1 = new Vec3(node1.getX() + offset1[0], node1.getY() + offset1[1], node1.getZ() + offset1[2]);
            Vec3 vec2 = new Vec3(node2.getX() + offset2[0], node2.getY() + offset2[1], node2.getZ() + offset2[2]);
            System.out.println("================================\n"
                    + "Point number " + iteration++);
            System.out.println("Point from #1 file: " + vec1);
            System.out.println("Point from #2 file: " + vec2);
            if (!vec1.equals(vec2)) {
                System.out.println("Points from file1 and file2 do not match.");
                failCount ++;
            }
        }
        System.out.println(failCount + " points were mismatches between export loops.");
            
    }

    /**
     * THIS was copied form AnnotationModel.  TODO consider refactoring.
     * note from CB, July 2013: Vaa3d can't handle large coordinates in swc files,
     * so he added an OFFSET header and recentered on zero when exporting
     * therefore, if that header is present, respect it
     *
     * @param data to extract headers.
     * @return array of offset.
     * @throws NumberFormatException 
     */
    private static double[] parseOffset(SWCData data) throws NumberFormatException {
        double[] offset = new double[3];
        String offsetHeader = data.findHeaderLine("OFFSET");
        if (offsetHeader != null) {
            String [] items = offsetHeader.split("\\s+");
            // expect # OFFSET x y z
            if (items.length == 5) {
                offset[0] = Double.parseDouble(items[2]);
                offset[1] = Double.parseDouble(items[3]);
                offset[2] = Double.parseDouble(items[4]);
            } else {
                // ignore the line if we can't parse it
            }
        }
        return offset;
    }
    
}
