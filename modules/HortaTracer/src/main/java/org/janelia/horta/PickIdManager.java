package org.janelia.horta;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * PickIdManager assigns temporarily unique positive integer IDs 
 * for display objects, from a potentially small range of possible IDs.
 * @author Christopher Bruns
 */
public class PickIdManager
{
    private final int maxId;
    private final Map<Object, Integer> idForItem = new HashMap<>();
    private final Map<Integer, Object> itemForId = new HashMap<>();
    private final Queue<Integer> recycledIds;

    private int maxUsedId = 0;
    
    public PickIdManager(int maxId) {
        this.maxId = maxId;
        recycledIds  = new PriorityQueue<>(255);
    }
    
    public int idForItem(Object item) {
        if (! idForItem.containsKey(item)) {
            int newId = maxUsedId + 1;
            if (! recycledIds.isEmpty()) {
                newId = recycledIds.remove();
            }
            if (newId > maxUsedId)
                maxUsedId = newId;
            idForItem.put(item, newId);
            itemForId.put(newId, item);
        }
        return idForItem.get(item);
    }
    
    public Object itemForId(int id) {
        return itemForId.get(id);
    }
    
    public void remove(Object item) {
        if (! idForItem.containsKey(item))
            return;
        int id = idForItem.get(item);
        idForItem.remove(item);
        itemForId.remove(id);
        recycledIds.add(id);
    }
}
