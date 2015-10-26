package org.janelia.it.workstation.shared.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class holds data from a file using the .swc format, which is
 * used for storing neuron skeletons; see http://research.mssm.edu/cnic/swc.html
 * for the best description I could find
 *
 * this is designed to be a dumb container; other than a minimal awareness
 * of what nodes should look like and how they interrelate, it doesn't know
 * or care anything about what the data means; the header lines could be
 * gibberish, and you could mark "ends" as "fork points" and it wouldn't care
 *
 * to read a file, call the static method, which returns the data instance;
 * the instance gives you a list of header lines and SWC nodes; it's up to
 * you to transform those lists into whatever kind of neuron object you want
 *
 * the writer method writes the file out; it runs its validator first
 *
 *
 * possible improvements:
 * - add write(String filename) (rather than File)
 * - add setter or equiv. for nodes, header lines?  not really needed; it's
 *      intended that construct the lists elsewhere; SWCData is a dumb wrapper
 *
 *
 * djo, 6/14
 *
 */
public class SWCData {

    public static final String STD_SWC_EXTENSION = ".swc";
    private static final Logger logger = LoggerFactory.getLogger(SWCData.class);
    private static final String COLOR_HEADER_PREFIX = "COLOR";
    private static final String NAME_HEADER_PREFIX = "NAME";
    private File swcFile;

    private List<SWCNode> nodeList = new ArrayList<>();
    private List<String> headerList = new ArrayList<>();

    private String invalidReason = null;

    public SWCData() {
        clear();
    }

    /**
     * create a new SWCData from component parts; does not check
     * validity at this point; not clear this is a good idea,
     * because we don't provide setters for the lists yet!
     */
    public SWCData(List<SWCNode> nodeList, List<String> headerList) {
        this.nodeList = nodeList;
        this.headerList = headerList;
    }

    public void clear() {
        swcFile = null;
        nodeList = new ArrayList<>();
        headerList = new ArrayList<>();
        invalidReason = null;
    }

    public static SWCData read(File swcFile) throws IOException {
        SWCData data = new SWCData();
        data.swcFile = swcFile;
        data.readParseFile();
        return data;
    }

    private void readParseFile() throws IOException {
        List<String> lines = Files.readAllLines(swcFile.toPath(), Charset.defaultCharset());

        for (String line: lines) {
            line = line.trim();

            if (line.length() == 0) {
                // if blank, skip--do nothing
            } else if (line.startsWith("#")) {
                // if starts with #, into header list
                headerList.add(line);
            } else {
                // if not, create SWCNode; put into node list
                nodeList.add(SWCNode.parseLine(line));
            }
        }
    }

    /**
     * Validate file contents and write back to target.
     * 
     * @param swcFile target file.
     * @param offset serial number of output file, if one of many.
     * @throws Exception thrown by called methods.
     */
    public void write(File swcFile, int offset) throws Exception {
        if (isValid()) {
            if (offset != -1) {
                final String swcFileName = swcFile.getName();
                String parentDirName = swcFileName.substring(0, swcFileName.length() - STD_SWC_EXTENSION.length());
                File parentDir = new File(swcFile.getParent(), parentDirName);
                // If anyone ever made a file of the name we wish to call our
                // directory, we'll make an alternative with unique name.
                if (parentDir.exists()  &&  !parentDir.isDirectory()) {
                    parentDir = new File(parentDir.getParentFile(), parentDirName + "_" + new java.util.Date().getTime());                    
                }
                if (! parentDir.exists() ) {
                    parentDir.mkdirs();
                }                
                String newName = StringUtils.getIteratedName(swcFileName, offset);
                swcFile = new File(parentDir, newName);
            }
            FileWriter writer = new FileWriter(swcFile);
            writeSwcFile(writer);
            this.swcFile = swcFile;
        }
        else {
            writeErrorSWC();
        }

    }

    /**
     * Look through the input file, and make smaller files for any line that
     * ends in the "-1 as parent" indicator.
     * 
     * @param infile to be subdivided.
     * @return list of divided files.
     * @throws IOException from called methods.
     */
    public List<File> breakOutByRoots(File infile) throws IOException {
        List<File> rtnVal = new ArrayList<>();        
        String lineTerm = System.getProperty("line.separator");
        try (BufferedReader br = new BufferedReader( new FileReader( infile ) )) {
            String inline = null;
            List<String> headerLines = new ArrayList<>();
            List<StringBuilder> newFileText = new ArrayList<>();
            StringBuilder currentBuilder = null;
            int rootNodeNum = -1;
            while (null != (inline = br.readLine())) {
                if (inline.startsWith("#")) {
                    headerLines.add(inline);                    
                }
                else if (inline.endsWith("-1")) {
                    int afterDigits = StringUtils.findFirstNonDigitPosition(inline);
                    rootNodeNum = Integer.parseInt(inline.substring(0, afterDigits)) - 1;
                    
                    currentBuilder = new StringBuilder();
                    newFileText.add(currentBuilder);
                    for (String headerLine: headerLines) {
                        currentBuilder.append(headerLine);
                        currentBuilder.append(lineTerm);
                    }
                    currentBuilder.append(1);
                    currentBuilder.append(inline.substring(afterDigits));
                    currentBuilder.append(lineTerm);
                }
                else {
                    currentBuilder.append(reorder(inline, rootNodeNum));
                    currentBuilder.append(lineTerm);
                }
            }
            
            if (newFileText.size() <= 1) {
                // We just wasted time scanning the input file.  Oh well.
                rtnVal.add(infile);
            }
            else {
                int fileNum = 1;

                File tempDir = new File(System.getProperty("java.io.tmpdir"), "SWCData_" + new java.util.Date().getTime());                
                tempDir.mkdirs();
                tempDir.deleteOnExit();
                logger.info("Making file {}", tempDir.toString());
                for (StringBuilder builder : newFileText) {
                    File newFile = new File(tempDir, StringUtils.getIteratedName(infile.getName(), fileNum));
                    newFile.deleteOnExit();
                    
                    try ( PrintWriter pw = new PrintWriter( new FileWriter( newFile ) ) ) {
                        pw.print(builder.toString());
                    }
                    
                    rtnVal.add(newFile);
                    fileNum++;
                }
            }
        }        
        return rtnVal;
    }
    
    /**
     * Validate contents and write back to target.
     * 
     * @param swcFile one and only file.
     * @throws Exception thrown by called methods.
     */
    public void write(File swcFile) throws Exception {
        write(swcFile, -1);
    }
    
    /**
     * check the swcFile; if false, call getInvalidReason()
     */
    public boolean isValid() {
        invalidReason = null;

        // header lines all start with #
        for (String line: getHeaderList()) {
            if (!line.startsWith("#")) {
                invalidReason = String.format("header line doesn't start with #: %s", line);
                return false;
            }
        }

        int nRoots = 0;
        Set<Integer> possibleParents = new HashSet<>();
        // -1 (no parent) is valid:
        possibleParents.add(-1);

        for (SWCNode node: getNodeList()) {
            possibleParents.add(node.getIndex());
        }
        
        int lastIndex = 0;
        for (SWCNode node: getNodeList()) {
            // node indices should increment by one each line
            if (node.getIndex() != lastIndex + 1) {
                invalidReason = String.format("index %d out of order", node.getIndex());
                return false;
            }
            lastIndex = node.getIndex();

            // must be at least one root
            if (node.getParentIndex() == -1) {
                nRoots += 1;
            }

            // is node valid: valid type, positive radius
            if (!node.isValid()) {
                invalidReason = String.format("invalid node (index %d)", node.getIndex());
                return false;
            }

            // each node parent exists (or is root)
            if (!possibleParents.contains(node.getParentIndex())) {
                invalidReason = String.format("node with invalid parent index %d", node.getParentIndex());
                return false;
            }
        }

        // at least one root
        if (nRoots == 0) {
            invalidReason = "no root node";
            return false;
        }

        return true;
    }

    /**
     * add the neuron data from a second instance; the header lines from the second
     * instance are lost; we do NOT check the header lines for anything, so if
     * they're using our OFFSET field, too bad; remember how I mentioned this is
     * a dumb data structure?
     */
    public void addDataFrom(SWCData swcData) {

        // loop over nodes in second data;
        // add to current data, adding original node count
        //  to each index and parent index, EXCEPT the -1 parents (!)

        int offset = nodeList.size();
        for (SWCNode node: swcData.getNodeList()) {
            node.setIndex(node.getIndex() + offset);
            if (node.getParentIndex() != -1) {
                node.setParentIndex(node.getParentIndex() + offset);
            }
            nodeList.add(node);
        }

    }

    /**
     * usually headers are of the form "# KEY thing1 thing2 ...";
     * given a KEY, return the full line if it exists in the header,
     * else null; returns first such line found
     */
    public String findHeaderLine(String key) {
        for (String line: headerList) {
            String [] items = line.split("\\s+");
            if (items.length >= 2 && items[1].equals(key)) {
                return line;
            }
        }
        return null;
    }

    public List<SWCNode> getNodeList() {
        return nodeList;
    }

    public List<String> getHeaderList() {
        return headerList;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    /**
     * THIS was copied form AnnotationModel.  TODO consider refactoring.
     * note from CB, July 2013: Vaa3d can't handle large coordinates in swc files,
     * so he added an OFFSET header and recentered on zero when exporting
     * therefore, if that header is present, respect it
     *
     * @return array of offset.
     * @throws NumberFormatException 
     */
    public double[] parseOffset() throws NumberFormatException {
        double[] offset = new double[3];
        String offsetHeader = findHeaderLine("OFFSET");
        if (offsetHeader != null) {
            String [] items = offsetHeader.split("\\s+");
            // expect # OFFSET x y z
            if (items.length == 5) {
                offset[0] = Double.parseDouble(items[2]);
                offset[1] = Double.parseDouble(items[3]);
                offset[2] = Double.parseDouble(items[4]);
            } else {
                // ignore the line if we can't parse it
                logger.warn("Failed to parse offset header {}.", offsetHeader);
            }
        }
        return offset;
    }
    
    /**
     * Newer SWCs may be serialized with the color as a triple.  These triples
     * are float values along 0.0..1.0. This method obtains the triple from
     * the header, but leaves color object interpretation for the caller.
     * 
     * Expected format:
     * # COLOR N.mmm N.mm N.mmmmm
     *   or
     * # COLOR N.mmm,N.mm,N.mmmmm
     * 
     * @return array of normalized rgb values, or null if header not found.
     */
    public float[] parseColorFloats() {
        float[] rgb = null;
        String rgbHeader = findHeaderLine(COLOR_HEADER_PREFIX);
        if (rgbHeader != null) {
            rgb = new float[3];
            // NOTE: if fewer colors are in header than 3, remainder
            // are just filled with 0f.
            int colorHdrOffs = rgbHeader.indexOf(COLOR_HEADER_PREFIX);
            colorHdrOffs += COLOR_HEADER_PREFIX.length();
            rgbHeader = rgbHeader.substring(colorHdrOffs).trim();
            String[] colors = rgbHeader.split("[, ]");
            for (int i = 0; i < colors.length  &&  i < rgb.length; i++) {
                try {
                    rgb[i] = Float.parseFloat(colors[i]);
                } catch (NumberFormatException nfe) {
                    // Ignore what we cannot parse.
                    logger.warn("Failed to parse color value {} of header {}.", i, rgbHeader);
                }
            }
        }
        return rgb;
    }
    
    /**
     * Newer SWCs may be serialized with the name from the original neuron.
     * @return name as written back.
     */
    public String parseName() {
        String name = null;
        String nameHeader = findHeaderLine(NAME_HEADER_PREFIX);
        if (nameHeader != null) {
            int hdrPos = nameHeader.indexOf(NAME_HEADER_PREFIX);
            name = nameHeader.substring(hdrPos + NAME_HEADER_PREFIX.length()).trim();
        }
        return name;
    }
    
    /**
     * Take an input line from SWC, and re-order it so that its root becomes
     * node 1.  This implies that its parent trace lineage back to 1.  In other
     * words, reduce parent by the root offset.
     * 
     * @param inline to be modified
     * @param rootOffset old node number of this line's root, minus 1.
     * @return re-ordered line.
     */
    protected String reorder(String inline, int rootOffset) {
        if (rootOffset == 0) {
            return inline;
        }
        int lastDigitPos = StringUtils.lastDigitPosition(inline);
        int afterDigitsPos = StringUtils.findFirstNonDigitPosition(inline);
        int nodeNum = Integer.parseInt(inline.substring(0, afterDigitsPos)) - rootOffset;
        int parentNodeNum = Integer.parseInt(inline.substring(lastDigitPos)) - rootOffset;
        return nodeNum + inline.substring(afterDigitsPos, lastDigitPos) + parentNodeNum;
    }

    /**
     * this routine is fairly dumb; it just has to write the lines,
     * since the hard work is done in generating the input data (eg, nodes)
     *
     * @param writer accepts the output
     * @throws IOException thrown by called methods.
     */
    private void writeSwcFile(Writer writer) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            for (String line: getHeaderList()) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            
            for (SWCNode node: getNodeList()) {
                bufferedWriter.write(node.toSWCline());
                bufferedWriter.newLine();
            }
        }

    }

    private void writeErrorSWC() throws IllegalStateException, IOException {
        final String message = String.format(
                "can't write SWC data; invalid for reason: %s",
                getInvalidReason()
        );
        logger.error(message);
        if (logger.isDebugEnabled()) {
            writeSwcFile(new OutputStreamWriter(System.err));
        }
        throw new IllegalStateException(message);
    }

}
