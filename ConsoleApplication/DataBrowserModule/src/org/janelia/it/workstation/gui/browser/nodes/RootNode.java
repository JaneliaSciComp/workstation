package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.model.DomainObjectComparator;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The root node of the Data Explorer, containing all the Workspace nodes
 * that the user can read.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootNode extends AbstractNode {
    
    private final static Logger log = LoggerFactory.getLogger(RootNode.class);
    
    private final RootNodeChildFactory childFactory;
    
    public RootNode() {
        this(new RootNodeChildFactory());
    }
    
    private RootNode(RootNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }
       
    @Override
    public String getDisplayName() {
        // This node should never be displayed
        return "ROOT";
    }
    
    @Override
    public Image getIcon(int type) {
        // This node should never be displayed
        return null;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    public void refreshChildren() {
        childFactory.refresh();
    }   
    
    private static class RootNodeChildFactory extends ChildFactory<DomainObject> {

        @Override
        protected boolean createKeys(List<DomainObject> list) {
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<Workspace> workspaces = new ArrayList<>(model.getWorkspaces());
                Collections.sort(workspaces, new DomainObjectComparator());
                list.addAll(workspaces);
            } catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
                return false;
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(DomainObject key) {
            try {
                if (Workspace.class.isAssignableFrom(key.getClass())) {
                    return new WorkspaceNode((Workspace) key);
                }
                else {
                    throw new IllegalStateException("Illegal root node: " + key.getClass().getName());
                }
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

        public void refresh() {
            refresh(true);
        }
    }
}
