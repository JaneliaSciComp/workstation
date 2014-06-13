package org.janelia.it.workstation.api.stub.data;

import java.io.Serializable;
import java.util.*;

public class ControlledVocabulary implements Serializable {
    private Map forwardMap = new HashMap();
    private Map reverseMap = new HashMap();
    private List keyOrder = null;

    public ControlledVocabulary(List valuesInOrder) {
        keyOrder = valuesInOrder;
    }

    public ControlledVocabulary() {
    }

    public void addEntry(String value, String name) {
        forwardMap.put(value, name);
        reverseMap.put(name, value);
    }

    /**
     * return the string associated with the input value
     * If there is not a name associated with the string, the input value is returned
     */
    public String lookup(String value) {
        String mapVal = (String) forwardMap.get(value);
        return (mapVal == null) ? value : mapVal;
    }

    public String reverseLookup(String name) {
        String mapVal = (String) reverseMap.get(name);
        return (mapVal == null) ? name : mapVal;
    }

    public Collection getNames() {
        if (keyOrder != null) {
            List retVal = new ArrayList(keyOrder.size());
            for (Iterator iter = keyOrder.iterator(); iter.hasNext(); ) {
                retVal.add(forwardMap.get(iter.next()));
            }
            return retVal;
        }
        return reverseMap.keySet();
    }

    public String[] getValues() {
        String[] retVal = null;
        if (keyOrder != null) {
            retVal = new String[keyOrder.size()];
            retVal = (String[]) keyOrder.toArray(retVal);
            return retVal;
        }
        else {
            retVal = new String[forwardMap.size()];
            retVal = (String[]) forwardMap.keySet().toArray(retVal);
        }
        return retVal;
    }

}
