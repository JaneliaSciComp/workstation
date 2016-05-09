package org.janelia.it.workstation.gui.browser.nodes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton which keeps track of DomainObjectNodes as they are created and destroyed, so that 
 * we can easily find them by id. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeTracker {

    private static final Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);
    
    private static final DomainObjectNodeTracker singleton = new DomainObjectNodeTracker();
    
    public static DomainObjectNodeTracker getInstance() {
        return singleton;
    }
    
    private final Multimap<Long, WeakReference> nodesById = HashMultimap.<Long, WeakReference>create();
    
    private DomainObjectNodeTracker() {
    }

    /**
     * Package private method for nodes to use to register themselves.
     * @param node
     */
    void registerNode(final DomainObjectNode node) {
        // Clear existing references to similar nodes
        int c = 0;
        for(Iterator<WeakReference> iterator = nodesById.get(node.getId()).iterator(); iterator.hasNext(); ) {
            WeakReference<DomainObjectNode> ref = iterator.next();
            if (ref.get()==null) {
                log.trace("removing expired reference for {}",node.getId());
                iterator.remove();
            }
            else {
                c++;
            }
        }
        if (c>1) {
            log.trace("Domain object {} has {} nodes",node.getDisplayName(), c);
        }
        
        nodesById.put(node.getId(), new WeakReference<>(node));
        log.debug("registered node@{} - {}",System.identityHashCode(node),node.getDisplayName());
    }
    
    /**
     * Package private method for nodes to use to deregister themselves.
     * @param node
     */
    void deregisterNode(final DomainObjectNode node) {
        Long id = node.getId();
        for(Iterator<WeakReference> iterator = nodesById.get(id).iterator(); iterator.hasNext(); ) {
            WeakReference<DomainObjectNode> ref = iterator.next();
            DomainObjectNode regNode = ref.get();
            if (regNode==node) {
                log.debug("unregistered node@{} - {}",System.identityHashCode(regNode),regNode.getDisplayName());
                iterator.remove();
            }
        }    
    }
    
    /**
     * Get all the nodes currently in use which hold a domain object with the given id.
     * @param id
     * @return
     */
    public Set<DomainObjectNode> getNodesById(Long id) {
        log.debug("getting nodes with id {}",id);
        Set<DomainObjectNode> nodes = new HashSet<>();
        for(Iterator<WeakReference> iterator = nodesById.get(id).iterator(); iterator.hasNext(); ) {
            WeakReference<DomainObjectNode> ref = iterator.next();
            DomainObjectNode node = ref.get();
            if (node==null) {
                iterator.remove();
            }
            else {
                nodes.add(node);
            }
        }
        return nodes;
    }
}
