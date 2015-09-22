package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.SparseVolumeRenderable;
import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 7/31/2015.
 */
public class VoxelViewerUtil {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewerUtil.class);

//    public static void doSystemCmd(String commandString) {
//        Properties scProps=new Properties();
//        scProps.setProperty(SystemCall.SHELL_PATH_PROP, "cmd /c");
//        SystemCall sc = new SystemCall(scProps, new File("/tmp/scratch"), null);
//        sc.setDeleteExecTmpFile(false);
//        try {
//            sc.execute(commandString, false);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }

    public static VoxelViewer4DImage createVoxelImageFromStack(File stack) throws Exception {
        FileResolver resolver = new TrivialFileResolver();
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        volumeLoader.loadVolume(stack.getAbsolutePath());
        VoxelViewer4DImage image=new VoxelViewer4DImage();
        volumeLoader.populateVolumeAcceptor(image);
        return image;
    }

    public static void initRenderableFromMaskFile(SparseVolumeRenderable sparseVolumeRenderable, File maskFile) throws Exception {

        // The input data is known to be little-endian or LSB.
        byte[] longArray = new byte[ 8 ];
        byte[] floatArray = new byte[ 4 ];

        ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer floatBuffer = ByteBuffer.wrap( floatArray );
        floatBuffer.order(ByteOrder.LITTLE_ENDIAN);

        List<Vector4> points=new ArrayList<>();
        InputStream dataStream=new FileInputStream(maskFile);

        long xdim=readLong(dataStream, longBuffer);
        long ydim=readLong(dataStream, longBuffer);
        long zdim=readLong(dataStream, longBuffer);

        float xmicrons=readFloat(dataStream, floatBuffer);
        float ymicrons=readFloat(dataStream, floatBuffer);
        float zmicrons=readFloat(dataStream, floatBuffer);

        // Bounding box
        long x0 = readLong(dataStream, longBuffer);
        long x1 = readLong(dataStream, longBuffer);
        long y0 = readLong(dataStream, longBuffer);
        long y1 = readLong(dataStream, longBuffer);
        long z0 = readLong(dataStream, longBuffer);
        long z1 = readLong(dataStream, longBuffer);

        long totalVoxels = readLong(dataStream, longBuffer);

        byte axis=readByte(dataStream);

        long planePosition=0L;
        long readVoxels=0L;

        while(readVoxels<totalVoxels) {
            long skip = readLong(dataStream, longBuffer);
            planePosition+=skip;
            long pairs = readLong(dataStream, longBuffer);
            for (long p=0;p<pairs;p++) {
                long start = readLong(dataStream, longBuffer);
                long end = readLong(dataStream, longBuffer);
                if (axis==0) { // yz(x)
                    long y = planePosition / zdim;
                    long z = planePosition - (zdim * y);
                    for (long x = start; x < end; x++) {
                        points.add(new Vector4(x * 1.0f, y * 1.0f, z * 1.0f, 1.0f));
                    }
                } else if (axis==1) { // xz(y)
                    long x = planePosition / zdim;
                    long z = planePosition - (zdim * x);
                    for (long y = start; y < end; y++) {
                        points.add(new Vector4(x * 1.0f, y * 1.0f, z * 1.0f, 1.0f));
                    }
                } else if (axis==2) { // xy(z)
                    long x = planePosition / ydim;
                    long y = planePosition - (ydim * x);
                    for (long z = start; z < end; z++) {
                        points.add(new Vector4(x * 1.0f, y * 1.0f, z * 1.0f, 1.0f));
                    }
                }
                readVoxels+=(end-start);
            }
            planePosition++;
        }

        dataStream.close();

        sparseVolumeRenderable.init( (int)xdim, (int)ydim, (int)zdim, ((float)(1.0/(1.0*xdim))), points);

    }
    
    private static byte readByte(InputStream is) throws Exception {
        return (byte)is.read();
    }
    
    private static long readLong(InputStream is, ByteBuffer longBuffer) throws Exception {
        is.read(longBuffer.array());
        longBuffer.rewind();
        return longBuffer.getLong();
    }
    
    private static float readFloat(InputStream is, ByteBuffer floatBuffer) throws Exception {
        is.read(floatBuffer.array());
        floatBuffer.rewind();
        return floatBuffer.getFloat();
    }


}
