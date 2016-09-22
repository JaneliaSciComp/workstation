
package src.org.janelia.it.jacs.shared.lucene;

import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the configurable parameters in indexer
 *
 * @author Leonid Kagan
 */
public class IndexerArgs {

    private static final String KEY_DOC_TYPES = "doctypes";
    private static final String KEY_GENERATE_COUNT = "generate";

    private Set<String> docTypesToIndex = new HashSet<String>();
    private int recsCount = 0;


    public IndexerArgs(String[] args) throws IOException {
        if (args != null && args.length > 0) {
            for (String arg : args) {
                String[] nameValue = arg.split("=");
                if (nameValue.length > 1) {
                    nameValue[0] = nameValue[0].trim();
                    nameValue[1] = nameValue[1].trim();
                }
                else {
                    throw new IllegalArgumentException("name and value must be separated by =");
                }
                String[] values = getArgValues(nameValue[1]);
                if (nameValue[0].equals(KEY_DOC_TYPES)) {
                    if (!values[0].equals(LuceneIndexer.INDEX_ALL)) {
                        for (String s : values) {
                            docTypesToIndex.add(validateDocType(s));
                        }
                    }
                    else {
                        docTypesToIndex = LuceneIndexer.SET_OF_ALL_DOC_TYPES;
                    }
                }
                else if (nameValue[0].equals(KEY_GENERATE_COUNT)) {
                    recsCount = validateInt(nameValue[0], values[0]);
                }
                else {
                    throw new IllegalArgumentException("Invalid argument:" + nameValue[0] + getUsage());
                }
            }
        }
        else {
            throw new IllegalArgumentException("At least one argument must be specified" + getUsage());
        }
    }

    private String validateDocType(String value) {
        if (LuceneIndexer.SET_OF_ALL_DOC_TYPES.contains(value)) {
            return value;
        }
        else {
            throw new IllegalArgumentException(value + " is invalid for document type. Valid values include: " + LuceneIndexer.SET_OF_ALL_DOC_TYPES.toString());
        }
    }

    private int validateInt(String name, String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(value + " of '" + name + "' must be an integer");
        }
    }

    private String[] getArgValues(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("value cannot be null or empty for a parameter");
        }
        return value.split(",");
    }

    private static String getUsage() throws IOException {
        return FileUtil.getResourceAsString("dma_usage.txt");
    }

    public Set<String> getDocTypesToIndex() {
        return docTypesToIndex;
    }

    public int getRecsCount() {
        return recsCount;
    }
}

