package org.janelia.it.workstation.gui.browser.model;

import java.lang.reflect.Method;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectAttribute {

    private final String name;
    private final String label;
    private final boolean facet;
    private final boolean display;
    private final Method getter;
    
    public DomainObjectAttribute(String name, String label, boolean facet, boolean display, Method getter) {
        this.name = name;
        this.label = label;
        this.facet = facet;
        this.display = display;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFacet() {
        return facet;
    }

    public boolean isDisplay() {
        return display;
    }

    public Method getGetter() {
        return getter;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
