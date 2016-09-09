
package org.janelia.it.jacs.compute.service.tools;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tools.Legacy2BsmlTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.Legacy2BsmlResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 11, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * # Legacy2Bsml
 Legacy2Bsml.Legacy2BsmlCmd = /usr/local/devel/ANNOTATION/ard/ergatis-v2r10b1/bin/legacy2bsml
 Legacy2Bsml.Queue=-l medium
 Legacy2Bsml.MaxEntriesPerJob=200
 Legacy2Bsml.RepositoryRoot=/usr/local/annotation/
 */
public class Legacy2BsmlService extends SubmitDrmaaJobService {

    public static final String LEGACY2BSML_CMD = SystemConfigurationProperties.getString("Legacy2Bsml.Legacy2BsmlCmd");
    public static final String LEGACY2BSML_QUEUE = SystemConfigurationProperties.getString("Legacy2Bsml.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Legacy2Bsml.MaxEntriesPerJob");

    Legacy2BsmlTask legacy2bsmlTask;
    Legacy2BsmlResultNode legacy2bsmlResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    ArrayList<String> databases = new ArrayList<String>();
    ArrayList<String> organism_type = new ArrayList<String>();
    ArrayList<String> include_genefinders = new ArrayList<String>();
    ArrayList<String> exclude_genefinders = new ArrayList<String>();
    ArrayList<String> alt_databases = new ArrayList<String>();
    ArrayList<String> alt_species = new ArrayList<String>();
    ArrayList<String> asmbl_id = new ArrayList<String>();
    ArrayList<String> sequence_type = new ArrayList<String>();
    ArrayList<String> tu_list_file = new ArrayList<String>();
    ArrayList<String> model_list_file = new ArrayList<String>();
    ArrayList<File> log4perl = new ArrayList<File>();
    ArrayList<File> bsmlFileList = new ArrayList<File>();

    protected String getGridServicePrefixName() {
        return "legacy2bsml";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        parseControlFile(new File(legacy2bsmlTask.getParameter(Legacy2BsmlTask.PARAM_control_file)));

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(legacy2bsmlTask, writer);

        int configIndex = 0;

        for (String asmblId : asmbl_id) {

            File log4perlFile = new File(legacy2bsmlResultNode.getFilePathByTag(Legacy2BsmlResultNode.TAG_LEGACY2BSML_LOG) + "." + configIndex);

            log4perl.add(log4perlFile);

            configIndex = writeConfigFile(asmblId, databases.get(configIndex), organism_type.get(configIndex),
                    include_genefinders.get(configIndex), exclude_genefinders.get(configIndex),
                    alt_databases.get(configIndex), alt_species.get(configIndex),
                    sequence_type.get(configIndex), tu_list_file.get(configIndex),
                    model_list_file.get(configIndex), log4perlFile, configIndex);

            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    protected void parseControlFile(File controlFile) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(controlFile)));

        Pattern headerPattern = Pattern.compile("(database:(\\w+))\\s+(organism_type:(\\w+))\\s+(include_genefinders:(\\w+)?)\\s+(exclude_genefinders:(\\w+)?)(\\s+alt_databases:(\\w+)?)?(\\s+alt_species:(\\w+)?)?");
        Pattern asmblIdPattern = Pattern.compile("(\\d+)(\\s+sequence_type:\\w+)?(\\s+tu_list_file:\\w+)?(\\s+model_list_file\\w+)?");

        String database = "";
        String organismType = "";
        String includeGenefinders = "";
        String excludeGenefinders = "";
        String altDatabases = "";
        String altSpecies = "";

        String asmblId;
        String sequenceType = "";
        String tuListFile = "";
        String modelListFile = "";

        try {
            String line;

            while (br.ready()) {
                line = br.readLine();

                Matcher headerMatcher = headerPattern.matcher(line);
                Matcher asmblIdMatcher = asmblIdPattern.matcher(line);

                if (headerMatcher.matches()) {

                    for (int i = 1; i <= headerMatcher.groupCount(); i += 2) {
                        if (headerMatcher.group(i) != null && headerMatcher.group(i).trim().length() > 0) {
                            String tempGroup = headerMatcher.group(i);
                            String[] splitTempGroup = tempGroup.split(":");

                            if (splitTempGroup[0].compareTo("alt_databases") == 0) {

                                altDatabases = splitTempGroup[1];

                            }
                            else if (splitTempGroup[0].compareTo("alt_species") == 0) {

                                altSpecies = splitTempGroup[1];

                            }
                            else if (splitTempGroup[0].compareTo("include_genefinders") == 0) {

                                if (splitTempGroup.length == 1) {

                                    includeGenefinders = "all";

                                }
                                else if (splitTempGroup.length == 2) {

                                    includeGenefinders = splitTempGroup[1].trim();

                                }

                            }
                            else if (splitTempGroup[0].compareTo("exclude_genefinders") == 0) {
                                if (splitTempGroup.length == 1) {

                                    excludeGenefinders = "none";

                                }
                                else if (splitTempGroup.length == 2) {

                                    excludeGenefinders = splitTempGroup[1].trim();

                                }

                            }
                            else if (splitTempGroup[0].compareTo("database") == 0) {

                                database = splitTempGroup[1];

                            }
                            else if (splitTempGroup[0].compareTo("organism_type") == 0) {
                                organismType = splitTempGroup[1];

                                if (organismType.compareTo("euk") != 0 && organismType.compareTo("ntprok") != 0 && organismType.compareTo("prok") != 0) {
                                    throw new MissingDataException("Not valid organism type: " + organismType);
                                }
                            }
                        }
                    }

                    if (altDatabases.isEmpty()) {
                        altDatabases = "none";
                    }

                    if (altSpecies.isEmpty()) {
                        altSpecies = "none";
                    }

                }

                if (asmblIdMatcher.matches()) {

                    asmblId = asmblIdMatcher.group(1);

                    //Start at 2 because group 1 garunteed to be there
                    for (int i = 2; i <= asmblIdMatcher.groupCount(); i++) {
                        if (asmblIdMatcher.group(i) != null && asmblIdMatcher.group(i).trim().length() > 0) {

                            String tempGroup = asmblIdMatcher.group(i);
                            String[] splitTempGroup = tempGroup.split(":");

                            if (splitTempGroup[0].compareTo("sequence_type") == 0) {

                                sequenceType = splitTempGroup[1];

                            }
                            else if (splitTempGroup[0].compareTo("tu_list_file") == 0) {

                                tuListFile = splitTempGroup[1];

                            }
                            else if (splitTempGroup[0].compareTo("model_list_file") == 0) {

                                modelListFile = splitTempGroup[1];

                            }
                        }
                    }

                    if (sequenceType.isEmpty()) {

                        sequenceType = "none";

                    }

                    if (tuListFile.isEmpty()) {

                        tuListFile = "none";

                    }

                    if (modelListFile.isEmpty()) {

                        modelListFile = "none";

                    }

                    databases.add(database);

                    organism_type.add(organismType);
                    include_genefinders.add(includeGenefinders);
                    exclude_genefinders.add(excludeGenefinders);
                    alt_databases.add(altDatabases);
                    alt_species.add(altSpecies);
                    asmbl_id.add(asmblId);
                    sequence_type.add(sequenceType);
                    tu_list_file.add(tuListFile);
                    model_list_file.add(modelListFile);
                }
            }

            br.close();

        }
        catch (Exception e) {
            throw new Exception(e.toString());
        }
    }

    private int writeConfigFile(String asmblId, String database, String organismType, String includeGenefinders,
                                String excludeGenefinders, String altDatabase, String altSpecies,
                                String sequenceType, String tuListFile, String modelListFile, File logFile, int configIndex) throws IOException {

        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex + 1));

        while (configFile.exists()) {
            configFile = new File(resultFileNode.getDirectoryPath(), buildConfigFileName(++configIndex));
        }

        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {

            File bsmlDoc = new File(resultFileNode.getDirectoryPath() + File.separator + database + "_" + asmblId + "_assembly." + organismType + "." + Legacy2BsmlResultNode.TAG_LEGACY2BSML_BSML);
            String bsmlPath = bsmlDoc.toString();

            configWriter.println(bsmlPath);
            bsmlFileList.add(bsmlDoc);

            configWriter.println(asmblId);
            configWriter.println(database);
            configWriter.println(organismType);
            configWriter.println(includeGenefinders);
            configWriter.println(excludeGenefinders);
            configWriter.println(altDatabase);
            configWriter.println(altSpecies);
            configWriter.println(sequenceType);
            configWriter.println(tuListFile);
            configWriter.println(modelListFile);
            configWriter.println(logFile);
        }
        finally {
            configWriter.close();
        }
        return configIndex;
    }

    private class ConfigurationFileFilter implements FilenameFilter {
        public ConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(getConfigPrefix());
        }
    }

    private String buildConfigFileName(int configIndex) {
        return getConfigPrefix() + configIndex;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + LEGACY2BSML_QUEUE);
        jt.setNativeSpecification(LEGACY2BSML_QUEUE);
    }

    private void createShellScript(Legacy2BsmlTask legacy2bsmlTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            File idmap = new File(legacy2bsmlResultNode.getFilePathByTag(Legacy2BsmlResultNode.TAG_LEGACY2BSML_IDMAP));
            String idRepository = (new File(SystemConfigurationProperties.getString("Legacy2Bsml.RepositoryRoot") + legacy2bsmlTask.getParameter(Legacy2BsmlTask.PARAM_root_project).toUpperCase() + File.separator + "workflow" + File.separator + "project_id_repository")).toString();


            StringBuffer script = new StringBuffer();
            script.append("read BSMLDOC\n");
            script.append("read ASMBLID\n");
            script.append("read DATABASE\n");
            script.append("read ORGTYPE\n");
            script.append("read INCGENEFINDER\n");
            script.append("read EXCGENEFINDER\n");
            script.append("read ALTDB\n");
            script.append("read ALTSPECIES\n");
            script.append("read SEQTYPE\n");
            script.append("read TULISTFILE\n");
            script.append("read MODELLISTFILE\n");
            script.append("read LOGFILE\n");

            String optionString = legacy2bsmlTask.generateCommandOptions();

            String bsmlDocString = "\"$BSMLDOC\"";
            String asmblIdString = "\"$ASMBLID\"";
            String databaseString = "\"$DATABASE\"";
            String orgTypeString = "\"$ORGTYPE\"";
            String includeGeneFinderString = "\"$INCGENEFINDER\"";
            String excludeGeneFinderString = "\"$EXCGENEFINDER\"";
            String altDBString = "\"$ALTDB\"";
            String altSpeciesString = "\"$ALTSPECIES\"";
            String sequenceTypeString = "\"$SEQTYPE\"";
            String tuListFileString = "\"$TULISTFILE\"";
            String modelListFileString = "\"$MODELLISTFILE\"";
            String logFileString = "\"$LOGFILE\"";

            String legacy2bsmlStr = LEGACY2BSML_CMD + " " + optionString + " --log4perl=" + logFileString +
                    " --output_id_mapping_file=" + idmap.getAbsolutePath() +
                    " --database=" + databaseString +
                    " --asmbl_id=" + asmblIdString +
                    " --schema_type=" + orgTypeString +
                    " --sequence_type=" + sequenceTypeString +
                    " --exclude_genefinders=" + excludeGeneFinderString +
                    " --include_genefinders=" + includeGeneFinderString +
                    " --alt_database=" + altDBString +
                    " --alt_species=" + altSpeciesString +
                    " --tu_list_file=" + tuListFileString +
                    " --id_repository=" + idRepository +
                    " --bsml_doc_name=" + bsmlDocString +
                    " --model_list_file=" + modelListFileString;

            script.append(legacy2bsmlStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File legacy2bsmlFastaList = new File(legacy2bsmlResultNode.getFilePathByTag(Legacy2BsmlResultNode.TAG_LEGACY2BSML_FSALIST));
            File legacy2bsmlBSMLList = new File(legacy2bsmlResultNode.getFilePathByTag(Legacy2BsmlResultNode.TAG_LEGACY2BSML_BSMLLIST));
            File legacy2bsmlDir = new File(legacy2bsmlResultNode.getDirectoryPath());

            logger.info("postProcess for legacy2bsmlTaskId=" + legacy2bsmlTask.getObjectId() + " and resultNodeDir=" + legacy2bsmlDir.getAbsolutePath());

            StringBuffer fastaBuffer = new StringBuffer();
            FileWriter fastaWriter = new FileWriter(legacy2bsmlFastaList);

            ArrayList<File> fsaFiles = new ArrayList<File>();
            findFilesByFilter(legacy2bsmlDir, fsaFilter, fsaFiles, 0, 2);

            for (File f : fsaFiles) {

                fastaBuffer.append(f.getAbsolutePath());
                fastaBuffer.append("\n");

            }

            fastaWriter.write(fastaBuffer.toString());
            fastaWriter.close();

            StringBuffer bsmlBuffer = new StringBuffer();
            FileWriter bsmlWriter = new FileWriter(legacy2bsmlBSMLList);

            for (File b : bsmlFileList) {

                bsmlBuffer.append(b.getAbsolutePath());
                bsmlBuffer.append("\n");

            }

            bsmlWriter.write(bsmlBuffer.toString());
            bsmlWriter.close();

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    public void findFilesByFilter(File dir, FilenameFilter filter, List<File> fileList, int depth, int maxdepth) {
        depth++;
        if (!(depth > maxdepth)) {


            // Add the files to our List of files
            String[] matches = dir.list(filter);
            for (String f : matches) {
                fileList.add(new File(new File(dir.getAbsolutePath() + File.separator + f).getAbsolutePath()).getAbsoluteFile());
            }

            // Check all the name here.  Visit any directories.
            String[] children = dir.list();
            for (String f : children) {
                File g = new File(new File(dir.getAbsolutePath() + File.separator + f).getAbsolutePath());
                if (g.isDirectory()) {
                    findFilesByFilter(g, filter, fileList, depth, maxdepth);
                }
            }
        }
    }

    // filter is based on searchTag
    FilenameFilter fsaFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(Legacy2BsmlResultNode.TAG_LEGACY2BSML_FSA);
        }
    };

    // filter is based on searchTag
    FilenameFilter bsmlfilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(Legacy2BsmlResultNode.TAG_LEGACY2BSML_BSML);
        }
    };

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        legacy2bsmlTask = getLegacy2BsmlTask(processData);
        task = legacy2bsmlTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Legacy2Bsml call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for Legacy2BsmlTask=" + legacy2bsmlTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        legacy2bsmlResultNode = createResultFileNode();
        logger.info("Setting legacy2bsmlResultNode=" + legacy2bsmlResultNode.getObjectId() + " path=" + legacy2bsmlResultNode.getDirectoryPath());
        resultFileNode = legacy2bsmlResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        logger.info(this.getClass().getName() + " legacy2bsmlTaskId=" + task.getObjectId() + " init() end");
    }

    private Legacy2BsmlTask getLegacy2BsmlTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null Legacy2BsmlTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (Legacy2BsmlTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getLegacy2BsmlTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private Legacy2BsmlResultNode createResultFileNode() throws Exception {
        Legacy2BsmlResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof Legacy2BsmlResultNode) {
                logger.info("Found already-extant legacy2bsmlResultNode path=" + ((Legacy2BsmlResultNode) node).getDirectoryPath());
                return (Legacy2BsmlResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating Legacy2BsmlResultNode with sessionName=" + sessionName);
        resultFileNode = new Legacy2BsmlResultNode(task.getOwner(), task,
                "Legacy2BsmlResultNode", "Legacy2BsmlResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }
}