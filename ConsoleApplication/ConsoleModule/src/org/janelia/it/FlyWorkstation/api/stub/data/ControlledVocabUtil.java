package org.janelia.it.FlyWorkstation.api.stub.data;

import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

import java.util.*;

public class ControlledVocabUtil {

    private ControlledVocabUtil() {
    }

    public static void main(String[] args) {
        Map htbl = ControlledVocabUtil.getControlledVocabulariesFromResource();
        for (Object o : htbl.keySet()) {
            String index = (String) o;
            System.out.println("---------" + index + "---------");
            List mappings = (List) htbl.get(index);
            for (Object mapping : mappings) {
                ControlledVocabElement element = (ControlledVocabElement) mapping;
                System.out.println(element.value + "=" + element.name);
            }
        }
    }

    public static Map getControlledVocabulariesFromResource() {
        return getControlledVocabulariesFromResource("resource.shared.ControlledVocab");
    }

    public static Map getControlledVocabulariesFromResource(String resourceName) {
        Map vocabularies = new TreeMap();
        ResourceBundle vocabBundle = ResourceBundle.getBundle(resourceName);

        // For each Vocab
        for (Enumeration e = vocabBundle.getKeys(); e.hasMoreElements(); ) {
            String index = (String) e.nextElement();
            String mapValues = vocabBundle.getString(index);
            List newVocab = new ArrayList();
            // For each element in the Vocab
            for (StringTokenizer mapValueTokens = new StringTokenizer(mapValues, ";"); mapValueTokens.hasMoreTokens(); ) {
                String mapElem = mapValueTokens.nextToken();

                // Get the value/name pair
                StringTokenizer mapElemTokens = new StringTokenizer(mapElem, ",");
                ControlledVocabElement newVocabElement = new ControlledVocabElement();
                newVocabElement.value = mapElemTokens.nextToken();
                newVocabElement.name = mapElemTokens.nextToken();
                newVocab.add(newVocabElement);
            }
            vocabularies.put(index, newVocab);
        }

        return vocabularies;
    }

    public static ControlledVocabElement[] getControlledVocab(String vocabIndex, Map vocabularies) throws NoDataException {
        List ctrlElemVec = (List) vocabularies.get(vocabIndex);
        if (ctrlElemVec == null) {
            throw new NoDataException();
        }
        ControlledVocabElement[] retVal = new ControlledVocabElement[ctrlElemVec.size()];
        retVal = (ControlledVocabElement[]) ctrlElemVec.toArray(retVal);
        return retVal;
    }

    private static String nullVocabString = "Null_Vocab";

    public static String getNullVocabIndex() {
        return nullVocabString;
    }

    public static boolean isNullVocabIndex(String vocabIndex) {
        return nullVocabString.equals(vocabIndex);
    }
}
