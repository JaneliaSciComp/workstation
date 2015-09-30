
package org.janelia.it.jacs.compute.service.barcodeDesign;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.barcodeDesigner.BarcodeDesignerTask;
import org.janelia.it.jacs.model.user_data.barcodeDesign.BarcodeDesignResultNode;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:47:21 PM
 */
public class BarcodeDesignSubmitJobService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "barcodeDesignConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "barcodeDesign";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        BarcodeDesignerTask barcodeDesignerTask = (BarcodeDesignerTask) task;
        BarcodeDesignResultNode tmpResultNode = (BarcodeDesignResultNode) resultFileNode;
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the 16S pipeline.");
        }

        // Create config file
        File tmpConfigFile = new File(tmpResultNode.getDirectoryPath() + File.separator + "454Barcode.config");
        FileWriter configWriter = new FileWriter(tmpConfigFile);
        try {
            configWriter.write("[Barcode_Rules]\n");
            configWriter.write("barcodeLength=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_barcodeLength) + "\n");
            configWriter.write("fivePrimeClamp=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_fivePrimeClamp) + "\n");
            configWriter.write("maxFlows=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_maxFlows) + "\n");
            configWriter.write("flowSequence=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_flowSequence) + "\n");
            configWriter.write("keyChar=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_keyChar) + "\n");
            configWriter.write("minEditDistance=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_minEditDistance) + "\n\n");
            configWriter.write("[Barcode_wPrimer_Rules]\n");
            configWriter.write("palindromeBin=" + SystemConfigurationProperties.getString("BarcodeDesign.PalindromeBin") + "\n");
            configWriter.write("minPalindromeHBonds=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_minPalindromeHBonds) + "\n");
            configWriter.write("maxPalindromeMateDistance=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_maxPalindromeMateDistance) + "\n");
            configWriter.write("palindromeTempDirectory=" + SystemConfigurationProperties.getString("BarcodeDesign.PalindromeTempDir") + "\n");
            configWriter.write("intDimerMaxScore=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_intDimerMaxScore) + "\n");
            configWriter.write("endDimerMaxScore=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_endDimerMaxScore) + "\n\n");
            configWriter.write("[Synthesis_Requirements]\n");
            configWriter.write("forwardPrimerAdapterSequence=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_forwardPrimerAdapterSequence) + "\n");
            configWriter.write("reversePrimerAdapterSequence=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_reversePrimerAdapterSequence) + "\n");
            configWriter.write("attachBarcodeToForwardPrimer=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_attachBarcodeToForwardPrimer) + "\n");
            configWriter.write("attachBarcodeToReversePrimer=" + barcodeDesignerTask.getParameter(BarcodeDesignerTask.PARAM_attachBarcodeToReversePrimer) + "\n");
        }
        finally {
            configWriter.flush();
            configWriter.close();
        }

        String perlPath = SystemConfigurationProperties.getString("Perl.Path");
        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
        String pipelineCmd = perlPath + " " + basePath + SystemConfigurationProperties.getString("BarcodeDesign.Cmd");
        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
        String tmpDirectoryName = properties.getProperty("Upload.ScratchDir");
        List<String> inputPrimerFiles = Task.listOfStringsFromCsvString(task.getParameter(BarcodeDesignerTask.PARAM_primerFile));
        List<String> inputAmpliconFiles = Task.listOfStringsFromCsvString(task.getParameter(BarcodeDesignerTask.PARAM_ampliconsFile));
        String fullCmd = pipelineCmd + " -c " + tmpConfigFile.getAbsolutePath() + " -p " + tmpDirectoryName + File.separator + inputPrimerFiles.get(0) + " -a " +
                tmpDirectoryName + File.separator + inputAmpliconFiles.get(0) +
                " -o " + tmpResultNode.getDirectoryPath() + " -n " +
                task.getParameter(BarcodeDesignerTask.PARAM_numBarcodesPerPrimerPair);

        StringBuffer script = new StringBuffer();
        // todo This needs to be removed in future releases > 2.3!
        fullCmd = "export PATH=$PATH:" + basePath + ";export PERL5LIB=$PERL5LIB:" + basePath + ";" + fullCmd;
        script.append(fullCmd).append("\n");
        writer.write(script.toString());
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + SubmitDrmaaJobService.NORMAL_QUEUE);
        jt.setNativeSpecification(SubmitDrmaaJobService.NORMAL_QUEUE);
    }

}