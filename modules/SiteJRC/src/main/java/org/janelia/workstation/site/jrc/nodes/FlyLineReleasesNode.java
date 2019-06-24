package org.janelia.workstation.site.jrc.nodes;

import java.awt.Image;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.sample.LineRelease;
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
public class FlyLineReleasesNode extends AbstractNode implements HasIdentifier {

    private final static Logger log = LoggerFactory.getLogger(FlyLineReleasesNode.class);

    private static final long RECENTLY_OPENED_ID = 30L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private final LineReleaseNodeChildFactory childFactory;

    public FlyLineReleasesNode() {
        this(new LineReleaseNodeChildFactory());
    }

    private FlyLineReleasesNode(LineReleaseNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }

    @Override
    public Long getId() {
        return RECENTLY_OPENED_ID;
    }

    @Override
    public String getDisplayName() {
        return "Fly Line Releases";
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
                return new FlyLineReleaseNode(this, (LineRelease)key);
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
