package org.janelia.it.workstation.gui.opengl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.it.workstation.geom.UnitVec3;

public class PolygonalMesh {
    private List<Face> faces = new Vector<Face>();
    private List<Vertex> vertexes = new Vector<Vertex>();

    /**
     * Factory method
     * @param objFileStream
     * @return
     * @throws IOException 
     */
    static public PolygonalMesh createMeshFromObjFile(InputStream objFileStream) 
            throws IOException 
    {
        PolygonalMesh mesh = new PolygonalMesh();
        mesh.loadFromObjFile(objFileStream);
        return mesh;
    }
    
    protected PolygonalMesh() {}

    /**
     * For "faceted" rendering
     */
    public void computeFaceNormals() {
        for (PolygonalMesh.Face face : getFaces()) {
            if (face.vertexIndexes.size() < 3)
                continue;
            if (face.computedNormal != null)
                continue;
            // 1) collect first three vertices as Vec3
            org.janelia.it.workstation.geom.Vec3[] nv = new org.janelia.it.workstation.geom.Vec3[3];
            for (int i = 0; i < 3; ++i) {
                int v = face.vertexIndexes.get(i);
                PolygonalMesh.Vertex vtx = getVertexes().get(v-1);
                nv[i] = new org.janelia.it.workstation.geom.Vec3(vtx.getX(), vtx.getY(), vtx.getZ()).times(1.0/vtx.getW());
            }
            face.computedNormal = new UnitVec3(nv[2].minus(nv[1]).cross(nv[0].minus(nv[1])));
        }        
    }
    
    /**
     * For "smooth" rendering.
     */
    public void computeVertexNormals() {
        // Average all face normals for each vertex
        int sv = this.vertexes.size();
        org.janelia.it.workstation.geom.Vec3[] vn = new org.janelia.it.workstation.geom.Vec3[sv];
        // Initialize to zero
        org.janelia.it.workstation.geom.Vec3 zeroVec3 = new org.janelia.it.workstation.geom.Vec3(0,0,0);
        for (int i = 0; i < sv; ++i)
            vn[i] = zeroVec3;
        // Average all faces vertex participates in
        computeFaceNormals();
        for (PolygonalMesh.Face face : getFaces()) {
            if (face.computedNormal == null)
                continue;
            for (int v : face.vertexIndexes)
                vn[v-1] = vn[v-1].plus(face.computedNormal);
        }
        // Store result
        for (int i = 0; i < sv; ++i) {
            if (vn[i].equals(zeroVec3))
                continue;
            Vertex vtx = vertexes.get(i);
            vtx.computedNormal = new UnitVec3(vn[i]);
        }
    }
    
    public List<Face> getFaces() {
        return faces;
    }

    public List<Vertex> getVertexes() {
        return vertexes;
    }

    /**
     * Load mesh from a Wavefront .obj file
     * @param inputFile
     * @throws IOException 
     */
    protected void loadFromObjFile(InputStream inputFile) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputFile));
        String line;
        
        // Regular expressions for parsing
        // http://en.wikipedia.org/wiki/Wavefront_.obj_file
        // 
        // List of Vertices, with (x,y,z[,w]) coordinates, w is optional and defaults to 1.0.
        // v 0.123 0.234 0.345 1.0
        String numberPattern = "[-+]?[0-9\\.]+(?:[eE][-+]?\\d+)?";
        Pattern vertexPattern = Pattern.compile(
                "^\\s*v\\s+" // "v"
                +"("+numberPattern+")" // x
                +"\\s+("+numberPattern+")" // y
                +"\\s+("+numberPattern+")" // z
                +"(?:\\s+("+numberPattern+"))?" // optional w
                +"\\s*$");
        
        Pattern facePattern = Pattern.compile(
                "^\\s*f" // "f"
                +"((?:\\s+\\d+)+)" // series of space delimited integers
                +"\\s*$");

        while ((line = br.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#"))
                continue; // comment line
            Matcher vertexMatcher = vertexPattern.matcher(trimmedLine);
            if (vertexMatcher.matches()) {
                double x = Double.parseDouble(vertexMatcher.group(1));
                double y = Double.parseDouble(vertexMatcher.group(2));
                double z = Double.parseDouble(vertexMatcher.group(3));
                double w = 1.0; // default value
                if (vertexMatcher.groupCount() > 3) {
                    String match4 = vertexMatcher.group(4);
                    if ((match4 != null) && (! match4.isEmpty()))
                        w = Double.parseDouble(vertexMatcher.group(4));
                }
                vertexes.add(new Vertex(x, y, z, w));
                continue;
            }
            Matcher faceMatcher = facePattern.matcher(trimmedLine);
            if (faceMatcher.matches()) {
                Face face = new Face();
                String[] ixs = faceMatcher.group(1).split("\\s+");
                for (String s : ixs) {
                    if (s.isEmpty())
                        continue;
                    int v = Integer.parseInt(s);
                    face.vertexIndexes.add(v);
                }
                faces.add(face);
            }
            // TODO - parse other types lines
        }
        br.close();
    }
    
    public static class Vertex {
        private double x;
        private double y;
        private double z;
        private double w;
        public UnitVec3 computedNormal = null;

        public Vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = 1.0;
        }
        
        public Vertex(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getW() {
            return w;
        }
    }
    
    public static class Face {
        public List<Integer> vertexIndexes = new Vector<Integer>();
        public UnitVec3 computedNormal = null;
    }
}
