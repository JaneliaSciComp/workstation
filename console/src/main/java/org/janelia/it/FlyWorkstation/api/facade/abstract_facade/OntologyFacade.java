package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
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

    public Entity createOntologyAnnotation(String username, String sessionId, String targetEntityId, String keyEntityId, String keyString, String valueEntityId, String valueString, String tag) throws Exception;

    public Entity createOntologyRoot(String username, String ontologyName) throws Exception;

    public EntityData createOntologyTerm(String username, Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception;

    public Entity getOntologyTree(String username, Long rootEntityId) throws Exception;

    public List<Entity> getPrivateOntologies(String username) throws Exception;

    public List<Entity> getPublicOntologies() throws Exception;

    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception;

    public void removeOntologyTerm(String username, Long termEntityId) throws Exception;
}
