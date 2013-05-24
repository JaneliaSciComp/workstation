package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

import org.janelia.it.jacs.model.entity.EntityType;

public class TaskFilter implements java.io.Serializable {

    private String name;
    private Long taskId;
    private EntityTypeSet entityTypes = null;
    private TaskFilterStatus taskFilterStatus;

    public TaskFilter(String name) {
        this.name = name;
    }

    /**
     * Use this constructor for simplistic filters, like properties.
     */
    public TaskFilter(String name, Long taskId) {
        this.name = name;
        this.taskId = taskId;
        taskFilterStatus = new TaskFilterStatus(this);
    }

    /**
     * Use this constructor for filtering on entity type, where range will not be part
     * of the request
     *
     * @parameter name- name of filter
     * @parameter entityTypes- the set of entityTypes that will be returned
     */
    public TaskFilter(String name, EntityTypeSet entityTypes) {
        this(name);
        this.entityTypes = entityTypes;
    }

    /*final void addEntityType(EntityType entityType) {
       entityTypes.add(entityType);
    }*/

    public final EntityTypeSet getEntityTypeSet() {
        //return Collections.unmodifiableCollection(entityTypes);
        return entityTypes;
    }

    public final EntityType[] getEntityTypesAsArray() {
        if (entityTypes == null) return new EntityType[0];
        return (EntityType[]) entityTypes.toArray(new EntityType[0]);
    }

    public final boolean isFilteringOnEntityType() {
        return entityTypes != null && (entityTypes.size() > 0);
    }

    public final String getFilterName() {
        return name;
    }

    public String toString() {
        return "TaskFilter: " + getFilterName();
    }

    public TaskFilterStatus getTaskFilterStatus() {
        return taskFilterStatus;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}