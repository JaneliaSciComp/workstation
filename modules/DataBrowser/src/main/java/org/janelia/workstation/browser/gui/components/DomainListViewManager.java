package org.janelia.workstation.browser.gui.components;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeSelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;

/**
 * Manages the life cycle of domain list viewers based on user generated selected events. This manager
 * either reuses existing viewers, or creates them as needed and docks them in the appropriate place.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class
DomainListViewManager implements ViewerManager<DomainListViewTopComponent> {

    private final static Logger log = LoggerFactory.getLogger(DomainListViewManager.class);
    
    public static DomainListViewManager instance;
    
    private DomainListViewManager() {
    }
    
    public static DomainListViewManager getInstance() {
        if (instance==null) {
            instance = new DomainListViewManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active instance of this top component */
    
    private DomainListViewTopComponent activeInstance;
    @Override
    public void activate(DomainListViewTopComponent instance) {
        activeInstance = instance;
    }
    @Override
    public boolean isActive(DomainListViewTopComponent instance) {
        return activeInstance == instance;
    }
    @Override
    public DomainListViewTopComponent getActiveViewer() {
        return activeInstance;
    }
    
    @Override
    public String getViewerName() {
        return "DomainListViewTopComponent";
    }

    @Override
    public Class<DomainListViewTopComponent> getViewerClass() {
        return DomainListViewTopComponent.class;
    }

    @Subscribe
    public void nodeSelected(NodeSelectionEvent event) {

        if (!DomainExplorerTopComponent.isNavigateOnClick()) {
            return;
        }

        IdentifiableNode node = event.getNode();

        if (node instanceof DomainObjectNode) {
            DomainObjectNode<?> domainObjectNode = (DomainObjectNode) node;

            DomainObject domainObject = domainObjectNode.getDomainObject();

            // We only care about selection events
            if (!event.isSelect()) {
                log.debug("Event is not selection: {}",event);
                return;
            }

            // We only care about events generated by the explorer or the context menu
            if (event.getSource() != null &&
                    (UIUtils.hasAncestorWithType((Component)event.getSource(),DomainExplorerTopComponent.class) ||
                            DomainObjectContextMenu.class.isAssignableFrom(event.getSource().getClass()))) {

                log.info("nodeSelected({})",Reference.createFor(domainObject));
                DomainListViewTopComponent targetViewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");

                log.info("Loading domain object node {} into {}",Reference.createFor(domainObject), targetViewer);
                targetViewer.loadDomainObjectNode(domainObjectNode, false);
            }
            else {
                log.trace("Event source is not domain explorer or context menu: {}",event);
            }
        }

    }

    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {

        if (!DomainExplorerTopComponent.isNavigateOnClick()) {
            return;
        }
        
        // We only care about single selections
        DomainObject domainObject = event.getObjectIfSingle();
        if (domainObject==null) {
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }

        // We only care about events generated by the explorer or the context menu
        if (event.getSource() != null && 
                (UIUtils.hasAncestorWithType((Component)event.getSource(),DomainExplorerTopComponent.class) ||
                DomainObjectContextMenu.class.isAssignableFrom(event.getSource().getClass()))) {

            log.info("domainObjectSelected({})",Reference.createFor(domainObject));
            DomainListViewTopComponent targetViewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");

            log.info("Loading domain object {} into {}",Reference.createFor(domainObject), targetViewer);
            targetViewer.loadDomainObject(domainObject, false);
        }
        else {
            log.trace("Event source is not domain explorer or context menu: {}",event);
            return;
        }
    }
}
