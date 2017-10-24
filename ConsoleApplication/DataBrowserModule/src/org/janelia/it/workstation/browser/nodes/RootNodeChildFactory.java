package org.janelia.it.workstation.browser.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.model.DomainObjectComparator;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.Workspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main child factory for the root node in the explorer.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootNodeChildFactory extends ChildFactory<DomainObject> {

    private static final Logger log = LoggerFactory.getLogger(RootNodeChildFactory.class);
    
    private final DummyObject RECENTLY_OPENED_ITEMS = new DummyObject();
    private final RecentOpenedItemsNode RECENTLY_OPENED_ITEMS_NODE = new RecentOpenedItemsNode();
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
        try {
            if (DomainExplorerTopComponent.isShowRecentMenuItems()) {
                list.add(RECENTLY_OPENED_ITEMS);
            }
            
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            List<Workspace> workspaces = new ArrayList<>(model.getWorkspaces());
            
            for(Workspace workspace : workspaces) {
                log.info("Adding workspace: {} ({})", workspace.getName(), workspace.getOwnerKey());
            }
            
            Collections.sort(workspaces, new DomainObjectComparator());
            list.addAll(workspaces);
        } 
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
            return false;
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(DomainObject key) {
        try {
            if (key.equals(RECENTLY_OPENED_ITEMS)) {
                return RECENTLY_OPENED_ITEMS_NODE;
            }
            if (Workspace.class.isAssignableFrom(key.getClass())) {
                return new WorkspaceNode((Workspace) key);
            }
            else {
                throw new IllegalStateException("Illegal root node: " + key.getClass().getName());
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
        return null;
    }

    public void refresh() {
        refresh(true);
    }
    
    public RecentOpenedItemsNode getRecentlyOpenedItemsNode() {
        return RECENTLY_OPENED_ITEMS_NODE;
    }
    
    /**
     * Object class for creating singleton nodes.
     */
    private static class DummyObject implements DomainObject {
        
        DummyObject() {
        }
        
        @Override
        public Long getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(Long id) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setName(String name) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public String getOwnerKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOwnerKey(String ownerKey) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public Set<String> getReaders() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setReaders(Set<String> readers) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public Set<String> getWriters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWriters(Set<String> writers) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public Date getCreationDate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCreationDate(Date creationDate) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public Date getUpdatedDate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setUpdatedDate(Date updatedDate) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public String getType() {
            throw new UnsupportedOperationException();
        }
    }
}
