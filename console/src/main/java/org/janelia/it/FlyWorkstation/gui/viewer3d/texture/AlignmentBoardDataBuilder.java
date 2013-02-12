package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.EntityFilenameFetcher;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.FragmentBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * This is where the data are pulled together for the alignment board viewer.
 */
public class AlignmentBoardDataBuilder implements Serializable {
    private Logger logger = LoggerFactory.getLogger( AlignmentBoardDataBuilder.class );

    private List<String> signalFilenames;
    //private List<String> maskFilenames;
    private List<FragmentBean> fragments;
    private Map<String,List<String>> signalToMaskFilenames;
    private Map<String,List<FragmentBean>> signalFilenameToFragments;

    private boolean sampleAncestorEncountered;

    public AlignmentBoardDataBuilder() {
    }

    public void setAlignmentBoard( Entity alignmentBoard ) {
        fragments = new ArrayList<FragmentBean>();
        Map<Entity,List<Entity>> ancestorToFragments = new HashMap<Entity,List<Entity>>();
        Map<Entity,Entity> labelToPipelineResult = new HashMap<Entity,Entity>();

        List<Entity> displayableList = getDisplayableList( alignmentBoard );
        Map<Entity,Entity> displayableToBaseEntity = getMaskContainerAncestors(ancestorToFragments, displayableList);
        Set<Entity> baseEntities = new HashSet<Entity>();
        baseEntities.addAll(displayableToBaseEntity.values());

        List<Entity> consolidatedLabelsList = getMaskLabelEntities(labelToPipelineResult, baseEntities);
        Map<Entity,Entity> sampleToBaseEntity = new HashMap<Entity,Entity>();
        Set<Entity> sampleEntities = getSampleEntities( displayableList, displayableToBaseEntity, sampleToBaseEntity );

        Map<Entity,Set<Entity>> signalToLabelEntities = getSignalToLabelEntities(labelToPipelineResult);

        Map<Entity,String> labelEntityToSignalFilename = findSignalFilenames(sampleEntities, sampleToBaseEntity, signalToLabelEntities);
        findLabelFilenames(ancestorToFragments, labelToPipelineResult, consolidatedLabelsList, labelEntityToSignalFilename);
        applyFalseCompartmentMask();


    }

    // *** TEMP ***
    private void applyFalseCompartmentMask() {
        // Special section, for testing: add some arbitrary compartment masks to the mix.
        // NOTE: use of assumed non-null keyset/iterator could lead to NPE.  Brittle. For test only.
        String firstFilename = signalToMaskFilenames.keySet().iterator().next();
        List<String> firstMaskList = signalToMaskFilenames.get( firstFilename );

        List<FragmentBean> firstBeanList = signalFilenameToFragments.get( firstFilename );

        String maskIndex =
                new CacheFileResolver().getResolvedFilename(
                        "/groups/scicomp/jacsData/MaskResources/Compartment/maskIndex.v3dpbd"
                );
        firstMaskList.add( maskIndex );
        FragmentBean maskIndexBean = new FragmentBean();
        maskIndexBean.setLabelFile(maskIndex);
        maskIndexBean.setLabelFileNum(17); // 17 OTU_L "Description" ( 112 1 51 )
        maskIndexBean.setTranslatedNum( firstBeanList.size() + 1 );
        maskIndexBean.setRgb( new byte[] { 20, 20, 20 } );

        firstBeanList.add( maskIndexBean );

        fragments.add( maskIndexBean );
    }

    //todo change this to get all the masks, given the filename out of this iterator.
    public List<String> getSignalFilenames() {
        return signalFilenames;
    }

    public List<String> getMaskFilenames( String signalFilename ) {
        return signalToMaskFilenames.get( signalFilename );
    }

    public List<String> getMaskFilenames() {
        List<String> rtnVal = new ArrayList<String>();
        for ( String signalFilename: signalToMaskFilenames.keySet() ) {
            List<String> maskFilenames = signalToMaskFilenames.get( signalFilename );
            rtnVal.addAll( maskFilenames );
        }
        return rtnVal;
    }

    public List<FragmentBean> getFragments() {
        return fragments;
    }

    public List<FragmentBean> getFragments( String signalFilename ) {
        return signalFilenameToFragments.get( signalFilename );
    }

    //--------------------------------------HELPERS
    private Map<Entity,Set<Entity>> getSignalToLabelEntities(Map<Entity, Entity> labelToPipelineResult) {
        Map<Entity,Set<Entity>> rtnVal = new HashMap<Entity,Set<Entity>> ();

        for ( Entity labelEntity: labelToPipelineResult.keySet() ) {
            Entity value = labelToPipelineResult.get( labelEntity );
            Set<Entity> labelSet = rtnVal.get( value );
            if ( labelSet == null ) {
                labelSet = new HashSet<Entity>();
                rtnVal.put( value, labelSet );
            }
            labelSet.add( labelEntity );
        }

        return rtnVal;
    }

    private Set<Entity> getSampleEntities(List<Entity> displayableList,
                                          Map<Entity,Entity> displayableToBaseEntity,
                                          Map<Entity,Entity> sampleToBaseEntity
    ) {
        // Get all the signal filenames.  These are to be masked-by the mask filenames' contents.
        Set<Entity> sampleEntities = new HashSet<Entity>();
        for ( Entity displayable: displayableList ) {
            try {
                Entity sampleAncestor = findSampleAncestor(displayable);
                sampleEntities.add(sampleAncestor);

                sampleToBaseEntity.put( sampleAncestor, displayableToBaseEntity.get( displayable ) );
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }
        return sampleEntities;
    }

    private List<Entity> getMaskLabelEntities(Map<Entity, Entity> labelToPipelineResult, Collection<Entity> baseEntities) {
        // Get the mask items.
        List<Entity> consolidatedLabelsList = new ArrayList<Entity>();
        try {
            List<Entity> nextLabelList = new ArrayList<Entity>();
            for ( Entity baseEntity: baseEntities ) {
                if ( baseEntity != null ) {
                    recursivelyFindConsolidatedLabels( nextLabelList, baseEntity );
                    consolidatedLabelsList.addAll( nextLabelList );
                    for ( Entity label: nextLabelList ) {
                        labelToPipelineResult.put(label, baseEntity);
                    }
                }
                else {
                    logger.warn( "Found no sample for neuron fragment {}.  Not displaying.", baseEntity.getId() );
                }
            }
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }
        return consolidatedLabelsList;
    }

    private List<Entity> getDisplayableList(Entity alignmentBoard) {
        // Find all interesting entities on the alignment board.
        List<Entity> displayableList = new ArrayList<Entity>();
        try {
            sampleAncestorEncountered = false;
            recursivelyFindDisplayableChildren( displayableList, alignmentBoard );
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
        return displayableList;
    }

    private Map<Entity,Entity> getMaskContainerAncestors(Map<Entity, List<Entity>> ancestorToFragments, List<Entity> displayableList) {
        // Resolve interesting entities to usable ancestors.
        Map<Entity,Entity> displayableToBaseEntity = new HashMap<Entity,Entity>();
        try {
            for ( Entity displayable: displayableList ) {
                Entity baseEntity = findPipelineResultAncestor(displayable);
                if ( displayable.getEntityType().getName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                    List<Entity> pipelineResultFragments = ancestorToFragments.get( baseEntity );
                    if ( pipelineResultFragments == null ) {
                        pipelineResultFragments = new ArrayList<Entity>();
                        ancestorToFragments.put(baseEntity, pipelineResultFragments);
                    }
                    pipelineResultFragments.add(displayable);
                }
                displayableToBaseEntity.put(displayable, baseEntity);
            }
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }
        return displayableToBaseEntity;
    }

    private Entity findSampleAncestor(Entity displayableChild) throws Exception {
        return findTypedAncestor( displayableChild, EntityConstants.TYPE_SAMPLE );
    }

    private Entity findPipelineResultAncestor(Entity displayableChild) throws Exception {
        return findTypedAncestor( displayableChild, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT );
    }

    private Entity findTypedAncestor(Entity displayableChild, String ancestorEntityTypeConstant) throws Exception {
        Entity baseEntity;
        if ( displayableChild.getEntityType().getName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
            baseEntity =
                    ModelMgr.getModelMgr().getAncestorWithType( displayableChild, ancestorEntityTypeConstant);
        }
        else {
            baseEntity = displayableChild;
        }
        if ( baseEntity != null )
            logger.info( "Displayable from base entity of {}: {}.", baseEntity.getName(), baseEntity.getId() );
        return baseEntity;
    }

    private void findLabelFilenames(
            Map<Entity, List<Entity>> sampleToFragments,
            Map<Entity, Entity> labelToPipelineResult,
            List<Entity> consolidatedLabelsList,
            Map<Entity,String> labelEntityToSignalFilename) {

        signalToMaskFilenames = new HashMap<String,List<String>>();
        signalFilenameToFragments = new HashMap<String,List<FragmentBean>>();
        FileResolver resolver = new CacheFileResolver();

        int fragmentOffset = 1;
        for ( Entity labelEntity: consolidatedLabelsList ) {
            String labelFilename = EntityUtils.getFilePath(labelEntity);
            if ( labelFilename != null ) {
//                if ( matchesSomeSignal( signalFilenames, labelFilename ) ) {
                String signalFilename = labelEntityToSignalFilename.get( labelEntity );
                labelFilename = resolver.getResolvedFilename( labelFilename );
                String finalLabelFile = ensureLabelFile(labelFilename);

                List<String> maskFilenamesForSignal = signalToMaskFilenames.get( signalFilename );
                if ( maskFilenamesForSignal == null ) {
                    maskFilenamesForSignal = new ArrayList<String>();
                    signalToMaskFilenames.put( signalFilename, maskFilenamesForSignal );
                }
                maskFilenamesForSignal.add( labelFilename );

                // Get any fragments associated. Create fragment bean.
                Entity sample = labelToPipelineResult.get( labelEntity );
                List<Entity> sampleFragments = sampleToFragments.get( sample );

                if ( sampleFragments != null ) {
                    for ( Entity fragment: sampleFragments ) {
                        FragmentBean bean = new FragmentBean();
                        bean.setLabelFile( finalLabelFile );
                        bean.setFragment( fragment );
                        bean.setTranslatedNum( fragmentOffset++ );

                        fragments.add( bean );

                        List<FragmentBean> fragmentsForSignalFn = signalFilenameToFragments.get( signalFilename );
                        if ( fragmentsForSignalFn == null ) {
                            fragmentsForSignalFn = new ArrayList<FragmentBean>();
                            signalFilenameToFragments.put( signalFilename, fragmentsForSignalFn );
                        }
                        fragmentsForSignalFn.add( bean );
                    }
                }
                sampleToFragments.remove( sample );
            }
//            }
        }
    }

    private Map<Entity,String> findSignalFilenames(
            Collection<Entity> sampleEntities,
            Map<Entity,Entity> sampleToBaseEntity,
            Map<Entity,Set<Entity>> signalToLabelEntities
    ) {
        signalFilenames = new ArrayList<String>();

        Map<Entity,String> labelEntityToSignalFilename = new HashMap<Entity,String>();
        try {
            EntityFilenameFetcher filenameFetcher = new EntityFilenameFetcher();

            for ( Entity sampleEntity: sampleEntities ) {

                // Find this entity's file name of interest.
                String typeName = sampleEntity.getEntityType().getName();
                String entityConstantFileType = filenameFetcher.getEntityConstantFileType( typeName );
                String filename = filenameFetcher.fetchFilename( sampleEntity, entityConstantFileType );
                if ( filename != null ) {
                    signalFilenames.add( filename );
                }

                // Side effect: build a mapping of label entity to signal filename.
                // NOTE: _emphasis_ this for-loop looks at the returned collection to get its
                //    iterator.  Had I phrased it as "iterate over keyset" I would then have
                //    to traverse all keys, getting each set in turn.
                Entity baseEntity = sampleToBaseEntity.get( sampleEntity );
                if ( baseEntity == null ) {
                    logger.warn("No mapping to base entity from sample entity {}/{}." +
                            sampleEntity.getName(), sampleEntity.getId() );
                }
                else {
                    for ( Entity labelEntity: signalToLabelEntities.get( baseEntity ) ) {
                        labelEntityToSignalFilename.put( labelEntity, filename );
                    }

                }
            }
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }

        return labelEntityToSignalFilename;
    }

    /**
     * This may/may not be temporary.  It makes sure that the base path of th mask filename matches one from
     * a signal filename, to avoid attempting to load things that are not compatible.
     */
    private boolean matchesSomeSignal( List<String> signalFileNames, String maskFilename ) {
        boolean rtnVal = false;
        File maskFile = new File( maskFilename );
        File baseLoc = maskFile.getParentFile();
        String maskParentPath = baseLoc.getPath();

        for ( String signalFileName: signalFileNames ) {
            File signalFileLoc = new File( signalFileName ).getParentFile();
            String signalParentPath = signalFileLoc.getPath();

            if ( signalParentPath.startsWith( maskParentPath ) ) {
                rtnVal = true;
                break;
            }
        }

        return rtnVal;
    }

    //@todo complete this as needed.
    private String ensureLabelFile(String filePath) {
        // This method is stubbed. The final implementation will be similar to below:
        //  the existing file will need to have a path like "/archive/fastLoad/" inserted
        //  into it.  Details not precise at the moment.
        if (0 == 0)
            return filePath;

        String rtnVal = null;
        try {
            // This is a fast loading separation. Ensure all
            // files are copied from archive.
//            String filePath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
//                    + "/archive";

            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>(); /* Here, add the archive path parts and try the sync */
            taskParameters.add(new TaskParameter("file path", filePath, null));
            Task task = new GenericTask(new HashSet<Node>(), SessionMgr.getSubjectKey(),
                    new ArrayList<Event>(), taskParameters, "syncFromArchive", "Sync From Archive");
            task.setJobName("Sync From Archive Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
            ModelMgr.getModelMgr().submitJob("SyncFromArchive", task.getObjectId());

        }
        catch (Exception ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }

        return rtnVal;
    }

    private void recursivelyFindDisplayableChildren(List<Entity> displayableList, Entity entity) throws Exception {
        entity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
        logger.debug("Recursing into " + entity.getName());
        String entityTypeName = entity.getEntityType().getName();

        //                EntityConstants.TYPE_CURATED_NEURON.equals(entityTypeName)
        //                EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(entityTypeName)

        boolean useThisEntity = false;
        if ( EntityConstants.TYPE_SAMPLE.equals(entityTypeName) ) {
            sampleAncestorEncountered = true;
            useThisEntity = true;
        }

        if ( EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityTypeName)  &&  sampleAncestorEncountered == false ) {
            useThisEntity = true;
//            fragments.add( entity );
        }

        if ( useThisEntity ) {
            logger.info("Adding a child of type " + entityTypeName + ", named " + entity.getName() );
            displayableList.add(entity);
        }

        if ( entity.hasChildren() ) {
            for (Entity childEntity: entity.getChildren()) {
                recursivelyFindDisplayableChildren(displayableList, childEntity);
            }
        }
    }

    private void recursivelyFindConsolidatedLabels(List<Entity> consLabelList, Entity entity) throws Exception {
        entity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
        logger.debug("Recursing into " + entity.getName());
        String entityTypeName = entity.getEntityType().getName();
        // Finding the right kind of supporting data.
        if (
/*
             entity.getName().equals("ConsolidatedLabel2_25.v3dpbd" )
*/
                entity.getName().equals("ConsolidatedLabel.v3dpbd" )
                ) {
            logger.info("Adding a child of type " + entityTypeName + ", named " + entity.getName() );
            // This is that kind of an entity!
            consLabelList.add(entity);
        }
        else {
            //  We assume supporting data does not HAVE supporting data.
            if ( entity.hasChildren() ) {
                for (Entity childEntity: entity.getChildren()) {
                    recursivelyFindConsolidatedLabels(consLabelList, childEntity);
                }
            }
        }
    }

}