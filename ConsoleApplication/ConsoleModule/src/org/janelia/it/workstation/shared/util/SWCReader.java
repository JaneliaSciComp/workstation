package org.janelia.it.workstation.shared.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * the SWCReader class reads and parses an SWC file, used for storing
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

    private String filepath;

    private List<SWCNode> nodeList = new ArrayList<SWCNode>();
    private List<String> headerList = new ArrayList<String>();

    public SWCReader(String filepath) throws IOException {

        this.filepath = filepath;

        // read and parse it
        parseFile();

    }

    private void parseFile() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(this.filepath),
                Charset.defaultCharset());

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

    public List<SWCNode> getNodeList() {
        return nodeList;
    }

    public List<String> getHeaderList() {
        return headerList;
    }
}
