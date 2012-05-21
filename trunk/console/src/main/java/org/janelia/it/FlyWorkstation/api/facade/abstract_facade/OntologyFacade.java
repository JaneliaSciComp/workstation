package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:51 PM
 */
public interface OntologyFacade extends EntityFacade {
    List<Entity> getOntologies();

    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception;

    public void removeOntologyAnnotation(Long annotationId) throws Exception;
    
    public Entity createOntologyRoot(String ontologyName) throws Exception;

    public EntityData createOntologyTerm(Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception;

    public Entity getOntologyTree(Long rootEntityId) throws Exception;

    public List<Entity> getPrivateOntologies() throws Exception;

    public List<Entity> getPublicOntologies() throws Exception;

    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception;

    public void removeOntologyTerm(Long termEntityId) throws Exception;
}
