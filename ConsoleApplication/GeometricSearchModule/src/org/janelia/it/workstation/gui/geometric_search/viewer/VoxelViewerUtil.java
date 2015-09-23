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
import java.nio.ShortBuffer;
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

    public static void initRenderableFromMaskFile(SparseVolumeRenderable sparseVolumeRenderable, File maskFile, File chanFile) throws Exception {

        // The input data is known to be little-endian or LSB.
        byte[] longArray = new byte[ 8 ];
        byte[] floatArray = new byte[ 4 ];
        byte[] shortArray = new byte[ 2 ];

        ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer floatBuffer = ByteBuffer.wrap( floatArray );
        floatBuffer.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer shortBuffer = ByteBuffer.wrap( shortArray );
        floatBuffer.order(ByteOrder.LITTLE_ENDIAN);

        short[] data8Arr=null;
        int[] data16Arr=null;

        int cdim=0;
        int chanVoxels=0;
        byte chanBytesPerChannel=0;

        List<Vector4> points=new ArrayList<>();
        InputStream maskStream=new FileInputStream(maskFile);
        if (chanFile!=null) {
            InputStream chanStream=new FileInputStream(chanFile);
            chanVoxels=(int)readLong(chanStream, longBuffer);
            cdim=readByte(chanStream);
            byte chanRecRed=readByte(chanStream);
            byte chanRecGreen=readByte(chanStream);
            byte chanRecBlue=readByte(chanStream);
            chanBytesPerChannel=readByte(chanStream);
            int chanTotalDataBytes=chanBytesPerChannel * chanVoxels * cdim;
            byte[] chanData = new byte[(int)chanTotalDataBytes];
            chanStream.read(chanData);
            if (chanBytesPerChannel==1) {
                ByteBuffer data8=ByteBuffer.wrap(chanData);
                data8Arr=new short[chanTotalDataBytes];
                for (int i=0;i<chanTotalDataBytes;i++) {
                    short s=data8.get(i);
                    if (s<0) s=(short)(256 + s);
                    data8Arr[i]=s;
                }
            } else if (chanBytesPerChannel==2) {
                ByteBuffer data16=ByteBuffer.wrap(chanData);
                data16Arr=new int[chanVoxels*cdim];
                for (int i=0;i<data16Arr.length;i++) {
                    int si=data16.getShort(i*2);
                    if (si<0) si=65536+si;
                    data16Arr[i]=si;
                }
            }
            chanStream.close();
        }

        long xdim=readLong(maskStream, longBuffer);
        long ydim=readLong(maskStream, longBuffer);
        long zdim=readLong(maskStream, longBuffer);

        float voxelUnitSize=1.0f/xdim;

        float xmicrons=readFloat(maskStream, floatBuffer);
        float ymicrons=readFloat(maskStream, floatBuffer);
        float zmicrons=readFloat(maskStream, floatBuffer);

        // Bounding box
        long x0 = readLong(maskStream, longBuffer);
        long x1 = readLong(maskStream, longBuffer);
        long y0 = readLong(maskStream, longBuffer);
        long y1 = readLong(maskStream, longBuffer);
        long z0 = readLong(maskStream, longBuffer);
        long z1 = readLong(maskStream, longBuffer);

        long totalVoxels = readLong(maskStream, longBuffer);

        byte axis=readByte(maskStream);

        long planePosition=0L;
        long readVoxels=0L;

        float cdimf=1.0f*cdim;

        while(readVoxels<totalVoxels) {
            long skip = readLong(maskStream, longBuffer);
            planePosition+=skip;
            long pairs = readLong(maskStream, longBuffer);
            for (long p=0;p<pairs;p++) {
                long start = readLong(maskStream, longBuffer);
                long end = readLong(maskStream, longBuffer);
                if (axis==0) { // yz(x)
                    long y = planePosition / zdim;
                    long z = planePosition - (zdim * y);
                    for (long x = start; x < end; x++) {

                        float iv=1.0f;
                        if (data8Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data8Arr[(int) (readVoxels + (x - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*255.0f);
                        } else if (data16Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data16Arr[(int) (readVoxels + (x - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*4096.0f);
                        }

                        points.add(new Vector4(x * voxelUnitSize, y * voxelUnitSize, z * voxelUnitSize, iv));

                    }
                } else if (axis==1) { // xz(y)
                    long x = planePosition / zdim;
                    long z = planePosition - (zdim * x);
                    for (long y = start; y < end; y++) {

                        float iv=1.0f;
                        if (data8Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data8Arr[(int) (readVoxels + (y - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*255.0f);
                        } else if (data16Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data16Arr[(int) (readVoxels + (y - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*4096.0f);
                        }

                        points.add(new Vector4(x * voxelUnitSize, y * voxelUnitSize, z * voxelUnitSize, iv));

                    }
                } else if (axis==2) { // xy(z)
                    long x = planePosition / ydim;
                    long y = planePosition - (ydim * x);
                    for (long z = start; z < end; z++) {

                        float iv=1.0f;
                        if (data8Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data8Arr[(int) (readVoxels + (z - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*255.0f);
                        } else if (data16Arr!=null) {
                            int iTotal=0;
                            for (int c=0;c<cdim;c++) {
                                iTotal += data16Arr[(int) (readVoxels + (z - start) + c * chanVoxels)];
                            }
                            iv=iTotal*1.0f/(cdimf*4096.0f);
                        }

                        points.add(new Vector4(x * voxelUnitSize, y * voxelUnitSize, z * voxelUnitSize, iv));

                    }
                }
                readVoxels+=(end-start);
            }
            planePosition++;
        }

        maskStream.close();

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
