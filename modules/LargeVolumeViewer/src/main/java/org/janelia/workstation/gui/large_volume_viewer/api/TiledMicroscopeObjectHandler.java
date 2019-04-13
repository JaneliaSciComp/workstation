package org.janelia.workstation.gui.large_volume_viewer.api;

import org.janelia.workstation.gui.large_volume_viewer.nodes.TmSampleNode;
import org.janelia.workstation.gui.large_volume_viewer.nodes.TmWorkspaceNode;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for making Tiled Microscope objects interoperable with other core modules.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class, path = DomainObjectHandler.DOMAIN_OBJECT_LOOKUP_PATH)
public class TiledMicroscopeObjectHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }

    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        if (TmSample.class.isAssignableFrom(clazz)) {
            return true;
        }
        else if (TmWorkspace.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
    
    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (TmSample.class.isAssignableFrom(domainObject.getClass())) {
            return new TmSampleNode(parentChildFactory, (TmSample)domainObject);
        }
        else if (TmWorkspace.class.isAssignableFrom(domainObject.getClass())) {
            return new TmWorkspaceNode(parentChildFactory, (TmWorkspace)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        return null;
    }
    
    @Override
    public String getLargeIcon(DomainObject domainObject) {
        if (TmSample.class.isAssignableFrom(domainObject.getClass())) {
            return "folder_files_large.png";
        }
        else if (TmWorkspace.class.isAssignableFrom(domainObject.getClass())) {
            return "workspace_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (TmSample.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else if (TmWorkspace.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        TiledMicroscopeDomainMgr mgr = TiledMicroscopeDomainMgr.getDomainMgr();
        if (TmSample.class.isAssignableFrom(domainObject.getClass())) {
            mgr.remove((TmSample)domainObject);
        }
        else if (TmWorkspace.class.isAssignableFrom(domainObject.getClass())) {
            mgr.remove((TmWorkspace)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

}
