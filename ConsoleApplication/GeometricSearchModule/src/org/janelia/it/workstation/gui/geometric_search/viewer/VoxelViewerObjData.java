package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 9/4/2015.
 */
public class VoxelViewerObjData {

    private static Logger logger = LoggerFactory.getLogger(VoxelViewerObjData.class);

    public List<Vector3> vertexList=new ArrayList<>();
    public List<Vector3> normalList=new ArrayList<>();
    public List<Integer> faceList=new ArrayList<>(); // in sequential sets of 3 for each vertex and normal

    static public VoxelViewerObjData createObjDataFromFile(File objFile) throws Exception {
        VoxelViewerObjData objData=new VoxelViewerObjData();
        BufferedReader reader = new BufferedReader(new FileReader(objFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String tline = line.trim();
            if (tline.length() > 0 && !tline.startsWith("#")) {
                if (tline.startsWith("vn")) {
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 4) {
                        objData.normalList.add(new Vector3(new Float(tArr[1].trim()), new Float(tArr[2].trim()), new Float(tArr[3].trim())));
                    }
                } else if (tline.startsWith("v")) {
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 4 || tArr.length == 7) { // ignore the last 3 values if length 7
                        objData.vertexList.add(new Vector3(new Float(tArr[1].trim()), new Float(tArr[2].trim()), new Float(tArr[3].trim())));
                    }
                } else if (tline.startsWith("f")) {
                    tline=tline.replace('/', ' ');
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 7) {
                        objData.faceList.add(new Integer(tArr[1].trim()));
                        objData.faceList.add(new Integer(tArr[3].trim()));
                        objData.faceList.add(new Integer(tArr[5].trim()));
                    }
                }
            }
        }
        logger.info("createObjDataFromFile() loaded " + objData.normalList.size()+" vn "+objData.vertexList.size()+" v "+objData.faceList.size()+" f  file="+objFile.getName());
        reader.close();
        return objData;
    }
}
