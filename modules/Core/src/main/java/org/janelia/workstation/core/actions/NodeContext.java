package org.janelia.workstation.core.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

    public <T extends Node> NodeContext(T node) {
        this(Collections.singletonList(node));
    }

    NodeContext(Collection<? extends Node> nodes) {
        this.nodes = nodes;
        this.objects = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof IdentifiableNode) {
                Object object = ((IdentifiableNode) node).getObject();
                objects.add(object);
            }
            else if (node instanceof ChildObjectsNode) {
                Collection objects = ((ChildObjectsNode) node).getObjects();
                this.objects.addAll(objects);
            }
        }
    }

    public Collection<? extends Node> getNodes() {
        return nodes;
    }

    public Collection<?> getObjects() {
        return objects;
    }

    public boolean isSingleNodeOfType(Class<?> type) {
        return nodes.size()==1
                && type.isAssignableFrom((nodes.iterator().next().getClass()));
    }

    public <T> T getSingleNodeOfType(Class<T> type) {
        return (T)nodes.iterator().next();
    }

    public boolean isSingleObjectOfType(Class<?> type) {
        return objects.size()==1
                && type.isAssignableFrom((objects.iterator().next().getClass()));
    }

    public <T> T getSingleObjectOfType(Class<T> type) {
        return (T)objects.iterator().next();
    }

    public boolean isOnlyObjectsOfType(Class<?> type) {
        Collection<?> typedObjects = getOnlyObjectsOfType(type);
        return !objects.isEmpty() && typedObjects.size() == objects.size();
    }

    private LoadingCache<Class<Object>, Collection<Object>> objectTypeCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                    new CacheLoader<Class<Object>, Collection<Object>>() {
                        public Collection<Object> load(Class<Object> type) {
                            Collection<Object> typedObjects = new ArrayList<>();
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
                    });

    public <T> Collection<T> getOnlyObjectsOfType(Class<T> type) {
        // TODO: this method should cache results
        try {
            // Why is it to hard to generify a class on types with the same generics?
            return (Collection<T>) objectTypeCache.get((Class<Object>)type);
        }
        catch (ExecutionException e) {
            FrameworkAccess.handleException(e);
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        if (objects.size()==1) {
            return nodes.iterator().next().getClass().getSimpleName()
                    +" containing "+objects.iterator().next();
        }
        else if (objects.isEmpty()) {
            return nodes.size()+" nodes containing nothing";
        }
        else {
            return nodes.size()+" nodes containing "+objects.size()+" objects";
        }
    }
}
