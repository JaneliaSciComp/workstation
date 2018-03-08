package org.janelia.it.workstation.browser.gui.support;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.workers.WorkerChangedEvent;
import org.janelia.it.workstation.browser.gui.dialogs.download.DownloadFileItem;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

/**
 * Worker for downloading sample images (or sets of split channel images) in a specific format.
 * The specified extension is used to determine whether conversion is necessary and when necessary,
 * if conversion can be managed locally or remotely (via task submission).
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDownloadWorker {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadWorker.class);
    
    private final Path downloadsDir = SystemInfo.getDownloadsDir();
    private final Collection<DownloadFileItem> downloadItems;
    private final Lock copyFileLock;
    private Multiset<String> parentDirs = HashMultiset.create();
    
    public FileDownloadWorker(Collection<DownloadFileItem> downloadItems, Lock copyFileLock) {
        this.downloadItems = downloadItems;
        this.copyFileLock = copyFileLock;
    }
    
    public FileDownloadWorker(DownloadFileItem downloadItem, Lock copyFileLock) {
        this(Arrays.asList(downloadItem), copyFileLock);
    }

    public void startDownload() {

        List<DownloadFileItem> toConvertOnServer = new ArrayList<>();
        List<DownloadFileItem> toTransfer = new ArrayList<>();
        
        for(DownloadFileItem downloadItem : downloadItems) {
            String targetExtension = downloadItem.getTargetExtension();
            String sourceFilePath = downloadItem.getSourceFile();
            
            String parentDir = downloadItem.getTargetFile().getParent().toString();
            parentDirs.add(parentDir);
            
            boolean convertOnServer = true;
            if (!downloadItem.isSplitChannels()) {
                if (sourceFilePath.endsWith(targetExtension)) {
                    // no conversion needed, simply transfer the file
                    convertOnServer = false;
                } 
                else if (Utils.EXTENSION_LSM.equals(targetExtension)) {
                    // Just need to convert bz2 to lsm, which we can do locally
                    convertOnServer = false;
                }
            }
            
            if (convertOnServer) {
                toConvertOnServer.add(downloadItem);
            }
            else {
                toTransfer.add(downloadItem);
            }
        }

        log.info("Will convert and download {} files, and directly download {} files.", toConvertOnServer.size(), toTransfer.size());
        
        for(DownloadFileItem downloadItem : toConvertOnServer) {

            try {
                String objectName = downloadItem.getDomainObject().getName();
                String targetExtension = downloadItem.getTargetExtension();
                String sourceFilePath = downloadItem.getSourceFile();
                
                log.info("Converting {} to {} (splitChannels={})",sourceFilePath,targetExtension,downloadItem.isSplitChannels());
                
                Task task;
                if (downloadItem.isSplitChannels()) {
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("filepath", sourceFilePath, null));
                    taskParameters.add(new TaskParameter("output extension", targetExtension, null));
                    task = StateMgr.getStateMgr().submitJob("ConsoleSplitChannels", "Split Channels: "+objectName, taskParameters);
                }
                else {
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("filepath", sourceFilePath, null));
                    taskParameters.add(new TaskParameter("output extension", targetExtension, null));
                    task = StateMgr.getStateMgr().submitJob("ConsoleConvertFile", "Convert: "+objectName, taskParameters);
                }

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Downloading " + downloadItem.getTargetFile().getFileName().toString();
                    }
                    
                    @Override
                    public void doStuff() throws Exception {
    
                        setStatus("Converting file on compute cluster");
                        
                        super.doStuff();
                        
                        throwExceptionIfCancelled();
    
                        setStatus("Parsing results");
    
                        // Since there is no way to log task output vars, we use a convention where the last message
                        // will contain the output files.
                        String resultFiles = null;
                        Task task = getTask();
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
    
                        setStatus("Downloading converted files");
                        
                        // Copy the files to the local drive
                        String[] pathAndFiles = resultFiles.split(":");
                        String path = pathAndFiles[0];
                        String[] filePaths = pathAndFiles[1].split(",");
                        for(String filePath : filePaths) {
                            String remoteFile = path + "/" + filePath;
                            String targetFile = downloadItem.getTargetFile().toString();
                            
                            String channelSuffix = getChannelSuffix(filePath);
                            if (channelSuffix!=null) {
                                targetFile = targetFile.replaceAll("#",channelSuffix);    
                            }
                            
                            File localFile = new File(targetFile);
                            copyFile(remoteFile, localFile, this, true);
                        }
    
                        throwExceptionIfCancelled();
                        setStatus("Done");
                    }
    
                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return getDownloadSuccessCallback();
                    }
                };
    
                taskWorker.executeWithEvents();
            }
            catch (Exception e) {
                FrameworkImplProvider.handleExceptionQuietly(e);
            }
        }
        
        if (!toTransfer.isEmpty()) {
            NamedBackgroundWorker transferWorker = new NamedBackgroundWorker() {
                
                @Override
                public void doStuff() {
    
                    int errors = 0;
                    int success = 0;
                    int i = 0;

                    setName("Download "+ toTransfer.size() +" items");
                    
                    for(DownloadFileItem downloadItem : toTransfer) {
                        String filename = downloadItem.getTargetFile().getFileName().toString();
                        try {
                            log.debug("Starting download of {} to {}", downloadItem.getSourceExtension(), downloadItem.getTargetExtension());
                            setName("Downloading "+filename);
                            // For 2d files, we pass in a null worker here because we may be downloading a lot of small files, and if that's the case, 
                            // the rapid-fire events generated by the download would overwhelm the GUI.
                            transferAndConvertLocallyAsNeeded(downloadItem, this, downloadItem.is3d());
                            success++;
                        }
                        catch (Exception e) {
                            errors++;
                            if (e instanceof CancellationException) {
                                log.error("Download was cancelled: {}", filename);
                                throw new CancellationException();
                            }
                            else {
                                FrameworkImplProvider.handleExceptionQuietly(e);
                            }
                        }
                        
                        setProgress(i++, toTransfer.size());
                    }                

                    setName("Download "+ toTransfer.size() +" items");
                    if (success==0) {
                        setFinalStatus("Failed to download all items.");
                    }
                    else if (errors>0) {
                        setFinalStatus("Successfully downloaded "+success+" items. Failed to download "+errors+" items.");
                    }
                    else {
                        setFinalStatus("Successfully downloaded all items.");
                    }
                }
    
                @Override
                public Callable<Void> getSuccessCallback() {
                    return getDownloadSuccessCallback();
                }
            };
    
            transferWorker.executeWithEvents();
        }
    }

    private String getChannelSuffix(String filepath) {
        Pattern p = Pattern.compile("(.*)(_(c\\d))(.*)");
        Matcher m = p.matcher(filepath);
        if (m.matches()) {
            return m.group(3);
        }
        return null;
    }
    
    private void transferAndConvertLocallyAsNeeded(DownloadFileItem downloadItem, BackgroundWorker worker, boolean hasProgress) throws Exception {
        final String remoteFile = downloadItem.getSourceFile();
        final Path localFile = downloadItem.getTargetFile();
        log.info("Transferring {} to {}",remoteFile,localFile);
        if (worker!=null) worker.throwExceptionIfCancelled();
        // TODO: should use Paths API here if possible
        copyFile(remoteFile, localFile.toFile(), worker, hasProgress);
        if (worker!=null) worker.throwExceptionIfCancelled();
    }

    private void copyFile(String remoteFile, File localFile, BackgroundWorker worker, boolean hasProgress) throws Exception {
        if (hasProgress && worker!=null) worker.setStatus("Waiting to download...");
        copyFileLock.lock();
        try {
            if (hasProgress && worker!=null) worker.setStatus("Downloading " + new File(remoteFile).getName());
            Utils.copyURLToFile(remoteFile, localFile, worker, hasProgress);
        } 
        finally {
            copyFileLock.unlock();
        }
    }
        
    private Callable<Void> getDownloadSuccessCallback() {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String maxPath = Multisets.copyHighestCountFirst(parentDirs).iterator().next();
                if (maxPath!=null) {
                    DesktopApi.browse(new File(maxPath));
                }
                else {
                    DesktopApi.browse(downloadsDir.toFile());
                }
                return null;
            }
        };
    }
    
    private static abstract class NamedBackgroundWorker extends BackgroundWorker {
        
        private String name;

        public void setName(String name) {
            this.name = name;
            Events.getInstance().postOnEventBus(new WorkerChangedEvent(this));
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
}
