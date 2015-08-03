package org.janelia.it.workstation.gui.geometric_search.viewer.gl.mesh;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;

/**
 * Created by murphys on 4/9/15.
 */
public class MeshObjActor extends GL4SimpleActor
{
    private final Logger logger = LoggerFactory.getLogger(MeshObjActor.class);
        
    File objFile;
    boolean loaded=false;
    boolean loadError=false;
    boolean drawLines=false;
    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    Vector4 color=new Vector4(0.0f, 0.0f, 0.0f, 0.0f);
    Matrix4 vertexRotation=null;

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
    
    public void setColor(Vector4 color) {
        this.color=color;
    }
    
    public Vector4 getColor() {
        return color;
    }

    public MeshObjActor(File objFile) {
        this.objFile=objFile;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines=drawLines;
    }
    
    public void setVertexRotation(Matrix4 rotation) {
        this.vertexRotation=rotation;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);


//        gl.glDisable(GL4.GL_DEPTH_TEST);
////        gl.glShadeModel(GL4.GL_SMOOTH);
////        gl.glDisable(GL4.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);
        
        gl.glDisable(GL4.GL_DEPTH_TEST);

        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, fList.size() * 9 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");
        
        logger.info("calling glDrawArrays for fList.size()="+fList.size());
        
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, fList.size()*3);
        checkGlError(gl, "d glDrawArrays error");

//        gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glDisable(GL4.GL_BLEND);
    }

    @Override
    public void init(GL4 gl) {
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

        // We want to create a triangle for each face, picking from the vertices
        FloatBuffer fb=FloatBuffer.allocate(fList.size()*9*2); // 9 floats per 3 vertices, and 9 floats for the vertex normals
        // First, the vertices
        for (int f=0;f<fList.size();f++) {
            fGroup fg=fList.get(f);
            int[] fa=new int[3];
            fa[0]=fg.f1-1;
            fa[1]=fg.f2-1;
            fa[2]=fg.f3-1;
            for (int i=0;i<3;i++) {
                vGroup vg=vList.get(fa[i]);
                float x=vg.x;
                float y=vg.y;
                float z=vg.z;
                if (vertexRotation!=null) {
                    Vector4 v = new Vector4(x, y, z, 1.0f);
                    Vector4 vr = vertexRotation.multiply(v);
                    x=vr.get(0);
                    y=vr.get(1);
                    z=vr.get(2);
                } 
                int s=f*9+i*3;
                fb.put(s, x);
                fb.put(s+1, y);
                fb.put(s+2, z);
            }
        }
        // Next, the normals
        int vOffset=fList.size()*9;
        for (int f=0;f<fList.size();f++) {
            fGroup fg=fList.get(f);
            int[] fa=new int[3];
            fa[0]=fg.f1-1;
            fa[1]=fg.f2-1;
            fa[2]=fg.f3-1;
            for (int i=0;i<3;i++) {
                vGroup vg=vnList.get(fa[i]);
                float x=vg.x;
                float y=vg.y;
                float z=vg.z;
                int s=vOffset+f*9+i*3;
                fb.put(s, x);
                fb.put(s+1, y);
                fb.put(s+2, z);
            }
        }


        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "glGenVertexArrays error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "glBindVertexArray error");
        gl.glGenBuffers(1, vertexBufferId);
        checkGlError(gl, "glGenBuffers error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, fb.capacity() * 4, fb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "glBufferData error");
    }

    @Override
    public void dispose(GL4 gl) {

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
                    if (tArr.length == 4 || tArr.length == 7) { // ignore the last 3 values if length 7
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
        logger.info("loadObjFile() loaded " + vnList.size()+" vn "+vList.size()+" v "+fList.size()+" f  file="+objFile.getName());
        reader.close();
    }


}
