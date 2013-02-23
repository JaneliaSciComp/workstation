package org.janelia.it.FlyWorkstation.model.utils;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;


/**
 * The set of key bindings for an ontology. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OntologyKeyBindings {

	@XmlElement
	private String user;

	@XmlElement
	private Long ontologyId;
	
    @XmlElement(name="keyBinding")
    @XmlElementWrapper(name="keyBindingSet")
	private Set<OntologyKeyBind> keybindSet = new HashSet<OntologyKeyBind>();

	public OntologyKeyBindings() {
	}
	
	public OntologyKeyBindings(String subjectKey, Long ontologyId) {
		super();
		this.user = subjectKey;
		this.ontologyId = ontologyId;
	}
	
	public void addBinding(String key, Long ontologyTermId) {
		keybindSet.add(new OntologyKeyBind(key, ontologyTermId));
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Long getOntologyId() {
		return ontologyId;
	}

	public void setOntologyId(Long ontologyId) {
		this.ontologyId = ontologyId;
	}

	public Set<OntologyKeyBind> getKeybinds() {
		return keybindSet;
	}
	

}
