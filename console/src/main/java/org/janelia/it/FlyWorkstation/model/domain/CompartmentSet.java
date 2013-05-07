package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private MaskedVolume maskedVolume;

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

    @Override
    public String getFast3dImageFilepath() {
        return null;
    }

    @Override
    public MaskedVolume getMaskedVolume() {
        return maskedVolume;
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        log.info("Loading contextualized children for compartment set '{}' (id={})", getName(), getId());

        initChildren();
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);

        String targetAlignmentSpace = alignmentContext.getAlignmentSpaceName();
        String targetOpticalResolution = alignmentContext.getOpticalResolution();
        String targetPixelResolution = alignmentContext.getPixelResolution();

        this.compartmentSet = new ArrayList<Compartment>();

        List<Entity> compartmentSets = ModelMgr.getModelMgr().getEntitiesByTypeName(EntityConstants.TYPE_COMPARTMENT_SET);

        for ( Entity compartmentSetEntity: compartmentSets ) {
            log.debug("Checking compartment set '{}', (id={})", compartmentSetEntity.getName(), compartmentSetEntity.getId());
            ModelMgr.getModelMgr().loadLazyEntity( compartmentSetEntity, false );

            String alignmentSpaceName = compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE );
            String opticalResolution = compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION );
            String pixelResolution = compartmentSetEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION );

            if ( targetAlignmentSpace.equals( alignmentSpaceName )  &&  targetOpticalResolution.equals( opticalResolution )  &&  targetPixelResolution.equals( pixelResolution ) ) {
                // Found the right one.
                log.info("Found compartment set '{}', (id={}).", compartmentSetEntity.getName(), compartmentSetEntity.getId());
                compartmentSet = new HashSet<Compartment>();

                // Getting all the compartments.
                compartmentSetEntity = ModelMgr.getModelMgr().getEntityAndChildren( compartmentSetEntity.getId() );
                Set<Entity> children = compartmentSetEntity.getChildren();
                for ( Entity child: children ) {
                    log.debug("Adding child compartment of {}.", child.getName());
                    if ( child.getEntityType().getName().equals( EntityConstants.TYPE_COMPARTMENT ) ) {
                        ModelMgr.getModelMgr().loadLazyEntity( child, false );
                        Compartment compartmentWrapper = new Compartment( new RootedEntity( child ) );
                        compartmentSet.add( compartmentWrapper );

                    }
                }
            }
        }
    }

    public Collection<Compartment> getCompartmentSet() {
        return compartmentSet;
    }
}
