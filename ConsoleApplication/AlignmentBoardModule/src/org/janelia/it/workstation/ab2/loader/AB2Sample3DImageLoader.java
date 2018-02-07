package org.janelia.it.workstation.ab2.loader;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.ab2.api.AB2RestClient;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.browser.model.SampleImage;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.interfaces.HasImageStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Sample3DImageLoader extends SimpleWorker {

    private static final Logger log = LoggerFactory.getLogger(AB2Sample3DImageLoader.class);

    private SampleImage sampleImage;
    private HasImageStack imageStack;
    private byte[] data = null;

    public AB2Sample3DImageLoader(SampleImage sampleImage) {
        this.sampleImage = sampleImage;
        if (sampleImage.getResult() instanceof HasImageStack) {
            imageStack = (HasImageStack)sampleImage.getResult();
        }
        else {
            throw new IllegalArgumentException("Sample image has no image stack");
        }
    }

    @Override
    protected void doStuff() throws Exception {
        log.info("Starting AB2Sample3DImageLoader");
        log.info("  Sample: " + sampleImage.getSample());
        
        String filepath = imageStack.getVisuallyLosslessStack();
        if (filepath==null) throw new IllegalArgumentException("Sample result has no visually lossless stack");
        
        log.info("  Filepath: {}", filepath);
        AB2RestClient ab2RestClient = new AB2RestClient();
        data = ab2RestClient.getSample3DImageXYZRGBA8(filepath, imageStack);
        log.info("Finished AB2Sample3DImageLoader");
    }
    
    public HasImageStack getImageStack() {
        return imageStack;
    }

    @Override
    protected void hadSuccess() {
        AB2Controller.getController().processEvent(new AB2Sample3DImageLoadedEvent(data));
        this.data = null;
    }

    @Override
    protected void hadError(Throwable error) {
        data = null;
        FrameworkImplProvider.handleException(error);
    }

//    private HasImageStack getLatestAlignedSampleImage(Sample sample) throws IOException {
//
//        log.info("Sample id=" + sample.getId());
//        List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
//        if (objectiveSamples == null || objectiveSamples.size() == 0) {
//            log.info("No ObjectSamples found");
//        }
//        else {
//            for (ObjectiveSample objectiveSample : objectiveSamples) {
//                SamplePipelineRun samplePipelineRun = objectiveSample.getLatestRun();
//                if (samplePipelineRun == null) {
//                    log.info("No SamplePipelineRuns found");
//                }
//                else {
//                    HasImageStack stack = samplePipelineRun.getLatestAlignmentResult();
//                    if (stack==null) {
//                        stack = samplePipelineRun.getLatestProcessingResult();
//                    }
//                    if (stack != null) {
//                        String filepath = stack.getVisuallyLosslessStack();
//                        if (filepath != null) {
//                            return stack;
//                        }
//                    }
//                }
//
//            }
//        }
//        
//        return null;
//    }
}
