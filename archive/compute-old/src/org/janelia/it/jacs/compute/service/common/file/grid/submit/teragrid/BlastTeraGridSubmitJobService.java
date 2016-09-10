
package src.org.janelia.it.jacs.compute.service.common.file.grid.submit.teragrid;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.blast.submit.BlastCommand;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;

/**
 * This service submit a job to the Grid.  It's entirely extracted from work done by Sean Murphy
 * and Todd Safford.
 *
 * @author Sean Murphy
 * @author Todd Safford
 */
public class BlastTeraGridSubmitJobService extends SubmitTeraGridJobService implements IService {

    private int numberOfHitsToKeep;

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "blastTG";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        BlastTask blastTask = (BlastTask) task;
        BlastCommand blastCommand = new BlastCommand();
        String tempBlastOutputFileName = "blastOutput";
        String blastCommandString = blastCommand.getCommandString(blastTask, new File("."), 1, tempBlastOutputFileName);
        String[] blastCommandArr = blastCommandString.split("\\s+");
        if (blastCommandArr == null || blastCommandArr.length < 2)
            throw new IllegalArgumentException("Could not parse blast command: " + blastCommandString);
        StringBuffer genericBlastBuffer = new StringBuffer();
        for (int j = 0; j < blastCommandArr.length; j++) {
            if (j > 0 && blastCommandArr[j - 1].equals("-o")) {
                genericBlastBuffer.append(" ").append(tempBlastOutputFileName);
            }
            else if (j > 0 && blastCommandArr[j - 1].equals("-d")) {
                genericBlastBuffer.append(" $BLASTDB");
            }
//            else if (j > 0 && blastCommandArr[j - 1].equals("-i")  && queryFilesShouldBeLocalToGridNode) {
//                genericBlastBuffer.append(" ").append(tmpQueryFile);
//            }
            else if (j > 0 && (blastCommandArr[j - 1].equals("-b") || blastCommandArr[j - 1].equals("-v"))) {
                numberOfHitsToKeep = Integer.parseInt(blastCommandArr[j]);
                if (numberOfHitsToKeep != BlastServiceUtil.getNumberOfHitsToKeepFromBlastTask(blastTask)) {
                    throw new ParameterException("Discrepancy between bestHitsToKeep from BlastTask and BlastCommand for blastTask=" + blastTask.getObjectId());
                }
                genericBlastBuffer.append(" ").append(numberOfHitsToKeep);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting numberOfHitsToKeep to " + numberOfHitsToKeep + " for -b and -v");
                }
            }
            else {
                genericBlastBuffer.append((" " + blastCommandArr[j]));
            }
        }
        processData.putItem(BlastProcessDataConstants.BLAST_NUMBER_OF_HITS_TO_KEEP, numberOfHitsToKeep);

        // Format the executable script
        // Note - the persist which runs blast will only parse its own xml output, which is why -f and -l are the same index, below
        StringBuffer script = new StringBuffer();
        script.append("#!/bin/bash\n");
        script.append("#$ -A ").append(SystemConfigurationProperties.getString("TeraGrid.GrantNumber")).append("\n");
        script.append("#$ -V                           # Inherit the submission environment\n");
        script.append("#$ -cwd                         # Start job in  submission directory\n");
        script.append("#$ -N ").append(task.getJobName()).append("                # Job Name\n");
        script.append("##$ -j y                        # combine stderr & stdout into stdout\n");
        script.append("#$ -e $JOB_NAME.$JOB_ID.err     # Name of stderr file\n");
        script.append("#$ -o $JOB_NAME.$JOB_ID.out     # Name of the output file (eg. myMPI.oJobID)\n");
        script.append("#$ -pe 14way 256                        # Requests 14 out of 16 cores/node, 128 cores / 8 nodes total\n");
        script.append("#$ -q normal                    # Queue name\n");
        script.append("#$ -l h_rt=6:00:00             # Run time (hh:mm:ss) - 1.5 hours\n");
        script.append("## -M ").append(task.getOwner()).append("@jcvi.org         # Email notification address (UNCOMMENT)\n");
        script.append("## -m be                        # Email at Begin/End of job  (UNCOMMENT)\n");
        script.append("\n");
        script.append("\n");
        script.append("\n");
        script.append("date\n");
        script.append("hostname\n");
        script.append("module list\n");
        script.append("ibrun mpiblast ").append(genericBlastBuffer.toString()).
                append(" --replica-group-size --predistribute-db --removedb --output-search-stats \n");
        // Tinos mpiBlast flags below
        // "-v 10 -b 10 -X 15 -e 1e-5 -M BLOSUM62 -J F -K 10 -f 11 -Z 25.0 -W 3 -U F -I F -E -1 -y 7.0 -G -1 -A 40 "+
        //  "-Y 0.0 -F T -g T -z 1702432768 "+

        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    public void cleanup() {

    }
    
    @Override
    public void postProcess() throws MissingDataException {

    }

    @Override
    public void handleErrors() throws Exception {

    }

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            submitAsynchronousJob();
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }
}