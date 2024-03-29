package org.janelia.workstation.colordepth.gui;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.AccessManager;
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
 * A node which shows the color depth libraries accessible to the current user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchesNode extends IdentifiableNode {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchesNode.class);

    public static final String NODE_NAME = "Color Depth Searches";
    public static final long NODE_ID = 31L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final ColorDepthSearchesChildFactory childFactory;

    ColorDepthSearchesNode() {
        this(new ColorDepthSearchesChildFactory());
    }

    private ColorDepthSearchesNode(ColorDepthSearchesChildFactory childFactory) {
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
        return Icons.getIcon("folder_palette.png").getImage();
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
            putValue(NAME, "Create New Color Depth Search");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "To create a new search, right-click any color depth MIP and select 'Create Mask for Color Depth Search'");
        }
    }

    private static class ColorDepthSearchesChildFactory extends ChildFactory<ColorDepthSearch> {

        @Override
        protected boolean createKeys(List<ColorDepthSearch> list) {
            try {
                log.debug("Creating children keys for ColorDepthSearchesNode");
                list.addAll(DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(ColorDepthSearch.class));
                list.sort(new DomainObjectComparator(AccessManager.getSubjectKey()));
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(ColorDepthSearch key) {
            try {
                return new ColorDepthSearchNode(this, key);
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
        if (event.getDomainObject() instanceof ColorDepthSearch) {
            refreshChildren();
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (event.getDomainObject() instanceof ColorDepthSearch) {
            refreshChildren();
        }
    }
}
