package org.janelia.it.workstation.shared.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private List<SWCNode> nodeList = new ArrayList<SWCNode>();
    private List<String> headerList = new ArrayList<String>();

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
        nodeList = new ArrayList<SWCNode>();
        headerList = new ArrayList<String>();
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

    public void write(File swcFile) throws Exception {
        // this routine is fairly dumb; it just has to write the lines,
        //  since the hard work is done in generating the input data (eg, nodes)

        if (!isValid()) {
            throw new IllegalStateException(String.format("can't write SWC data; invalid for reason: %s",
                    getInvalidReason()));
        }

        FileWriter writer = new FileWriter(swcFile);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        for (String line: getHeaderList()) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        for (SWCNode node: getNodeList()) {
            bufferedWriter.write(node.toSWCline());
            bufferedWriter.newLine();
        }

        bufferedWriter.close();

        this.swcFile = swcFile;
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
}
