package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Component;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.DesktopApi;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker for downloading a sample image (or set of split channel images) in a specific format.
 * The specified extension is used to determine whether conversion is necessary and when necessary,
 * if conversion can be managed locally or remotely (via task submission).
 */
public class FileDownloadWorker extends SimpleWorker {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadWorker.class);

    // Download directories
    private static final File WS_IMAGES_DIR = new File(SystemInfo.getDownloadsDir(), "Workstation Images");
    private static final File SPLIT_CHANNEL_IMAGES_DIR = new File(SystemInfo.getDownloadsDir(), "Split Channel Images");

    private final File rootDownloadDir;
    private final DomainObject domainObject;
    private final ResultDescriptor resultDescriptor;
    private final String extension;
    private final boolean splitChannels;
    private final Lock copyFileLock;

    private String objectName;
    private String sourceFilePath;
    private File targetDir;
    private String localFilePrefix;

    public FileDownloadWorker(DomainObject domainObject, ResultDescriptor resultDescriptor, String extension, boolean splitChannels, Lock copyFileLock) {
        this.domainObject = domainObject;
        this.resultDescriptor = resultDescriptor;
        this.extension = extension;
        this.splitChannels = splitChannels;
        this.copyFileLock = copyFileLock;
        this.rootDownloadDir = splitChannels ? SPLIT_CHANNEL_IMAGES_DIR : WS_IMAGES_DIR;
    }
    
    @Override
    protected void doStuff() throws Exception {
        
        HasFiles fileProvider = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            fileProvider = DomainModelViewUtils.getResult(sample, resultDescriptor);
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
        }

        objectName = domainObject.getName();
        
        if (domainObject instanceof Sample) {
            targetDir = new File(rootDownloadDir, objectName);
            localFilePrefix = objectName + "-";
            if (resultDescriptor!=null) localFilePrefix += resultDescriptor.toString().replaceAll("\\W+", "_")+"-";
        }
        else if (domainObject instanceof HasFiles) {
            targetDir = new File(rootDownloadDir, "Images");
            localFilePrefix = "";
        }
        else {
            throw new IllegalStateException("FileDownloadWorker only works with Samples and HasFiles instances");
        }

        sourceFilePath = DomainUtils.getDefault3dImageFilePath(fileProvider);
    }

    @Override
    protected void hadSuccess() {
        try {
            startDownload();
        }
        catch (Exception e) {
            hadError(e);
        }
    }

    @Override
    protected void hadError(Throwable error) {
        SessionMgr.getSessionMgr().handleException(error);
    }

    private void startDownload() throws Exception {

        log.info("startDownload: entry, extension={}, sourceFilePath={}", extension, sourceFilePath);

        File sourceFile = null;
        String localFileName = null;

        if (!splitChannels) {
            if (sourceFilePath.endsWith(extension)) {
                // no conversion needed, simply transfer the file
                sourceFile = new File(sourceFilePath);
                localFileName = localFilePrefix + sourceFile.getName();
            } 
            else if (Utils.EXTENSION_LSM.equals(extension)) {
                // need to convert bz2 to lsm
                sourceFile = new File(sourceFilePath);
                final String compressedSourceName = sourceFile.getName();
                final String uncompressedSourceName =
                        compressedSourceName.substring(0, compressedSourceName.lastIndexOf('.'));
                localFileName = localFilePrefix + uncompressedSourceName;
            } // else leave sourceFile and localFileName null for server side conversions
        }
        
        if (checkForAlreadyDownloadedFiles(localFileName)) {
            if (sourceFile == null) {
                convertOnServer(localFilePrefix);
            } 
            else {
                transferAndConvertLocallyAsNeeded(sourceFilePath, localFileName);
            }
        }
    }

    private boolean checkForAlreadyDownloadedFiles(final String localFileName) {

        boolean continueWithDownload = true;

        final boolean checkForExactFileNames = (localFileName != null);

        if (checkForExactFileNames) {
            log.info("checkForAlreadyDownloadedFiles: checking for {}", localFileName);
        } else {
            log.info("checkForAlreadyDownloadedFiles: checking {} for files that start with {} and end with {}",
                    targetDir, localFilePrefix, extension);
        }

        File[] files = targetDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean accept;
                if (checkForExactFileNames) {
                    accept = localFileName.equals(name);
                } else {
                    accept = name.startsWith(localFilePrefix) && name.endsWith(extension);
                }
                return accept;
            }
        });

        if ((files != null) && (files.length > 0)) {

            String msg;
            String titlePrefix = "File";
            if (checkForExactFileNames) {
                msg = "The file " + files[0].getName() + " was";
            } else if (files.length == 1) {
                msg = "One " + extension + " file for " + objectName + " was";
            } else {
                msg = "Multiple " + extension + " files for " + objectName + " were";
                titlePrefix = titlePrefix + "s";
            }

            final String[] options = { "Open Folder", "Run Download", "Cancel" };
            final Component mainFrame = SessionMgr.getMainFrame();
            final int chosenOptionIndex = JOptionPane.showOptionDialog(
                    mainFrame,
                    msg + " previously downloaded.\nOpen the existing download folder or re-run the download anyway?",
                    titlePrefix + " Previously Downloaded",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            continueWithDownload = (chosenOptionIndex == 1);

            if (chosenOptionIndex == 0) {
                DesktopApi.browse(targetDir);
            }
        }

        return continueWithDownload;
    }

    private void copyFile(File remoteFile,
                          File localFile,
                          BackgroundWorker worker) throws Exception {
        worker.setStatus("Waiting to download...");
        copyFileLock.lock();
        try {
            worker.setStatus("Downloading " + remoteFile.getName());
            Utils.copyURLToFile(remoteFile.getPath(), localFile, worker);
        } finally {
            copyFileLock.unlock();
        }
    }

    private void convertOnServer(final String localFilePrefix) throws Exception {

        log.info("convertOnServer: entry, extension={}, localFilePrefix={}, sampleTargetDir={}",
                 extension, localFilePrefix, targetDir);

        Task task;
        if (splitChannels) {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("filepath", sourceFilePath, null));
            taskParameters.add(new TaskParameter("output extension", extension, null));
            task = ModelMgr.getModelMgr().submitJob("ConsoleSplitChannels", "Split Channels: "+objectName, taskParameters);
        }
        else {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("filepath", sourceFilePath, null));
            taskParameters.add(new TaskParameter("output extension", extension, null));
            task = ModelMgr.getModelMgr().submitJob("ConsoleConvertFile", "Convert: "+objectName, taskParameters);
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Downloading " + objectName;
            }

            @Override
            protected void doStuff() throws Exception {

                setStatus("Grid execution");

                // Wait until task is finished
                super.doStuff();

                throwExceptionIfCancelled();

                setStatus("Parse result");

                // Since there is no way to log task output vars, we use a convention where the last message
                // will contain the output directory path.
                String resultFiles = null;
                final Task task = getTask();
                List<TaskMessage> messages = new ArrayList<>(task.getMessages());
                if (! messages.isEmpty()) {
                    Collections.sort(messages, new Comparator<TaskMessage>() {
                        @Override
                        public int compare(TaskMessage o1, TaskMessage o2) {
                            return o2.getMessageId().compareTo(o1.getMessageId());
                        }
                    });
                    resultFiles = messages.get(0).getMessage();
                }

                throwExceptionIfCancelled();

                if (resultFiles==null) {
                    throw new Exception("No result files generated");
                }

                // Copy the files to the local drive
                String[] pathAndFiles = resultFiles.split(":");
                String path = pathAndFiles[0];
                String[] filePaths = pathAndFiles[1].split(",");
                File remoteFile;
                File localFile;
                for(String filePath : filePaths) {
                    remoteFile = new File(path + "/" + filePath);
                    localFile = new File(targetDir, localFilePrefix + remoteFile.getName());
                    copyFile(remoteFile, localFile, this);
                }

                throwExceptionIfCancelled();

                setFinalStatus("Done");
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return getDownloadSuccessCallback();
            }
        };

        taskWorker.executeWithEvents();
    }

    private void transferAndConvertLocallyAsNeeded(final String remoteFilePath,
                                                   final String localFileName) {

        log.info("transferAndConvertLocallyAsNeeded: entry, remoteFilePath={}, localFileName={}, sampleTargetDir={}",
                 remoteFilePath, localFileName, targetDir);

        final File remoteFile = new File(remoteFilePath);
        final File localFile = new File(targetDir, localFileName);

        final BackgroundWorker transferWorker = new BackgroundWorker() {

            @Override
            public String getName() {
                return "Downloading " + remoteFile.getName();
            }

            @Override
            protected void doStuff() throws Exception {
                setStatus("Local execution");
                throwExceptionIfCancelled();
                copyFile(remoteFile, localFile, this);
                throwExceptionIfCancelled();
                setFinalStatus("Done");
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return getDownloadSuccessCallback();
            }
        };

        transferWorker.executeWithEvents();
    }

    private Callable<Void> getDownloadSuccessCallback() {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                DesktopApi.browse(targetDir);
                return null;
            }
        };
    }

}
