package org.janelia.workstation.browser.api.actions;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.janelia.workstation.browser.nodes.ChildObjectsNode;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates information about the currently selected nodes, for use by contextual actions.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeContext {

    private static final Logger log = LoggerFactory.getLogger(NodeContext.class);

    private Collection<? extends Node> nodes;
    private Collection<Object> objects;
    private Multiset<Class<?>> selectionSummary;

    NodeContext(Collection<? extends Node> nodes) {
        this.nodes = nodes;
        this.objects = new ArrayList<>();
        this.selectionSummary = ConcurrentHashMultiset.create();
        for (Node node : nodes) {
            if (node instanceof IdentifiableNode) {
                Object object = ((IdentifiableNode) node).getObject();
                objects.add(object);
                selectionSummary.add(object.getClass());
            }
            else if (node instanceof ChildObjectsNode) {
                Collection objects = ((ChildObjectsNode) node).getObjects();
                this.objects.addAll(objects);
                for (Object object : objects) {
                    selectionSummary.add(object.getClass());
                }
            }
            else {
                log.warn("Unrecognized node type: {}", node.getClass().getName());
            }
        }

        if (objects.size() != selectionSummary.size()) {
            log.warn("objects.size()={} but selectionSummary.size()={}", objects.size(), selectionSummary.size());
        }
    }

    public Collection<? extends Node> getNodes() {
        return nodes;
    }

    public Collection getObjects() {
        return objects;
    }

    public Multiset<Class<?>> getSelectionSummary() {
        return selectionSummary;
    }

    public boolean isSingleObjectOfType(Class<?> type) {
        return getSelectionSummary().size()==1
                && type.isAssignableFrom((objects.iterator().next().getClass()));
    }

    public <T> T getSingleObjectOfType(Class<T> type) {
        return (T)objects.iterator().next();
    }

    public boolean isOnlyObjectsOfType(Class<?> type) {
        Collection<?> typedObjects = getOnlyObjectsOfType(type);
        return !objects.isEmpty() && typedObjects.size() == objects.size();
    }

    public <T> Collection<T> getOnlyObjectsOfType(Class<T> type) {
        Collection<T> typedObjects = new ArrayList<>();
        for (Object object : objects) {
            if (type.isAssignableFrom(object.getClass())) {
                log.trace("{} isAssignableFrom {}", type, object.getClass());
                typedObjects.add(type.cast(object));
            }
            else {
                log.trace("{} isNotAssignableFrom {}", type, object.getClass());
            }
        }
        return typedObjects;
    }

    @Override
    public String toString() {
        if (selectionSummary.elementSet().size()==1) {
            // Only one type is selected
            Class<?> type = selectionSummary.elementSet().iterator().next();
            if (selectionSummary.count(type)==1) {
                return objects.iterator().next().toString();
            }
            return selectionSummary.count(type)+" "+type.getSimpleName();
        }
        else if (objects.isEmpty()) {
            return "nothing";
        }
        else {
            return selectionSummary.entrySet().size()+" items of various types";
        }
    }
}
