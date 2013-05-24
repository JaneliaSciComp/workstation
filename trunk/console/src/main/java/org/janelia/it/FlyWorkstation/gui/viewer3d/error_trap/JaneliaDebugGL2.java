package org.janelia.it.FlyWorkstation.gui.viewer3d.error_trap;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/7/13
 * Time: 11:28 AM
 *
 * Subclassing DebugGL2 to allow better handling of found errors.  The original is reporting errors upstream from
 * our code, throwing its exception, and hence making the technique useless.
 */
public class JaneliaDebugGL2 extends DebugGL2 {
    private GL2 downstreamGL2;
    private GLContext _context;

    public JaneliaDebugGL2(GL2 downstreamGL2)
    {
        super( downstreamGL2 );
        this.downstreamGL2 = downstreamGL2;
        this._context = downstreamGL2.getContext();
    }

    public  void glGetIntegerv(int arg0,int[] arg1,int arg2)
    {
        if ( insideBeginEndPair )
            return;

        checkContext();
        downstreamGL2.glGetIntegerv(arg0,arg1,arg2);
        String txt = new String("glGetIntegerv(" +
                "<int> 0x"+Integer.toHexString(arg0).toUpperCase() +    ", " +
                "<[I>" +    ", " +
                "<int> 0x"+Integer.toHexString(arg2).toUpperCase() +    ")");
        checkGLGetError( txt );
    }

    private boolean insideBeginEndPair = false;

    /**
     * Must override gl end and begin, simply to know when the process is between these two calls, and avoid
     * doing bad things.
     */
    public  void glEnd()
    {
        super.glEnd();
        insideBeginEndPair = false;
    }

    public  void glBegin(int arg0)
    {
        super.glBegin(arg0);
        insideBeginEndPair = true;
    }

    private void checkContext() {
        if ( insideBeginEndPair )
            return;

        GLContext currentContext = GLContext.getCurrent();
        if (currentContext == null) {
            new GLException("No OpenGL context is current on this thread").printStackTrace();
        }
        if ((_context != null) && (_context != currentContext)) {
            new GLException("This GL object is being incorrectly used with a different GLContext than that which created it").printStackTrace();
        }
    }

    protected void checkGLGetError(String caller)
    {
        if (insideBeginEndPair) {
            return;
        }
        // Debug code to make sure the pipeline is working; leave commented out unless testing this class
        //System.err.println("Checking for GL errors after call to " + caller);

        int err = downstreamGL2.glGetError();
        if (err == GL_NO_ERROR) { return; }

        StringBuilder buf = new StringBuilder(Thread.currentThread()+
                " glGetError() returned the following error codes after a call to " + caller + ": ");

        // Loop repeatedly to allow for distributed GL implementations,
        // as detailed in the glGetError() specification
        int recursionDepth = 10;
        do {
            switch (err) {
                case GL_INVALID_ENUM: buf.append("GL_INVALID_ENUM "); break;
                case GL_INVALID_VALUE: buf.append("GL_INVALID_VALUE "); break;
                case GL_INVALID_OPERATION: buf.append("GL_INVALID_OPERATION "); break;
                case GL_STACK_OVERFLOW: buf.append("GL_STACK_OVERFLOW "); break;
                case GL_STACK_UNDERFLOW: buf.append("GL_STACK_UNDERFLOW "); break;
                case GL_OUT_OF_MEMORY: buf.append("GL_OUT_OF_MEMORY "); break;
                case GL_NO_ERROR: throw new InternalError("Should not be treating GL_NO_ERROR as error");
                default: buf.append("Unknown glGetError() return value: ");
            }
            buf.append("( " + err + " 0x"+Integer.toHexString(err).toUpperCase() + "), ");
        } while ((--recursionDepth >= 0) && (err = downstreamGL2.glGetError()) != GL_NO_ERROR);
        new GLException(buf.toString()).printStackTrace();
    }

}
