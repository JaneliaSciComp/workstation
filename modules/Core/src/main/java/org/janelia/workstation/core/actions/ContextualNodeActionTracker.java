package org.janelia.workstation.core.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.janelia.workstation.core.events.Events;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A global action tracker which looks for global node selection and aggregates the information so that actions
 * can make decisions more quickly, without each action needing to process all selected nodes.
 *
 * This is essentially an optimization of NetBeans' NodeAction, which centralizes some of the aggregation logic.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ContextualNodeActionTracker implements LookupListener {

    private static final Logger log = LoggerFactory.getLogger(ContextualNodeActionTracker.class);

    // Singleton
    private static ContextualNodeActionTracker instance;

    private Lookup.Result<Node> nodeResult;
    private Lookup.Result<ViewerContext> viewerContextResult;

    public static ContextualNodeActionTracker getInstance() {
        if (instance == null) {
            instance = new ContextualNodeActionTracker();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private ContextualNodeActionTracker() {
        // Listen to the global lookup, which always contains the selected nodes
        // for the currently focused TopComponent
        nodeResult = Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeResult.addLookupListener(this);
        viewerContextResult = Utilities.actionsGlobalContext().lookupResult(ViewerContext.class);
        viewerContextResult.addLookupListener(this);
    }

    private Queue<ContextualNodeAction> dependents = new ConcurrentLinkedQueue<>();

    public void register(ContextualNodeAction dependent) {
        dependents.add(dependent);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

        Collection<? extends Node> selectedNodes = nodeResult.allInstances();
        NodeContext nodeContext = new NodeContext(selectedNodes);

        Collection<? extends ViewerContext> viewerContexts = viewerContextResult.allInstances();
        ViewerContext viewerContext = viewerContexts.isEmpty()?null:viewerContexts.iterator().next();

        if (lookupEvent.getSource()==nodeResult) {
            log.info("Node changed: {} nodes and {} objects selected", selectedNodes.size(), nodeContext.getObjects().size());
        }
        else if (lookupEvent.getSource()==viewerContextResult) {
            log.info("Viewer context changed: {}", viewerContext);
        }
        else {
            throw new IllegalStateException("Unexpected lookup event");
        }

        for (ContextualNodeAction dependent : dependents) {
            try {
                dependent.enable(nodeContext, viewerContext);
            }
            catch (Throwable t) {
                // Handle exceptions here so that one bad action doesn't spoil everything
                log.error("Error encountered while setting node context on "+dependent.getClass().getName(), t);
            }
        }
    }
}
