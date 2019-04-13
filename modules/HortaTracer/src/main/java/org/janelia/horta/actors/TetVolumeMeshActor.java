package org.janelia.horta.actors;

import com.jogamp.common.nio.Buffers;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.blocks.BlockTileResolution;
import org.janelia.horta.blocks.KtxOctreeResolution;
import org.janelia.horta.ktx.KtxData;

/**
 * TetVolumeMeshActor represents one volume rendered block, 
 * consisting of five tetrahedra.
 * Multiple TetVolumeMeshActors may reside in a higher level TetVolumeActor.
 * TetVolumeMeshActor is responsible for managing the material and geometry
 *  for one volume rendered block.
 * 
 * @author brunsc
 */
public class TetVolumeMeshActor extends MeshActor
implements SortableBlockActor, SortableBlockActorSource
{
    private final List<List<Integer>> outerTetrahedra = new ArrayList<>();
    private final List<Integer> centralTetrahedron = new ArrayList<>();
    private final KtxData ktxData;
    private Vector4 cachedCentroid;
    private BlockTileResolution cachedResolution;
    private final List<SortableBlockActor> listOfThis;
    
    public TetVolumeMeshActor(KtxData ktxData, TetVolumeActor parentActor) {
        super(new TetVolumeMeshGeometry(ktxData), new TetVolumeMaterial(ktxData, parentActor), parentActor);
        this.ktxData = ktxData;
        
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
        addOuterTetrahedron(0, 5, 3, 1); // upper right front
        final boolean showFullBlock = true; // false for easier debugging of non-blending issues
        if (showFullBlock) {
            addOuterTetrahedron(0, 6, 5, 4); // upper left rear
            setCentralTetrahedron(0, 3, 5, 6); // inner tetrahedron
            addOuterTetrahedron(3, 5, 6, 7); // lower right rear
            addOuterTetrahedron(0, 3, 6, 2); // lower left front
        }

        // TODO: alternate tetrahedralization - used for alternating subblocks in raw tiles.
        /** @TODO something */
        
        listOfThis = new ArrayList<>();
        listOfThis.add(this);
    }
    
    public final void addOuterTetrahedron(int a, int b, int c, int apex) {
        List<Integer> tet = new ArrayList<>();
        tet.add(a);
        tet.add(b);
        tet.add(c);
        tet.add(apex);
        outerTetrahedra.add(tet);
    }
    
    public final void setCentralTetrahedron(int a, int b, int c, int apex) {
        List<Integer> tet = centralTetrahedron;
        tet.clear();
        tet.add(a);
        tet.add(b);
        tet.add(c);
        tet.add(apex);     
    }
    
    @Override
    public void displayTriangleAdjacencies(GL3 gl) 
    {
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());

        if (vboTriangleAdjacencyIndices == 0)
            initTriangleAdjacencyIndices(gl);
        
        // All three passes now in one index buffer
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, triangleAdjacencyIndexCount, GL3.GL_UNSIGNED_INT, 0);

        vertexBufferObject.unbind(gl);
    }

    @Override
    protected void initTriangleAdjacencyIndices(GL3 gl) 
    {
        // Outer tetrahedra
        if ((vboTriangleAdjacencyIndices == 0) && (outerTetrahedra.size() > 0))
        {
            // Conceptually there are three render passes, to get the five tetrahedra comprising a block to render
            // in painter's algorithm order:
            //  1) all tetrahedra BEHIND the central tetrahedron
            //  2) the central tetrahedron
            //  3) all tetrahedra IN FRONT of the central tetrahedron
            // We rely on on the geometry shader to reject tetrahedra that do not qualify for passes 1 & 3.
            // We send two copies of the central tetrahedron in the middle, so the shader will always accept one of them.
            int tetCount = 2 * outerTetrahedra.size(); // front and back versions of each tetrahedron
            if (centralTetrahedron.size() > 0)
                tetCount += 2;
            triangleAdjacencyIndexCount = 6 * tetCount;
            IntBuffer indices = Buffers.newDirectIntBuffer(triangleAdjacencyIndexCount); // for first render pass
            
            // For first render pass, ordinary tetrahedra
            for (List<Integer> tet : outerTetrahedra) {
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Forward
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(b); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(c); // abuse elements 3&5 to encode front-ness
            }

            // For second render pass, include two copies of central tetrahedron
            if (centralTetrahedron.size() > 0)
            {
                List<Integer> tet = centralTetrahedron;
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Central Tetrahedron gets both forward and reverse forms stored,
                // so it will always get drawn exactly once, after culling.
                // 1) forward version
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(b); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                // 2) reverse version
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(b); // abuse elements 3&5 to encode front-ness          
            }
            
            // For third render pass, tetrahedra with partially flipped base triangle
            for (List<Integer> tet : outerTetrahedra) {
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Inverted
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(b); // abuse elements 3&5 to encode front-ness
            }

            indices.flip();
            IntBuffer vbos = IntBuffer.allocate(1);
            vbos.rewind();
            gl.glGenBuffers(1, vbos);
            vboTriangleAdjacencyIndices = vbos.get(0);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
            gl.glBufferData(
                    GL3.GL_ELEMENT_ARRAY_BUFFER,
                    indices.capacity() * Buffers.SIZEOF_INT,
                    indices,
                    GL3.GL_STATIC_DRAW);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public Vector4 getHomogeneousCentroid() {
        if (cachedCentroid == null) {
            String s = ktxData.header.keyValueMetadata.get("bounding_sphere_center");
            final String np = "([-+0-9.e]+)"; // regular expression for parsing and capturing one number from the matrix
            final String rp = "\\[\\s*"+np+"\\s+"+np+"\\s+"+np+"\\s*\\]"; // regex for parsing one matrix row
            Pattern p = Pattern.compile("^"+rp+".*$", Pattern.DOTALL);
            Matcher m = p.matcher(s);
            boolean b = m.matches();
            cachedCentroid = new Vector4(
                    Float.parseFloat(m.group(1)),
                    Float.parseFloat(m.group(2)),
                    Float.parseFloat(m.group(3)),
                    1.0f);
        }
        return cachedCentroid;
    }

    @Override
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return listOfThis;
    }

    @Override
    public BlockTileResolution getResolution() {
        if (cachedResolution == null) {
            int res = Integer.parseInt(ktxData.header.keyValueMetadata.get("multiscale_level_id").trim()) - 1;
            cachedResolution = new KtxOctreeResolution(res);
        }
        return cachedResolution;
    }

    private static class TetVolumeMeshGeometry extends MeshGeometry {

        public TetVolumeMeshGeometry(KtxData ktxData)
        {
            // Parse spatial transformation matrix from block metadata
            String xformString = ktxData.header.keyValueMetadata.get("xyz_from_texcoord_xform");
            // [[  1.05224424e+04   0.00000000e+00   0.00000000e+00   7.27855312e+04]  [  0.00000000e+00   7.26326904e+03   0.00000000e+00   4.04875508e+04]  [  0.00000000e+00   0.00000000e+00   1.12891562e+04   1.78165703e+04]  [  0.00000000e+00   0.00000000e+00   0.00000000e+00   1.00000000e+00]]
            final String np = "([-+0-9.e]+)"; // regular expression for parsing and capturing one number from the matrix
            final String rp = "\\[\\s*"+np+"\\s+"+np+"\\s+"+np+"\\s+"+np+"\\s*\\]"; // regex for parsing one matrix row
            final String mp = "\\["+rp+"\\s*"+rp+"\\s*"+rp+"\\s*"+rp+"\\s*\\]"; // regex for entire matrix
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
                        add(vertex);
                        // logger.info(v.toString());
                    }
                }
            }
        }
    }

}
