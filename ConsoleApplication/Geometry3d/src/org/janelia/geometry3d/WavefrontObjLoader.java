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

package org.janelia.geometry3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author brunsc
 */
public class WavefrontObjLoader {
    static public MeshGeometry load(InputStream objFile) throws IOException 
    {
        MeshGeometry result = new MeshGeometry();

        Pattern commentPattern = Pattern.compile("^\\s*#.*");
        Pattern vertexPattern = Pattern.compile("^\\s*v\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\S+))?.*");
        Pattern normalPattern = Pattern.compile("^\\s*vn\\s+(\\S+)\\s+(\\S+)\\s+(\\S+).*");
        Pattern facePattern = Pattern.compile("^\\s*f((?:\\s+[0-9/]+)+).*");
        Pattern faceVertexPattern = Pattern.compile("\\s*([0-9]+)(?:/([0-9]*))?(?:/([0-9]*))?");
        
        List<Vector3> vertexNormals = new ArrayList<>();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(objFile));
        String line;
        Matcher matcher;
        while((line = in.readLine()) != null) {
            
            matcher = vertexPattern.matcher(line);
            if (matcher.matches()) {  
                float x = Float.parseFloat(matcher.group(1));
                float y = Float.parseFloat(matcher.group(2));
                float z = Float.parseFloat(matcher.group(3));
                float w = 1.0f;
                String ws = matcher.group(4);
                if ( (ws != null) && (ws.length() > 0) ) {
                    w = Float.parseFloat(ws);
                }
                result.addVertex(x/w, y/w, z/w);
                continue;
            }
            
            matcher = commentPattern.matcher(line);
            if (matcher.matches()) {
                String g1 = matcher.group(0);
                continue; // skip comment lines
            }
            
            matcher = normalPattern.matcher(line);
            if (matcher.matches()) {  
                float x = Float.parseFloat(matcher.group(1));
                float y = Float.parseFloat(matcher.group(2));
                float z = Float.parseFloat(matcher.group(3));
                vertexNormals.add(new Vector3(x, y, z));
                continue;
            }

            matcher = facePattern.matcher(line);
            if (matcher.matches()) {
                String whole = matcher.group(0);
                
                List<Integer> faceVertices = new ArrayList<>();
                
                String vertices = matcher.group(1);
                matcher = faceVertexPattern.matcher(vertices);
                while (matcher.find()) {
                    int vertexIx = Integer.parseInt(matcher.group(1)) - 1;
                    faceVertices.add(vertexIx);
                    String texCoord = matcher.group(2);
                    String normal = matcher.group(3); // TODO:
                }
                assert(faceVertices.size() >= 3);
                Integer[] f0 = new Integer[faceVertices.size()];
                faceVertices.toArray(f0);
                int[] face = ArrayUtils.toPrimitive(f0);
                result.addFace(face);
                continue;
            }
            
        }
        
        // TODO: handle vertex normals
        
        
        return result;
    }
}
