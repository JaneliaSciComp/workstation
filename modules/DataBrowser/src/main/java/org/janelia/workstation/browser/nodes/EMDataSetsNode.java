package org.janelia.workstation.browser.nodes;

import org.janelia.model.domain.flyem.EMDataSet;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.DomainMgr;
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
public class EMDataSetsNode extends IdentifiableNode {

    private final static Logger log = LoggerFactory.getLogger(EMDataSetsNode.class);

    public static final String NODE_NAME = "FlyEM Data Sets";
    public static final long NODE_ID = 25L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final DataSetNodeChildFactory childFactory;

    public EMDataSetsNode() {
        this(new DataSetNodeChildFactory());
    }

    private EMDataSetsNode(DataSetNodeChildFactory childFactory) {
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

    private static class DataSetNodeChildFactory extends ChildFactory<EMDataSet> {

        @Override
        protected boolean createKeys(List<EMDataSet> list) {
            try {
                log.debug("Creating children keys for DataSetsNode");
                list.addAll(DomainMgr.getDomainMgr().getModel().getEMDataSets());
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(EMDataSet key) {
            try {
                return new EMDataSetNode(this, key);
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
}
