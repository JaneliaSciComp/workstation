package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 5/31/13
 * Time: 10:03 AM
 *
 * This minimal-footprint widget exists only so it can be queried for information off the graphics card.
 */
public class GpuSampler implements GLEventListener {
    public static final String UNKNOWN_VALUE = "Unknown";
    public static final String STANDARD_CARD_RENDERER_STR = "GeForce GTX 680";

    // See http://developer.download.nvidia.com/opengl/specs/GL_NVX_gpu_memory_info.txt
    private static final int GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX = 0x9049;   // NVidia
    private static final int TEXTURE_FREE_MEMORY_ATI = 0x87FC;                        // Radeon

    private static final int NO_ESTIMATE = 0;
    private static final int WAIT_TIME_MS = 1000;
    private static final int MAX_WAIT_LOOPS = 10 * 1000 / WAIT_TIME_MS; // Up to this many seconds.

    private Logger logger = LoggerFactory.getLogger( GpuSampler.class );

    private int freeTexMem = 0;
    private String highestSupportedGlsl = "";
    private String venderIdString = null;
    private AtomicBoolean isInitialized = new AtomicBoolean( false );
    private Color camoColor;
    private static final char GUI_ID_SEPARATOR = '#';

    public GpuSampler( Color camoColor ) {
        this.camoColor = camoColor;
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GL2 gl2 = glAutoDrawable.getGL().getGL2();
        gl2.glClearColor( camoColor.getRed() / 255.0f, camoColor.getGreen() / 255.0f, camoColor.getBlue() / 255.0f, 1.0f );
        gl2.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT );

        freeTexMem = getFreeTextureMemory(gl2);
        highestSupportedGlsl = gl2.glGetString( GL2.GL_SHADING_LANGUAGE_VERSION );
        venderIdString = getGpuIdString( gl2 );
logger.info( "Vender id {} found in init method.", venderIdString );

        isInitialized.set(true);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i2, int i3, int i4) {
    }

    /** Treats input lines containing the key string, from any like source. */
    public static boolean isDeptStandardGpu(String gpuRendererString) {
        boolean rtnVal = false;
        if ( gpuRendererString != null ) {
            String[] allLines = gpuRendererString.split( "\n" );
            for ( String line: allLines ) {
                if ( line.startsWith( "OpenGL" ) ) {
                    rtnVal = line.trim().contains( STANDARD_CARD_RENDERER_STR );
                    if ( rtnVal ) {
                        break;
                    }
                }
            }
        }

        return rtnVal;
    }

    public static GpuInfo parseGpuID( String idString ) {
        GpuInfo id = new GpuInfo();
        if ( idString == null )
            return null;
        String[] parts = idString.split( "" + GUI_ID_SEPARATOR );
        if ( parts == null || parts.length < 3 )
            return null;
        id.renderer = parts[ 1 ];
        id.vender = parts[ 0 ];
        id.version = parts[ 2 ];

        return id;
    }

    /**
     * Get a future-handle on the info from the GPU.
     *
     * @return all relevant info from GPU.
     */
    public GpuInfo getGpuInfo() throws Exception {
        FutureTask<GpuInfo> future =
                new FutureTask<GpuInfo>(new Callable<GpuInfo>() {
                    public GpuInfo call() {
                        int numLoops = 0;
                        while ( ! isInitialized.get() ) {
logger.info("On loop {}.  Got value {}.", numLoops, venderIdString);
                            try {
                                Thread.sleep( WAIT_TIME_MS );
                                numLoops ++;
                                if ( loopMaxTest( numLoops ) ) {
                                    return null;
                                }
                                logger.debug("Wait loop iteration {}.", numLoops );
                            } catch ( Exception ex ) {
                                logger.error( "Failed to obtain gpu id.  Returning null.");
                                ex.printStackTrace();
                                return null;
                            }
                        }
                        GpuInfo info = parseGpuID( venderIdString );
                        info.setFreeTexMem( freeTexMem );
                        info.setHighestGlslVersion( highestSupportedGlsl );
                        return info;
                    }
                });
        ExecutorService executor = Executors.newFixedThreadPool( 1 );
        executor.execute( future );
        return future.get( 2, TimeUnit.MINUTES );
    }

    /**
     * This may be called _after_ getEstimatedTextureMemory has failed.  It is specific to MacIntosh operating systems. It
     * leverages a program expected to be in a standard location, etc.  The string returned is also expected to match
     * what is seen when this program is queried.
     *
     * @see #getEstimatedTextureMemory
     * @return can call get on the returned object to tell if this is standard or not.
     */
    public Future<Boolean> isDepartmentStandardGraphicsMac() {
        FutureTask<Boolean> future =
                new FutureTask<Boolean>( new Callable<Boolean>() {
                    public Boolean call() {
                        boolean rtnVal = false;
                        try {
                            String cmd = "/opt/x11/bin/glxinfo";
                            Process proc = Runtime.getRuntime().exec( cmd );
                            BufferedReader progReader = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
                            BufferedReader errReader = new BufferedReader( new InputStreamReader( proc.getErrorStream() ) );

                            ProgOutputThread progReaderThread = new ProgOutputThread( progReader );
                            ProgOutputThread errReaderThread = new ProgOutputThread( errReader );

                            progReaderThread.start();
                            errReaderThread.start();

                            int status = proc.waitFor();

                            progReaderThread.join();
                            errReaderThread.join();

                            String programOutput = progReaderThread.getResult();
                            String programError = errReaderThread.getResult();

                            // Could have failed with status...
                            if ( status != 0 ) {
                                throw new Exception("Failed with error " + status);
                            }
                            else {
                                if ( programError != null  &&  programError.trim().length() > 0 ) {
                                    throw new Exception( programError );
                                }
                            }

                            // Finally, parse the output.
                            rtnVal = isDeptStandardGpu(programOutput);

                        } catch ( Exception ex ) {
                            ex.printStackTrace();
                            logger.warn("Failed to check whether the graphics card is the department standard.  Returning false.");
                        }

                        return rtnVal;
                    }
                });
        ExecutorService executor = Executors.newFixedThreadPool( 1 );
        executor.execute( future );
        return future;
    }

    private boolean loopMaxTest(int numLoops) {
        if ( numLoops > MAX_WAIT_LOOPS ) {
            logger.warn( "Exceeded max wait loops {} to estimate texture memory.  Returning the not-found value.", MAX_WAIT_LOOPS );
            return true;
        }
        return false;
    }

    /** Processor thread to collect all output from stdout or sterr. */
    private class ProgOutputThread extends Thread {
        private StringBuilder builder;
        private BufferedReader br;
        private Exception ex;

        public ProgOutputThread( BufferedReader br ) {
            this.br = br;
            builder = new StringBuilder();
        }

        @Override
        public void run() {
            try {
                String nextLine = null;
                while ( null != ( nextLine = br.readLine() ) ) {
                    builder.append( nextLine ).append("\n");
                }
            } catch ( Exception ex ) {
                this.ex = ex;
            }
        }

        public String getResult() throws Exception {
            if ( ex != null ) {
                ex.printStackTrace();
                throw ex;
            }
            else if ( builder.toString().trim().length() == 0 ) {
                return null;
            }
            else {
                return builder.toString();
            }
        }
    }

    private String getGpuIdString(GL2 gl) {
        // http://www.opengl.org/discussion_boards/showthread.php/165320-Get-amount-of-graphics-memory
        /*
        For the name, you can use
        GLubyte *vendor=glGetString(GL_VENDOR);
        GLubyte *renderer=glGetString(GL_RENDERER);
        GLubyte *version=glGetString(GL_VERSION);
         */
        gl.glGetError(); // Clear any old errors.
        String rtnVal = null;
        String vendorStr = gl.glGetString( GL2.GL_VENDOR );
        if ( vendorStr == null ) {
            vendorStr = UNKNOWN_VALUE;
        }
        String rendererString = gl.glGetString( GL2.GL_RENDERER );
        if ( rendererString == null ) {
            rendererString = UNKNOWN_VALUE;
        }
        String version = gl.glGetString( GL2.GL_VERSION );
        if ( version == null ) {
            version = UNKNOWN_VALUE;
        }

        int gpuIdErr = gl.glGetError();
        if ( gpuIdErr != 0 ) {
            logger.warn( "Failed to get GPU ID info {}.", gpuIdErr );
        }

        rtnVal = vendorStr + GUI_ID_SEPARATOR + rendererString + GUI_ID_SEPARATOR + version;
        return rtnVal;
    }

    /**
     * Attempt to determine how much memory is free for use with textures.
     *
     * @param gl descriptor for OpenGL calls.
     * @return value obtained from either NVidea or Radeon call.
     */
    private int getFreeTextureMemory(GL2 gl) {
        /*
           This technique is derived from information available at these sources:

            http://www.opengl.org/registry/specs/ATI/meminfo.txt
            http://developer.download.nvidia.com/opengl/specs/GL_NVX_gpu_memory_info.txt
         */
        gl.glGetError(); // Clear any old errors.
        int rtnVal = 0;  // Default to 0, in case neither returns.
        IntBuffer rtnBuf = IntBuffer.allocate( 4 ); // Max required, under Radeon.
        gl.glGetIntegerv(GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, rtnBuf);
        int errnum = gl.glGetError();
        if ( errnum == 0 ) {
            rtnVal = rtnBuf.array()[ 0 ];
        }
        else {
            rtnBuf.rewind();
            gl.glGetIntegerv(TEXTURE_FREE_MEMORY_ATI, rtnBuf);
            errnum = gl.glGetError();
            if ( errnum == 0 ) {
                rtnVal = rtnBuf.array()[ 0 ];
            }
            else {
                logger.warn( "Neither NVidea nor Radeon video memory calls succeeded {}/{}.", errnum, rtnBuf.array()[0] );
            }
        }

        logger.info( "Free texture memory {}.", rtnVal );
        return rtnVal;
    }

    public static class GpuInfo {
        private String vender;
        private String renderer;
        private String version;
        private String highestGlslVersion;
        private int freeTexMem;

        public String getVender() {
            return vender;
        }

        public void setVender(String vender) {
            this.vender = vender;
        }

        public String getRenderer() {
            return renderer;
        }

        public void setRenderer(String renderer) {
            this.renderer = renderer;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getHighestGlslVersion() {
            return highestGlslVersion;
        }

        public void setHighestGlslVersion(String highestGlslVersion) {
            this.highestGlslVersion = highestGlslVersion;
        }

        public int getFreeTexMem() {
            return freeTexMem;
        }

        public void setFreeTexMem(int freeTexMem) {
            this.freeTexMem = freeTexMem;
        }
    }

}
