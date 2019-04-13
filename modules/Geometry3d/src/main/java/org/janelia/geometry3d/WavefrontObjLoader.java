package org.janelia.geometry3d;

import java.awt.Color;
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
        
        // e.g. "# Compartment color: 0xFF7080"
        Pattern colorPattern = Pattern.compile("^.*\\bcolor\\b.*\\b(0x[0-9A-F]{6})\\b.*", Pattern.CASE_INSENSITIVE);
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
                Matcher colorMatcher = colorPattern.matcher(line);
                if (colorMatcher.matches()) {
                    String hexColor = colorMatcher.group(1);
                    Color color = Color.decode(hexColor);
                    if (color != null) {
                        result.setDefaultColor(color);
                    }
                }
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
