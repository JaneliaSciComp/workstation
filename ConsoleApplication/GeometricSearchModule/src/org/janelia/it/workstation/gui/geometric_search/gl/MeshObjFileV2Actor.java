package org.janelia.it.workstation.gui.geometric_search.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

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
        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL3.GL_FLOAT, false, 0, 0);
        gl.glVertexAttribPointer(1, 3, GL3.GL_FLOAT, false, 0, fList.size() * 9 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        gl.glEnableVertexAttribArray(1);
        checkGlError(gl, "d glEnableVertexAttribArray 1 error");
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, fList.size()*3);
        checkGlError(gl, "d glDrawArrays error");
    }

    @Override
    public void init(GL3 gl) {
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
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "glBindBuffer error");
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, fb.capacity() * 4, fb, GL3.GL_STATIC_DRAW);
        checkGlError(gl, "glBufferData error");
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


}
