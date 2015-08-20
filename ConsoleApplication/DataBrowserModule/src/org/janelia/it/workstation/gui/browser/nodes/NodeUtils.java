package org.janelia.it.workstation.gui.browser.nodes;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.Node;
import org.openide.util.Enumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with domain object nodes. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeUtils {

    private final static Logger log = LoggerFactory.getLogger(NodeUtils.class);
    
    public static String createPathString(Long[] idPath) {
        StringBuilder sb = new StringBuilder();
        for(Long id : idPath) {
            if (sb.length()>0) sb.append("/");
            sb.append(id);
        }
        return sb.toString();
    }
    
    public static String createPathString(Node node) {
        Long[] ids = createIdPath(node);
        return createPathString(ids);
    }
    
    public static Long[] createIdPath(String pathString) {
        List<Long> path = new ArrayList<>();
        for(String s : pathString.split("/")) {
            path.add(new Long(s));
        }
        return path.toArray(new Long[path.size()]);
    }
    
    public static Long[] createIdPath(HasIdentifier... objs) {
        
        Long[] array = new Long[objs.length];
        
        int i = 0;
        for(HasIdentifier obj : objs) {
            array[i++] = obj.getId();
        }
        
        return array;
    }
    
    public static Long[] createIdPath(Node parentNode, HasIdentifier obj) {
        
        Long[] parentPath = createIdPath(parentNode);
        Long[] array = new Long[parentPath.length+1];
        
        int i = 0;
        for(Long id : parentPath) {
            array[i++] = id;
        }
        
        array[array.length-1] = obj.getId();
        return array;
    }
    
    public static Long[] createIdPath(Node node) {
        
        LinkedList<Long> ar = new LinkedList<>();
        
        if (node instanceof RootNode) {
            return new Long[0];
        }
        else if (node instanceof HasIdentifier) {
            while ((node != null) && (node instanceof HasIdentifier)) {
                ar.addFirst(((HasIdentifier)node).getId());
                node = node.getParentNode();
            }
        }
        else {
            log.warn("Unsupported node type: {}",node.getClass());
            return null;
        }

        Long[] res = new Long[ar.size()];
        ar.toArray(res);
        return res;
    }
    
    public static Node findNodeWithPath(Node start, Long[] ids) {

        if (log.isTraceEnabled()) {
            log.trace("findNodeWithPath({},{})",start.getDisplayName(),NodeUtils.createPathString(ids));
        }
        
        if (ids==null || ids.length==0) {
            return start;
        }
        
        Enumeration<Long> enumeration = Enumerations.array(ids);
        
        if (start instanceof HasIdentifier) {
            HasIdentifier hasId = (HasIdentifier)start;
            if (ids[0].equals(hasId.getId())) { 
                // skip the first node, because its this one
                enumeration.nextElement();
            }
        }
        
        while (enumeration.hasMoreElements()) {
            Long id = enumeration.nextElement();
            Node next = findChild(start, id);
            if (next == null) {
                return null;
            }
            else {
                start = next;
            }
        }

        return start;
    }

    public static Node findChild(Node node, Long id) {
        
        if (log.isTraceEnabled()) {
            log.trace("findChild({},{})",node.getDisplayName(),id);
        }
        
        Node[] list = node.getChildren().getNodes();

        if (list.length == 0) {
            return null;
        }

        for (int i = 0; i < list.length; i++) {
            Node child = list[i];
            if (log.isTraceEnabled()) {
                log.trace("findChild - checking {}",child.getDisplayName());
            }
            if (child instanceof HasIdentifier) {
                HasIdentifier hasId = (HasIdentifier)list[i];
                if (id.equals(hasId.getId())) { 
                    return list[i];
                }
            }
        }

        return null;
    }
}
