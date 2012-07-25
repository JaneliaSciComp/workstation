package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:10 PM
 * This class allows you to define a set of entities and give it a certain name.
 * <p/>
 * The static methods allow you to get all defined EntityTypeSets, or a given set by name.
 * <p/>
 * The initial sets are created by reading the resource.shared.EntityTypeSet.properties file.
 * <p/>
 * Other sets can be added using the public constructor, which will had them into the array
 * returned by the static method getEntityTypeSets().
 */
public class EntityTypeSet extends AbstractSet implements java.io.Serializable {

    private static Map nameToEntityTypeSet = new HashMap();

    private static final long serialVersionUID = 1;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("resource.shared.EntityTypeSet");
        String setName;
        String resourceValue;
        StringTokenizer tokenizer;
        EntityTypeSet newSet;
        List typeList = new ArrayList();
        try {
            for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
                setName = (String) e.nextElement();
                resourceValue = bundle.getString(setName);
                if (resourceValue.indexOf(",") == -1) {
                    newSet = new EntityTypeSet(setName, new EntityTypeMapping[]{EntityTypeMapping.getEntityTypeMappingForName(resourceValue)});
                    nameToEntityTypeSet.put(setName, newSet);
                }
                else {
                    tokenizer = new StringTokenizer(resourceValue, ",");
                    typeList.clear();
                    while (tokenizer.hasMoreTokens()) {
                        typeList.add(EntityTypeMapping.getEntityTypeMappingForName(tokenizer.nextToken()));
                    }
                    newSet = new EntityTypeSet(setName, (EntityTypeMapping[]) typeList.toArray(new EntityTypeMapping[0]));
                    nameToEntityTypeSet.put(setName, newSet);
                }
            }
        }
        catch (Exception ex) {
            FacadeManager.handleException(ex);
        }
    } // End static initializer

    public static EntityTypeSet[] getEntityTypeSets() {
        ArrayList sets = new ArrayList();
        Set keySet = nameToEntityTypeSet.keySet();
        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
            sets.add(nameToEntityTypeSet.get(it.next()));
        }
        return (EntityTypeSet[]) sets.toArray(new EntityTypeSet[0]);
    }

    public static String[] getEntityTypeSetNames() {
        ArrayList names = new ArrayList();
        Set keySet = nameToEntityTypeSet.keySet();
        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
            names.add(it.next());
        }
        return (String[]) names.toArray(new String[0]);
    }

    public static EntityTypeSet getEntityTypeSet(String setName) {
        return (EntityTypeSet) nameToEntityTypeSet.get(setName);
    }

    //private ArrayList entityTypes=new ArrayList();
    private Set entityTypes = new HashSet();
    private String name;

    public EntityTypeSet() {
    }

    ;


    public EntityTypeSet(String setName, EntityTypeMapping[] entityTypes) {
        for (int i = 0; i < entityTypes.length; i++) {
            this.entityTypes.add(entityTypes[i]);
        }
        nameToEntityTypeSet.put(setName, this);
        name = setName;
    }

    /**
     * Creates a more temporary EntityTypeSet that cannot be retrieved by
     * name at a later date.
     */
    public EntityTypeSet(EntityTypeMapping[] entityTypes) {
        for (int i = 0; i < entityTypes.length; i++) {
            this.entityTypes.add(entityTypes[i]);
        }
        name = "";
    }


    public int size() {
        return entityTypes.size();
    }

    public Iterator iterator() {
        return entityTypes.iterator();
    }

    public String toString() {
        return "Entity Type Set: " + name;
    }

    public EntityTypeMapping[] getEntityTypes() {
        return (EntityTypeMapping[]) entityTypes.toArray(new EntityTypeMapping[0]);
    }

    public boolean equals(Object other) {
        if ((other != null) && (other instanceof EntityTypeSet)) {
            EntityTypeSet otherSet = (EntityTypeSet) other;
            return (this.entityTypes.equals(otherSet.entityTypes));
        }
        else {
            return false;
        }
    }

    public boolean add(Object o) {
        if (!(o instanceof EntityTypeMapping))
            throw new IllegalArgumentException("Entity Type Set can only contain EntityTypes");
        name = "";
        return entityTypes.add(o);
    }


}