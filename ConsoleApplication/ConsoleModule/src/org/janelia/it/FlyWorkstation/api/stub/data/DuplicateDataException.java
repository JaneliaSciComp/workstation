package org.janelia.it.FlyWorkstation.api.stub.data;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 11:08 AM
 */
public class DuplicateDataException extends UserException {
    public DuplicateDataException() {
        super("There is more than one piece of data available with this identifier.");
    }
}
