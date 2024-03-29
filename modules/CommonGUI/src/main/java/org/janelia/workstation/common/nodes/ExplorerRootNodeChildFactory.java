package org.janelia.workstation.common.nodes;

import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
                for (NodeGenerator generator : generators) {
                    NodePreference nodePreference = generator.getNodePreference();
                    if (nodePreference != null) {
                        if (nodePreference.isNodeShown()) {
                            log.info("Adding '{}' node generator provided by {}", nodePreference.getNodeName(), provider.getClass().getName());
                            allGenerators.add(generator);
                        }
                    }
                    else {
                        log.info("Adding node generator provided by {}", provider.getClass().getName());
                        allGenerators.add(generator);
                    }
                }
            }
            
            allGenerators.sort(Comparator.comparing(NodeGenerator::getIndex));
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
            FrameworkAccess.handleException(ex);
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(NodeGenerator key) {
        try {
            return key.createNode();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        return null;
    }

    public void refresh() {
        refresh(true);
    }
}
