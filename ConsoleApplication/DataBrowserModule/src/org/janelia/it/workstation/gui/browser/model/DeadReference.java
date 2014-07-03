package org.janelia.it.workstation.gui.browser.model;

import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DeadReference implements DomainObject {

    private Reference reference;
    
    public DeadReference(Reference reference) {
        this.reference = reference;
    }
    
    public Reference getReference() {
        return reference;
    }

    public Long getId() {
        return reference.getTargetId();
    }
    
    public void setId(Long id) {
    }

    public String getType() {
        return reference.getTargetType();
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

	@Override
	public Date getCreationDate() {
		return null;
	}

	@Override
	public void setCreationDate(Date creationDate) {
	}

	@Override
	public Date getUpdatedDate() {
		return null;
	}

	@Override
	public void setUpdatedDate(Date updatedDate) {
	}
    
    
}
