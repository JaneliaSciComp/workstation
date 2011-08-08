package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;

/**
 * Will strip intervening tabs out of text content, and will
 * prepend a prefix to the main element to cause it to be
 * validated using a GAME schema.
 */
public class SchemaValidatingParser extends DefaultHandler {

    private static final String SCHEMA_HEADER_PREFIX = " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation=";
    private static final String DEFAULT_SCHEMA_LOCATION = "CEF.xsd";
    private static final String PRESENTATION_TITLE = "XML Schema Validation Report";

    /**
     * Default parser name.
     */
    private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private String fileName = null;
    private StringBuffer characters = new StringBuffer();
    private PrintWriter writer = null;
    private DefaultHandler nextHandler = null;
    private DefaultHandler overrideHandler = null;
    private ErrorHandler overrideErrorHandler = null;
    private String schemaLocation = DEFAULT_SCHEMA_LOCATION;

    /**
     * Returns a nice title to be presented with for errors.
     */
    public static String getPresentationTitle() {
        return PRESENTATION_TITLE;
    }

    /**
     * Constructor keeps the file name for later parsing.
     */
    public SchemaValidatingParser(String fileName) {
        try {
            new File(fileName);
            this.fileName = fileName;
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("ERROR: file " + fileName + " could not be read");
        } // End catch block for file test.
    } // End constructor

    /**
     * Constructor keeps the file name for later parsing.
     */
    public SchemaValidatingParser(String fileName, ErrorHandler overrideErrorHandler, String schemaLocation) throws Exception {
        this(fileName, null, overrideErrorHandler, schemaLocation);
    } // End constructor

    /**
     * Constructor keeps the file name for later parsing.
     */
    public SchemaValidatingParser(String fileName, DefaultHandler overrideHandler, ErrorHandler overrideErrorHandler, String schemaLocation) throws Exception {
        this(fileName);
        this.overrideHandler = overrideHandler;
        this.overrideErrorHandler = overrideErrorHandler;
        if (schemaLocation != null) this.schemaLocation = schemaLocation;
        this.parse();
    } // End constructor

    /**
     * Having this makes an application.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SchemaValidatingParser <infile>");
            System.exit(0);
        } // No args given.

        SchemaValidatingParser filter = new SchemaValidatingParser(args[0]);
        try {
            filter.parse();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    } // End method: main

    /**
     * Parses the input file set in the instance.
     */
    public void parse() throws Exception {

        PipedWriter producer = new PipedWriter();
        PipedReader consumer = new PipedReader(producer);

        // Setup draw to expect reader input.
        SchemaValidatingDraw draw = new SchemaValidatingDraw(consumer);
        draw.start();

        // Start pumping modified XML down to the validating software.
        ModifiedXmlPump pump = new ModifiedXmlPump(producer);
        pump.start();

        // Make sure the output doesn't 'get by'.
        draw.join();
        pump.join();

    } // End method: parse

    //
    // DocumentHandler methods
    //

    /**
     * Start element.
     */
    public void startElement(String uri, String local, String raw, Attributes attrs) {

        writeAccumulatedCharacters(); // Necessary if mixed content.

        try {
            writer.print("<" + raw);

            if (raw.equals("game")) writer.println(SCHEMA_HEADER_PREFIX + "'" + schemaLocation + "'");

            // Iterate over all the attributes, printing in standard x='y' fashion.
            for (int i = 0; i < attrs.getLength(); i++) {
                writer.print(" " + attrs.getQName(i) + "='" + substituteEntityReferences(attrs.getValue(i)) + "'");
            } // For all attributes
            writer.print(">"); // Terminate the start element.

        }
        catch (Exception e) {
            throw new IllegalArgumentException("ERROR: during prefix print.");
        } // End catch block for writeback.

    } // startElement(String,AttributeList)

    /**
     * Ends the element currently open. Here, clean up and print back any collected
     * text.
     */
    public void endElement(String uri, String local, String raw) {
        writeAccumulatedCharacters();
        writer.print("</" + raw);
        writer.println(">");

    } // End method: endElement

    /**
     * Collect any and all input text except whitespace.
     */
    public void characters(char ch[], int start, int length) {
        for (int i = start; i < length; i++) {
            if (ch[i] == '\t') continue;
            characters.append(substituteEntityReferences(ch[i]));
        } // For all characters of input.
    } // End method: characters

    /**
     * helper to push characters to writer when needed.
     */
    private void writeAccumulatedCharacters() {
        if (characters.length() > 0) {
            writer.print(characters.toString().trim());
        } // Must print characters.
        characters.setLength(0);
    } // End method: writeAccumulatedCharacters

    //
    // ErrorHandler methods
    //

    /**
     * Warning.
     */
    public void warning(SAXParseException ex) {

        System.err.println("[Warning] " + getLocationString(ex) + ": " + ex.getMessage());
    }

    /**
     * Error.
     */
    public void error(SAXParseException ex) {

        System.err.println("[Error] " + getLocationString(ex) + ": " + ex.getMessage());
    }

    /**
     * Fatal error.
     */
    public void fatalError(SAXParseException ex) throws SAXException {

        System.err.println("[Fatal Error] " + getLocationString(ex) + ": " + ex.getMessage());
    }

    /**
     * Returns a string of the location.
     */
    private String getLocationString(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1) systemId = systemId.substring(index + 1);
            str.append(systemId);
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();

    } // getLocationString(SAXParseException):String

    /**
     * Back-subs an entire string worth of characters.
     */
    private String substituteEntityReferences(String st) {
        StringBuffer collector = new StringBuffer(st.length() + 50);
        for (int i = 0; i < st.length(); i++) {
            collector.append(substituteEntityReferences(st.charAt(i)));
        } // FOr all chars.
        return collector.toString();
    } // End method

    /**
     * Back-subs ref for translated special characters.
     */
    private String substituteEntityReferences(char ch) {
        switch (ch) {
            case '&':
                return "&amp;";
            case '\'':
                return "&quot;";
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            default:
                return new String(new char[]{ch});
        }
    } // End method

    //-------------------------------------INNER CLASSES

    /**
     * Pushes modified (cleaned, and with schema prefix) version of XML.
     */
    public class ModifiedXmlPump extends Thread {

        private Writer pumpWriter = null;

        /**
         * Takes reader which will pickup output from here.
         */
        public ModifiedXmlPump(Writer pumpWriter) {
            this.pumpWriter = pumpWriter;
        } // End constructor

        /**
         * Establish runnable.
         */
        public void run() {
            try {
                String parserName = DEFAULT_PARSER_NAME;

                writer = new PrintWriter(pumpWriter);

                XMLReader parser = (XMLReader) Class.forName(parserName).newInstance();
                parser.setContentHandler(SchemaValidatingParser.this);

                parser.setFeature("http://xml.org/sax/features/namespaces", false);
                parser.parse(fileName);

                pumpWriter.flush();
                pumpWriter.close();
                writer.close();
            } // End try block for parsing.
            catch (Exception ex) {
                throw new IllegalStateException(ex.getMessage());
            } // End catch block for parsing.

        } // End method

    } // End class

    public class SchemaValidatingDraw extends Thread {

        private Reader reader = null;

        /**
         * Keeps copy of reader.
         */
        public SchemaValidatingDraw(Reader reader) {
            this.reader = reader;
        } // End constructor

        /**
         * Validates in own thread.
         */
        public void run() {
            // Parse the modified version and send its errors.
            String parserName = DEFAULT_PARSER_NAME;

            if (overrideHandler == null) nextHandler = new DefaultHandler();
            else nextHandler = overrideHandler;

            XMLReader nextParser = null;
            try {
                nextParser = (XMLReader) Class.forName(parserName).newInstance();
                nextParser.setFeature("http://xml.org/sax/features/namespaces", true);

                nextParser.setContentHandler(nextHandler);
                nextParser.setFeature("http://xml.org/sax/features/validation", true);
                nextParser.setFeature("http://apache.org/xml/features/validation/schema", true);

            } // End try block for parse.
            catch (Exception ex) {
                throw new IllegalStateException(ex.getMessage());
            } // End catch block for parse.

            if (overrideErrorHandler == null) nextParser.setErrorHandler(SchemaValidatingParser.this); // Pickup errors.
            else nextParser.setErrorHandler(overrideErrorHandler); // Channel errors.

            try {
                InputSource inputSource = new InputSource(reader);
                nextParser.parse(inputSource);
            }
            catch (Exception ex) {
                nextParser = null;

                throw new IllegalStateException(ex.getMessage());
            }
        } // End method
    } // End class:

} // End class: SchemaValidatingParser
