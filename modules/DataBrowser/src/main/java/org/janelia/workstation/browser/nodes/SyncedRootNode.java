package org.janelia.workstation.browser.nodes;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.model.DomainObjectCreateEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A node corresponding to a single SynchedRoot in the domain model. It shows the root's SynchedPaths as children.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncedRootNode extends IdentifiableNode {

    private final static Logger log = LoggerFactory.getLogger(SyncedRootNode.class);

    public static final String NODE_NAME = "Synchronized Folders";
    public static final long NODE_ID = 30L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final SyncedRootChildFactory childFactory;

    SyncedRootNode() {
        this(new SyncedRootChildFactory());
    }

    private SyncedRootNode(SyncedRootChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
        NodeTracker.getInstance().registerNode(this);
    }

    @Override
    public Long getId() {
        return NODE_ID;
    }

    @Override
    public String getDisplayName() {
        return NODE_NAME;
    }

    @Override
    public String getHtmlDisplayName() {
        String primary = getDisplayName();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        return sb.toString();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_database.png").getImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    public void refreshChildren() {
        childFactory.refresh();
    }

    @Override
    public Action[] getActions(boolean context) {
        Collection<Action> actions = new ArrayList<>();
        actions.add(new PopupLabelAction());
        actions.add(new SearchAction());
        return actions.toArray(new Action[0]);
    }

    protected final class PopupLabelAction extends AbstractAction {

        PopupLabelAction() {
            putValue(NAME, getDisplayName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    protected final class SearchAction extends AbstractAction {

        SearchAction() {
            putValue(NAME, "Search Color Depth MIPs");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "To search a library, right-click any color depth MIP and select 'Create Mask for Color Depth Search'");
        }
    }

    private static class SyncedRootChildFactory extends ChildFactory<SyncedRoot> {

        @Override
        protected boolean createKeys(List<SyncedRoot> list) {
            try {
                log.debug("Creating children keys for SyncedRootNode");
                list.addAll(DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(SyncedRoot.class));
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(SyncedRoot key) {
            try {
                return new SyncedPathNode(this, key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

        public void refresh() {
            log.debug("Refreshing child factory for "+getClass().getSimpleName());
            refresh(true);
        }
    }

    @Subscribe
    public void domainObjectAdded(DomainObjectCreateEvent event) {
        if (event.getDomainObject() instanceof ColorDepthLibrary) {
            refreshChildren();
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (event.getDomainObject() instanceof ColorDepthLibrary) {
            refreshChildren();
        }
    }
}
