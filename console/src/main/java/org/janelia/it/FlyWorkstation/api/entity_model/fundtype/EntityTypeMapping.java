package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.jacs.model.entity.EntityType;

import java.util.*;

public class EntityTypeMapping implements java.io.Serializable {
    static private Map environmentToEntityName = new HashMap();
    static private Map valueToFeatureName = new HashMap();
    static private Map valueToDisplayName = new HashMap();
    static private Map featureNameToValue = new HashMap();
    static private Map valueToEntityType = new HashMap();

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
                if (resourceValue.indexOf("|") == -1) {
                    featureName = resourceValue;
                    displayName = featureName;
                }
                else {
                    tokenizer = new StringTokenizer(resourceValue, "|");
                    featureName = tokenizer.nextToken();
                    displayName = tokenizer.nextToken();
                    while (tokenizer.hasMoreTokens()) {
                        environmentToEntityName.put(tokenizer.nextToken(), featureName);
                    }
                }
                valueToFeatureName.put(newValue, featureName);
                valueToDisplayName.put(newValue, displayName);
                featureNameToValue.put(featureName, newValue);
                valueToEntityType.put(newValue, new EntityTypeMapping(newValue.intValue()));
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
        EntityTypeMapping et = (EntityTypeMapping) valueToEntityType.get(new Integer(value));
        if (et == null) throw new IllegalArgumentException("Entity Value " + value + " is undefined");
        return et;
    }

    public static EntityTypeMapping getEntityTypeMappingForName(String name) {
        Integer value = (Integer) featureNameToValue.get(name);
        if (value == null) throw new IllegalArgumentException("Feature " + name + " is undefined");
        return getEntityTypeMappingForValue(value.intValue());
    }

    public static EntityTypeMapping getEntityTypeMappingForEnvironment(String discoveryEnvironment) {
        String name = (String) environmentToEntityName.get(discoveryEnvironment);
        if (name == null)
            throw new IllegalArgumentException("Feature with discovery environment " + discoveryEnvironment + " is undefined");
        return getEntityTypeMappingForName(name);
    }

    public static EntityType[] allEntityTypes() {
        ArrayList entities = new ArrayList();
        Set keySet = valueToEntityType.keySet();
        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
            entities.add(valueToEntityType.get(it.next()));
        }
        return (EntityType[]) entities.toArray(new EntityType[0]);
    }

    public static String[] allEntityNames() {
        ArrayList names = new ArrayList();
        Set keySet = valueToFeatureName.keySet();
        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
            names.add(valueToFeatureName.get(it.next()));
        }
        return (String[]) names.toArray(new String[0]);
    }

    public String getEntityName() {
        return (String) valueToFeatureName.get(new Integer(value()));
    }

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
        return (String) valueToDisplayName.get(new Integer(value()));
    }

}
