package org.janelia.it.workstation.gui.geometric_search.gl;

import org.janelia.geometry3d.Matrix4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 4/9/15.
 */
public class MeshObjFileV2Actor extends GL3SimpleActor
{
    private final Logger logger = LoggerFactory.getLogger(MeshObjFileV2Actor.class);
    File objFile;
    boolean loaded=false;
    boolean loadError=false;
    boolean drawLines=false;

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


    public MeshObjFileV2Actor(File objFile) {
        this.objFile=objFile;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines=drawLines;
    }

    @Override
    public void display(GL3 gl) {
        super.display(gl);
    }

    @Override
    public void init(GL3 gl) {

    }

    @Override
    public void dispose(GL3 gl) {

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

    /*

    @Override
    public void display(GL3 gl, Matrix4 viewProjection) {

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

        gl2.glClearColor(0f, 0f, 0f, 1f);
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);


        gl2.glEnable(gl2.GL_DEPTH_TEST);
        gl2.glDepthFunc(gl2.GL_LEQUAL);

        gl2.glShadeModel(gl2.GL_SMOOTH);
        gl2.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

        float[] lightPos = { -30, 0, 5, 1 };
        float[] lightAmbient = { 0.3f, 0.3f, 0.3f, 1f };
        float[] lightSpecular = { 0.8f, 0.8f, 0.8f, 1f };

        gl2.glEnable(GL2.GL_LIGHT1);
        gl2.glEnable(GL2.GL_LIGHTING);

        gl2.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPos, 0);
        gl2.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, lightAmbient, 0);
        gl2.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightSpecular, 0);

        float[] rgba = { 0.3f, 0.5f, 1f };
        gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, rgba, 0);
        gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, rgba, 0);
        gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SHININESS, FloatBuffer.wrap(new float[]{0.5f}));

        drawTrianglesAndLines(gl2);

    }

    private void drawTrianglesAndLines(GL3 gl) {
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

        if (drawLines) {
            gl2.glColor3f(0.0f, 0.0f, 0.0f);
            Iterator<fGroup> fd=fList.iterator();
            while (fd.hasNext()) {
                fGroup vg=fd.next();
                if (vg.f1<=vMax && vg.f2<=vMax && vg.f3<=vMax) {
                    vGroup v1=vList.get(vg.f1-1);
                    vGroup v2=vList.get(vg.f2-1);
                    vGroup v3=vList.get(vg.f3-1);

                    gl2.glBegin(gl2.GL_LINE_STRIP);
                    gl2.glVertex3f(v1.x*100f, v1.y*100f, v1.z*100f);
                    gl2.glVertex3f(v2.x*100f, v2.y*100f, v2.z*100f);
                    gl2.glVertex3f(v3.x*100f, v3.y*100f, v3.z*100f);
                    gl2.glVertex3f(v1.x*100f, v1.y*100f, v1.z*100f);
                    gl2.glEnd();
                }
            }
        }
    }
    */


}
