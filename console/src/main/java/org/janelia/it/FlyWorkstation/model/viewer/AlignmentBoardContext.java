package org.janelia.it.FlyWorkstation.model.viewer;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardEvent;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.model.domain.*;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlignmentBoardContext extends AlignedItem {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardContext.class);

    private AlignmentContext context;
    
    public AlignmentBoardContext(RootedEntity rootedEntity) {
        super(rootedEntity);
        String as = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);
        String ores = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pres = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        this.context = new AlignmentContext(as, ores, pres);
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {

        log.debug("Loading contextualized children for alignment board '{}' (id={})",getName(),getId());
        initChildren();
        
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        RootedEntity rootedEntity = getInternalRootedEntity();
        
        for(RootedEntity child : rootedEntity.getChildrenForAttribute(EntityConstants.ATTRIBUTE_ITEM)) {
            log.debug("Adding child item: {} (id={})",child.getName(),child.getId());
            AlignedItem alignedItem = new AlignedItem(child);
            addChild(alignedItem);
            if (EntityUtils.areLoaded(child.getEntity().getEntityData())) {
                alignedItem.loadContextualizedChildren(alignmentContext);
            }
        }
    }

    public AlignmentContext getAlignmentContext() {
        return context;
    }

    private boolean verifyCompatability(String itemName, String alignmentSpaceName, String opticalResolution, String pixelResolution) {
        if (context==null) return true;
        
        if (!context.getAlignmentSpaceName().equals(alignmentSpaceName)) {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                    "Neuron is not aligned to a compatible alignment space ("+context.getAlignmentSpaceName()+"!="+alignmentSpaceName+")", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if (!context.getOpticalResolution().equals(opticalResolution)) {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                    "Neuron is not aligned to a compatible optical resolution ("+context.getOpticalResolution()+"!="+opticalResolution+")", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if (!context.getPixelResolution().equals(pixelResolution)) {
            JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                    "Neuron is not aligned to a compatible pixel resolution ("+context.getPixelResolution()+"!="+pixelResolution+")", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    /**
     * Add the given entities to the specified alignment board, if possible.
     * @param alignmentBoardContext
     * @param entitiesToAdd
     */
    public void addRootedEntity(RootedEntity rootedEntity) throws Exception {
    
        String type = rootedEntity.getType();
        
        if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            Sample sample = (Sample)EntityWrapperFactory.wrap(rootedEntity);
            sample.loadContextualizedChildren(context);
            addNewAlignedEntity(sample);
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)) {
            Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_SAMPLE);
            Sample sample = (Sample)EntityWrapperFactory.wrap(new RootedEntity(sampleEntity));

            Entity separationEntity = getPipelineAncestor(rootedEntity);
            Entity alignmentEntity = ModelMgr.getModelMgr().getAncestorWithType(separationEntity, EntityConstants.TYPE_ALIGNMENT_RESULT);
            if (alignmentEntity==null) {
                JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Neuron is not aligned", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ( ! isCompatibleAlignmentSpace(rootedEntity, separationEntity, alignmentEntity) ) {
                return;
            }

            sample.loadContextualizedChildren(context);
            
            for(Neuron neuron : sample.getNeuronSet()) {
                if (neuron.getId().equals(rootedEntity.getEntityId())) {
                    addNewAlignedEntity(neuron);
                    return;
                }
            }

            JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                    "Could not find neuron in the aligned neuron separation", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else if (EntityConstants.TYPE_IMAGE_3D.equals(type)  &&  rootedEntity.getName().startsWith( "Reference" )) {
            Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_SAMPLE);
            Sample sample = (Sample)EntityWrapperFactory.wrap(new RootedEntity(sampleEntity));

            Entity separationEntity = getPipelineAncestor(rootedEntity);
            Entity alignmentEntity = ModelMgr.getModelMgr().getAncestorWithType(separationEntity, EntityConstants.TYPE_ALIGNMENT_RESULT);
            if (alignmentEntity==null) {
                JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Neuron is not aligned", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ( ! isCompatibleAlignmentSpace(rootedEntity, separationEntity, alignmentEntity) ) {
                return;
            }

            sample.loadContextualizedChildren(context);
            VolumeImage volumeImage = sample.getReference();
            if ( volumeImage != null ) {
                addNewAlignedEntity( volumeImage );
            }

        }
        else {
            throw new Exception("This entity cannot be viewed in the alignment board.");
        }
    }

    public boolean isAcceptedType( String type ) {
        return EntityConstants.TYPE_NEURON_FRAGMENT.equals( type ) ||
               EntityConstants.TYPE_SAMPLE.equals( type ) ||
               EntityConstants.TYPE_IMAGE_3D.equals( type );
    }
    
    /**
     * Add a new aligned entity to the board. This method must be called from a worker thread.
     * 
     * @param wrapper to be added to the board
     * @throws Exception
     */
    public void addNewAlignedEntity(EntityWrapper wrapper) throws Exception {

        log.info("Adding new aligned entity: {}", wrapper.getName());
        
        final Collection<AlignmentBoardEvent> events = new ArrayList<AlignmentBoardEvent>();
        
        if (wrapper instanceof Sample) {
            Sample sample = (Sample)wrapper;
            AlignedItem sampleAlignedItem = getAlignedItemWithEntityId(sample.getId());

            if (sampleAlignedItem==null) {
                if (sample.getChildren()==null) {
                    sample.loadContextualizedChildren(getAlignmentContext());
                }
                
                sampleAlignedItem = ModelMgr.getModelMgr().addAlignedItem(this, sample, true);
                sampleAlignedItem.loadContextualizedChildren(getAlignmentContext());
                VolumeImage reference = sample.getReference();
                if ( reference != null ) {
                    log.info("Adding reference: {}", reference.getName());
                    AlignedItem childItem = ModelMgr.getModelMgr().addAlignedItem(sampleAlignedItem, reference, true);
                    childItem.loadContextualizedChildren(getAlignmentContext());
                }
                for (EntityWrapper neuron : sample.getNeuronSet()) {
                    log.debug("Adding neuron: {}", neuron.getName());
                    AlignedItem childItem = ModelMgr.getModelMgr().addAlignedItem(sampleAlignedItem, neuron, true);
                    childItem.loadContextualizedChildren(getAlignmentContext());
                }
                
                events.add(new AlignmentBoardItemChangeEvent(this, sampleAlignedItem, ChangeType.Added));
            }
            else {
                events.add(new AlignmentBoardItemChangeEvent(this, sampleAlignedItem, ChangeType.VisibilityChange));
            }
        }
        else if (wrapper instanceof VolumeImage ) {
            log.info("Adding reference: {}", wrapper.getName());
            events.addAll(handleChildWrapper(wrapper));
        }
        else if (wrapper instanceof Neuron) {
            events.addAll(handleChildWrapper(wrapper));
        }
        else if (wrapper instanceof CompartmentSet) {
            CompartmentSet compartmentSet = (CompartmentSet) wrapper;
            AlignedItem compartmentSetAlignedItem = getAlignedItemWithEntityId(compartmentSet.getId());
            
            if (compartmentSetAlignedItem == null) {
                if (compartmentSet.getChildren() == null) {
                    compartmentSet.loadContextualizedChildren(getAlignmentContext());
                }

                compartmentSetAlignedItem = ModelMgr.getModelMgr().addAlignedItem(this, compartmentSet, true);
                compartmentSetAlignedItem.loadContextualizedChildren(getAlignmentContext());
                
                for (Compartment child : compartmentSet.getCompartmentSet()) {
                    log.debug("Adding compartment: {}", child.getName());
                    AlignedItem alignedItem = ModelMgr.getModelMgr().addAlignedItem(compartmentSetAlignedItem, child, true);
                    alignedItem.loadContextualizedChildren(getAlignmentContext());
                }
                
                events.add(new AlignmentBoardItemChangeEvent(this, compartmentSetAlignedItem, ChangeType.Added));
            }
            else {
                events.add(new AlignmentBoardItemChangeEvent(this, compartmentSetAlignedItem, ChangeType.VisibilityChange));
            }

        }
        else if (wrapper instanceof Compartment) {
            log.debug("Handling compartment " + wrapper.getName());
            events.addAll(handleChildWrapper(wrapper));
        }
        else {
            throw new IllegalStateException("Cannot add entity of type "+wrapper.getType()+" to the alignment board.");
        }

        for (AlignmentBoardEvent event : events) {
            ModelMgr.getModelMgr().postOnEventBus(event);
        }
    }
    
    private Collection<AlignmentBoardEvent> handleChildWrapper(EntityWrapper wrapper) throws Exception {
        
        Collection<AlignmentBoardEvent> events = new ArrayList<AlignmentBoardEvent>();
        
        EntityWrapper child = wrapper;
        EntityWrapper parent = wrapper.getParent();

        AlignedItem parentAlignedItem = getAlignedItemWithEntityId(parent.getId());
        if (parentAlignedItem==null) {
            parentAlignedItem = ModelMgr.getModelMgr().addAlignedItem(this, parent, true);
            parentAlignedItem.loadContextualizedChildren(getAlignmentContext());
            log.debug("No parent found for {}.", parent.getName());
        }
        else {
            log.debug("Found parent item for {}, of {}.", parent.getName(), parentAlignedItem.getName() );
        }

        AlignedItem childAlignedItem = parentAlignedItem.getAlignedItemWithEntityId(child.getId());
        if (childAlignedItem == null) {
            childAlignedItem = ModelMgr.getModelMgr().addAlignedItem(parentAlignedItem, child, true);
            childAlignedItem.loadContextualizedChildren(getAlignmentContext());
            events.add(new AlignmentBoardItemChangeEvent(this, childAlignedItem, ChangeType.Added));
        }
        else {
            childAlignedItem.setIsVisible(true);
            events.add(new AlignmentBoardItemChangeEvent(this, childAlignedItem, ChangeType.VisibilityChange));
        }
        
        return events;
    }

    private boolean isCompatibleAlignmentSpace(RootedEntity rootedEntity, Entity separationEntity, Entity alignmentEntity) {
        String alignmentSpaceName = alignmentEntity.getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE);
        String opticalResolution = separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        String pixelResolution = separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        if (!verifyCompatability(rootedEntity.getName(), alignmentSpaceName, opticalResolution, pixelResolution)) {
            return false;
        }

        if (context==null) {
            this.context = new AlignmentContext(alignmentSpaceName, opticalResolution, pixelResolution);
        }
        return true;
    }

    private Entity getPipelineAncestor(RootedEntity rootedEntity) throws Exception {
        Entity separationEntity = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        if (separationEntity==null) {
            throw new IllegalStateException("Neuron is not part of a neuron separation result");
        }
        return separationEntity;
    }

}
