package org.janelia.workstation.browser.nodes;

import org.janelia.model.domain.files.SyncedPath;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.browser.actions.NewSyncedRootAction;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.common.nodes.FilterNode;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A node corresponding to a single SyncedRoot in the domain model. It shows the root's SyncedPaths as children.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncedRootNode extends AbstractDomainObjectNode<SyncedRoot> {

    private final static Logger log = LoggerFactory.getLogger(SyncedRootNode.class);

    public SyncedRootNode(ChildFactory<?> parentChildFactory, SyncedRoot library) {
        this(parentChildFactory, new SyncedPathFactory(library), library);
    }

    private SyncedRootNode(ChildFactory<?> parentChildFactory, final SyncedPathFactory childFactory, SyncedRoot library) {
        super(parentChildFactory, library.getChildren().size()==0 ? Children.LEAF : Children.create(childFactory, false), library);
    }

    @Override
    public Long getId() {
        return getSyncedRoot().getId();
    }

    public SyncedRoot getSyncedRoot() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getSyncedRoot().getName();
    }

    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getSyncedRoot())) {
            return Icons.getIcon("folder_brick.png").getImage();
        }
        else {
            return Icons.getIcon("folder_blue.png").getImage();
        }
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    private static class SyncedPathFactory extends ChildFactory<SyncedPath> {

        private SyncedRoot syncedRoot;

        SyncedPathFactory(SyncedRoot syncedRoot) {
            this.syncedRoot = syncedRoot;
        }

        @Override
        protected boolean createKeys(java.util.List<SyncedPath> list) {
            try {
                log.debug("Creating children keys for SyncedRootNode");
                List<SyncedPath> children = DomainMgr.getDomainMgr().getModel().getChildren(syncedRoot);
                for (SyncedPath syncedPath : children) {
                    if (syncedPath.isExistsInStorage()) {
                        list.add(syncedPath);
                    }
                }
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
            return true;
        }

        @Override
        protected org.openide.nodes.Node createNodeForKey(SyncedPath key) {
            log.debug("Creating node for '{}'",key.getName());
            try {
                DomainObjectHandler provider = ServiceAcceptorHelper.findFirstHelper(key);
                if (provider!=null) {
                    return provider.getNode(key, this);
                }
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
