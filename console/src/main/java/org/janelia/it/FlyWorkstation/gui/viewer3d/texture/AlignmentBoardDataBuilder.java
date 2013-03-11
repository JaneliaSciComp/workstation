package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.SampleData;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.Serializable;
import java.util.*;
import java.util.List;

/**
 * This is where the data are pulled together for the alignment board viewer.
 */
public class AlignmentBoardDataBuilder implements Serializable {
    private static final String COMPARTMENT_MASK_FILE = "/groups/scicomp/jacsData/MaskResources/Compartment/maskIndex.v3dpbd";
    private static final String COMPARTMENT_MASK_MAPPING_FILE = "/groups/scicomp/jacsData/MaskResources/Compartment/maskNameIndex.txt";
    //"/Users/fosterl/Documents/alignment_board/samples/174213816581829437/ConsolidatedSignal2_25.mp4"; //
    private static final String COMPARTMENT_MASK_ONLY_SIGNAL = "/groups/scicomp/jacsData/MaskResources/Compartment/maskRGB.v3dpbd";
    private static final String COMPARTMENT_ENTITY_NAME = "Compartment";
    private static final int TARGET_MVOXELS = 25;
    private static final byte REFERENCE_INTENSITY = 7;

    private Logger logger = LoggerFactory.getLogger( AlignmentBoardDataBuilder.class );

    private List<SampleData> sampleDataList;

    public AlignmentBoardDataBuilder() {
    }

    /**
     * Plug in the alignment board entity, from which all returned information is derived.
     *
     * @param abContext to retrieve all relevant data.
     * @return reference to this object to support pipelining.
     */
    public AlignmentBoardDataBuilder setAlignmentBoardContext( AlignmentBoardContext abContext ) {
        clear();

        logger.info("Starting 'setAlignmentBoardContext'");
        sampleDataList = new ArrayList<SampleData>();

        // Build out the bean list, from info seen.
        for ( AlignedItem alignedItem : abContext.getAlignedItems() ) {

            EntityWrapper itemEntity = alignedItem.getItemWrapper();
            if ( itemEntity instanceof Sample ) {
                Sample sample = (Sample)itemEntity;
                RenderableBean sampleBean = new RenderableBean();
                sampleBean.setLabelFileNum( 0 );
                sampleBean.setTranslatedNum( 0 );
                sampleBean.setRenderableEntity(sample.getInternalEntity());
                sampleBean.setRgb(
                        new byte[] {
                                (byte)255, (byte) 255, (byte) 0, RenderMappingI.NON_RENDERING
                        }
                );

                SampleData sampleData = new SampleData();
                sampleData.setSample( sampleBean );

                String labelFile = null;
                MaskedVolume vol = sample.getMaskedVolume();
                String referenceFile = null;
                if ( vol != null ) {
                    logger.debug("    subsampled volumes:");
                    for ( MaskedVolume.Size size : MaskedVolume.Size.values() ) {
                        if ( size.getMegaVoxels() == TARGET_MVOXELS ) {
                            labelFile = vol.getFastVolumePath(
                                    MaskedVolume.ArtifactType.ConsolidatedLabel, size, MaskedVolume.Channels.All, true
                            );

                            // Fetching the lossy version, an MP4 file.  The lossless version is a v3dpbd, but its
                            // rendering in grayscale is very poor.
                            referenceFile = vol.getFastVolumePath(
                                    MaskedVolume.ArtifactType.Reference, size, MaskedVolume.Channels.All, false
                            );
                            break;
                        }
                    }

                    // TEMP.
                    //referenceFile = vol.getReferenceVolumePath();

                }

                sampleData.setLabelFile( labelFile );
                String signalFile = sample.getFast3dImageFilepath();

                if ( signalFile == null ) {
                    logger.error( "No signal file found for {}/{}.", sample.getName(), sample.getId() );
                }
                else {
                    logger.info( "Found signal file {} for {}.", signalFile, sample.getName() );
                }
                sampleData.setSignalFile( signalFile );

                // Reference should be treated as a separate overlaid volume.  Another "signal".
                if ( referenceFile != null ) {
                    logger.info( "Reference file is {}.", referenceFile );
                    RenderableBean referenceBean = new RenderableBean();
                    referenceBean.setTranslatedNum( 0 );
                    referenceBean.setRenderableEntity( sample.getInternalEntity() );
                    referenceBean.setRgb(
                            new byte[] {
                                    REFERENCE_INTENSITY, REFERENCE_INTENSITY, REFERENCE_INTENSITY, RenderMappingI.NO_SHADER_USE
                            }
                    );

                    sampleData.setReference( referenceBean );
                    sampleData.setReferenceFile( referenceFile );
                }

                Collection<AlignedItem> childItems = alignedItem.getAlignedItems();
                int translatedNum = 1;
                if ( childItems != null ) {
                    for ( AlignedItem item: childItems ) {
                        if ( item.getItemWrapper() instanceof Neuron ) {
                            RenderableBean neuronBean = createRenderableBean(sampleBean, translatedNum, item);
                            sampleData.addNeuronFragment( neuronBean );
                            translatedNum ++;
                        }
                    }
                }

                sampleDataList.add( sampleData );

            }
            else {
                logger.error("Cannot handle entites of type: " + itemEntity.getType());
            }
        }

        logger.info("Ending 'setAlignmentBoardContext'");
        return this;
    }

    /**
     * Returns all the samples, with all their affecting data, like compartments, reference, neurons fragments, etc.
     *
     * @return everything for which a volume could be created.
     */
    public Collection<SampleData> getSamples() {
        return sampleDataList;
    }

    //--------------------------------------HELPERS
    private void clear() {
        sampleDataList = null;
    }

    private RenderableBean createRenderableBean(RenderableBean sampleBean, int translatedNum, AlignedItem item ) {
        Neuron neuron = (Neuron)item.getItemWrapper();
        logger.debug(
                "Creating Renderable Bean for: " + neuron.getName() + " original index=" + neuron.getMaskIndex() +
                " new index=" + translatedNum
        );

        RenderableBean neuronBean = new RenderableBean();
        neuronBean.setLabelFileNum( neuron.getMaskIndex() + 1 ); // From 0-based to 1-based.
        neuronBean.setTranslatedNum(translatedNum);
        //neuronBean.setSignalFile(sampleBean.getSignalFile());
        //neuronBean.setLabelFile(sampleBean.getLabelFile());
        neuronBean.setRenderableEntity(neuron.getInternalEntity());

        // See to the appearance.
        Color neuronColor = item.getColor();
        if ( neuronColor == null ) {
            // If visible, leave RGB as null, and allow downstream automated-color to take place.
            // Otherwise, if not visible, ensure that the bean has a non-render setting.
            if ( ! item.isVisible() ) {
                byte[] rgb = new byte[ 4 ];
                rgb[ 0 ] = 0;
                rgb[ 1 ] = 0;
                rgb[ 2 ] = 0;
                rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                neuronBean.setRgb( rgb );
            }
        }
        else {
            logger.info( "Neuron color is {} for {}.", neuronColor, item.getItemWrapper().getName() );
            // A Neuron Color was set, but the neuron could still be "turned off" for render.
            byte[] rgb = new byte[ 4 ];
            rgb[ 0 ] = (byte)neuronColor.getRed();
            rgb[ 1 ] = (byte)neuronColor.getGreen();
            rgb[ 2 ] = (byte)neuronColor.getBlue();
            rgb[ 3 ] = item.isVisible()    ?    RenderMappingI.FRAGMENT_RENDERING : RenderMappingI.NON_RENDERING;
            neuronBean.setRgb( rgb );
        }

        return neuronBean;
    }

    // Eventually, will get compartment data.  When this is available, below will be re-examined, re-implemented
    // in terms of the new SampleData class.
//    private void applyCompartmentMask( List<Entity> displayableList ) {
//        if ( displayableList.size() == 0 ) {
//            return;
//        }
//
//        // First, figure out if we have anything relevant to do here.
//        List<String> compartmentNames = new ArrayList<String>();
//        for ( Entity entity: displayableList ) {
//            try {
//                Entity definingAncestor = ModelMgr.getModelMgr().getAncestorWithType( entity, EntityConstants.TYPE_FOLDER );
//                if ( definingAncestor.getName().equals( COMPARTMENT_ENTITY_NAME ) ) {
//                    // Know: we have a compartment.
//                    String compartmentName = entity.getName();
//                    compartmentNames.add( compartmentName );
//                }
//            } catch ( Exception ex ) {
//                SessionMgr.getSessionMgr().handleException( ex );
//            }
//
//        }
//
//        if ( compartmentNames.size() > 0 ) {
//            CacheFileResolver cacheFileResolver = new CacheFileResolver();
//            String maskIndex =
//                    cacheFileResolver.getResolvedFilename(
//                            COMPARTMENT_MASK_FILE
//                    );
//
//            // Tack along to the last existing bean, and get its signal file name.
//            String compartmentSignalFile = null;
//            for ( RenderableBean bean: renderableBeanList ) {
//                compartmentSignalFile = bean.getSignalFile();
//            }
//            boolean compartmentsOnly = false;
//            if ( compartmentSignalFile == null ) {
//                // Need to force a signal, compatible with compartments.
//                compartmentSignalFile = COMPARTMENT_MASK_ONLY_SIGNAL;
//                compartmentsOnly = true;
//            }
//
//            // Look for highest-numbered translation.
//            int highestTranslatedLabel = -1;
//            for ( RenderableBean bean: renderableBeanList ) {
//                if ( bean.getTranslatedNum() >= highestTranslatedLabel ) {
//                    highestTranslatedLabel = bean.getTranslatedNum() + 1;
//                }
//            }
//            if ( highestTranslatedLabel == -1 ) {
//                highestTranslatedLabel = 1;
//            }
//
//            // Pull in info from the compartments definition text file.
//            File maskNameIndex = new File(
//                cacheFileResolver.getResolvedFilename(
//                    COMPARTMENT_MASK_MAPPING_FILE
//                )
//            );
//
//            Map<String, Integer> nameVsNum = new HashMap<String,Integer>();
//            try {
//                BufferedReader rdr = new BufferedReader( new FileReader( maskNameIndex ) );
//                String nextLine = null;
//
//                // Prototype input line:
//                // 56 WED_L "Description" ( 53 45 215 )
//                while ( null != ( nextLine = rdr.readLine() ) ) {
//                    String[] fields = nextLine.trim().split( " " );
//                    if ( fields.length >= 8 ) {
//                        Integer labelNum = Integer.parseInt( fields[ 0 ] );
//                        String compartmentName = fields[ 1 ];
//                        nameVsNum.put( compartmentName, labelNum );
//                    }
//                }
//                rdr.close();
//            } catch ( Exception ex ) {
//                SessionMgr.getSessionMgr().handleException( ex );
//            }
//
//            // Now, add all the renderableBeanList.
//            for ( String compartmentName: compartmentNames ) {
//                Integer labelNum = nameVsNum.get( compartmentName );
//                if ( labelNum != null ) {
//                    RenderableBean maskIndexBean = new RenderableBean();
//                    maskIndexBean.setLabelFile( maskIndex );
//                    maskIndexBean.setSignalFile( compartmentSignalFile );
//                    maskIndexBean.setLabelFileNum( labelNum );
//
//                    maskIndexBean.setTranslatedNum( highestTranslatedLabel ++ );
//                    byte renderMethod = RenderMappingI.COMPARTMENT_RENDERING;
//                    if ( compartmentsOnly ) {
//                        renderMethod = RenderMappingI.SOLID_COMPARTMENT_RENDERING;
//                    }
//                    maskIndexBean.setRgb( new byte[] { 20, 20, 20, renderMethod } );
//
//                    renderableBeanList.add(maskIndexBean);
//                }
//                else {
//                    logger.warn("Encountered compartment {} with no known label number.", compartmentName);
//                }
//            }
//        }
//
//    }

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

}