package org.janelia.it.workstation.gui.browser.model;

import java.lang.reflect.Method;

import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * An indexed attribute on a domain object. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectAttribute {

    private final String name;
    private final String label;
    private final String searchKey;
    private final String facetKey;
    private final boolean display;
    private final boolean sortable;
    private final Method getter;
    
    public DomainObjectAttribute(String name, String label, String searchKey, String facetKey, boolean display, boolean sortable, Method getter) {
        this.name = name;
        this.label = label;
        this.searchKey = searchKey;
        this.facetKey = facetKey;
        this.display = display;
        this.sortable = sortable;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
    
    public String getSearchKey() {
        return StringUtils.isEmpty(searchKey)?null:searchKey;
    }

    public String getFacetKey() {
        return StringUtils.isEmpty(facetKey)?null:facetKey;
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isSortable() {
        return sortable;
    }

    public Method getGetter() {
        return getter;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
