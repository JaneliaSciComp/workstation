package org.janelia.it.jacs.compute.service.geci;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.geci.NeuronalAssayAnalysisTask;
import org.janelia.it.jacs.model.user_data.geci.NeuronalAssayAnalysisResultNode;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class NeuronalAssayAnalysisSubmitJobService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "neuronalAssayAnalysisConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "neuronalAssayAnalysis";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        //ProfileComparisonTask profileComparisonTask = (ProfileComparisonTask) task;
        NeuronalAssayAnalysisResultNode tmpResultNode = (NeuronalAssayAnalysisResultNode) resultFileNode;

        // Creating the default config file for the Drmaa Template
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess) {
            logger.error("Unable to create configFile for NeuronalAssayAnalysis process.");
        }

        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
        String pipelineCmd = basePath + SystemConfigurationProperties.getString("NeuronalAssayAnalysis.Cmd");
        String tmpDirectoryName = SystemConfigurationProperties.getString("Upload.ScratchDir");
        List<String> inputFiles = Task.listOfStringsFromCsvString(task.getParameter(NeuronalAssayAnalysisTask.PARAM_inputFile));

        // Takes a list of files, smart enough to figure out the file type based on extension
        String fullCmd = pipelineCmd + " -o " + tmpResultNode.getDirectoryPath();
        for (String inputFile : inputFiles) {
            fullCmd += " -f " + tmpDirectoryName + File.separator + inputFile;
        }
        fullCmd = "export PATH=$PATH:" + basePath + ";" + fullCmd;
        StringBuilder script = new StringBuilder();
        script.append(fullCmd).append("\n");
        writer.write(script.toString());
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + SubmitDrmaaJobService.NORMAL_QUEUE);
        jt.setNativeSpecification(SubmitDrmaaJobService.NORMAL_QUEUE);
    }

}