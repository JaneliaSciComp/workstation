package org.janelia.it.workstation.gui.browser.icongrid.node;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.icongrid.IconGridViewer;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
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
    
    /** Explorer manager to work with. Is not null only if the component is showing
    * in components hierarchy
    */
    private transient ExplorerManager manager;
    
    //
    // listeners
    //

    /** Listener to nearly everything */
    transient Listener managerListener;

    /** weak variation of the listener for property change on the explorer manager */
    transient PropertyChangeListener wlpc;

    /** weak variation of the listener for vetoable change on the explorer manager */
    transient VetoableChangeListener wlvc;

    /** True, if the selection listener is attached. */
    transient boolean listenerActive;
    
    
    
    public NodeIconGridViewer() {
        super();
        managerListener = new Listener();
    }
    
    /* Initilizes the view.
    */
    @Override
    public void addNotify() {
        super.addNotify();
        log.info("addNotify");

        // run under mutex
        ExplorerManager em = ExplorerManager.find(this);

        if (em != manager) {
            if (manager != null) {
                manager.removeVetoableChangeListener(wlvc);
                manager.removePropertyChangeListener(wlpc);
            }

            manager = em;

            manager.addVetoableChangeListener(wlvc = WeakListeners.vetoableChange(managerListener, manager));
            manager.addPropertyChangeListener(wlpc = WeakListeners.propertyChange(managerListener, manager));

            log.info("ADDDDDD manager listener "+wlvc);
            
            //setNode(manager.getExploredContext());
            updateSelection();
            
        } 
        else {
            // bugfix #23509, the listener were removed --> add it again
            if (!listenerActive && (manager != null)) {
                manager.addVetoableChangeListener(wlvc = WeakListeners.vetoableChange(managerListener, manager));
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
        log.info("removeNotify");
        listenerActive = false;

        // bugfix #23509, remove useless listeners
        if (manager != null) {
            manager.removeVetoableChangeListener(wlvc);
            manager.removePropertyChangeListener(wlpc);
        }
    }
    
    
    private void updateSelection() {
        Set<Node> selected = new HashSet<Node>(Arrays.asList(manager.getSelectedNodes()));
        log.info("selection: "+selected);
        imagesPanel.setSelectedObjects(selected);
    }
    
    
    @Override
    public Node getContextObject() {
        return manager.getExploredContext();
    }
    
    @Override
    public void setContextObject(Node contextNode) {
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
        if (node instanceof DomainObjectNode) {
            DomainObjectNode domainObjectNode = (DomainObjectNode)node;
            DomainObject domainObject = domainObjectNode.getDomainObject();
            StringBuilder urlSb = new StringBuilder();
            
            if (domainObject instanceof HasFiles) {
                if (domainObject instanceof HasFilepath) {
                    String rootPath = ((HasFilepath)domainObject).getFilepath();
                    if (rootPath!=null) {
                        urlSb.append(rootPath);
                    }
                }
                HasFiles hasFiles = (HasFiles)domainObject;
                FileType fileType = FileType.valueOf(role);
                String filepath = hasFiles.getFiles().get(fileType);
                if (filepath!=null) {
                    if (urlSb.length()>0) urlSb.append("/");
                    urlSb.append(filepath);
                }
                else {
                    // Clear the URL if there is no filepath for the given role
                    urlSb = new StringBuilder();
                }
            }
            
            return urlSb.length()>0 ? urlSb.toString() : null;
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
        explorerTc.activateNode((Node)imageObject);
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
    
    public void setNode(final Node node) {
        
        if (node==null) {
            setContextObject(null);
            clear();
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            private Node[] children;

            @Override
            protected void doStuff() throws Exception {
                children = node.getChildren().getNodes(true);
            }

            @Override
            protected void hadSuccess() {
                List<Node> nodes = Arrays.asList(children);
                setContextObject(node);
                showImageObjects(nodes);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }
    
    private final class Listener implements PropertyChangeListener, VetoableChangeListener {
        
        @Override
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
//            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
//                Node[] newNodes = (Node[]) evt.getNewValue();
//
//                if (!selectionAccept(newNodes)) {
//                    throw new PropertyVetoException("", evt); // NOI18N
//                }
//            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                updateSelection();
            }
            else if (ExplorerManager.PROP_EXPLORED_CONTEXT.equals(evt.getPropertyName())) {
                setNode(manager.getExploredContext());
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
