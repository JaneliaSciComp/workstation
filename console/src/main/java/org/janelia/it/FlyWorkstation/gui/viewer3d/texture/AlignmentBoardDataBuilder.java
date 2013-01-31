package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.EntityFilenameFetcher;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This is where the data are pulled together for the alignment board viewer.
 */
public class AlignmentBoardDataBuilder implements Serializable {
    private Logger logger = LoggerFactory.getLogger( AlignmentBoardDataBuilder.class );

    private List<String> signalFilenames;
    private List<String> maskFilenames;

    public AlignmentBoardDataBuilder() {
    }

    public void setAlignmentBoard( Entity alignmentBoard ) {
        // First how to find the items?
        List<Entity> displayableList = new ArrayList<Entity>();
        try {
            recursivelyFindDisplayableChildren( displayableList, alignmentBoard );
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException(ex);
        }

        // Also get the mask items.
        List<Entity> consolidatedLabelsList = new ArrayList<Entity>();
        try {
            recursivelyFindConsolidatedLabels( consolidatedLabelsList, alignmentBoard );
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }

        // Get all the signal filenames.  These are to be masked-by the mask filenames' contents.
        signalFilenames = new ArrayList<String>();
        maskFilenames = new ArrayList<String>();

        EntityFilenameFetcher filenameFetcher = new EntityFilenameFetcher();
        for ( Entity displayable: displayableList ) {
            // Find this displayable entity's file name of interest.
            String typeName = displayable.getEntityType().getName();
            String entityConstantFileType = filenameFetcher.getEntityConstantFileType( typeName );

            String filename = filenameFetcher.fetchFilename( displayable, entityConstantFileType );
            if ( filename != null ) {
                signalFilenames.add( filename );
            }

        }

        for ( Entity consolidatedLabel: consolidatedLabelsList ) {
            String filename = EntityUtils.getFilePath(consolidatedLabel);
            if ( filename != null ) {
                if ( matchesSomeSignal( signalFilenames, filename ) ) {
                    maskFilenames.add(ensureMaskFile(filename));
                }
            }
        }

    }

    public List<String> getSignalFilenames() {
        return signalFilenames;
    }

    public List<String> getMaskFilenames() {
        return maskFilenames;
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
    private String ensureMaskFile(String filePath) {
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
        if (
                EntityConstants.TYPE_CURATED_NEURON.equals(entityTypeName)
                        ||
                        EntityConstants.TYPE_SAMPLE.equals(entityTypeName)
//                        ||
//                        EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityTypeName)
//                        ||
//                EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(entityTypeName)
                ) {
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