package org.janelia.it.workstation.gui.large_volume_viewer.nodes;

import java.util.List;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A child factory for showing TmWorkspaces as children of a TmSample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmSampleChildFactory extends ChildFactory<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(TmSampleChildFactory.class);
    
    private TmSample sample;

    TmSampleChildFactory(TmSample sample) {
        this.sample = sample;
    }

    public void update(TmSample sample) {
        this.sample = sample;
    }

    public boolean hasNodeChildren() {
        // TODO: check if sample has workspaces or sessions
        return true;
    }

    @Override
    protected boolean createKeys(List<DomainObject> list) {
        try {
            if (sample==null) return false;
            TiledMicroscopeDomainMgr mgr = TiledMicroscopeDomainMgr.getDomainMgr();

            log.debug("Creating workspace children keys for {}",sample.getName());
            List<TmWorkspace> workspaces = mgr.getWorkspaces(sample.getId());
            log.debug("Got workspace children: {}",workspaces);
            list.addAll(workspaces);
            
            log.debug("Creating session children keys for {}",sample.getName());
            List<TmDirectedSession> sessions = mgr.getSessions(sample.getId());
            log.debug("Got session children: {}",sessions);
            list.addAll(sessions);
        }
        catch (Exception ex) {
            log.error("Error creating tree node child keys",ex);
            return true;
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(DomainObject key) {
        log.debug("Creating node for '{}'",key.getName());
        try {
            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(key);
            if (provider!=null) {
                return provider.getNode(key, this);
            }
            return null;
        }
        catch (Exception e) {
            log.error("Error creating node for '"+key+"'", e);
        }
        return null;
    }

    public void refresh() {
        log.debug("Refreshing child factory for: {}",sample.getName());
        refresh(true);
    }
}