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
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A node which shows all of the color depth masks that a user has created.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMasksNode extends AbstractNode implements HasIdentifier {
        
    private final static Logger log = LoggerFactory.getLogger(ColorDepthMasksNode.class);
    
    private static final long COLOR_DEPTH_MASKS_ID = 3L; // This magic number means nothing, it just needs to be unique and different from GUID space.
    
    private static Set<ColorDepthMasksNode> instances = Collections.newSetFromMap(new WeakHashMap<ColorDepthMasksNode, Boolean>());
    
    private void register(ColorDepthMasksNode instance) {
        instances.add(instance);
    }
    
    public static Set<ColorDepthMasksNode> getInstances() {
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
        if (event.getDomainObject() instanceof ColorDepthMask) {
            for (ColorDepthMasksNode colorDepthMasksNode : ColorDepthMasksNode.getInstances()) {
                colorDepthMasksNode.refreshChildren();
            }
        }
    }
    
    private final DomainObjectNodeChildFactory childFactory;

    public ColorDepthMasksNode() {
        this(new DomainObjectNodeChildFactory());
    }
    
    private ColorDepthMasksNode(DomainObjectNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
        register(this);
    }

    @Override
    public Long getId() {
        return COLOR_DEPTH_MASKS_ID;
    }

    @Override
    public String getDisplayName() {
        return "My Color Depth Masks";
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
    
    private static class DomainObjectNodeChildFactory extends ChildFactory<ColorDepthMask> {
        
        @Override
        protected boolean createKeys(List<ColorDepthMask> list) {
            try {
                log.debug("Creating children keys for ColorDepthMasksNode");
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<ColorDepthMask> children = model.getAllDomainObjectsByClass(ColorDepthMask.class);
                log.debug("Got children: {}",children);
                list.addAll(children);
            } 
            catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(ColorDepthMask key) {
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
