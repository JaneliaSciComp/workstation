package org.janelia.it.workstation.api.entity_model.fundtype;

import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;

import java.io.Serializable;
import java.util.*;

public class EntityTypeMapping implements Serializable {
//    static private Map<String, String> environmentToEntityName = new HashMap<>();
//    static private Map<Integer, String> valueToFeatureName = new HashMap<>();
    static private Map<Integer, String> valueToDisplayName = new HashMap<>();
    static private Map<String, Integer> featureNameToValue = new HashMap<>();
    static private Map<Integer, EntityTypeMapping> valueToEntityType = new HashMap<>();

    private static final long serialVersionUID = 1;

    //  private final static String RESOURCE_FILE_NAME = "/resource/shared/DiscoveryEnvironment2FeatureType.txt";

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("resource.shared.EntityType");
        String key;
        Integer newValue;
        String resourceValue;
        String featureName;
        String displayName;
        StringTokenizer tokenizer;
        try {
            for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
                key = (String) e.nextElement();
                resourceValue = bundle.getString(key);
                newValue = new Integer(key);
                if (!resourceValue.contains("|")) {
                    featureName = resourceValue;
                    displayName = featureName;
                }
                else {
                    tokenizer = new StringTokenizer(resourceValue, "|");
                    featureName = tokenizer.nextToken();
                    displayName = tokenizer.nextToken();
//                    while (tokenizer.hasMoreTokens()) {
//                        environmentToEntityName.put(tokenizer.nextToken(), featureName);
//                    }
                }
//                valueToFeatureName.put(newValue, featureName);
                valueToDisplayName.put(newValue, displayName);
                featureNameToValue.put(featureName, newValue);
                valueToEntityType.put(newValue, new EntityTypeMapping(newValue));
            }
        }
        catch (Exception ex) {
            FacadeManager.handleException(ex);
        }
    } // End static initializer

    private int value;

    private EntityTypeMapping(int value) {
        this.value = value;
    }

    public static EntityTypeMapping getEntityTypeMappingForValue(int value) {
        EntityTypeMapping et = valueToEntityType.get(value);
        if (et == null) throw new IllegalArgumentException("Entity Value " + value + " is undefined");
        return et;
    }

    public static EntityTypeMapping getEntityTypeMappingForName(String name) {
        Integer value = featureNameToValue.get(name);
        if (value == null) throw new IllegalArgumentException("Feature " + name + " is undefined");
        return getEntityTypeMappingForValue(value);
    }

//    public static EntityTypeMapping getEntityTypeMappingForEnvironment(String discoveryEnvironment) {
//        String name = environmentToEntityName.get(discoveryEnvironment);
//        if (name == null)
//            throw new IllegalArgumentException("Feature with discovery environment " + discoveryEnvironment + " is undefined");
//        return getEntityTypeMappingForName(name);
//    }

//    public static EntityType[] allEntityTypes() {
//        ArrayList entities = new ArrayList();
//        Set keySet = valueToEntityType.keySet();
//        for (Object aKeySet : keySet) {
//            entities.add(valueToEntityType.get(aKeySet));
//        }
//        return (EntityType[]) entities.toArray(new EntityType[0]);
//    }
//
//    public static String[] allEntityNames() {
//        ArrayList<String> names = new ArrayList<>();
//        Set<Integer> keySet = valueToFeatureName.keySet();
//        for (Integer aKeySet : keySet) {
//            names.add(valueToFeatureName.get(aKeySet));
//        }
//        return names.toArray(new String[names.size()]);
//    }
//
//    public String getEntityName() {
//        return valueToFeatureName.get(value());
//    }
//
    public final int value() {
        return value;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EntityTypeMapping)) {
            return false;
        }
        EntityTypeMapping other = (EntityTypeMapping) obj;
        return other.value() == this.value();
    }

    public int hashCode() {
        return this.value();
    }

    public String toString() {
        return valueToDisplayName.get(value());
    }

}
