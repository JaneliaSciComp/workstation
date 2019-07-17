package org.janelia.workstation.browser.gui.support;

import java.util.ArrayList;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePostProcessingResult;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.model.SampleDecorator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with Sample objects in the GUI.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleUIUtils {

    private final static Logger log = LoggerFactory.getLogger(SampleUIUtils.class);

    /**
     * Returns a 3d result for the currently selected item.
     * @param viewerContext
     * @return
     */
    public static HasFiles getSingle3dResult(ViewerContext viewerContext) {

        ImageModel imageModel = viewerContext.getImageModel();
        if (imageModel instanceof DomainObjectImageModel) {
            DomainObjectImageModel doim = (DomainObjectImageModel) imageModel;
            Object lastSelectedObject = viewerContext.getLastSelectedObject();
            if (lastSelectedObject instanceof DomainObject) {
                DomainObject domainObject = (DomainObject) lastSelectedObject;

                HasFiles result = null;
                if (domainObject instanceof Sample) {
                    ArtifactDescriptor rd = doim.getArtifactDescriptor();
                    if (rd != null) {
                        log.trace("getSingle3dResult({}, descriptor={})", domainObject, rd);
                        if (rd instanceof ResultArtifactDescriptor) {
                            ResultArtifactDescriptor rad = (ResultArtifactDescriptor) rd;
                            // For post-processing results, open the corresponding processing result,
                            if (SamplePostProcessingResult.class.getName().equals(rad.getResultClass())) {
                                rd = new ResultArtifactDescriptor(rd.getObjective(), rd.getArea(),
                                        SampleProcessingResult.class.getName(), null, false);
                                log.trace("getSingle3dResult - using descriptor: {}", rd);
                            }
                        }
                        Sample sample = (Sample) domainObject;
                        result = DescriptorUtils.getResult(sample, rd);
                    }
                }
                else if (domainObject instanceof HasFiles) {
                    log.trace("getSingle3dResult({})", domainObject);
                    result = (HasFiles) domainObject;
                }
                if (result!=null) {
                    log.trace("getSingle3dResult - Found result {}", result);
                }
                return result;
            }
            else {
                log.trace("getSingle3dResult - no domain object selected");
            }
        }

        return null;
    }


    /**
     * Return a list of image decorators for the given domain object.
     * @param imageObject
     * @return
     */
    public static List<Decorator> getDecorators(DomainObject imageObject) {
        List<Decorator> decorators = new ArrayList<>();
        if (imageObject instanceof Sample) {
            Sample sample = (Sample)imageObject;
            if (sample.isSamplePurged()) {
                decorators.add(SampleDecorator.PURGED);
            }
            if (!sample.isSampleSageSynced()) {
                decorators.add(SampleDecorator.DESYNC);
            }
        }
        else if (imageObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)imageObject;
            if (!lsm.isLSMSageSynced()) {
                decorators.add(SampleDecorator.DESYNC);
            }
        }

        return decorators;
    }

    /**
     * Return a list of image decorators for the given pipeline result.
     * @param result
     * @return
     */
    public static List<Decorator> getDecorators(PipelineResult result) {
        List<Decorator> decorators = new ArrayList<>();
        if (result.getPurged()!=null && result.getPurged()) {
            decorators.add(SampleDecorator.PURGED);
        }
        return decorators;
    }
}
