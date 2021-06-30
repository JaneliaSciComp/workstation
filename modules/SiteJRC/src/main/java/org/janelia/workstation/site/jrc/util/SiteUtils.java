package org.janelia.workstation.site.jrc.util;

import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.site.jrc.nodes.FlyLineReleasesNode;

/**
 * Common utility functions for site-specific code.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SiteUtils {

    /**
     * Navigate to the given LineRelease in the Data Explorer.
     */
    public static void navigateToLineRelease(Long lineReleaseId) {
        DomainExplorerTopComponent.getInstance().refresh(() -> {
            DomainExplorerTopComponent.getInstance().expandNodeById(FlyLineReleasesNode.NODE_ID);
            for (IdentifiableNode node : NodeTracker.getInstance().getNodesById(lineReleaseId)) {
                if (node instanceof AbstractDomainObjectNode) {
                    AbstractDomainObjectNode<?> nodeToLoad = (AbstractDomainObjectNode<?>)node;
                    DomainListViewTopComponent viewer = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
                    viewer.requestActive();
                    viewer.loadDomainObjectNode(nodeToLoad, true);
                    break;
                }
            }
            return null;
        });
    }
}
