/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.ktx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.TetVolumeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class KtxData 
{
    public final KtxHeader header = new KtxHeader();
    public final List<ByteBuffer> mipmaps = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public KtxData loadStream(InputStream stream) throws IOException {
        header.loadStream(stream);
        mipmaps.clear();
        byte[] unused = new byte[4]; // for bulk reading of unused padding bytes
        ByteBuffer sizeBuf = ByteBuffer.allocate(4); // to hold binary representation of image size
        sizeBuf.order(header.byteOrder);
        for (int m = 0; m < header.numberOfMipmapLevels; ++m) {
            stream.read(sizeBuf.array());
            sizeBuf.rewind();
            int imageSize = (int)((long)sizeBuf.getInt() & 0xffffffffL);
            // TODO: figure out how to stream straight into the direct buffer
            // For now, copy into the direct buffer
            byte[] b = new byte[imageSize];
            int bytesRead = 0;
            bytesRead = stream.read(b, bytesRead, imageSize - bytesRead);
            while (bytesRead < imageSize) {
                int moreBytes = stream.read(b, bytesRead, imageSize - bytesRead);
                if (moreBytes < 1) {
                    throw new IOException("Error reading mipmap number "+m);
                }
                bytesRead += moreBytes;
            }
            if (bytesRead != imageSize) {
                throw new IOException("Error reading mipmap number "+m);
            }
            // Use a DIRECT buffer for later efficient slurping into OpenGL
            final boolean useDirect = true;
            ByteBuffer mipmap;
            if (useDirect) {
                mipmap = ByteBuffer.allocateDirect(imageSize);
                mipmap.put(b);
            } else {
                mipmap = ByteBuffer.wrap(b);
            }
            mipmaps.add(mipmap);
            int padding = 3 - ((imageSize + 3) % 4);
            stream.read(unused, 0, padding);
        }
        return this;
    }

    public GL3Actor createActor(ImageColorModel brightnessModel) 
    {
        // Parse spatial transformation matrix from block metadata
        String xformString = header.keyValueMetadata.get("xyz_from_texcoord_xform");
        // [[  1.05224424e+04   0.00000000e+00   0.00000000e+00   7.27855312e+04]  [  0.00000000e+00   7.26326904e+03   0.00000000e+00   4.04875508e+04]  [  0.00000000e+00   0.00000000e+00   1.12891562e+04   1.78165703e+04]  [  0.00000000e+00   0.00000000e+00   0.00000000e+00   1.00000000e+00]]
        String np = "([-+0-9.e]+)"; // regular expression for parsing and capturing one number from the matrix
        String rp = "\\[\\s*"+np+"\\s+"+np+"\\s+"+np+"\\s+"+np+"\\s*\\]"; // regex for parsing one matrix row
        String mp = "\\["+rp+"\\s*"+rp+"\\s*"+rp+"\\s*"+rp+"\\s*\\]"; // regex for entire matrix
        Pattern p = Pattern.compile("^"+mp+".*$", Pattern.DOTALL);
        Matcher m = p.matcher(xformString);
        boolean b = m.matches();
        double[][] m1 = new double[4][4];
        int n = m.groupCount();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                m1[i][j] = Double.parseDouble(m.group(4*i+j+1));
            }
        }
        Jama.Matrix mat = new Jama.Matrix(m1);
        
        // Create mesh geometry
        MeshGeometry meshGeometry = new MeshGeometry();
        // Loop over texture coordinate extremes
        float[] tt = {0.0f, 1.0f};
        for (float tz : tt) {
            for (float ty : tt) {
                for (float tx :tt) {
                    Jama.Matrix texCoord = new Jama.Matrix(new double[]{tx, ty, tz, 1.0}, 1);
                    Jama.Matrix xyz = mat.times(texCoord.transpose());
                    ConstVector3 v = new Vector3((float)xyz.get(0,0), (float)xyz.get(1,0), (float)xyz.get(2,0));
                    ConstVector3 t = new Vector3((float)texCoord.get(0,0), (float)texCoord.get(0,1), (float)texCoord.get(0,2));
                    Vertex vertex = new Vertex(v);
                    vertex.setAttribute("texCoord", t);
                    meshGeometry.add(vertex);
                    // logger.info(v.toString());
                }
            }
        }
        
        TetVolumeActor actor = new TetVolumeActor(this, meshGeometry, brightnessModel);
        
        /*
                4___________5                  
                /|         /|             These are texture coordinate axes,
               / |        / |             not world axes.
             0/_________1/  |                   z
              | 6|_______|__|7                 /
              |  /       |  /                 /
              | /        | /                 |---->X
              |/_________|/                  |
              2          3                   | 
                                             v
                                             Y
        */

        // Compose the brick from five tetrahedra
        actor.addOuterTetrahedron(0, 5, 3, 1); // upper right front
        final boolean showFullBlock = false; // false for easier debugging of non-blending issues
        if (showFullBlock) {
            actor.addOuterTetrahedron(0, 6, 5, 4); // upper left rear
            actor.setCentralTetrahedron(0, 3, 5, 6); // inner tetrahedron
            actor.addOuterTetrahedron(3, 5, 6, 7); // lower right rear
            actor.addOuterTetrahedron(0, 3, 6, 2); // lower left front
        }

        // TODO: alternate tetrahedralization - used for alternating subblocks in raw tiles.
        /** @TODO something */
        
        return actor;
    }
}
