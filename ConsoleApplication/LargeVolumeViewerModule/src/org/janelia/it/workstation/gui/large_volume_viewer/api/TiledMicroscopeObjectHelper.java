package org.janelia.it.workstation.gui.large_volume_viewer.api;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.nodes.TmSampleNode;
import org.janelia.it.workstation.gui.large_volume_viewer.nodes.TmWorkspaceNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for making Tiled Microscope objects interoperable with other core modules.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHelper.class, path = DomainObjectHelper.DOMAIN_OBJECT_LOOKUP_PATH)
public class TiledMicroscopeObjectHelper implements DomainObjectHelper {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return (domainObject instanceof TmWorkspace) || (domainObject instanceof TmSample);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (domainObject instanceof TmSample) {
            return new TmSampleNode(parentChildFactory, (TmSample)domainObject);
        }
        else if (domainObject instanceof TmWorkspace) {
            return new TmWorkspaceNode(parentChildFactory, (TmWorkspace)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }
    
    @Override
    public String getLargeIcon(DomainObject domainObject) {
        if (domainObject instanceof TmWorkspace) {
            return "workspace_large.png";
        }
        else if (domainObject instanceof TmSample) {
            return "folder_files_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public void remove(DomainObject domainObject) throws Exception {
        TiledMicroscopeDomainMgr mgr = TiledMicroscopeDomainMgr.getDomainMgr();
        if (domainObject instanceof TmWorkspace) {
            mgr.remove((TmWorkspace)domainObject);
        }
        else if (domainObject instanceof TmSample) {
            mgr.remove((TmSample)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

}
