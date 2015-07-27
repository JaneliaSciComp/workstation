package org.janelia.it.workstation.gui.browser.nodes;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.openide.nodes.Node;
import org.openide.util.Enumerations;

/**
 * Utilities for dealing with domain object nodes. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeUtils {

    public static String createPathString(Long[] idPath) {
        StringBuilder sb = new StringBuilder();
        for(Long id : idPath) {
            if (sb.length()>0) sb.append("/");
            sb.append(id);
        }
        return sb.toString();
    }
    
    public static String createPathString(DomainObjectNode node) {
        LinkedList<Long> ids = new LinkedList<>();

        Node currNode = node;
        while ((currNode != null) && (currNode instanceof DomainObjectNode)) {

            DomainObjectNode objNode = (DomainObjectNode)currNode;
            ids.addFirst(objNode.getDomainObject().getId());
            currNode = currNode.getParentNode();
        }

        return createPathString(ids.toArray(new Long[ids.size()]));
    }
    
    public static Long[] createIdPath(String pathString) {
        List<Long> path = new ArrayList<>();
        for(String s : pathString.split("/")) {
            path.add(new Long(s));
        }
        return path.toArray(new Long[path.size()]);
    }
    
    public static Long[] createIdPath(DomainObject... domainObjects) {
        Long[] array = new Long[domainObjects.length];
        int i = 0;
        for(DomainObject domainObject : domainObjects) {
            array[i++] = domainObject.getId();
        }
        return array;
    }
    
    public static Long[] createIdPath(Node parentNode, DomainObject domainObject) {
        Long[] parentPath = createIdPath(parentNode);
        Long[] array = new Long[parentPath.length+1];
        int i = 0;
        for(Long id : parentPath) {
            array[i++] = id;
        }
        array[array.length-1] = domainObject.getId();
        return array;
    }
    
    public static Long[] createIdPath(Node node) {
        if (node instanceof RootNode) {
            return new Long[0];
        }
        else if (node instanceof DomainObjectNode) {
            DomainObjectNode objNode = (DomainObjectNode)node;
            LinkedList<Long> ar = new LinkedList<>();

            Node currNode = objNode;
            while ((currNode != null) && (currNode instanceof DomainObjectNode)) {
                ar.addFirst(((DomainObjectNode)currNode).getDomainObject().getId());
                currNode = currNode.getParentNode();
            }

            Long[] res = new Long[ar.size()];
            ar.toArray(res);
            return res;
        }
        else {
            throw new IllegalArgumentException("Unsupported node type: "+node.getClass());
        }
    }
    
    public static Node findNodeWithPath(Node start, Long[] ids) {

        if (ids.length==0) {
            return start;
        }
        
        Enumeration<Long> enumeration = Enumerations.array(ids);
        
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
        
        Node[] list = node.getChildren().getNodes();

        if (list.length == 0) {
            return null;
        }

        for (int i = 0; i < list.length; i++) {
            DomainObjectNode objNode = (DomainObjectNode)list[i];
            if (id.equals(objNode.getDomainObject().getId())) { 
                return list[i];
            }
        }

        return null;
    }
}
