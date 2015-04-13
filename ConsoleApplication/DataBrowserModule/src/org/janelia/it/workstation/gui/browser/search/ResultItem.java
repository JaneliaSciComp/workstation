package org.janelia.it.workstation.gui.browser.search;

import java.util.HashMap;
import java.util.Map;

/**
 * A single result item.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultItem extends HashMap<String,Object> {

    public ResultItem(Long id) {
        put("id",id);
    }
            
    public ResultItem(Map<String,Object> map) {
        for(String key : map.keySet()) {
            put(key, map.get(key));
        }
    }
    
    public Long getId() {
        Object id = get("id");
        if (id==null) {
            throw new IllegalStateException("Result item has no id");
        }
        return new Long(id.toString());
    }
}
