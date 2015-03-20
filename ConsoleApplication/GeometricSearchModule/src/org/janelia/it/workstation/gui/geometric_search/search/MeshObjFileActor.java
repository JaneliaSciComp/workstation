package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by murphys on 3/16/15.
 */
public class MeshObjFileActor implements GLActor
{
    private final Logger logger = LoggerFactory.getLogger(MeshObjFileActor.class);
    File objFile;
    boolean loaded=false;
    boolean loadError=false;

    private class vGroup {

        public vGroup(String xs, String ys, String zs) {
            this.x=new Float(xs);
            this.y=new Float(ys);
            this.z=new Float(zs);
        }
        public float x;
        public float y;
        public float z;
    }

    private class fGroup {
        public fGroup(String f1s, String f2s, String f3s) {
            this.f1=new Integer(f1s);
            this.f2=new Integer(f2s);
            this.f3=new Integer(f3s);
        }
        public int f1;
        public int f2;
        public int f3;
    }

    List<vGroup> vnList=new ArrayList<>();
    List<vGroup> vList=new ArrayList<>();
    List<fGroup> fList=new ArrayList<>();


    public MeshObjFileActor(File objFile) {
        this.objFile=objFile;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {

        if (loadError) {
            return;
        }

        if (!loaded) {
            try {
                loadObjFile();
            } catch (Exception ex) {
                logger.error("Could not load file "+objFile.getAbsolutePath());
                ex.printStackTrace();
                loadError=true;
                return;
            }
            loaded=true;
        }

        //logger.info("display()");

        GL2 gl2 = glDrawable.getGL().getGL2();

        //gl2.glPointSize(1.0f);
        // gl2.glColor3f(0.3f, 0.3f, 0.7f);
        gl2.glBegin(gl2.GL_TRIANGLES);

        int vMax=vList.size();

        Iterator<fGroup> fi=fList.iterator();
        while (fi.hasNext()) {
            fGroup vg=fi.next();

            if (vg.f1<=vMax && vg.f2<=vMax && vg.f3<=vMax) {
                vGroup v1=vList.get(vg.f1-1);
                vGroup v2=vList.get(vg.f2-1);
                vGroup v3=vList.get(vg.f3-1);
                gl2.glVertex3f(v1.x*100f, v1.y*100f, v1.z*100f);
                gl2.glVertex3f(v2.x*100f, v2.y*100f, v2.z*100f);
                gl2.glVertex3f(v3.x*100f, v3.y*100f, v3.z*100f);
            }
        }
        gl2.glEnd();

    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        // NOTE - Y coordinate is inverted w.r.t. glVertex3d(...)
        BoundingBox3d result = new BoundingBox3d();
        result.setMin(-100.0, -100.0, -100.0);
        result.setMax(100.0,  100.0, 100.0);
        return result;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
    }

    private void loadObjFile() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(objFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String tline = line.trim();
            if (tline.length() > 0 && !tline.startsWith("#")) {
                if (tline.startsWith("vn")) {
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 4) {
                        vnList.add(new vGroup(tArr[1], tArr[2], tArr[3]));
                    }
                } else if (tline.startsWith("v")) {
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 4) {
                        vList.add(new vGroup(tArr[1], tArr[2], tArr[3]));
                    }
                } else if (tline.startsWith("f")) {
                    tline=tline.replace('/', ' ');
                    String[] tArr = tline.split("\\s+");
                    if (tArr.length == 7) {
                        fList.add(new fGroup(tArr[1], tArr[3], tArr[5]));
                    }
                }
            }
        }
        logger.info("loadObjFile() loaded " + vnList.size()+" vn "+vList.size()+" v "+fList.size()+" f");
        reader.close();
    }

}
