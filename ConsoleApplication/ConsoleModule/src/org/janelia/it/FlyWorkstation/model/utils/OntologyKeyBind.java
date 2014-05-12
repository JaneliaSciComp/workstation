package org.janelia.it.FlyWorkstation.model.utils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Binding of a keyboard shortcut to an ontology term.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OntologyKeyBind {
	
	@XmlElement
	private String key;
	
	@XmlElement
	private Long ontologyTermId;
	
	public OntologyKeyBind() {
	}
	
	public OntologyKeyBind(String key, Long ontologyTermId) {
		super();
		this.key = key;
		this.ontologyTermId = ontologyTermId;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Long getOntologyTermId() {
		return ontologyTermId;
	}
	public void setOntologyTermId(Long ontologyTermId) {
		this.ontologyTermId = ontologyTermId;
	}
}