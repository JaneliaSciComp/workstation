package org.janelia.it.workstation.api.facade.concrete_facade.xml;

/*********************************************************************
 *********************************************************************
 CVS_ID:  $Id: ValidatingParser.java,v 1.1 2006/11/09 21:35:57 rjturner Exp $
 *********************************************************************/

import com.sun.org.apache.xerces.internal.parsers.SAXParser;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;

/**
 * Runs validation against an XML file.  Must be given the file name, its DTD file name,
 * and the root element or doctype string. [See usage in main method].
 *
 * @author Les Foster
 */
public class ValidatingParser {

    // This constant meant to be used externally, but specified here, because
    // it may or may not be dependent on the specific implementation of the validating parser!
    public static final String BAD_IDREF_MESSAGE = "No element has an ID attribute with value";
    private static final String XML_HEADER_BEGIN = "<?xml ";
    private static final String XML_HEADER_END = "?>";
    private static final String DEFAULT_XML_DECLARATION = "<?xml version=\"1.0\"?>";
    private static final String PRESENTATION_TITLE = "DTD Validation Report";

    /**
     * Returns a nice title to be presented with for errors.
     */
    public static String getPresentationTitle() {
        return PRESENTATION_TITLE;
    }

    /**
     * Override constructor to retrofit with older applications.
     */
    public ValidatingParser(String lDoctype, String lDTDfileName, String lInputfileName, ErrorHandler lOverrideErrorHandler) {
        this(lDoctype, lDTDfileName, lInputfileName, null, lOverrideErrorHandler);
    } // End constructor

    /**
     * Constructor takes the document type (or root element name), DTD filename and input
     * file name.
     */
    public ValidatingParser(String lDoctype, String lDTDfileName, String lInputfileName, DefaultHandler lOverrideHandler, ErrorHandler lOverrideErrorHandler) {

        try {

            SAXParser lParser = new SAXParser();
            //lParser.setFeature( "http://apache.org/xml/features/dom/defer-node-expansion", false );
            lParser.setFeature("http://xml.org/sax/features/validation", true);

            // Satisfy the requirement for a protocol prefix to the DTD's URI.
            String lDTDprotocolName = null;
            if (lDTDfileName.indexOf(":") <= 1) {
                lDTDprotocolName = "file:" + lDTDfileName;
            } // Must prepend the file protocol.
            else {
                lDTDprotocolName = lDTDfileName;
            } // No need to prepend.

            // Make a temporary file which refers to the DTD.
            String lTempfileName = this.getClass().getName() + ".xml";

            PrintWriter lPW = new PrintWriter(new FileWriter(lTempfileName));

            BufferedReader lBR = null;
            String lInputline = null;

            lBR = new BufferedReader(new FileReader(lInputfileName));
            boolean inXMLHeader = false;
            int endHeaderInx = -1;
            int lineCount = 0;
            StringBuffer userSuppliedXMLDeclaration = new StringBuffer();

            while (null != (lInputline = lBR.readLine())) {

                if (lineCount == 0) {
                    if (lInputline.startsWith(XML_HEADER_BEGIN)) {
                        inXMLHeader = true;
                    } // Starting the xml header
                    else {
                        printHeaderAndDTDRef(lPW, DEFAULT_XML_DECLARATION, lDoctype, lDTDprotocolName);
                    } // No XML header given by user
                } // First line of input.

                if (inXMLHeader) {
                    if (-1 != (endHeaderInx = lInputline.indexOf(XML_HEADER_END))) {
                        userSuppliedXMLDeclaration.append(lInputline.substring(0, endHeaderInx + XML_HEADER_END.length()));

                        printHeaderAndDTDRef(lPW, userSuppliedXMLDeclaration.toString(), lDoctype, lDTDprotocolName);
                        lInputline = lInputline.substring(endHeaderInx + XML_HEADER_END.length());
                        inXMLHeader = false;
                    } // Ending the xml header
                } // Within the XML header

                if (!inXMLHeader) lPW.println(lInputline);

                lineCount++;
            } // For all lines of input.

            lBR.close();
            lPW.flush();
            lPW.close();

            // Parse the input file.
            // 'file:', as protocol for the URI, must be preset to satisfy
            // the entity resolver from Sun.
            if (lTempfileName.indexOf(":") <= 1) {
                lTempfileName = "file:" + lTempfileName;
            } // Must prepend the file protocol.

            if (lOverrideErrorHandler == null) lParser.setErrorHandler(new ValidatingErrorHandler(lInputfileName));
            else lParser.setErrorHandler(lOverrideErrorHandler);

            if (lOverrideHandler != null) lParser.setContentHandler(lOverrideHandler);

            // Set disposition of temporary file.
            new File(lTempfileName).deleteOnExit();

            lParser.parse(lTempfileName);

            // Cleanup the temporary file.
            new File(lTempfileName).delete();

        }
        catch (org.xml.sax.SAXException lSAXE) {
            System.out.println("ERROR: SAX Exception during parse");
            System.out.println("INFO: " + lSAXE.getMessage());
            throw new IllegalArgumentException(lSAXE.getMessage());
            // lSAXE.printStackTrace(System.out);

        }
        catch (IOException lIOE) {
            System.out.println("ERROR: IO failure during parse");
            System.out.println("INFO: " + lIOE.getMessage());
            throw new IllegalArgumentException(lIOE.getMessage());
            // lIOE.printStackTrace(System.out);

        } // End catch block for parsing

    } // End constructor

    /**
     * Constructor takes all that the other one does, but uses the local error
     * handler.
     */
    public ValidatingParser(String lDoctype, String lDTDfileName, String lInputfileName) {
        this(lDoctype, lDTDfileName, lInputfileName, null);
    } // End constructor

    /**
     * Prints XML header, and makes known to doc reader which DTD to use in validation.
     */
    private void printHeaderAndDTDRef(PrintWriter lPW, String xmlHeader, String lDoctype, String lDTDprotocolName) {
        lPW.print(xmlHeader);
        lPW.print("<!DOCTYPE " + lDoctype + " SYSTEM " + "\"" + lDTDprotocolName + "\">");
    } // End method: printHeaderAndDTDRef

    /**
     * ERROR handler class is required to trap errors coming from invalid XML files.
     */
    class ValidatingErrorHandler implements ErrorHandler {
        private String mInputFile = null;

        /**
         * Constructor to allow for an offset by number of lines in dtd.
         */
        public ValidatingErrorHandler(String lInputFile) {
            mInputFile = lInputFile;
        } // End constructor.

        /**
         * Receive notification of a recoverable error.
         */
        public void error(SAXParseException exception) {
            System.out.print("Recoverable error:" + reportLine(exception.getLineNumber()));
            System.out.println(" [" + exception.getMessage() + "]");
        } // End method: error

        /**
         * Receive notification of a non-recoverable error.
         */
        public void fatalError(SAXParseException exception) {
            System.out.print("Non-recoverable error:" + reportLine(exception.getLineNumber()));
            System.out.println(" [" + exception.getMessage() + "]");
        } // End method: fatalError

        /**
         * Receive notification of a warning.
         */
        public void warning(SAXParseException exception) {
            System.out.print("Warning:" + reportLine(exception.getLineNumber()));
            System.out.println(" [" + exception.getMessage() + "]");
        } // End method: warning

        /**
         * Calculates the line number, given an offset.  Error may be
         * in the DTD, or may be in the input file.  Produces
         * a very brief report based on this.
         */
        private String reportLine(int lErrorLine) {
            return " in " + mInputFile + " at Line " + (lErrorLine);
        } // End method: reportLine

    } // End class: ValidatingErrorHandler

    public static void main(String[] args) {
        if (args.length < 3) System.out.println("USAGE: java ValidatingErrorHandler <doctype-string> <DTD> <infile>");
        else new ValidatingParser(args[0], args[1], args[2]);
    } // End main

} // End class: ValidatingParser
