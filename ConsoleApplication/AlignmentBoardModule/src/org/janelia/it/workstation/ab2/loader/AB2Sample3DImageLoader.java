package org.janelia.it.workstation.ab2.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.janelia.it.workstation.ab2.api.AB2RestClient;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.interfaces.HasImageStack;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Sample3DImageLoader extends SimpleWorker {

    private static final Logger log = LoggerFactory.getLogger(AB2Sample3DImageLoader.class);

    protected Sample sample;
    protected HasImageStack imageStack;
    byte[] data=null;

    public AB2Sample3DImageLoader(Sample sample) {
        this.sample=sample;
    }

    @Override
    protected void doStuff() throws Exception {
        log.info("Starting AB2Sample3DImageLoader");
        log.info("Sample id="+sample.getId());
        
        HasImageStack imageStack = getLatestAlignedSampleImage(sample);
        String filepath = imageStack.getVisuallyLosslessStack();
        
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
        this.data=null;
    }

    @Override
    protected void hadError(Throwable error) {
        data=null;
        log.error("Error during load: "+error.getMessage());
        error.printStackTrace();
    }

    private HasImageStack getLatestAlignedSampleImage(Sample sample) throws IOException {

        log.info("Sample id=" + sample.getId());
        List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
        if (objectiveSamples == null || objectiveSamples.size() == 0) {
            log.info("No ObjectSamples found");
        }
        else {
            for (ObjectiveSample objectiveSample : objectiveSamples) {
                SamplePipelineRun samplePipelineRun = objectiveSample.getLatestRun();
                if (samplePipelineRun == null) {
                    log.info("No SamplePipelineRuns found");
                }
                else {
                    HasImageStack stack = samplePipelineRun.getLatestAlignmentResult();
                    if (stack==null) {
                        stack = samplePipelineRun.getLatestProcessingResult();
                    }
                    if (stack != null) {
                        String filepath = stack.getVisuallyLosslessStack();
                        if (filepath != null) {
                            return stack;
                        }
                    }
                }

            }
        }
        
        return null;
    }
}
