package org.janelia.it.workstation.gui.browser.nodes;

import com.google.common.collect.ComparisonChain;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A root node for Workspaces, containing only workspaces owned by the user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserViewRootNode extends AbstractNode {
    
    private final static Logger log = LoggerFactory.getLogger(UserViewRootNode.class);
    
    private final UserViewConfiguration config;
    private final UserViewRootNodeChildFactory childFactory;
    
    public UserViewRootNode(UserViewConfiguration config) {
        this(new UserViewRootNodeChildFactory(config), config);
    }
    
    private UserViewRootNode(UserViewRootNodeChildFactory childFactory, UserViewConfiguration config) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
        this.config = config;
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
    
    private static class UserViewRootNodeChildFactory extends ChildFactory<DomainObject> {

        private final UserViewConfiguration config;
        
        UserViewRootNodeChildFactory(UserViewConfiguration config) {
            this.config = config;
        }
        
        @Override
        protected boolean createKeys(List<DomainObject> list) {
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<Workspace> workspaces = new ArrayList<>(model.getWorkspaces());

                List<Workspace> owned = new ArrayList<>();
                for (Workspace workspace : workspaces) {
                    if (ClientDomainUtils.isOwner(workspace)) {
                        owned.add(workspace);
                    }
                }

                Collections.sort(owned, new Comparator<Workspace>() {
                    @Override
                    public int compare(Workspace o1, Workspace o2) {
                        return ComparisonChain.start().compare(o1.getId(), o2.getId()).result();
                    }
                });

                // It's tempting to add directly to the list above, but it seems
                // to perform much better when we use addAll.
                list.addAll(owned);
                return true;
            } catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
                return false;
            }
        }

        @Override
        protected Node createNodeForKey(DomainObject key) {
            try {
                if (Workspace.class.isAssignableFrom(key.getClass())) {
                    return new UserViewTreeNodeNode((TreeNode) key, config);
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
