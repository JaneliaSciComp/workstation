package org.janelia.it.workstation.shared.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * @throws Exception thrown by called methods.
     */
    public void write(File swcFile) throws Exception {
        if (isValid()) {
            FileWriter writer = new FileWriter(swcFile);
            writeSwcFile(writer);
            this.swcFile = swcFile;
        }
        else {
            final String message = String.format(
                    "can't write SWC data; invalid for reason: %s",
                    getInvalidReason()
            );
            Logger logger = LoggerFactory.getLogger(SWCData.class);
            logger.error(message);
            if ( logger.isDebugEnabled()  ||  0 == 0 ) {
                writeSwcFile(new OutputStreamWriter(System.err));
            }
            throw new IllegalStateException(message);
        }

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
            possibleParents.add(node.getIndex());
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

}
