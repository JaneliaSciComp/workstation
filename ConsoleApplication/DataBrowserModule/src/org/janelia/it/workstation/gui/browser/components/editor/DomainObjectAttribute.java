package org.janelia.it.workstation.gui.browser.components.editor;

import java.lang.reflect.Method;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectAttribute {

    private final String name;
    private final String label;
    private final boolean facet;
    private Method getter;
    
    public DomainObjectAttribute(String name, String label, boolean facet, Method getter) {
        this.name = name;
        this.label = label;
//        this.dataType = type.isAssignableFrom(Date.class) ? DataType.DATE : DataType.STRING;
        this.facet = facet;
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

    public Method getGetter() {
        return getter;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
