package org.janelia.it.workstation.gui.alignment_board;

import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardReference;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardEvent;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.workstation.gui.alignment_board_viewer.CompatibilityChecker;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.model.domain.Compartment;
import org.janelia.it.workstation.model.domain.VolumeImage;
import org.janelia.it.workstation.model.viewer.AlignedItem.InclusionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AB Context that is domain-object friendly.
 * @author fosterl
 */
public class AlignmentBoardContext extends AlignmentBoardItem {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardContext.class);

    private AlignmentContext context;
    private AlignmentBoard alignmentBoard;
    private CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
    private DomainHelper domainHelper = new DomainHelper();
    
    public AlignmentBoardContext(AlignmentBoard alignmentBoard, AlignmentContext alignmentContext) {
        this.context = alignmentContext;
        this.alignmentBoard = alignmentBoard;
    }
    
    public AlignmentBoard getAlignmentBoard() {
        return alignmentBoard;
    }
    
    public AlignmentContext getAlignmentContext() {
        return context;
    }

    /** Unknown if this is still a concept. */
    public boolean isAcceptedType( DomainObject domainObject ) {
        // Not using "type" which is inaccessible, and not declared constant.
        return domainObject instanceof NeuronFragment  ||
               domainObject instanceof Sample  ||
               domainObject instanceof Image;
    }
    
    /**
     * Add a new aligned entity to the board. This method must be called from a worker thread.
     * This is the dispatcher, called with abstract instance.
     * 
     * @param domainObject to be added to the board
     * @throws Exception
     */
    public void addDomainObject(DomainObject domainObject, String objective) throws Exception {

        log.info("Adding new aligned entity: {}, under objective {}.", domainObject.getName(), objective);
        
        final Collection<AlignmentBoardEvent> events = new ArrayList<>();
        
        if (domainObject instanceof Sample) {
            if (! addNewSample(domainObject, events))
                return;
        }
        else if (domainObject instanceof VolumeImage  &&  domainObject instanceof Image) {
            log.warn("Not handling reference " + domainObject.getName() + ".  Not yet supported.");
            if (! addNewReference(domainObject, objective, events)) {
                return;
            }
        }
        else if (domainObject instanceof NeuronFragment) {
            if (! addNewNeuronFragment(domainObject, events)) {
                return;
            }
        }
        else if (domainObject instanceof CompartmentSet) {
            CompartmentSet compartmentSet = (CompartmentSet) domainObject;
            AlignmentBoardItem compartmentSetAlignmentBoardItem = getPreviouslyAddedItem(compartmentSet);
            
            if (compartmentSetAlignmentBoardItem == null) {
                addItem(CompartmentSet.class, compartmentSet, events);                
            }
            else {
                events.add(new AlignmentBoardItemChangeEvent(this, compartmentSetAlignmentBoardItem, ChangeType.VisibilityChange));
            }

        }
        else if (domainObject instanceof Compartment) {
            log.warn("Not handling compartment " + domainObject.getName() + ".  Not yet supported.");
        }
        else {
            throw new IllegalStateException("Cannot add entity of type "+domainObject.getType()+" to the alignment board.");
        }

        // Queue up all events accumulated above.
        for (AlignmentBoardEvent event : events) {
            Events.getInstance().postOnEventBus(event);
        }
    }

    /**
     * Returns the child item with the given id.
     *
     * @param id find this.
     * @return matching item.
     */
    public AlignmentBoardItem getAlignmentBoardItemWithId(Long id) {
        if (alignmentBoard == null) {
            log.warn("This Context (space={}) has not been initialized with children. "
                    + "Calling getAlignmentBoardItemWithId will return null.", context.getAlignmentSpace());
            return null;
        }
        for (AlignmentBoardItem item: alignmentBoard.getChildren()) {
            ABItem abItem = domainHelper.getObjectForItem(item);
            if ( item.getTarget() != null
                    && abItem.getId() != null
                    && abItem.getId().equals(id)) {
                return item;
            } else {
                // Step in one more level.
                for (AlignmentBoardItem childItem : item.getChildren()) {
                    ABItem abChildItem = domainHelper.getObjectForItem(item);
                    if (abChildItem.getId().equals(id)) {
                        return childItem;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Returns parent of the item given.
     * @param childId whose parent to find.
     * @return parent of this child.
     */
    public AlignmentBoardItem getAlignmentBoardItemParent(Long childId) {
        AlignmentBoardItem rtnVal = null;
        for (AlignmentBoardItem item: alignmentBoard.getChildren()) {
            ABItem abItem = domainHelper.getObjectForItem(item);
            if (abItem.getId().equals(childId)) {
                return null;
            }
            rtnVal = getAlignmentBoardItemParent(childId, item);
        }
        return rtnVal;
    }
    
    /**
     * Returns all items belonging to this context.
     * 
     * @return everything
     */
    public List<AlignmentBoardItem> getAlignmentBoardItems() {
        return this.getAlignmentBoard().getChildren();
    }
    
    /** Easily check what this context is about. */
    @Override
    public String toString() {
        return context.getAlignmentSpace() + " " + context.getImageSize() +
               " " + context.getOpticalResolution() + 
               this.alignmentBoard == null ? " NULL Alignment Board ":(" " +
               alignmentBoard.getName());
    }
    
    //=========================================Overrides AlignmentBoardItem
    @Override
    public List<AlignmentBoardItem> getChildren() {
        return alignmentBoard.getChildren();
    }

    @Override
    public AlignmentBoardReference getTarget() {
        return null;
    }

    @Override
    public String getInclusionStatus() {
        return InclusionStatus.In.toString();
    }

    @Override
    public String getColor() {
        return null;
    }

    @Override
    public String getRenderMethod() {
        return null;
    }

    /**
     * Sufficient for fairly small data.  Re-implementation would require
     * adding parent pointer to the object, or caching the tree structure.
     * 
     * @param childId whose parent to look for.
     * @param currentParent candidate as parent of the child id.
     * @return whatever parent is found, or null to signal: keep searching.
     */
    private AlignmentBoardItem getAlignmentBoardItemParent(Long childId, AlignmentBoardItem currentParent) {
        AlignmentBoardItem rtnVal = null;
        for (AlignmentBoardItem item: currentParent.getChildren()) {
            ABItem abItem = domainHelper.getObjectForItem(item);
            if (abItem.getId().equals(childId)) {
                rtnVal = currentParent;
            }
            else {
                rtnVal = getAlignmentBoardItemParent(childId, item);
            }
            if (rtnVal != null) {
                break;
            }
        }
        return rtnVal;
    }

    private boolean addNewNeuronFragment(DomainObject domainObject, final Collection<AlignmentBoardEvent> events) throws Exception {
        NeuronFragment neuronFragment = (NeuronFragment) domainObject;
        Reference sampleRef = neuronFragment.getSample();
        Sample sample = DomainMgr.getDomainMgr().getModel().<Sample>getDomainObject(Sample.class, sampleRef.getTargetId());
        if (sample == null) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Neuron fragment is not aligned", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!compatibilityChecker.isSampleCompatible(context, sample)) {
            return false;
        }
        AlignmentBoardItem sampleItem = getPreviouslyAddedItem(sample);
        if (sampleItem == null) {
            sampleItem = addItem(Sample.class, sample, events);
        }
        AlignmentBoardItem nfItem = getPreviouslyAddedSubItem(sampleItem, neuronFragment);
        if (nfItem == null) {
            addSubItem(NeuronFragment.class, sampleItem, neuronFragment, events);
        } else {
            events.add(new AlignmentBoardItemChangeEvent(this, sampleItem, ChangeType.VisibilityChange));
        }
        return true;
    }

    private boolean addNewSample(DomainObject domainObject, final Collection<AlignmentBoardEvent> events) throws Exception {
        Sample sample = (Sample)domainObject;
        if (! compatibilityChecker.isSampleCompatible(context, sample)) {
            return false;
        }
        AlignmentBoardItem oldSampleItem = this.getPreviouslyAddedItem(sample);
        if (oldSampleItem == null) {
            ReverseReference fragmentsReference = domainHelper.getNeuronRRefForSample(sample, context);
            if (fragmentsReference == null) {
                log.warn("No rev-ref found for sample {}.", sample.getName());
                return false;
            }
            else {
                List<DomainObject> fragments = DomainMgr.getDomainMgr().getModel().getDomainObjects(fragmentsReference);
                AlignmentBoardItem sampleItem = addItem(Sample.class, sample, events);
                for (DomainObject fragmentDO : fragments) {
                    // Need to add neuron to sample.
                    addSubItem(NeuronFragment.class, sampleItem, fragmentDO, events);
                }
            }

        }
        else {
            events.add(new AlignmentBoardItemChangeEvent(this, oldSampleItem, ChangeType.VisibilityChange));
        }
        return true;
    }

    private boolean addNewReference(DomainObject domainObject, String objective, final Collection<AlignmentBoardEvent> events) {
// TODO find the sample entity.
// Holding off on this.  Let's get some neurons first.

//            DomainObject sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(domainObject.getEntity(), EntityConstants.TYPE_SAMPLE);
//            Sample sample = (Sample) EntityWrapperFactory.wrap(new RootedEntity(sampleEntity));
//
//            Entity separationEntity = getPipelineAncestor(domainObject);
//            Entity alignmentEntity = ModelMgr.getModelMgr().getAncestorWithType(separationEntity, EntityConstants.TYPE_ALIGNMENT_RESULT);
//            if (alignmentEntity==null) {
//                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Neuron is not aligned", "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
            // TODO is it compatible?
//            if ( ! isCompatibleAlignmentSpace(domainObject, separationEntity, alignmentEntity) ) {
//                return;
//            }
            // TODO add the new aligned entity.
//            sample.loadContextualizedChildren(context);
//            VolumeImage volumeImage = sample.getItem();
//            if ( volumeImage != null ) {
//                addNewAlignedEntity( volumeImage );
//            }
        return false; // Not yet implemented.
    }
    
    private AlignmentBoardItem addItem(Class modelClass, DomainObject domainObject, final Collection<AlignmentBoardEvent> events) {
        AlignmentBoardItem alignmentBoardItem = createAlignmentBoardItem(domainObject, modelClass);
        // set color?  set inclusion status?
        alignmentBoard.getChildren().add(alignmentBoardItem);
        events.add(new AlignmentBoardItemChangeEvent(this, alignmentBoardItem, ChangeType.Added));
        return alignmentBoardItem;
    }

    private AlignmentBoardItem addSubItem(Class modelClass, AlignmentBoardItem parentItem, DomainObject domainObject, final Collection<AlignmentBoardEvent> events) throws Exception {
        AlignmentBoardItem alignmentBoardItem = createAlignmentBoardItem(domainObject, modelClass);
        // set color?  set inclusion status?
        parentItem.getChildren().add(alignmentBoardItem);
        domainHelper.saveAlignmentBoard(alignmentBoard);
        events.add(new AlignmentBoardItemChangeEvent(this, alignmentBoardItem, ChangeType.Added));        
        return alignmentBoardItem;
    }

    private AlignmentBoardItem createAlignmentBoardItem(DomainObject domainObject, Class modelClass) {
        AlignmentBoardItem alignmentBoardItem = new AlignmentBoardItem();
        Reference reference = new Reference();
        reference.setTargetId(domainObject.getId());
        reference.setTargetClassName(modelClass.getSimpleName());
        alignmentBoardItem.setInclusionStatus(InclusionStatus.In.name());
        alignmentBoardItem.setVisible(true);
        AlignmentBoardReference abRef = new AlignmentBoardReference();
        // TODO: populate abRef

        alignmentBoardItem.setTarget(abRef);
        return alignmentBoardItem;
    }

    private AlignmentBoardItem getPreviouslyAddedItem(DomainObject domainObject) {
        // Add the compartments and the compartment set itself.
        AlignmentBoardItem compartmentSetAlignmentBoardItem = null;
        for (AlignmentBoardItem abi: alignmentBoard.getChildren()) {
            if (abi.getTarget().getObjectRef().getTargetId().equals(domainObject.getId())) {
                compartmentSetAlignmentBoardItem = abi;
            }
        }
        return compartmentSetAlignmentBoardItem;
    }
    
    private AlignmentBoardItem getPreviouslyAddedSubItem(AlignmentBoardItem parent, DomainObject domainObject) {
        // Add the compartments and the compartment set itself.
        AlignmentBoardItem compartmentSetAlignmentBoardItem = null;
        for (AlignmentBoardItem abi: parent.getChildren()) {
            if (abi.getTarget().getObjectRef().getTargetId().equals(domainObject.getId())) {
                compartmentSetAlignmentBoardItem = abi;
            }
        }
        return compartmentSetAlignmentBoardItem;
    }
    
}
