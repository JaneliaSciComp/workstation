
package org.janelia.it.jacs.compute.service.profileComparison;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.profileComparison.ProfileComparisonTask;
import org.janelia.it.jacs.model.user_data.profileComparison.ProfileComparisonResultNode;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * User: tsafford, naxelrod
 * Date: Sep 03, 2009
 * From jacs.properties
 * # Profile Comparison
 ProfileComparison.PERL5LIB=/usr/local/perl
 ProfileComparison.PerlBase=16sDataAnalysis/site_comparison/profile_comparison_pipeline
 ProfileComparison.Cmd=/profile_comparison.pl
 Wiki.ProfileComparisonHelp=DAS/Automatic+Bar+Code+Designer+and+Deconvolution
 */
public class ProfileComparisonSubmitJobService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "profileComparisonConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "profileComparison";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        //ProfileComparisonTask profileComparisonTask = (ProfileComparisonTask) task;
        ProfileComparisonResultNode tmpResultNode = (ProfileComparisonResultNode) resultFileNode;

        // Creating the default config file for the Drmaa Template
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess) {
            logger.error("Unable to create configFile for ProfileComparison process.");
        }

        String perlPath = SystemConfigurationProperties.getString("Perl.Path");
        String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
        String pipelineCmd = perlPath + " " + basePath + SystemConfigurationProperties.getString("ProfileComparison.PerlBase") +
                SystemConfigurationProperties.getString("ProfileComparison.Cmd");
        String tmpDirectoryName = SystemConfigurationProperties.getString("Upload.ScratchDir");
        List<String> inputFiles = Task.listOfStringsFromCsvString(task.getParameter(ProfileComparisonTask.PARAM_inputFile));

        // Takes a list of files, smart enough to figure out the file type based on extension
        String fullCmd = pipelineCmd + " -o " + tmpResultNode.getDirectoryPath();
        for (String inputFile : inputFiles) {
            fullCmd += " -f " + tmpDirectoryName + File.separator + inputFile;
        }
        fullCmd = "export PATH=$PATH:" + basePath + ";export PERL5LIB=$PERL5LIB:" + basePath +
                SystemConfigurationProperties.getString("ProfileComparison.PerlBase") + ";" + fullCmd;
        StringBuffer script = new StringBuffer();
        script.append(fullCmd).append("\n");
        writer.write(script.toString());
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + SubmitDrmaaJobService.NORMAL_QUEUE);
        jt.setNativeSpecification(SubmitDrmaaJobService.NORMAL_QUEUE);
    }

}