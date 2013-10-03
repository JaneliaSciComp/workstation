package org.janelia.it.FlyWorkstation.model.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 5/3/13
 * Time: 12:41 PM
 *
 * This is a container for
 * @see org.janelia.it.FlyWorkstation.model.domain.Compartment
 * objects which are all in the same alignment space.
 */
public class CompartmentSet extends AlignedEntityWrapper implements Viewable2d, Viewable3d, Viewable4d {

    private Logger log;
    private Collection<Compartment> compartmentSet;

    public CompartmentSet( RootedEntity wrappedEntity ) {
        super( wrappedEntity );
        log = LoggerFactory.getLogger( CompartmentSet.class );

    }
    @Override
    public String get2dImageFilepath() {
        return null;
    }

    @Override
    public String get3dImageFilepath() {
        return null;
    }

    public String getFast3dImageFilepath() {
        return null;
    }

    @Override
    public MaskedVolume getMaskedVolume() {
        return null;
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        log.info("Loading contextualized children for compartment set '{}' (id={})", getName(), getId());

        initChildren();
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        AlignmentSpace targetSpace = new AlignmentSpace( alignmentContext );

        this.compartmentSet = Collections.EMPTY_LIST;

        List<Entity> compartmentSets = ModelMgr.getModelMgr().getEntitiesByTypeName(EntityConstants.TYPE_COMPARTMENT_SET);

        for ( Entity compartmentSetEntity: compartmentSets ) {
            log.debug("Checking compartment set '{}', (id={})", compartmentSetEntity.getName(), compartmentSetEntity.getId());
            ModelMgr.getModelMgr().loadLazyEntity( compartmentSetEntity, false );

            AlignmentSpace compartmentSetSpace = new AlignmentSpace( compartmentSetEntity );

            if ( targetSpace.equals( compartmentSetSpace ) ) {
                // Found the right one.
                log.info("Found compartment set '{}', (id={}).", compartmentSetEntity.getName(), compartmentSetEntity.getId());
                compartmentSet = new TreeSet<Compartment>();

                // Getting all the compartments.
                compartmentSetEntity = ModelMgr.getModelMgr().getEntityAndChildren( compartmentSetEntity.getId() );
                Set<Entity> children = compartmentSetEntity.getChildren();
                for ( Entity child: children ) {
                    log.debug("Adding child compartment of {}.", child.getName());
                    if ( child.getEntityType().getName().equals( EntityConstants.TYPE_COMPARTMENT ) ) {
                        ModelMgr.getModelMgr().loadLazyEntity( child, false );
                        Compartment compartmentWrapper = new Compartment( new RootedEntity( child ) );
                        compartmentSet.add( compartmentWrapper );
                        addChild(compartmentWrapper);

                    }
                }
            }
        }
    }

    public Collection<Compartment> getCompartmentSet() {
        return compartmentSet;
    }
}
