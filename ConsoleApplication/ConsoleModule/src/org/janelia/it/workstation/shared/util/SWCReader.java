package org.janelia.it.workstation.shared.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * the SWCReader class reads and parses an SWC swcFile, used for storing
 * neuron skeletons; see http://research.mssm.edu/cnic/swc.html for
 * the best description I could find
 *
 * the reader does its thing when instantiated, and thereafter gives
 * you a list of header lines and SWC nodes; it's up to you to transform
 * those lists into whatever kind of neuron object you want
 *
 *
 * djo, 6/14
 *
 */
public class SWCReader {

    private File swcFile;

    private List<SWCNode> nodeList = new ArrayList<SWCNode>();
    private List<String> headerList = new ArrayList<String>();

    private String invalidReason = null;

    public SWCReader(File swcFile) throws IOException {

        this.swcFile = swcFile;
        readParseFile();

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
     * check the swcFile; if false, call getInvalidReason()
     */
    public boolean isValid() {
        invalidReason = null;

        int nRoots = 0;
        Set<Integer> possibleParents = new HashSet<Integer>();
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
}
