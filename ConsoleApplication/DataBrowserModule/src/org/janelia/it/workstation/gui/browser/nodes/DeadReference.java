package org.janelia.it.workstation.gui.browser.nodes;

import java.util.Set;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DeadReference implements DomainObject {

    private Long id;
    private String type;
    
    public DeadReference(Reference reference) {
        this.id = reference.getTargetId();
        this.type = reference.getTargetType();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getOwnerKey() {
        return null;
    }

    public void setOwnerKey(String ownerKey) {
    }

    public Set<String> getReaders() {
        return null;
    }

    public void setReaders(Set<String> readers) {
    }

    public Set<String> getWriters() {
        return null;
    }

    public void setWriters(Set<String> writers) {
    }
}
