package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:38 PM
 */
public class InvalidXmlException extends Throwable {

    StringBuffer msg = null;
    String fileName = null;
    String title = null;

    /**
     * Simple constructor to allow set of message.
     */
    public InvalidXmlException(StringBuffer msg, String fileName, String title) {
        super("XML Parse Failure");
        this.msg = msg;
        this.fileName = fileName;
        this.title = title;
    } // End constructor

    /**
     * Returns the parse error output.
     */
    public StringBuffer getParseData() {
        return msg;
    }

    /**
     * Tells which file this was on.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Can be used for special presentation.
     */
    public String getTitle() {
        return title;
    }
} // End class
