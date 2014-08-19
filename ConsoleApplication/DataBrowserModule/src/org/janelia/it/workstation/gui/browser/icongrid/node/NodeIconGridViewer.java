package org.janelia.it.workstation.gui.browser.icongrid.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.components.DomainBrowserTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.icongrid.IconGridViewer;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.HasUniqueId;
import org.janelia.it.workstation.gui.browser.nodes.InternalNode;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeIconGridViewer extends IconGridViewer<Node> {
    
    private static final Logger log = LoggerFactory.getLogger(NodeIconGridViewer.class);
    
    private Node contextNode;
    
    @Override
    public Node getContextObject() {
        return contextNode;
    }
    
    @Override
    public void setContextObject(Node contextNode) {
        this.contextNode = contextNode;
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
        explorerTc.activate((Node)imageObject);
        
        //ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);
    }

    @Override
    protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
        final String category = getSelectionCategory();
        HasUniqueId imageObject = (HasUniqueId)button.getImageObject();
        String uniqueId = imageObject.getUniqueId()+"";
        
//        DomainBrowserTopComponent browserTc = (DomainBrowserTopComponent)WindowLocator.getByName(DomainBrowserTopComponent.TC_NAME);
        
        selectionButtonContainer.setVisible(false);

        if (multiSelect) {
            // With the meta key we toggle items in the current
            // selection without clearing it
            if (!button.isSelected()) {
                ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, uniqueId, false);
            }
            else {
                ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(category, uniqueId);
            }
        }
        else {
            // With shift, we select ranges
            String lastSelected = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityIdByCategory(getSelectionCategory());
            if (rangeSelect && lastSelected != null) {
//                // Walk through the buttons and select everything between the last and current selections
//                boolean selecting = false;
//                List<RootedEntity> rootedEntities = getRootedEntities();
//                for (RootedEntity otherRootedEntity : rootedEntities) {
//                    if (otherRootedEntity.getId().equals(lastSelected) || otherRootedEntity.getId().equals(uniqueId)) {
//                        if (otherRootedEntity.getId().equals(uniqueId)) {
//                            // Always select the button that was clicked
//                            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
//                        }
//                        if (selecting) {
//                            return; // We already selected, this is the end
//                        }
//                        selecting = true; // Start selecting
//                        continue; // Skip selection of the first and last items, which should already be selected
//                    }
//                    if (selecting) {
//                        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
//                    }
//                }
            }
            else {
                // This is a good old fashioned single button selection
                ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, uniqueId, true);
            }
        }

        button.requestFocus();
    }
}
