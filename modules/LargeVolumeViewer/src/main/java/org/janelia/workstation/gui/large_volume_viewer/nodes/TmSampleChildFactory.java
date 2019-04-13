package org.janelia.workstation.gui.large_volume_viewer.nodes;

import java.util.ArrayList;
import java.util.List;

import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A child factory for showing TmWorkspaces as children of a TmSample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmSampleChildFactory extends ChildFactory<TmWorkspace> {

    private final static Logger log = LoggerFactory.getLogger(TmSampleChildFactory.class);
    
    private TmSample sample;

    TmSampleChildFactory(TmSample sample) {
        this.sample = sample;
    }

    public void update(TmSample sample) {
        this.sample = sample;
    }

    public boolean hasNodeChildren() {
        // TODO: check if sample has workspaces
        return true;
    }

    @Override
    protected boolean createKeys(List<TmWorkspace> list) {
        try {
            if (sample==null) return false;

            log.debug("Creating children keys for {}",sample.getName());
            
            TiledMicroscopeDomainMgr mgr = TiledMicroscopeDomainMgr.getDomainMgr();
            List<TmWorkspace> children = mgr.getWorkspaces(sample.getId());
            log.debug("Got children: {}",children);

            List<TmWorkspace> temp = new ArrayList<>();
            for (TmWorkspace obj : children) {
                temp.add(obj);
            }
            list.addAll(temp);
        }
        catch (Exception ex) {
            log.error("Error creating tree node child keys",ex);
            return false;
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(TmWorkspace key) {
        log.debug("Creating node for '{}'",key.getName());
        try {
            return new TmWorkspaceNode(this, key);
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