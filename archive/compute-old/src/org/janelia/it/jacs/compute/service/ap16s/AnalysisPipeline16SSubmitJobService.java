
package org.janelia.it.jacs.compute.service.ap16s;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.ap16s.AnalysisPipeline16sTask;
import org.janelia.it.jacs.model.user_data.ap16s.AnalysisPipeline16SResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:47:21 PM
 */
public class AnalysisPipeline16SSubmitJobService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "ap16sConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "ap16s";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        AnalysisPipeline16sTask ap16sTask = (AnalysisPipeline16sTask) task;
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        // Create the required FASTA file for the primers
        AnalysisPipeline16sTask tmpTask = (AnalysisPipeline16sTask) task;
        FileWriter fos = null;
        try {
            fos = new FileWriter(new File(resultFileNode.getDirectoryPath() + File.separator +
                    AnalysisPipeline16SResultNode.TAG_PRIMER_FASTA));
            fos.write(">" + tmpTask.getParameter(AnalysisPipeline16sTask.PARAM_primer1Defline) + "\n");
            fos.write(tmpTask.getParameter(AnalysisPipeline16sTask.PARAM_primer1Sequence) + "\n");
            fos.write(">" + tmpTask.getParameter(AnalysisPipeline16sTask.PARAM_primer2Defline) + "\n");
            fos.write(tmpTask.getParameter(AnalysisPipeline16sTask.PARAM_primer2Sequence) + "\n");
        }
        finally {
            if (null != fos) {
                fos.close();
            }
        }

        // Write the quality config file
        String finalQualityConfigPath = resultFileNode.getDirectoryPath() + File.separator + AnalysisPipeline16SResultNode.TAG_QUALITY_CONFIG;
        FileWriter configWriter = new FileWriter(finalQualityConfigPath);
        try {
            configWriter.write("### final cutoffs for layout clustering\n");
            configWriter.write("cutoff=.1,.05,.03,.02,.01,.005\n");
            configWriter.write("minovllen=200\n");
            configWriter.write("maxerr=.15\n\n");
            configWriter.write("# Primer percent identity threshold for trimming\n");
            configWriter.write("primerIdent=75\n");
            configWriter.write("# initial length & QV filtering of frg files\n");
            configWriter.write("read_len_min=" + Integer.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_readLengthMinimum)) + "\n");
            configWriter.write("avg_qv_min=" + Integer.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_minAvgQV)) + "\n");
            configWriter.write("# Maximum number of N's in a good read\n");
            configWriter.write("maxNCnt=" + Integer.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_maxNCount)) + "\n\n");
            configWriter.write("### SSU ref db match thresholds\n");
            configWriter.write("# minium number of identies allowed in alignment\n");
            configWriter.write("minIdent=" + Integer.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_minIdentCount)) + "\n\n");
            configWriter.write("# minimum percent of query covered by alignment\n");
            configWriter.write("minQueryCovg=.3\n\n");
            configWriter.write("# filter if percent identical is less then pIdent &&\n");
            configWriter.write("# less then pLen of query is aligned\n");
            configWriter.write("pIdent=.7\n");
            configWriter.write("pLen=.5\n");
        }
        finally {
            configWriter.close();
        }

        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the 16S pipeline.");
        }
        String outputFilenamePrefix = ap16sTask.getParameter(AnalysisPipeline16sTask.PARAM_filenamePrefix);
        createShellScript(outputFilenamePrefix, writer, finalQualityConfigPath);
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + SubmitDrmaaJobService.NORMAL_QUEUE);
        jt.setNativeSpecification(SubmitDrmaaJobService.NORMAL_QUEUE);
    }

    private void createShellScript(String outputFilenamePrefix, FileWriter writer, String finalQualityConfigPath)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
        // original perl path /usr/local/devel/DAS/software/16sDataAnalysis/bin/
        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");

        String pipelineCmd = basePath + SystemConfigurationProperties.getString("AP16S.PipelineCmd");
        String classifierCmd = basePath + SystemConfigurationProperties.getString("AP16S.ClassifierCmd");
        String massagerCmd = basePath + SystemConfigurationProperties.getString("AP16S.MassagingCmd");
        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
        String tmpDirectoryName = properties.getProperty("Upload.ScratchDir");
        String tmpProjectCode = task.getParameter(Task.PARAM_project).trim();
        AnalysisPipeline16SResultNode tmpResultNode = (AnalysisPipeline16SResultNode) resultFileNode;
        List<String> inputFragmentFiles = Task.listOfStringsFromCsvString(task.getParameter(AnalysisPipeline16sTask.PARAM_fragmentFiles));
        String fullCmd = pipelineCmd + " -i \"" + tmpDirectoryName + File.separator + inputFragmentFiles.get(0) + "\" -d " +
                task.getParameterVO(AnalysisPipeline16sTask.PARAM_subjectDatabase).getStringValue() + " -n " +
                task.getParameter(AnalysisPipeline16sTask.PARAM_filenamePrefix) + " -p " +
                tmpResultNode.getFilePathByTag(AnalysisPipeline16SResultNode.TAG_PRIMER_FASTA) + " -a " +
                task.getParameter(AnalysisPipeline16sTask.PARAM_ampliconSize) +
                " -g " + tmpProjectCode + " -x " + finalQualityConfigPath;

        // Add in some switches, maybe
        String qualLoc = task.getParameter(AnalysisPipeline16sTask.PARAM_qualFile);
        if (null != qualLoc && !"".equals(qualLoc)) {
            fullCmd += (" -q \"" + tmpDirectoryName + File.separator + task.getParameter(AnalysisPipeline16sTask.PARAM_qualFile) + "\"");
        }
        if (Boolean.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_skipClustalW))) {
            fullCmd += " -s";
        }
        if (Boolean.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_iterateCdHitESTClustering))) {
            fullCmd += " -c";
        }

        StringBuffer script = new StringBuffer();
        script.append("set -o errexit\n");
        script.append("cd ").append(resultFileNode.getDirectoryPath()).append("\n");
        script.append(fullCmd).append("\n");
        // The classifier script only works for 16S data - maybe this logic would be better off somewhere else
        if (Boolean.valueOf(task.getParameter(AnalysisPipeline16sTask.PARAM_useMsuRdpClassifier)) &&
                task.getParameter(AnalysisPipeline16sTask.PARAM_subjectDatabase).toLowerCase().endsWith("16s")) {
            script.append(classifierCmd).append(" ").append(resultFileNode.getDirectoryPath()).append(File.separator).append(outputFilenamePrefix).append("_intChimChe60.fa").append("\n");
            script.append(massagerCmd).append(" ").append(resultFileNode.getDirectoryPath()).append(File.separator).append(outputFilenamePrefix).append("_intChimChe60.fa.assignment_detail.txt").append("\n");
        }
        writer.write(script.toString());
    }

}