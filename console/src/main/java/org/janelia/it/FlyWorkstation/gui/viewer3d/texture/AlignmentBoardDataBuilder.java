package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.EntityFilenameFetcher;
import org.janelia.it.FlyWorkstation.gui.viewer3d.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.*;

/**
 * This is where the data are pulled together for the alignment board viewer.
 */
public class AlignmentBoardDataBuilder implements Serializable {
    private static final String COMPARTMENT_MASK_FILE = "/groups/scicomp/jacsData/MaskResources/Compartment/maskIndex.v3dpbd";
    private static final String COMPARTMENT_MASK_MAPPING_FILE = "/groups/scicomp/jacsData/MaskResources/Compartment/maskNameIndex.txt";
    private static final String COMPARTMENT_ENTITY_NAME = "Compartment";

    private Logger logger = LoggerFactory.getLogger( AlignmentBoardDataBuilder.class );

    private List<RenderableBean> renderableBeanList;

    private boolean sampleAncestorEncountered;

    public AlignmentBoardDataBuilder() {
    }

    /**
     * Plug in the alignment board entity, from which all returned information is derived.
     *
     * @param alignmentBoard support for pipelining the output.
     * @return reference to this object.
     */
    public AlignmentBoardDataBuilder setAlignmentBoard( Entity alignmentBoard ) {
        clear();
        renderableBeanList = new ArrayList<RenderableBean>();
        Map<Entity,List<Entity>> ancestorToRenderables = new HashMap<Entity,List<Entity>>();
        Map<Entity,Entity> labelToPipelineResult = new HashMap<Entity,Entity>();

        List<Entity> displayableList = getDisplayableList( alignmentBoard );
        Map<Entity,Entity> displayableToBaseEntity = getMaskContainerAncestors(ancestorToRenderables, displayableList);
        Set<Entity> baseEntities = new HashSet<Entity>();
        baseEntities.addAll(displayableToBaseEntity.values());

        List<Entity> consolidatedLabelsList = getMaskLabelEntities(labelToPipelineResult, baseEntities);
        Map<Entity,Entity> sampleToBaseEntity = new HashMap<Entity,Entity>();
        Set<Entity> sampleEntities = getSampleEntities( displayableList, displayableToBaseEntity, sampleToBaseEntity );

        Map<Entity,Set<Entity>> signalToLabelEntities = getSignalToLabelEntities(labelToPipelineResult);

        Map<Entity,String> labelEntityToSignalFilename =
                findLabelEntityToSignalFilename( sampleEntities, sampleToBaseEntity, signalToLabelEntities );
        createRenderableBeanList(ancestorToRenderables, labelToPipelineResult, consolidatedLabelsList, labelEntityToSignalFilename);

        applyCompartmentMask(displayableList);

        return this;
    }

    /**
     * Returns the full collection of things found by this builder, which may be rendered.
     *
     * @return list configured into existence at construction time.
     */
    public Collection<RenderableBean> getRenderableBeanList() {
        return renderableBeanList;
    }

    //--------------------------------------HELPERS
    private void clear() {
        renderableBeanList = null;
        sampleAncestorEncountered = false;
    }

    private void applyCompartmentMask( List<Entity> displayableList ) {
        if ( renderableBeanList.size() == 0  ||  displayableList.size() == 0 ) {
            return;
        }

        // First, figure out if we have anything relevant to do here.
        List<String> compartmentNames = new ArrayList<String>();
        for ( Entity entity: displayableList ) {
            try {
                Entity definingAncestor = ModelMgr.getModelMgr().getAncestorWithType( entity, EntityConstants.TYPE_FOLDER );
                if ( definingAncestor.getName().equals( COMPARTMENT_ENTITY_NAME ) ) {
                    // Know: we have a compartment.
                    String compartmentName = entity.getName();
                    compartmentNames.add( compartmentName );
                }
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }

        }

        if ( compartmentNames.size() > 0 ) {
            CacheFileResolver cacheFileResolver = new CacheFileResolver();
            String maskIndex =
                    cacheFileResolver.getResolvedFilename(
                            COMPARTMENT_MASK_FILE
                    );

            // Tack along to the last existing bean, and get its signal file name.
            String lastSignalFile = null;
            for ( RenderableBean bean: renderableBeanList ) {
                lastSignalFile = bean.getSignalFile();
            }

            // Look for highest-numbered translation.
            int highestTranslatedLabel = -1;
            for ( RenderableBean bean: renderableBeanList ) {
                if ( bean.getTranslatedNum() >= highestTranslatedLabel ) {
                    highestTranslatedLabel = bean.getTranslatedNum() + 1;
                }
            }

            // Pull in info from the compartments definition text file.
            File maskNameIndex = new File(
                cacheFileResolver.getResolvedFilename(
                    COMPARTMENT_MASK_MAPPING_FILE
                )
            );

            Map<String, Integer> nameVsNum = new HashMap<String,Integer>();
            try {
                BufferedReader rdr = new BufferedReader( new FileReader( maskNameIndex ) );
                String nextLine = null;

                // Prototype input line:
                // 56 WED_L "Description" ( 53 45 215 )
                while ( null != ( nextLine = rdr.readLine() ) ) {
                    String[] fields = nextLine.trim().split( " " );
                    if ( fields.length >= 8 ) {
                        Integer labelNum = Integer.parseInt( fields[ 0 ] );
                        String compartmentName = fields[ 1 ];
                        nameVsNum.put( compartmentName, labelNum );
                    }
                }
                rdr.close();
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }

            // Now, add all the renderableBeanList.
            for ( String compartmentName: compartmentNames ) {
                Integer labelNum = nameVsNum.get( compartmentName );
                if ( labelNum != null ) {
                    RenderableBean maskIndexBean = new RenderableBean();
                    maskIndexBean.setLabelFile( maskIndex );
                    maskIndexBean.setSignalFile( lastSignalFile );
                    maskIndexBean.setLabelFileNum( labelNum );

                    maskIndexBean.setTranslatedNum( highestTranslatedLabel ++ );
                    maskIndexBean.setRgb( new byte[] { 20, 20, 20, RenderMappingI.COMPARTMENT_RENDERING } );

                    renderableBeanList.add(maskIndexBean);
                }
                else {
                    logger.warn( "Encountered compartment {} with no known label number.", compartmentName );
                }
            }
        }

    }

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
            for ( Entity baseEntity: baseEntities ) {
                List<Entity> nextLabelList = new ArrayList<Entity>();
                if ( baseEntity != null ) {
                    recursivelyFindConsolidatedLabels(nextLabelList, baseEntity);
                    if ( nextLabelList.size() == 0 ) {
                        logger.info(
                                "No labels found for base entity {}:{}", baseEntity.getName(), baseEntity.getId()
                        );
                    }
                    consolidatedLabelsList.addAll( nextLabelList );
                    for ( Entity label: nextLabelList ) {
                        labelToPipelineResult.put(label, baseEntity);
                    }
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

    private Map<Entity,Entity> getMaskContainerAncestors(Map<Entity, List<Entity>> ancestorToRenderables, List<Entity> displayableList) {
        // Resolve interesting entities to usable ancestors.
        Map<Entity,Entity> displayableToBaseEntity = new HashMap<Entity,Entity>();
        try {
            for ( Entity displayable: displayableList ) {
                Entity baseEntity = findPipelineResultAncestor(displayable);
                if ( displayable.getEntityType().getName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                    List<Entity> pipelineResultFragments = ancestorToRenderables.get( baseEntity );
                    if ( pipelineResultFragments == null ) {
                        pipelineResultFragments = new ArrayList<Entity>();
                        ancestorToRenderables.put(baseEntity, pipelineResultFragments);
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
            logger.debug("Displayable from base entity of {}: {}.", baseEntity.getName(), baseEntity.getId());
        return baseEntity;
    }

    private void createRenderableBeanList(
            Map<Entity, List<Entity>> ancestorToRenderables,
            Map<Entity, Entity> labelToPipelineResult,
            List<Entity> consolidatedLabelsList,
            Map<Entity, String> labelEntityToSignalFilename) {

        FileResolver resolver = new CacheFileResolver();

        int fragmentOffset = 1;
        for ( Entity labelEntity: consolidatedLabelsList ) {
            String labelFilename = EntityUtils.getFilePath(labelEntity);
            if ( labelFilename != null ) {
                String signalFilename = labelEntityToSignalFilename.get( labelEntity );
                labelFilename = resolver.getResolvedFilename( labelFilename );
                String finalLabelFile = ensureLabelFile(labelFilename);

                // Get any renderableBeanList associated. Create fragment bean.
                Entity sample = labelToPipelineResult.get( labelEntity );
                List<Entity> sampleFragments = ancestorToRenderables.get( sample );

                if ( sampleFragments != null ) {
                    for ( Entity fragment: sampleFragments ) {
                        RenderableBean bean = new RenderableBean();
                        bean.setLabelFile( finalLabelFile );
                        bean.setSignalFile( signalFilename );
                        bean.setEntity( fragment );
                        bean.setTranslatedNum( fragmentOffset++ );

                        renderableBeanList.add(bean);
                    }
                }
                ancestorToRenderables.remove(sample);
            }
        }

        if ( renderableBeanList.size() == 0 ) {
            logger.warn("No renderables produced.  See info below.");
            logger.info("Got {} ancestorToRenderables.", ancestorToRenderables.size());
            logger.info( "Got {} labelToPipelineResults.", labelToPipelineResult.size() );
            logger.info( "Got {} consolidatedLabelsList.", consolidatedLabelsList.size() );
            logger.info( "Got {} labelEntityToSignalFilename.", labelEntityToSignalFilename.size() );
        }

    }

    private Map<Entity,String> findLabelEntityToSignalFilename(
            Collection<Entity> sampleEntities,
            Map<Entity, Entity> sampleToBaseEntity,
            Map<Entity, Set<Entity>> signalToLabelEntities
    ) {
        Map<Entity,String> labelEntityToSignalFilename = new HashMap<Entity,String>();
        try {
            EntityFilenameFetcher filenameFetcher = new EntityFilenameFetcher();

            for ( Entity sampleEntity: sampleEntities ) {

                // Find this entity's file name of interest.
                String typeName = sampleEntity.getEntityType().getName();
                String entityConstantFileType = filenameFetcher.getEntityConstantFileType( typeName );
                String filename = filenameFetcher.fetchFilename( sampleEntity, entityConstantFileType );
                if ( filename != null ) {
                    // Build a mapping of label entity to signal filename.
                    Entity baseEntity = sampleToBaseEntity.get( sampleEntity );
                    if ( baseEntity == null ) {
                        logger.warn("No mapping to base entity from sample entity {}/{}." +
                                sampleEntity.getName(), sampleEntity.getId() );
                    }
                    else {
                        Set<Entity> labelEntities = signalToLabelEntities.get( baseEntity );
                        if ( labelEntities != null ) {
                            for ( Entity labelEntity: labelEntities ) {
                                labelEntityToSignalFilename.put( labelEntity, filename );
                            }
                        }
                        else {
                            logger.warn( "No label entities found for {}.", baseEntity.getName() );
                            logger.info( "signalToLabelEntities size is {}.", signalToLabelEntities.size() ) ;
                            for ( Entity signalEntity: signalToLabelEntities.keySet() ) {
                                Set<Entity> labelEntitySet = signalToLabelEntities.get( signalEntity );
                                logger.info( "Signal Entity Key: {}:{}", signalEntity.getName(), signalEntity.getId() );
                                for ( Entity labelEntity: labelEntitySet ) {
                                    logger.info( "    Label entity {}:{}", labelEntity.getName(), labelEntity.getId() );
                                }
                            }
                        }

                    }
                }

            }
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }

        return labelEntityToSignalFilename;
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

        boolean useThisEntity = false;
        if ( EntityConstants.TYPE_SAMPLE.equals(entityTypeName) ) {
            sampleAncestorEncountered = true;
            useThisEntity = true;
        }

        if ( EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityTypeName)  &&  sampleAncestorEncountered == false ) {
            useThisEntity = true;
        }
        else if ( entityTypeName.equals( EntityConstants.TYPE_IMAGE_3D ) ) {
            Entity definingAncestor = ModelMgr.getModelMgr().getAncestorWithType( entity, EntityConstants.TYPE_FOLDER );
            if ( definingAncestor.getName().equals( COMPARTMENT_ENTITY_NAME ) ) {
                useThisEntity = true;
            }
        }

        if ( useThisEntity ) {
            logger.debug("Adding a child of type " + entityTypeName + ", named " + entity.getName());
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
        if ( entity.getName().equals("ConsolidatedLabel.v3dpbd" ) ) {
            logger.debug("Adding a child of type " + entityTypeName + ", named " + entity.getName());
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