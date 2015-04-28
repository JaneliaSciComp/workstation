package org.janelia.it.workstation.gui.browser.icongrid.node;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.icongrid.IconGridViewer;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.Has2dRepresentation;
import org.janelia.it.workstation.gui.browser.nodes.InternalNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeIconGridViewer extends IconGridViewer<Node> {
    
    private static final Logger log = LoggerFactory.getLogger(NodeIconGridViewer.class);
    
    /** 
     * Explorer manager to work with. Is not null only if the component is showing
     * in components hierarchy
     */
    private transient ExplorerManager manager;
    
    /** Listener to nearly everything */
    transient Listener managerListener;

    /** weak variation of the listener for property change on the explorer manager */
    transient PropertyChangeListener wlpc;

    /** True, if the selection listener is attached. */
    transient boolean listenerActive;
    
    protected AtomicBoolean childrenLoadInProgress = new AtomicBoolean(false);
    
    public NodeIconGridViewer() {
        super();
        managerListener = new Listener();
    }
    
    /* Initilizes the view.
    */
    @Override
    public void addNotify() {
        super.addNotify();
        
        ExplorerManager em = ExplorerManager.find(this);

        if (em != manager) {
            if (manager != null) {
                manager.removePropertyChangeListener(wlpc);
            }
            manager = em;
            manager.addPropertyChangeListener(wlpc = WeakListeners.propertyChange(managerListener, manager));
            updateSelection();
            
        } 
        else {
            // bugfix #23509, the listener were removed --> add it again
            if (!listenerActive && (manager != null)) {
                manager.addPropertyChangeListener(wlpc = WeakListeners.propertyChange(managerListener, manager));
            }
        }

        if (!listenerActive) {
            listenerActive = true;
        }
    }

    /** Removes listeners.
    */
    @Override
    public void removeNotify() {
        super.removeNotify();
        listenerActive = false;

        // bugfix #23509, remove useless listeners
        if (manager != null) {
            manager.removePropertyChangeListener(wlpc);
        }
    }
    
    private void updateSelection() {
        Set<Node> selected = new HashSet<Node>(Arrays.asList(manager.getSelectedNodes()));
        imagesPanel.setSelectedObjects(selected);
    }
    
    @Override
    public Node getContextObject() {
        return manager.getExploredContext();
    }
    
    @Override
    public void setContextObject(Node contextNode) {
        log.trace("setContextObject");
        log.trace("    Old: "+manager.getExploredContext());
        log.trace("    New: "+contextNode);
        if (manager.getExploredContext()!=null) {
            if (manager.getExploredContext()!=null && manager.getExploredContext().equals(contextNode)) {
                return;
            }
        }
        
        try {
            manager.setExploredContextAndSelection(contextNode, new Node[0]);
        }
        catch (PropertyVetoException e) {
            log.warn("Could not set explored context",e);
        }
    }
    
    @Override
    protected void populateImageRoles(List<Node> nodes) {
        Set<String> imageRoles = new HashSet<String>();
        for(Node node : nodes) {
            if (node instanceof DomainObjectNode) {
                DomainObjectNode domainObjectNode = (DomainObjectNode)node;
                DomainObject domainObject = domainObjectNode.getDomainObject();
                if (domainObject instanceof HasFiles) {
                    HasFiles hasFiles = (HasFiles)domainObject;
                    for(FileType fileType : hasFiles.getFiles().keySet()) {
                        if (fileType.isIs2dImage()) {
                            imageRoles.add(fileType.name());
                        }
                    }
                }
            }
        }
        allImageRoles.clear();
        allImageRoles.addAll(imageRoles);
        Collections.sort(allImageRoles);
    }

    @Override
    public Object getImageUniqueId(Node node) {
        if (node instanceof DomainObjectNode) {
            DomainObjectNode domainObjectNode = (DomainObjectNode)node;
            return domainObjectNode.getUniqueId();
        }
        else if (node instanceof InternalNode) {
            return ((InternalNode)node).getUniqueId();
        }
        else {
            log.warn("Unrecognized node type: "+node.getClass().getName());
            return node.hashCode();
        }
    }

    @Override
    public String getImageFilepath(Node node) {
        return getImageFilepath(node, FileType.SignalMip.toString());
    }

    @Override
    public String getImageFilepath(Node node, String role) {
        if (node instanceof Has2dRepresentation) {
            Has2dRepresentation node2d = (Has2dRepresentation)node;
            return node2d.get2dImageFilepath(role);
        }
        return null;
    }

    @Override
    public Object getImageLabel(Node node) {
        return node.getDisplayName();
    }
    
    @Override
    protected void buttonDrillDown(AnnotatedImageButton button) {
        
        Node imageObject = (Node)button.getImageObject();
        Node currImageObject = getContextObject();
        
        if (currImageObject == null || currImageObject == imageObject) {
            return;
        }
        DomainExplorerTopComponent explorerTc = (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
//        explorerTc.activateNode((Node)imageObject);
    }

    private Node lastSelected = null;
    
    @Override
    protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
        
        Node imageObject = (Node)button.getImageObject();
        
        selectionButtonContainer.setVisible(false);

        Set<Node> selected = new HashSet<Node>(Arrays.asList(manager.getSelectedNodes()));
                
        if (multiSelect) {
            // With the meta key we toggle items in the current
            // selection without clearing it
            if (!button.isSelected()) {
                selected.add(imageObject);
                lastSelected = imageObject;
            }
            else {
                selected.remove(imageObject);
                lastSelected = null;
            }
        }
        else {        
            // With shift, we select ranges
            if (rangeSelect && lastSelected != null) {
                // Walk through the buttons and select everything between the last and current selections
                boolean selecting = false;
                List<Node> imageObjects = getPageImageObjects();
                for (Node otherImageObject : imageObjects) {
                    if (otherImageObject.equals(lastSelected) || otherImageObject.equals(imageObject)) {
                        if (otherImageObject.equals(imageObject)) {
                            // Always select the button that was clicked
                            selected.add(otherImageObject);
                        }
                        if (selecting) {
                            break;
                        }
                        selecting = true; // Start selecting
                        continue; // Skip selection of the first and last items, which should already be selected
                    }
                    if (selecting) {
                        selected.add(otherImageObject);
                    }
                }
            }
            else {
                selected.clear();
                selected.add(imageObject);
            }
            lastSelected = imageObject;
        }
        
        try {
            manager.setSelectedNodes(selected.toArray(new Node[selected.size()]));
        }
        catch (PropertyVetoException e) {
            log.warn("Could not select node", e);
        }

        button.requestFocus();
    }
    
    private SimpleWorker childLoadingWorker;
    
    public synchronized void loadNode(final Node node) {
        
        childrenLoadInProgress.set(true);

        // Indicate a load
        showLoadingIndicator();
        
        // Cancel previous loads
        imagesPanel.cancelAllLoads();
        if (childLoadingWorker != null && !childLoadingWorker.isDone()) {
            childLoadingWorker.disregard();
        }
        
        childLoadingWorker = new SimpleWorker() {

            private Node[] children;

            @Override
            protected void doStuff() throws Exception {
                if (node!=null) {
                    log.info("Waiting for child nodes..");
                    children = node.getChildren().getNodes(true);
                    log.info("Got "+children.length+" children");
                }
            }

            @Override
            protected void hadSuccess() {
                
                if (node==null) {
                    //setContextObject(null);
                    clear();
                    return;
                }
                else {
                    List<Node> nodes = Arrays.asList(children);
                    //setContextObject(node);
                    showImageObjects(nodes);
                }
            
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        childLoadingWorker.execute();
    }
    
    private final class Listener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                updateSelection();
            }
            else if (ExplorerManager.PROP_EXPLORED_CONTEXT.equals(evt.getPropertyName())) {
                log.info("Heard property change: "+evt.getPropertyName()+" "+evt.getOldValue()+"->"+evt.getNewValue());
                loadNode(manager.getExploredContext());
            }
        }
    }
    
//    // Backspace jumps to parent folder of explored context
//    private final class GoUpAction extends AbstractAction {
//        static final long serialVersionUID = 1599999335583246715L;
//
//        public GoUpAction() {
//            super("GoUpAction"); // NOI18N
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            if (traversalAllowed) {
//                Node pan = manager.getExploredContext();
//                pan = pan.getParentNode();
//
//                if (pan != null) {
//                    manager.setExploredContext(pan, manager.getSelectedNodes());
//                }
//            }
//        }
//
//        @Override
//        public boolean isEnabled() {
//            return true;
//        }
//    }
//
//    //Enter key performObjectAt selected index.
//    private final class EnterAction extends AbstractAction {
//        static final long serialVersionUID = -239805141416294016L;
//
//        public EnterAction() {
//            super("Enter"); // NOI18N
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            int index = list.getSelectedIndex();
//            performObjectAt(index, e.getModifiers());
//        }
//
//        @Override
//        public boolean isEnabled() {
//            return true;
//        }
//    }
}
