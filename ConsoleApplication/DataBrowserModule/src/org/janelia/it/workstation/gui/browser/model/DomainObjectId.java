package org.janelia.it.workstation.gui.browser.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * A unique identifier for domain objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectId {

    private final String className;
    private final Long id;

    public DomainObjectId(Class<?> clazz, Long id) {
        this.className = clazz.getName();
        this.id = id;
    }
    
    public DomainObjectId(String className, Long id) {
        this.className = className;
        this.id = id;
    }
    
    public String getClassName() {
        return className;
    }

    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DomainObjectId other = (DomainObjectId) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return className + "#" + id;
    }
    
    public static DomainObjectId createFor(DomainObject domainObject) {
        return new DomainObjectId(domainObject.getClass().getName(), domainObject.getId());
    }
}
