package org.janelia.workstation.browser.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.DomainModelViewUtils;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.openide.util.Lookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with viewers. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerUtils {

    private final static Logger log = LoggerFactory.getLogger(ViewerUtils.class);

    public static <T extends TopComponent> T getViewer(ViewerManager<T> manager, final String modeName) {
        
        log.info("Getting viewer: {} (mode={})",manager.getViewerName(), modeName);
        
        T tc = manager.getActiveViewer();
        if (tc==null) {
            log.warn("No active viewer");
            return null;
        }
        else if (!tc.isVisible()) {
            log.warn("Active viewer is not visible");
            return null;
        }
        else if (!tc.isOpened()) {
            log.warn("Viewer is not open");
            return null;
        }
        else {
            // TODO: this should probably also check to make sure the viewer is in the correct mode
            log.warn("Returning active, visible viewer");
            return tc;
        }
    }

    public static <T extends TopComponent> T createNewViewer(ViewerManager<T> manager, final String modeName) {

        log.info("Creating viewer: {}",manager.getViewerName());

        T tc;
        try {
            tc = manager.getViewerClass().newInstance();
            manager.activate(tc);
        }
        catch (Exception e) {
            throw new IllegalStateException("Viewer instantiation failed",e);
        }
        
        log.debug("Docking new instance of {} into {}",tc.getName(),modeName);
        Mode mode = WindowManager.getDefault().findMode(modeName);
        if (mode!=null) {
            mode.dockInto(tc);
        }
        else {
            log.warn("No such mode found: "+modeName);
        }
        // Against all reason, dockInto may cause the component to close after docking. 
        // So, unintuitively, this open() has to happen at the end. Thanks, NetBeans.
        tc.open();
        tc.requestActive();
        
        return tc;
    }
    
    public static <T extends TopComponent> T provisionViewer(ViewerManager<T> manager, final String modeName) {

        log.info("Provisioning viewer: {}",manager.getViewerName());
        
        T tc = manager.getActiveViewer();

        if (tc==null) {
            log.info("Active viewer not found, creating...");
            tc = createNewViewer(manager, modeName);
        }
        else {
            log.info("Found active viewer");
            if (!tc.isOpened()) {
                log.info("Viewer is not open, opening.");
                tc.open();
            }
            if (!tc.isVisible()) {
                log.info("Viewer is not visible, requesting active.");
                tc.requestVisible();
            }
        }

        return tc;
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    public static String serialize(DomainObjectEditorState<?,?,?> state) throws Exception {
        return mapper.writeValueAsString(state);
    }

    public static DomainObjectEditorState<?,?,?> deserialize(String serializedState) throws Exception {
        return mapper.readValue(DomainModelViewUtils.convertModelPackages(serializedState), DomainObjectEditorState.class);
    }

    public static void updateContextIfChanged(Lookup.Provider lookupProvider, InstanceContent content, ViewerContext viewerContext) {
        Collection<? extends ViewerContext> viewerContexts = lookupProvider.getLookup().lookupAll(ViewerContext.class);
        if (viewerContexts.isEmpty() || !viewerContexts.iterator().next().equals(viewerContext)) {
            log.info("Updating ViewerContext ({})", viewerContext);
            // Clear all existing nodes
            viewerContexts.forEach(content::remove);
            // Add new node
            if (viewerContext!=null) {
                content.add(viewerContext);
            }
        }
    }

    public static void updateNodeIfChanged(Lookup.Provider lookupProvider, InstanceContent content, Collection objects) {

        List<Object> currentObjects = new ArrayList<>();
        for (ChildObjectsNode childObjectsNode : lookupProvider.getLookup().lookupAll(ChildObjectsNode.class)) {
            currentObjects.addAll(childObjectsNode.getObjects());
        }

        List<Object> newObjects = new ArrayList<>(objects);
        if (!currentObjects.equals(newObjects)) {
            log.info("Updating ChildObjectsNode (current={}, new={})", currentObjects.size(), newObjects.size());
            // Clear all existing nodes
            lookupProvider.getLookup().lookupAll(ChildObjectsNode.class).forEach(content::remove);
            // Add new node
            content.add(new ChildObjectsNode(newObjects));
        }
    }

}
