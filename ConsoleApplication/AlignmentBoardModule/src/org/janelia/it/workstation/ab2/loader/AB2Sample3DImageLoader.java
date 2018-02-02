package org.janelia.it.workstation.ab2.loader;

import java.util.List;

import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.it.workstation.ab2.api.AB2RestClient;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Sample3DImageLoader extends SimpleWorker {

    private static final Logger logger = LoggerFactory.getLogger(AB2Sample3DImageLoader.class);

    protected Sample sample;
    byte[] data=null;

    public AB2Sample3DImageLoader(Sample sample) {
        this.sample=sample;
    }

    @Override
    protected void doStuff() throws Exception {
        logger.info("Sample id="+sample.getId());
        List<ObjectiveSample> objectiveSamples=sample.getObjectiveSamples();
        if (objectiveSamples==null || objectiveSamples.size()==0) {
            logger.info("No ObjectSamples found");
        } else {
            for (ObjectiveSample objectiveSample : objectiveSamples) {
                SamplePipelineRun samplePipelineRun=objectiveSample.getLatestRun();
                if (samplePipelineRun==null) {
                    logger.info("No SamplePipelineRuns found");
                } else {
                    PipelineResult pipelineResult = samplePipelineRun.getLatestResult();
                    if (pipelineResult == null) {
                        logger.info("No PipelineResults found");
                    }
                    else {
                        String filepath = DomainUtils.getDefault3dImageFilePath(pipelineResult);
                        if (filepath == null) {
                            logger.info("Filepath for 3dImage is null");
                        }
                        else {
                            logger.info("3D filepath=" + filepath);
                        }
                    }
                }
            }
        }
        logger.info("Starting AB2RestClient");
        AB2RestClient ab2RestClient=new AB2RestClient();
        data = ab2RestClient.getSampleDefault3DImageXYZRGBA8(sample.getId());
        logger.info("Finished AB2RestClient");
    }

    @Override
    protected void hadSuccess() {
        AB2Controller.getController().processEvent(new AB2Sample3DImageLoadedEvent(data));
        this.data=null;
    }

    @Override
    protected void hadError(Throwable error) {
        data=null;
        logger.error("Error during load: "+error.getMessage());
        error.printStackTrace();
    }
}
