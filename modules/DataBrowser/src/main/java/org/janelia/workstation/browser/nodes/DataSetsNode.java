package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.util.List;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which shows the items most recently opened by the user.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetsNode extends AbstractNode implements HasIdentifier {

    private final static Logger log = LoggerFactory.getLogger(DataSetsNode.class);

    private static final long DATA_SETS_NODE_ID = 20L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final DataSetNodeChildFactory childFactory;

    public DataSetsNode() {
        this(new DataSetNodeChildFactory());
    }

    private DataSetsNode(DataSetNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }

    @Override
    public Long getId() {
        return DATA_SETS_NODE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Data Sets";
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
        return Icons.getIcon("folder-white-icon.png").getImage();
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

    private static class DataSetNodeChildFactory extends ChildFactory<DataSet> {

        @Override
        protected boolean createKeys(List<DataSet> list) {
            try {
                log.debug("Creating children keys for FlyLineReleasesNode");
                list.addAll(DomainMgr.getDomainMgr().getModel().getDataSets());
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(DataSet key) {
            try {
                return new DataSetNode(this, key);
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
