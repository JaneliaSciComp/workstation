
package src.org.janelia.it.jacs.compute.service.search;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 30, 2007
 * Time: 2:50:10 PM
 */
public class SystemSearchException extends ServiceException {

    /**
     * Construct a SystemSearchException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public SystemSearchException(String msg) {
        super(msg);
    }

    /**
     * Construct a SystemSearchException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public SystemSearchException(Throwable e) {
        super(e);
    }

    /**
     * Construct a SystemSearchException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public SystemSearchException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a SystemSearchException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public SystemSearchException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a SystemSearchException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public SystemSearchException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a SystemSearchException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public SystemSearchException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }

}
