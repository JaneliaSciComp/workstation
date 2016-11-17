package org.janelia.jacs2.model.page;

public class SortCriteria {
    private String field;
    private SortDirection direction;

    public SortCriteria() {
        this(null, SortDirection.ASC);
    }

    public SortCriteria(String field) {
        this(field, SortDirection.ASC);
    }

    public SortCriteria(String field, SortDirection direction) {
        this.field = field;
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

}
