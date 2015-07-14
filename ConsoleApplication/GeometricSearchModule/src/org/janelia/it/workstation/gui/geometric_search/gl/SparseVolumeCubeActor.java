package org.janelia.it.workstation.gui.geometric_search.gl;

import java.util.List;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 7/8/2015.
 */
public class SparseVolumeCubeActor extends SparseVolumeBaseActor {
    
    private final Logger logger = LoggerFactory.getLogger(SparseVolumeCubeActor.class);

    public SparseVolumeCubeActor(File volumeFile, int volumeChannel, float volumeCutoff) {
        super(volumeFile, volumeChannel, volumeCutoff);
    }
    
    @Override
    public void init(GL4 gl) {

        super.init(gl);

        // We want to create a cube for each vertex. There are 6 faces per cube, and each face
        // requires 2 triangles. This means 12 triangles per original vertex. Each triangle
        // requires 3 vertices, so this is 36 vertices. Then, we need a normal vector for each
        // vertex, so we need 72 vectors per original vertex. Each vector has 3 components, so 
        // this is 3 * 72 = 216 floats per original vertex, or 108 for vertices and 108 for normals.
        // For debugging purposes, we are adding another 36x4=144 values to indicate the color for
        // each vertex.
        
        //viList.clear();
        //viList.add(new viGroup(0.0f, 0.0f, 0.0f, 1.0f));
        //viList.add(new viGroup(0.0f, 0.0f, 0.5f, 1.0f));
        
        List<List<viGroup>> viColorList = new ArrayList<List<viGroup>>();
        
        FloatBuffer fb=FloatBuffer.allocate(viList.size()*(216+144));
        
        float vs = getVoxelUnitSize();
        //float vs = 0.2f;
        
        logger.info("init() using vs="+vs+", viList size="+viList.size());
        
//        List<viGroup> multiColorCube = new ArrayList<>();
//        
//        viGroup bottomColor = new viGroup(1.0f, 0.0f, 0.0f, 0.5f);
//        viGroup topColor    = new viGroup(0.0f, 1.0f, 0.0f, 0.5f);
//        viGroup backColor   = new viGroup(0.0f, 0.0f, 1.0f, 0.5f);
//        viGroup frontColor  = new viGroup(0.0f, 0.3f, 0.0f, 0.5f);
//        viGroup leftColor   = new viGroup(1.0f, 1.0f, 0.0f, 0.5f);
//        viGroup rightColor  = new viGroup(0.5f, 0.5f, 0.5f, 0.5f);
//        
//        multiColorCube.add(bottomColor);
//        multiColorCube.add(topColor);
//        multiColorCube.add(backColor);
//        multiColorCube.add(frontColor);
//        multiColorCube.add(leftColor);
//        multiColorCube.add(rightColor);
//        
//        viColorList.add(multiColorCube);
        
        viGroup transparentColor = new viGroup(1.0f, 0.0f, 0.0f, 0.1f);
        
        List<viGroup> transparentColorCube = new ArrayList<>();
        
        transparentColorCube.add(transparentColor);
        transparentColorCube.add(transparentColor);
        transparentColorCube.add(transparentColor);
        transparentColorCube.add(transparentColor);
        transparentColorCube.add(transparentColor);
        transparentColorCube.add(transparentColor);
        
        for (viGroup v : viList) {
            viColorList.add(transparentColorCube);
        }
             
        // First, vertex information
        int nOffset = viList.size() * 36 * 3;
        int cOffset = viList.size() * 72 * 3;
        for (int v=0;v<viList.size();v++) {
            viGroup vg=viList.get(v);
            List<viGroup> colorList = viColorList.get(v);
            List<viGroup> vcl = getCubicVerticesForVertex(vg, vs);
            for (int v2=0;v2<36;v2++) {
                viGroup vc2 = vcl.get(v2);
                int v3 = (v * 36 + v2) * 3;
                fb.put(v3,  vc2.x);
                fb.put(v3+1,vc2.y);
                fb.put(v3+2,vc2.z);
            }
            for (int v4=36;v4<72;v4++) {
                viGroup vc2 = vcl.get(v4);
                int v5 = nOffset + (v * 36 + (v4-36)) * 3;
                fb.put(v5, vc2.x);
                fb.put(v5+1, vc2.y);
                fb.put(v5+2, vc2.z);
            }
            for (int c1=0;c1<36;c1++) {
                int v6 = cOffset + v*144 + c1*4;
                viGroup colorGroup = null;
                if (c1<6) {
                    colorGroup=colorList.get(0);
                } else if (c1<12) {
                    colorGroup=colorList.get(1);
                } else if (c1<18) {
                    colorGroup=colorList.get(2);
                } else if (c1<24) {
                    colorGroup=colorList.get(3);
                } else if (c1<30) {
                    colorGroup=colorList.get(4);
                } else if (c1<36) {
                    colorGroup=colorList.get(5);
                }
                     fb.put(v6, colorGroup.x);
                    fb.put(v6+1, colorGroup.y);
                    fb.put(v6+2, colorGroup.z);
                    fb.put(v6+3, colorGroup.w);
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
    
    // This method returns a list of 72 vectors (one vector per viGroup).
    // The first 36 vectors are for the triangle face vertices, the second 36
    // are the corresponding normal vertices.
    
    public List<viGroup> getCubicVerticesForVertex(viGroup vg, float vs) {
        List<viGroup> vList = new ArrayList<>();
        
        // We will generate a generic 0-based set of coordinates, and then just
        // add the offsets for the return. The normals are indpendent of position.
        
        float nl = (float)Math.sqrt(3.0);
        
        // Bottom
        viGroup b11 = new viGroup(  vs, 0.0f,   vs, 1.0f);
        viGroup b12 = new viGroup(  vs, 0.0f, 0.0f, 1.0f);
        viGroup b13 = new viGroup(0.0f, 0.0f, 0.0f, 1.0f);
        
        viGroup b11f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
        viGroup b12f = new viGroup(  nl, -nl, -nl, 1.0f); // updated
        viGroup b13f = new viGroup( -nl, -nl, -nl, 1.0f); // updated
 
        viGroup b21 = new viGroup(vs, 0.0f, vs, 1.0f);
        viGroup b22 = new viGroup(0.0f, 0.0f, vs, 1.0f);
        viGroup b23 = new viGroup(0.0f, 0.0f, 0.0f, 1.0f);

        viGroup b21f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
        viGroup b22f = new viGroup( -nl, -nl,  nl, 1.0f); // updated
        viGroup b23f = new viGroup( -nl, -nl, -nl, 1.0f); // updated
       
        // Top
        viGroup t11 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup t12 = new viGroup(vs, vs, 0.0f, 1.0f);
        viGroup t13 = new viGroup(vs, vs, vs, 1.0f);
        
        viGroup t11f = new viGroup( -nl, nl, -nl, 1.0f); // updated
        viGroup t12f = new viGroup(  nl, nl, -nl, 1.0f); // updated
        viGroup t13f = new viGroup(  nl, nl,  nl, 1.0f); // updated
        
        viGroup t21 = new viGroup(0.0f, vs, vs, 1.0f);
        viGroup t22 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup t23 = new viGroup(vs, vs, vs, 1.0f);
        
        viGroup t21f = new viGroup( -nl, nl,  nl, 1.0f); // updated
        viGroup t22f = new viGroup( -nl, nl, -nl, 1.0f); // updated
        viGroup t23f = new viGroup(  nl, nl,  nl, 1.0f); // updated
        
        // Back
        viGroup ba11 = new viGroup(0.0f, 0.0f, 0.0f, 1.0f);
        viGroup ba12 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup ba13 = new viGroup(vs, 0.0f, 0.0f, 1.0f);
        
        viGroup ba11f = new viGroup( -nl, -nl, -nl, 1.0f); // updated
        viGroup ba12f = new viGroup( -nl,  nl, -nl, 1.0f); // updated
        viGroup ba13f = new viGroup(  nl, -nl, -nl, 1.0f); // updated
        
        viGroup ba21 = new viGroup(vs, vs, 0.0f, 1.0f);
        viGroup ba22 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup ba23 = new viGroup(vs, 0.0f, 0.0f, 1.0f);
        
        viGroup ba21f = new viGroup(  nl,  nl, -nl, 1.0f); // updated
        viGroup ba22f = new viGroup( -nl,  nl, -nl, 1.0f); // updated
        viGroup ba23f = new viGroup(  nl, -nl, -nl, 1.0f); // updated
        
        // Front
        viGroup f11 = new viGroup(vs, 0.0f, vs, 1.0f);        
        viGroup f12 = new viGroup(0.0f, 0.0f, vs, 1.0f);
        viGroup f13 = new viGroup(0.0f, vs, vs, 1.0f);
        
        viGroup f11f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
        viGroup f12f = new viGroup( -nl, -nl,  nl, 1.0f); // updated
        viGroup f13f = new viGroup( -nl,  nl,  nl, 1.0f); // updated
        
        viGroup f21 = new viGroup(vs, 0.0f, vs, 1.0f);
        viGroup f22 = new viGroup(vs, vs, vs, 1.0f);
        viGroup f23 = new viGroup(0.0f, vs, vs, 1.0f);
        
        viGroup f21f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
        viGroup f22f = new viGroup(  nl,  nl,  nl, 1.0f); // updated       
        viGroup f23f = new viGroup( -nl,  nl,  nl, 1.0f); // updated  
        
        // Left
        viGroup l11 = new viGroup(0.0f, 0.0f, 0.0f, 1.0f);
        viGroup l12 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup l13 = new viGroup(0.0f, 0.0f, vs, 1.0f);
        
        viGroup l11f = new viGroup( -nl, -nl, -nl, 1.0f); // updated
        viGroup l12f = new viGroup( -nl,  nl, -nl, 1.0f); // updated
        viGroup l13f = new viGroup( -nl, -nl,  nl, 1.0f); // updated
       
        viGroup l21 = new viGroup(0.0f, vs, vs, 1.0f);
        viGroup l22 = new viGroup(0.0f, vs, 0.0f, 1.0f);
        viGroup l23 = new viGroup(0.0f, 0.0f, vs, 1.0f);
       
        viGroup l21f = new viGroup( -nl, nl,  nl, 1.0f); // updated
        viGroup l22f = new viGroup( -nl, nl, -nl, 1.0f); // updated        
        viGroup l23f = new viGroup( -nl, -nl,  nl, 1.0f); // updated
        
         // Right
        viGroup r11 = new viGroup(vs, 0.0f, 0.0f, 1.0f);
        viGroup r12 = new viGroup(vs, vs, 0.0f, 1.0f);
        viGroup r13 = new viGroup(vs, 0.0f, vs, 1.0f);
        
        viGroup r11f = new viGroup(  nl, -nl, -nl, 1.0f); // updated
        viGroup r12f = new viGroup(  nl, nl, -nl, 1.0f); // updated
        viGroup r13f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
       
        viGroup r21 = new viGroup(vs, vs, vs, 1.0f);
        viGroup r22 = new viGroup(vs, vs, 0.0f, 1.0f);
        viGroup r23 = new viGroup(vs, 0.0f, vs, 1.0f);
       
        viGroup r21f = new viGroup(  nl,  nl,  nl, 1.0f); // updated
        viGroup r22f = new viGroup(  nl,  nl, -nl, 1.0f); // updated
        viGroup r23f = new viGroup(  nl, -nl,  nl, 1.0f); // updated
        
        // Vertices - total of 36
        
        vList.add(b11);
        vList.add(b12);
        vList.add(b13);
        vList.add(b21);
        vList.add(b22);
        vList.add(b23);
        
        vList.add(t11);
        vList.add(t12);
        vList.add(t13);
        vList.add(t21);
        vList.add(t22);
        vList.add(t23);
        
        vList.add(ba11);
        vList.add(ba12);
        vList.add(ba13);
        vList.add(ba21);
        vList.add(ba22);
        vList.add(ba23);
        
        vList.add(f11);
        vList.add(f12);
        vList.add(f13);
        vList.add(f21);
        vList.add(f22);
        vList.add(f23);
        
        vList.add(l11);
        vList.add(l12);
        vList.add(l13);
        vList.add(l21);
        vList.add(l22);
        vList.add(l23);
        
        vList.add(r11);
        vList.add(r12);
        vList.add(r13);
        vList.add(r21);
        vList.add(r22);
        vList.add(r23);
        
        // Add offset to vertices 
        for (viGroup v : vList) {
            v.x+=vg.x;
            v.y+=vg.y;
            v.z+=vg.z;
        }
        
        // Normals
        
        vList.add(b11f);
        vList.add(b12f);
        vList.add(b13f);
        vList.add(b21f);
        vList.add(b22f);
        vList.add(b23f);
        
        vList.add(t11f);
        vList.add(t12f);
        vList.add(t13f);
        vList.add(t21f);
        vList.add(t22f);
        vList.add(t23f);
        
        vList.add(ba11f);
        vList.add(ba12f);
        vList.add(ba13f);
        vList.add(ba21f);
        vList.add(ba22f);
        vList.add(ba23f);
        
        vList.add(f11f);
        vList.add(f12f);
        vList.add(f13f);
        vList.add(f21f);
        vList.add(f22f);
        vList.add(f23f);
        
        vList.add(l11f);
        vList.add(l12f);
        vList.add(l13f);
        vList.add(l21f);
        vList.add(l22f);
        vList.add(l23f);
        
        vList.add(r11f);
        vList.add(r12f);
        vList.add(r13f);
        vList.add(r21f);
        vList.add(r22f);
        vList.add(r23f);
        
        return vList;      
    }
    
    @Override
    public void display(GL4 gl) {
        super.display(gl);
        
        gl.glDisable(GL4.GL_DEPTH_TEST);

//        gl.glDisable(GL4.GL_DEPTH_TEST);
////        gl.glShadeModel(GL4.GL_SMOOTH);
////        gl.glDisable(GL4.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);
        
        checkGlError(gl, "d super.display() error");
        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "d glBindVertexArray error");
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
        checkGlError(gl, "d glBindBuffer error");
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        //gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, viList.size() * 108 * 4);
        //gl.glVertexAttribPointer(2, 4, GL4.GL_FLOAT, false, 0, viList.size() * 216 * 4);
        checkGlError(gl, "d glVertexAttribPointer error");
        gl.glEnableVertexAttribArray(0);
        checkGlError(gl, "d glEnableVertexAttribArray 0 error");
        //gl.glEnableVertexAttribArray(1);
        //checkGlError(gl, "d glEnableVertexAttribArray 1 error");
        //gl.glEnableVertexAttribArray(2);
        //checkGlError(gl, "d glEnableVertexAttribArray 2 error");        
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, viList.size() * 36);
        checkGlError(gl, "d glDrawArrays error");

//        gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glDisable(GL4.GL_BLEND);
    }

}
