package org.janelia.it.workstation.gui.browser.icongrid.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.gui.browser.icongrid.IconGridViewer;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
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
            return domainObjectNode.getDomainObject().getId();
        }
        else {
            return node.getName();
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
            
            if (domainObject instanceof HasFilepath) {
                urlSb.append(((HasFilepath)domainObject).getFilepath());
            }
            
            if (domainObject instanceof HasFiles) {
                HasFiles hasFiles = (HasFiles)domainObject;
                FileType fileType = FileType.valueOf(role);
                String filepath = hasFiles.getFiles().get(fileType);
                if (urlSb.length()>0) urlSb.append("/");
                urlSb.append(filepath);
            }
            
            return urlSb.length()>0 ? urlSb.toString() : null;
        }
        return null;
    }

    @Override
    public Object getImageLabel(Node node) {
        return node.getDisplayName();
    }
}
