package org.janelia.workstation.core.nodes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton which keeps track of IdentifiableNodes as they are created and destroyed, 
 * so that we can easily find them by id. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"}) // This class is a generic type disaster
public class NodeTracker {

    private static final Logger log = LoggerFactory.getLogger(NodeTracker.class);
    
    private static final NodeTracker singleton = new NodeTracker();
    
    public static NodeTracker getInstance() {
        return singleton;
    }
    
    private final Multimap<Long, WeakReference> nodesById = LinkedListMultimap.create();
    
    private NodeTracker() {
    }

    /**
     * Package private method for nodes to use to register themselves.
     * @param node
     */
    public void registerNode(final IdentifiableNode node) {
        // Clear existing references to similar nodes
        int c = 0;
        for(Iterator<WeakReference> iterator = nodesById.get(node.getId()).iterator(); iterator.hasNext(); ) {
            WeakReference<IdentifiableNode> ref = iterator.next();
            if (ref.get()==null) {
                log.trace("removing expired reference for {}",node.getId());
                iterator.remove();
            }
            else {
                c++;
            }
        }
        if (c>1) {
            log.warn("Object {} has {} nodes",node.getDisplayName(), c);
        }

        nodesById.put(node.getId(), new WeakReference<>(node));
        log.debug("registered node@{} - {}",System.identityHashCode(node),node.getDisplayName());
    }

    /**
     * Package private method for nodes to use to deregister themselves.
     * @param node
     */
    public void deregisterNode(final IdentifiableNode node) {
        Long id = node.getId();
        for(Iterator<WeakReference> iterator = nodesById.get(id).iterator(); iterator.hasNext(); ) {
            WeakReference<IdentifiableNode> ref = iterator.next();
            IdentifiableNode regNode = ref.get();
            if (regNode==node) {
                log.debug("unregistered node@{} - {}",System.identityHashCode(regNode),regNode.getDisplayName());
                iterator.remove();
            }
        }    
    }

    /**
     * Returns all of the nodes currently existing for the given object. 
     * @param object
     * @return
     */
    public Set<IdentifiableNode> getNodesByObject(HasIdentifier object) {
        log.debug("getting nodes for {}",object);
        return getNodesById(object.getId());
    }

    /**
     * Get all the nodes currently in use which hold a domain object with the given id.
     * @param id
     * @return
     */
    public Set<IdentifiableNode> getNodesById(Long id) {
        log.debug("getting nodes with id {}",id);
        List<IdentifiableNode> nodes = new ArrayList<>();
        for(Iterator<WeakReference> iterator = nodesById.get(id).iterator(); iterator.hasNext(); ) {
            WeakReference<IdentifiableNode> ref = iterator.next();
            IdentifiableNode node = ref.get();
            if (node==null) {
                iterator.remove();
            }
            else {
                nodes.add(node);
            }
        }
        // Newest nodes first
        Collections.reverse(nodes);
        return new LinkedHashSet<>(nodes);
    }
}
