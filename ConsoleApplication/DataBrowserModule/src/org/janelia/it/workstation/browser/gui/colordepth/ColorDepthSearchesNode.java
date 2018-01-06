package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Image;
import java.util.List;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which shows all of the color depth searches that a user has created.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchesNode extends AbstractNode implements HasIdentifier {
        
    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchesNode.class);
    
    private static final long COLOR_DEPTH_SEARCHES_ID = 3L; // This magic number means nothing, it just needs to be unique and different from GUID space.
    
    private final DomainObjectNodeChildFactory childFactory;

    public ColorDepthSearchesNode() {
        this(new DomainObjectNodeChildFactory());
    }
    
    private ColorDepthSearchesNode(DomainObjectNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }

    @Override
    public Long getId() {
        return COLOR_DEPTH_SEARCHES_ID;
    }

    @Override
    public String getDisplayName() {
        return "My Color Depth Searches";
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
        return Icons.getIcon("drop-folder-white-icon.png").getImage();
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
    
    private static class DomainObjectNodeChildFactory extends ChildFactory<ColorDepthSearch> {
        
        @Override
        protected boolean createKeys(List<ColorDepthSearch> list) {
            try {
                log.debug("Creating children keys for ColorDepthSearchesNode");
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<ColorDepthSearch> children = model.getAllDomainObjectsByClass(ColorDepthSearch.class);
                log.debug("Got children: {}",children);
                list.addAll(children);
            } 
            catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(ColorDepthSearch key) {
            try {
                DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(key);
                if (provider!=null) {
                    return provider.getNode(key, this);
                }
                return null;
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
