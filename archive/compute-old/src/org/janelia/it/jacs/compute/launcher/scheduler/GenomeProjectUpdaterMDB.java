
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 3AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 3 * * ?")
})
@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class GenomeProjectUpdaterMDB implements Job {
    private Logger log = Logger.getLogger(GenomeProjectUpdaterMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //ComputeServer.UseRecruitmentScheduler=false
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseRecruitmentScheduler")) {
            return;
        }
//        try {
//            String messageBody = "";
//            // todo Need to record which specific genome projects were added/modified
//            // Update the Bacterial genome projects
//            GenomeProjectUpdateTask gpUpdateBacterialTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL,
//                    GenomeProjectUpdateTask.COMPLETE_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
//            gpUpdateBacterialTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateBacterialTask);
//            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateBacterialTask.getObjectId());
//            String status = waitAndVerifyCompletion(gpUpdateBacterialTask.getObjectId());
//            if (!"completed".equals(status)) {
//                log.error("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
//                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL + "Genome Projects\n";
//            }
//            else {
//                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL + " Genome Projects were updated successfully.\n";
//            }
//
//            // Update the Viral genome projects
//            GenomeProjectUpdateTask gpUpdateViralTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_VIRAL,
//                    GenomeProjectUpdateTask.COMPLETE_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
//            gpUpdateViralTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateViralTask);
//            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateViralTask.getObjectId());
//            status = waitAndVerifyCompletion(gpUpdateViralTask.getObjectId());
//            if (!"completed".equals(status)) {
//                log.error("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
//                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_VIRAL + "Genome Projects\n";
//            }
//            else {
//                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_VIRAL + " Genome Projects were updated successfully.\n";
//            }
//
//            // Update the Draft bacterial genome projects
//            GenomeProjectUpdateTask gpUpdateDraftTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL,
//                    GenomeProjectUpdateTask.DRAFT_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
//            gpUpdateDraftTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateDraftTask);
//            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateDraftTask.getObjectId());
//            status = waitAndVerifyCompletion(gpUpdateDraftTask.getObjectId());
//            if (!"completed".equals(status)) {
//                log.error("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
//                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL + "Genome Projects\n";
//            }
//            else {
//                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL + " Genome Projects were updated successfully.\n";
//            }
//
//            // Update the Genome Project lookup file
//            RecruitmentDataHelper.buildGenbankFileList();
//
//            // todo Diff the file and report the differences.
//
//            // Notify interested parties that the update is complete.
//            // Might want to get the email list from a Notification table (Notification flag, user email) people can register
//            MailHelper helper = new MailHelper();
//            helper.sendEmail("saffordt@janelia.hhmi.org", "saffordt@janelia.hhmi.org", "Genome Project Updater Complete", messageBody);
//        }
//        catch (Exception e) {
//            log.error("There was a problem updating the genome project data.");
//        }
        log.debug("Genome project updating complete.");
    }

    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = computeBean.getTaskStatus(taskId);
        }
        log.debug(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

}