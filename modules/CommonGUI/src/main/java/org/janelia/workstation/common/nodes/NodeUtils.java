package org.janelia.workstation.common.nodes;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.interfaces.HasIdentifier;
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

    // We use a single threaded executor so that all node operations are serialized. Re-ordering operations 
    // in particular must be done sequentially, for obvious reasons.
    private static final Executor nodeOperationExecutor = Executors.newSingleThreadExecutor();
    
    public static void executeNodeOperation(SimpleWorker worker) {
        nodeOperationExecutor.execute(worker);
    }
    
    /**
     * Given a number of children, reorder the last node to the given index, and return the ordering array.
     * @param size
     * @param index
     * @return
     */
    public static int[] getReordering(int size, int index) {
        int[] order = new int[size];
        int curr = 0;
        for(int i=0; i<size; i++) {
            if (i==index) {
                order[i] = size-1;
                curr++;
            }
            order[i] = curr++;
        }
        order[size-1] = index;
        return order;
    }
    
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

        if (node==null) throw new IllegalArgumentException("Null node provided");

        LinkedList<Long> ar = new LinkedList<>();
        
        if (node instanceof HasIdentifier) {
            while (node instanceof HasIdentifier) {
                ar.addFirst(((HasIdentifier)node).getId());
                node = node.getParentNode();
            }
        }
        else {
            log.warn("Unsupported node type {} for node '{}'",node.getClass(),node.getDisplayName());
            return null;
        }

        Long[] res = new Long[ar.size()];
        ar.toArray(res);
        return res;
    }
    
    public static Node findNodeWithPath(Node start, Long[] ids) {

        if (log.isTraceEnabled()) {
            log.trace("findNodeWithPath(start={},{})",start.getDisplayName(),NodeUtils.createPathString(ids));
        }
        
        if (ids==null || ids.length==0) {
            return start;
        }
        
        Enumeration<Long> path = Enumerations.array(ids);
        
        if (start instanceof HasIdentifier) {
            HasIdentifier hasId = (HasIdentifier)start;
            if (ids[0].equals(hasId.getId())) { 
                // skip the first node, because its this one
                Long first = path.nextElement();
                log.trace("Skipping first path element: {}",first);
            }
        }
        
        while (path.hasMoreElements()) {
            Long id = path.nextElement();
            log.trace("Looking in {} for next path element: {}",start.getName(),id);
            Node next = findChild(start, id);
            if (next == null) {
                log.trace("Could not find child with id: {}",id);
                return null;
            }
            else {
                start = next;
            }
        }

        return start;
    }

    public static Node findChild(Node node, Long id) {
        
        if (id==null) return null;
        log.trace("  findChild(parent={},childId={})",node.getDisplayName(),id);
        
        Node[] list = node.getChildren().getNodes();
        if (list.length == 0) {
            return null;
        }

        for (int i = 0; i < list.length; i++) {
            Node child = list[i];
            if (child instanceof HasIdentifier) {
                HasIdentifier hasId = (HasIdentifier)list[i];
                log.trace("    findChild checking {} ({})",hasId.getId(),child.getDisplayName());
                if (hasId!=null && id.equals(hasId.getId())) { 
                    return list[i];
                }
            }
        }

        return null;
    }
}
