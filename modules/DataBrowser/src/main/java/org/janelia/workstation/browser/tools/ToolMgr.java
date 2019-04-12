package org.janelia.workstation.browser.tools;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.browser.gui.options.ToolsOptionsPanelController;
import org.janelia.workstation.browser.tools.preferences.InfoObject;
import org.janelia.workstation.core.api.LocalPreferenceMgr;
import org.janelia.workstation.core.api.ServiceMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.events.prefs.LocalPreferenceChanged;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.browser.tools.preferences.PrefMgrListener;
import org.janelia.workstation.browser.tools.preferences.PreferenceManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.netbeans.api.options.OptionsDisplayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/1/12
 * Time: 10:39 AM
 */
public class ToolMgr extends PreferenceManager {
	
    private static final Logger log = LoggerFactory.getLogger(ToolMgr.class);
    // Singleton
    private static ToolMgr toolMgr;

    public static synchronized ToolMgr getToolMgr() {
        if (toolMgr==null) {
            toolMgr = new ToolMgr();
            Events.getInstance().registerOnEventBus(toolMgr);
        }
        return toolMgr;
    }
    
	private static final String CONSOLE_PORT_VAR_NAME = "WORKSTATION_SERVICE_PORT";

    public static final String TOOL_FIJI = "Fiji.app";
    public static final String TOOL_VAA3D = "Vaa3d";
    public static final String TOOL_NA = "Vaa3d - Neuron Annotator";
    public static final String TOOL_VVD = "VVD Viewer";
    public static final String NA_SUFFIX = SystemInfo.isWindows ? " /na" : " -na";
    public static final String MODE_VAA3D_3D = "3D View";
    
    private static String rootExecutablePath = ToolMgr.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static TreeMap<String, ToolInfo> toolTreeMap = new TreeMap<String, ToolInfo>();

    private ToolMgr() {
        log.info("Initializing Tool Manager");
        DEBUG = false;
        userFileDescription = "Workstation Tools";
        reload();
    }

    public final void reload() {
        // Set up the necessary attributes.
        setFileParameters();

        // This listener is to hear when the client session is exiting and save.
        initializeMasterInfoObjects();
        resetWorkingCollections();

        // Temporary code to clean up a NA deficiency
        ToolInfo tmpTool = toolTreeMap.get(TOOL_NA);
        
        // Add Neuron Annotator option to Vaa3d
        if (tmpTool != null) {
            if (!tmpTool.getPath().endsWith(NA_SUFFIX)) {
                tmpTool.setPath(tmpTool.getPath()+NA_SUFFIX);
            }
        }
        
    }

    public void saveChanges() {
        closeOutCurrentUserFile();
    }
    
    /**
     * Inputs the required values to make the mechanism work.
     */
    public void setFileParameters() {
        this.filenameFilter  = "_Tools.properties";
        this.defaultFilename = "";

        // Check for the existence of the group directory property and/or set values.
        String groupDir = "";
        if (FrameworkImplProvider.getModelProperty(GROUP_DIR)==null) {
            FrameworkImplProvider.setModelProperty(GROUP_DIR, groupDir);
        }
        else groupDir = (String)FrameworkImplProvider.getModelProperty(GROUP_DIR);
        setGroupPreferenceDirectory(groupDir);

        // Establish the initial "default" user file.
        this.userDirectory = LocalPreferenceMgr.getInstance().getApplicationOutputDirectory();
        this.userFilename = userDirectory + "User_Tools.properties";
    }

    /**
     * This method will send all of its proprietary collections to the base class method
     * formatOutput(), one-by-one.
     */
    protected void writeOutAllCollections(FileWriter writer, String destinationFile) {
        addCollectionToOutput(writer, toolTreeMap, destinationFile);
    }


    /**
     * This method is supposed to hierarchially add info objects to the final
     * collections.  Any deletion should be a cause for remerging; for example, if
     * a user defined info object is removed it could be replaced with a group
     * or default object.  Currently, this manager only knows about BookmarkInfo
     * objects.
     */
    protected void mergeIntoWorkingCollections(Map<String, InfoObject> targetMasterCollection) {
        for (Object o : targetMasterCollection.keySet()) {
            ToolInfo tmpObject = (ToolInfo)(targetMasterCollection.get(o).clone());
            toolTreeMap.put(tmpObject.getName(), tmpObject);
        }
    }


    /**
     * This method sifts through the Java property keys and puts them in sub-groups by
     * type; in this case only Bookmarks.  It then passes those Properties objects
     * on to the build methods.  Remember this is for only one source,
     * Default or User, at a time.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void populateInfoObjects(Properties allProperties, Map targetMasterCollection,
                                       String sourceFile) {
        Properties allToolProperties=new Properties();
        //Separate all properties into the separate categories
        for (Enumeration<?> e=allProperties.propertyNames();e.hasMoreElements();) {
            String tempKey = (String) e.nextElement();
            StringTokenizer mainToken = new StringTokenizer(tempKey,".");
            if (tempKey!=null && !tempKey.equals("")) {
                String firstToken = mainToken.nextToken();
                String tmpValue = (String)allProperties.get(tempKey);
                String remainingString;
                if (!mainToken.hasMoreTokens()) remainingString = "";
                else remainingString = tempKey.substring(firstToken.length()+1);
                if (firstToken.equals("Tool")) allToolProperties.setProperty(remainingString, tmpValue);
                else if (DEBUG) System.out.println("This key is not known: "+firstToken);
            }
        }
        // This area constructs the Info Objects for this source and adds them to the
        // specific Master collection.
        buildTools(allToolProperties, targetMasterCollection, sourceFile);
    }

    /**
     * This method is a no op to this tool class as there are no default tools.
     */
    public void handleDefaultKeyOverrideRequest() {}

    protected void handleOutputWriteError(){
        /**
         * @todo need to figure out what to do with write errors.
         */
    }

    /**
     * @todo Need to give the base class an abstract method to make
     * this work properly.
     */
    protected void commitChangesToSourceObjects() {
        // First remove the unwanteds.  Only need to check top of hierarchy on down.
        // If the object is found in the user map, bingo.
        super.commitChangesToSourceObjects();
        handleDirtyOrUnknownInfoObjects(toolTreeMap);
        resetWorkingCollections();
    }


    /**
     * Clears the Working collections of Info Objects and rebuilds from the
     * Master collections; merging them into their usable state.
     * Clear out specific InfoObject collection(s) before calling super.
     */
    public void resetWorkingCollections() {
        toolTreeMap = new TreeMap<String, ToolInfo>(new MyStringComparator());
        //  Call the superclass which will merge the collections.
        super.resetWorkingCollections();
    }


    /**
     * Builds the ToolInfo objects for the given Master collection.
     */
    private void buildTools(Properties toolProperties, Map<String, ToolInfo> targetMasterCollection, String sourceFile) {
        Set<?> uniqueTools = getUniqueKeys(toolProperties);
        for (Object uniqueTool : uniqueTools) {
            String nameBase = (String) uniqueTool;
            ToolInfo tmpToolInfo = new ToolInfo(nameBase, toolProperties, sourceFile);
            targetMasterCollection.put(ToolInfo.TOOL_PREFIX + nameBase, tmpToolInfo);
        }

        // Todo Remove this evil hack
        rootExecutablePath=rootExecutablePath.substring(0,rootExecutablePath.lastIndexOf(File.separator)+1);
//      rootExecutablePath="/Applications/JaneliaWorkstation.app/Contents/Resources/workstation.jar";

        log.info("Base root executable path: "+rootExecutablePath);
        
        String vaa3dExePath="";
        if (SystemInfo.isMac || SystemInfo.isWindows) {
            vaa3dExePath = rootExecutablePath+"vaa3d64.app/Contents/MacOS/vaa3d64";
        }
        else if (SystemInfo.isLinux) {
            vaa3dExePath = rootExecutablePath+"vaa3d";
        }

        if (!targetMasterCollection.containsKey(ToolInfo.TOOL_PREFIX+PreferenceManager.getKeyForName(TOOL_VAA3D, true))) {
            ToolInfo tmpTool = new ToolInfo(TOOL_VAA3D, vaa3dExePath, "v3d_16x16x32.png");
            tmpTool.setSourceFile(sourceFile);
            targetMasterCollection.put(ToolInfo.TOOL_PREFIX + tmpTool.getName(), tmpTool);
        }
        if (!targetMasterCollection.containsKey(ToolInfo.TOOL_PREFIX+PreferenceManager.getKeyForName(TOOL_NA, true))) {
            ToolInfo tmpTool = new ToolInfo(TOOL_NA, vaa3dExePath+" -na", "v3d_16x16x32.png");
            tmpTool.setSourceFile(sourceFile);
            targetMasterCollection.put(ToolInfo.TOOL_PREFIX + tmpTool.getName(), tmpTool);
        }
        if (!targetMasterCollection.containsKey(ToolInfo.TOOL_PREFIX+PreferenceManager.getKeyForName(TOOL_FIJI, true))) {
            // Providing a blank path as we don't bundle the tool yet
            ToolInfo tmpTool = new ToolInfo(TOOL_FIJI, "", "brain.png");
            tmpTool.setSourceFile(sourceFile);
            targetMasterCollection.put(ToolInfo.TOOL_PREFIX + tmpTool.getName(), tmpTool);
        }
        if (!targetMasterCollection.containsKey(ToolInfo.TOOL_PREFIX+PreferenceManager.getKeyForName(TOOL_VVD, true))) {
            ToolInfo tmpTool = new ToolInfo(TOOL_VVD, "", "vvd16.svg.png");
            tmpTool.setSourceFile(sourceFile);
            targetMasterCollection.put(ToolInfo.TOOL_PREFIX + tmpTool.getName(), tmpTool);
        }
        // end evil hack
    }


    public void addToolListener(ToolListener listener) {
        if (listeners==null) listeners=new ArrayList<PrefMgrListener>();
        listeners.add(listener);
    }

    public void removeToolListener(ToolListener listener) {
        if (listeners==null) return;
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            listeners=null;
        }
    }

    public boolean addTool(ToolInfo tool) throws Exception {
        String tmpName = tool.getName();
        String tmpPath = tool.getPath();
        if (!tmpName.equals("") && !tmpPath.equals("")){
            toolTreeMap.put(tmpName, tool);
            fireToolsChanged();
            log.info("Added tool {}", tmpName);
            return true;
        }
        else {
            throw new Exception("Please make sure the tool name and path are correct.");
        }
    }

    public boolean removeTool(ToolInfo targetTool) throws Exception {
        String tmpName = targetTool.getName();
        if(isSystemTool(tmpName)) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "Cannot remove a system tool", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (null!=toolTreeMap.get(tmpName)){
            toolTreeMap.remove(targetTool.getName());
            deletedInfos.put(targetTool.getKeyName(), targetTool);
            log.info("Removed tool {}", tmpName);
            fireToolsChanged();
        }
        else {
            throw new Exception("The tool specified does not exist.");
        }
        return true;
    }
    
    public boolean isSystemTool(String toolName) {
        return toolName.equals(TOOL_NA) || toolName.equals(TOOL_VAA3D) || toolName.equals(TOOL_FIJI) || toolName.equals(TOOL_VVD);
    }

    public ToolInfo getTool(String toolName) {
        return toolTreeMap.get(toolName);
    }

    public ToolInfo getToolSafely(String toolName) {
        ToolInfo tool = toolTreeMap.get(toolName);

        if (tool==null || null == tool.getPath() || "".equals(tool.getPath())) {
            log.error("Cannot find tool "+toolName+" in map: "+toolTreeMap.keySet());
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                    "'"+toolName+"' is not configured. Please set a path for this tool in "+SystemInfo.optionsMenuName+".", "Error", JOptionPane.ERROR_MESSAGE);
            OptionsDisplayer.getDefault().open(ToolsOptionsPanelController.PATH);
            return null;
        }
        
        return tool;
    }
    
    public TreeMap<String, ToolInfo> getTools() {
        return toolTreeMap;
    }

    public static void runToolSafely(final String toolName) {
        runToolSafely(toolName, new ArrayList<String>());
    }
    
    public static void runToolSafely(String toolName, List<String> arguments) {
        try {
            runTool(toolName);
        }
        catch (Exception e1) {
            log.error("Could launch tool: "+toolName,e1);
            JOptionPane.showMessageDialog(
                    FrameworkImplProvider.getMainFrame(),
                    "Could not launch this tool. "
                    + "Please choose the appropriate file path from the Tools->Configure Tools area",
                    "ToolInfo Launch ERROR",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void runTool(final String toolName) throws Exception {
        runTool(toolName, new ArrayList<String>());
    }
    
    public static void runTool(final String toolName, List<String> arguments) throws Exception {
        
        ToolInfo tool = getToolMgr().getToolSafely(toolName);
        if (tool==null) {
            return;
        }
        
        String path = tool.getPath();
        log.info("Running tool {} with path {} and arguments {}", toolName, path, arguments);
        
        if (SystemInfo.isMac && tool.getPath().endsWith(".app")) {
            Desktop.getDesktop().open(new File(path));
        } 
        else {
            List<String> command = new ArrayList<>();
            
            boolean foundArg = false;
            StringBuilder toolPathSb = new StringBuilder();
            List<String> toolArgs = new ArrayList<>();
            // TODO: this is very brittle and needs to be re-thought entirely 
            for(String pathComponent : path.split(" ")) {
                if (!foundArg && !pathComponent.startsWith("-") // On non-Windows systems, arguments usually start with a dash 
                        && (!SystemInfo.isWindows || !pathComponent.startsWith("/"))) { // On Windows, arguments start with a forward slash
                    toolPathSb.append(" ").append(pathComponent);
                }
                else {
                    toolArgs.add(pathComponent);
                    foundArg = true;
                }
            }

            String toolPath = toolPathSb.toString().trim();
            log.info("Calculated tool path: {}", toolPath);
            log.info("Calculated tool args: {}", toolArgs);
            log.info("User specified arguments: {}", arguments);
            
            final File exeFile = new File(toolPath);
            if (!exeFile.exists()) {
                String msg = "Tool " + toolName + " (" + exeFile.getAbsolutePath() + ") does not exist.";
                log.error(msg);
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } 
            else if (!exeFile.canExecute()) {
                String msg = "Tool " + toolName + " (" + exeFile.getAbsolutePath() + ") cannot be executed.";
                log.error(msg);
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            command.add(toolPath);
            command.addAll(toolArgs);
            command.addAll(arguments);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Map<String, String> env = pb.environment();
            
            // When executing vaa3d, give it a port to connect back to the console
            if (toolName.equals(TOOL_NA) || toolName.equals(TOOL_VAA3D)) {
                int consolePort = ServiceMgr.getServiceMgr().getAxisServerPort();
                if (consolePort > 0) {
                    env.put(CONSOLE_PORT_VAR_NAME, consolePort+"");
                    log.info("Executing {} with environment: {}", command, env);
                } 
                else {
                    log.info("Executing {}", command);
                }
            }
            else {
                log.info("Executing {}", command);
            }
            
            final Process p = pb.start();

            // Log output in background thread
            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line = null;
                        while((line = reader.readLine()) != null) {
                            log.info("Tool output from {}: {}", toolName, line);
                        }
                    }
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkImplProvider.handleException(error);
                }
            };

            worker.execute();
            
            if (p.waitFor(100, TimeUnit.MILLISECONDS)) {
                // Process terminated immediately, check the exit code
                if (p.exitValue()!=0) {
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                        "'"+toolName+"' could not start. Please check your configuration.", "Error", JOptionPane.ERROR_MESSAGE);
                    OptionsDisplayer.getDefault().open(ToolsOptionsPanelController.PATH);
                }
            }
        }
    }

    public static void openFile(final String toolName, final String standardFilepath, final String mode) throws Exception {
        
        if (standardFilepath == null) {
            throw new Exception("Entity has no file path");
        }

        Utils.processStandardFilepath(standardFilepath, new FileCallable() {
            @Override
            public void call(File file) throws Exception {
                
                if (file==null) {
                    log.error("Could not open file path "+standardFilepath);
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                            "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<String> arguments = new ArrayList<>();

                ToolInfo tool = getToolMgr().getToolSafely(toolName);
                if (tool==null) {
                    return;
                }
                
                // remove leading or trailing spaces to ensure checks work properly
                String toolPath = tool.getPath().trim();

                if (TOOL_VAA3D.equals(toolName)) {
                    arguments.add(SystemInfo.isWindows ? "/i" : "-i");
                    arguments.add(file.getAbsolutePath());
                    if (MODE_VAA3D_3D.equals(mode)) {
                        arguments.add(SystemInfo.isWindows ? "/v" : "-v");
                    }
                }
                else if (TOOL_FIJI.equals(toolName)) {
                    if (toolPath.endsWith(".app")) {
                        tool.setPath(toolPath+"/Contents/MacOS/fiji-macosx");
                    }
                    arguments.add(file.getAbsolutePath());
                }
                else {
                    arguments.add(file.getAbsolutePath());
                }

                runTool(toolName, arguments);
            }
        });

    }

    public void fireToolsChanged() {
        if (listeners!=null) {
            for (Object listener : listeners) {
                ((ToolListener) listener).toolsChanged();
            }
        }
    }

    @Subscribe
    public void systemWillExit(ApplicationClosing event) {
        /**
         * This method first commits any changes to be safe, and then sets all dirty
         * infos to the selected writeback file.  Then for each selection in the
         * writebackFileCollection (sources that have had thier infos changed)
         * the method calls writeOutToDestinationFile.
         */
        closeOutCurrentUserFile();
    }
    
    @Subscribe
    public void modelPropertyChanged(LocalPreferenceChanged event) {
        if (event.getKey().equals(GROUP_DIR)) {
            String groupDirectory = "";
            if (event.getNewValue() !=null) groupDirectory = event.getNewValue() + File.separator;
            // Make the change, flush and reset the preferences.
            setGroupPreferenceDirectory(groupDirectory);
            fireToolsChanged();
        }
    }
}
