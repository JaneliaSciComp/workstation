package org.janelia.it.workstation.gui.browser.nodes.children;

import com.google.common.collect.ComparisonChain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The children of the Explorer's root node are always the accessible Workspaces 
 * for the current user.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootNodeChildFactory extends ChildFactory<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(RootNodeChildFactory.class);
    
    public RootNodeChildFactory() {
    }
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
        DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        List<Workspace> workspaces = new ArrayList<>(dao.getWorkspaces(SessionMgr.getSubjectKey()));
        
        Collections.sort(workspaces, new Comparator<Workspace>() {
            @Override
            public int compare(Workspace o1, Workspace o2) {
                return ComparisonChain.start()
                    .compareTrueFirst(DomainUtils.isOwner(o1), DomainUtils.isOwner(o2))
                    .compare(o1.getOwnerKey(), o2.getOwnerKey())
                    .compare(o1.getId(), o2.getId()).result();
            }
        });
        
        list.addAll(workspaces);
        return true;
    }
    
    @Override
    protected Node createNodeForKey(DomainObject key) {
        try {
            if (Workspace.class.isAssignableFrom(key.getClass())) {
                return new TreeNodeNode(null, (TreeNode)key);
            }
            else {
                throw new IllegalStateException("Illegal root node: "+key.getClass().getName());
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
