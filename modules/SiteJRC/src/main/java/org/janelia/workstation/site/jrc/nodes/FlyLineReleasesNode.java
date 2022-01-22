package org.janelia.workstation.site.jrc.nodes;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.sample.LineRelease;
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
 * A node which shows the items most recently opened by the user.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleasesNode extends IdentifiableNode {

    private final static Logger log = LoggerFactory.getLogger(FlyLineReleasesNode.class);

    public static final String NODE_NAME = "Fly Line Releases";
    public static final long NODE_ID = 40L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final LineReleaseNodeChildFactory childFactory;

    public FlyLineReleasesNode() {
        this(new LineReleaseNodeChildFactory());
    }

    private FlyLineReleasesNode(LineReleaseNodeChildFactory childFactory) {
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
        return Icons.getIcon("folder_image.png").getImage();
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
        actions.add(new CreateNewReleaseAction());
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

    protected final class CreateNewReleaseAction extends AbstractAction {

        CreateNewReleaseAction() {
            putValue(NAME, "Create New Fly Line Release...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "<html>Creating a <b>Fly Line Release</b> will allow you to publish your data to Janelia's external Split-GAL4 website.<br><br>" +
                                   "To create a new Fly Line Release, select one or more Samples that you would like to publish, <br>" +
                                   "right-click one of them, and choose the '<b>Stage Samples for Publishing</b>' option.</html>",
                    "How to create a new Fly Line Release",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static class LineReleaseNodeChildFactory extends ChildFactory<LineRelease> {
        
        @Override
        protected boolean createKeys(List<LineRelease> list) {
            try {
                log.debug("Creating children keys for FlyLineReleasesNode");
                list.addAll(DomainMgr.getDomainMgr().getModel().getLineReleases());
            } 
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(LineRelease key) {
            try {
                return new FlyLineReleaseNode(this, key);
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
        if (event.getDomainObject() instanceof LineRelease) {
            refreshChildren();
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (event.getDomainObject() instanceof LineRelease) {
            refreshChildren();
        }
    }
}
