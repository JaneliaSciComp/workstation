package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.Date;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.util.ActivityLogHelper;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.joda.time.DateTime;

/**
 * Sets the Status attribute of the Sample and updates the Completion Date if necessary. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSampleStatusService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {
        SampleHelperNG sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        String status = data.getRequiredItemAsString("STATUS");
        sampleHelper.setProcess(data.getRequiredItemAsString("PIPELINE_NAME"));
        String currentStatus=(sample.getStatus()==null)?"New":sample.getStatus();
        sampleHelper.logStatusTransition(sample.getId(), PipelineStatus.valueOf(currentStatus), PipelineStatus.valueOf(status));

	    Date now = new Date();
	    DateTime sampleCompletion = new DateTime(sample.getCompletionDate());
	    
        if (PipelineStatus.Complete.toString().equals(status)) {
            this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
            // One objective has completed, but are all the objectives completed?
            
            boolean complete = true;
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {

                DateTime firstCompletion = null;
                for(SampleTile tile : objectiveSample.getTiles()) {
                    for(LSMImage lsm : domainDao.getDomainObjectsAs(tile.getLsmReferences(), LSMImage.class)) {
                        if (objectiveSample.equals(this.objectiveSample) && lsm.getCompletionDate()==null) {
                            // This is the objective we are completing right now, and one of its LSM has no completion date, so let's fill it in.
                            lsm.setCompletionDate(now);
                            contextLogger.info("Setting completion date on LSM "+lsm.getName()+" (id="+lsm.getId()+")");
                            sampleHelper.saveLsm(lsm);
                        }
                        if (lsm.getCompletionDate()!=null) {
                            DateTime lsmCompletionDate = new DateTime(lsm.getCompletionDate());
                            if (firstCompletion==null || lsmCompletionDate.isBefore(firstCompletion)) {
                                firstCompletion = lsmCompletionDate;
                            }
                        }
                    }
                }
                
                if (firstCompletion!=null && firstCompletion.isAfter(sampleCompletion)) {
                    sampleCompletion = firstCompletion;
                }
                
                if (objectiveSample.getLatestSuccessfulRun()==null) {
                    complete = false;
                }   
            }
            
            if (complete) {
                contextLogger.info("Set status to "+status+" on sample "+sample.getName()+" (id="+sample.getId()+")");

                sample.setStatus(PipelineStatus.Complete.toString());
            }
            else {
                contextLogger.info("Cannot set status to "+status+", because entire sample is not complete: "+sample.getName()+" (id="+sample.getId()+")");
            }
            
            sample.setCompletionDate(sampleCompletion.toDate());

            ActivityLogHelper activityLogHelper = ActivityLogHelper.getInstance();
            activityLogHelper.logSampleProcessingCompletion(sample.getOwnerKey(), sample.getName(), sample.getId(), status, "Success");
        }
        else {
            contextLogger.info("Set status to "+status+" on sample "+sample.getName()+" (id="+sample.getId()+")");
            sample.setStatus(status);
        }

        sampleHelper.saveSample(sample);
    }

}
