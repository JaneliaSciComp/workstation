package org.janelia.workstation.browser.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.nodes.NodeGenerator;
import org.janelia.it.jacs.integration.framework.nodes.NodeProvider;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.domain.workspace.Workspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main child factory for the root node in the explorer.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExplorerRootNodeChildFactory extends ChildFactory<NodeGenerator> {

    private static final Logger log = LoggerFactory.getLogger(ExplorerRootNodeChildFactory.class);
    
    @Override
    protected boolean createKeys(List<NodeGenerator> list) {
        try {
            
            List<NodeGenerator> allGenerators = new ArrayList<>();
            for(NodeProvider provider : Lookups.forPath(NodeProvider.LOOKUP_PATH).lookupAll(NodeProvider.class)) {
                List<NodeGenerator> generators = provider.getNodeGenerators();
                log.info("Adding {} node generators from provider: {}", generators.size(), provider.getClass().getName());
                allGenerators.addAll(generators);
            }
            
            Collections.sort(allGenerators, new Comparator<NodeGenerator>() {
                @Override
                public int compare(NodeGenerator o1, NodeGenerator o2) {
                    return o1.getIndex().compareTo(o2.getIndex());
                }
            });
            
            list.addAll(allGenerators);
            
            for(Workspace workspace : DomainMgr.getDomainMgr().getModel().getWorkspaces()) {
                log.info("Adding workspace node generator: {} ({})", workspace.getName(), workspace.getOwnerKey());
                list.add(new NodeGenerator() {
                    
                    @Override
                    public Integer getIndex() {
                        return -1; // This doesn't matter, we've already sorted everything
                    }
                    
                    @Override
                    public Node createNode() {
                        return new WorkspaceNode(workspace);
                    }
                });
            }
        } 
        catch (Exception ex) {
            FrameworkImplProvider.handleException(ex);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(NodeGenerator key) {
        try {
            return key.createNode();
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
        return null;
    }

    public void refresh() {
        refresh(true);
    }
}
