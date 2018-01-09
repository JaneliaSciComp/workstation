package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Image;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nodes.IdentifiableNode;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A node which shows all of the color depth searches that a user has created.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchesNode extends IdentifiableNode {
        
    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchesNode.class);
    
    private static final long COLOR_DEPTH_SEARCHES_ID = 4L; // This magic number means nothing, it just needs to be unique and different from GUID space.

    private static Set<ColorDepthSearchesNode> instances = Collections.newSetFromMap(new WeakHashMap<ColorDepthSearchesNode, Boolean>());

    private void register(ColorDepthSearchesNode instance) {
        instances.add(instance);
    }
    
    public static Set<ColorDepthSearchesNode> getInstances() {
        return Collections.unmodifiableSet(instances);
    }
    
    static {
        Events.getInstance().registerOnEventBus(new Object() {

            @Subscribe
            public void objectCreated(DomainObjectCreateEvent event) {
                updateNodes(event);
            }
            
            @Subscribe
            public void objectRemoved(DomainObjectRemoveEvent event) {
                updateNodes(event);
            }
        });
    }

    private static void updateNodes(DomainObjectEvent event) {
        if (event.getDomainObject() instanceof ColorDepthSearch) {
            for (ColorDepthSearchesNode colorDepthSearchesNode : ColorDepthSearchesNode.getInstances()) {
                colorDepthSearchesNode.refreshChildren();
            }
        }
    }
    
    private final DomainObjectNodeChildFactory childFactory;

    public ColorDepthSearchesNode() {
        this(new DomainObjectNodeChildFactory());
    }
    
    private ColorDepthSearchesNode(DomainObjectNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
        register(this);
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
        return Icons.getIcon("folder_explore.png").getImage();
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
