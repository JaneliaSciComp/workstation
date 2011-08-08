package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:48 AM
 */
public class EJBOntologyFacade extends EJBEntityFacade implements OntologyFacade {
    @Override
    public List<Entity> getOntologies() {
//        return EJBFactory.getRemoteAnnotationBean().
        // todo fix this
        return new ArrayList<Entity>();
    }

    @Override
    public Entity createOntologyAnnotation(String username, String sessionId, String targetEntityId, String keyEntityId, String keyString, String valueEntityId, String valueString, String tag) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyAnnotation(username, sessionId, targetEntityId, keyEntityId, keyString, valueEntityId, valueString, tag);
    }

    @Override
    public Entity createOntologyRoot(String username, String ontologyName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyRoot(username, ontologyName);
    }

    @Override
    public EntityData createOntologyTerm(String username, Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyTerm(username, parentEntityId, label, type, orderIndex);
    }

    @Override
    public Entity getOntologyTree(String username, Long rootEntityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getOntologyTree(username, rootEntityId);
    }

    @Override
    public List<Entity> getPrivateOntologies(String username) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPrivateOntologies(username);
    }

    @Override
    public List<Entity> getPublicOntologies() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPublicOntologies();
    }

    @Override
    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().publishOntology(ontologyEntityId, rootName);
    }

    @Override
    public void removeOntologyTerm(String username, Long termEntityId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(username, termEntityId);
    }
}
