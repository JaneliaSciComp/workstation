package org.janelia.workstation.browser.api.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private Lookup.Result<Node> result;

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
        result = Utilities.actionsGlobalContext().lookupResult(Node.class);
        result.addLookupListener(this);
    }

    private List<ContextualNodeAction> dependents = new ArrayList<>();

    public void register(ContextualNodeAction dependent) {
        dependents.add(dependent);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends Node> selectedNodes = result.allInstances();
        NodeContext nodeSelection = new NodeContext(selectedNodes);
        log.info("New node selection: {}", nodeSelection);
        for (ContextualNodeAction dependent : dependents) {
            dependent.enable(nodeSelection);
        }
    }
}
