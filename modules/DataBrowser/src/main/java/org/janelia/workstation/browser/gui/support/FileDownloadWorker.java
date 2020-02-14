package org.janelia.workstation.browser.gui.support;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadFileItem;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.NamedBackgroundWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker for downloading sample images (or sets of split channel images) in a specific format.
 * The specified extension is used to determine whether conversion is necessary and when necessary,
 * if conversion can be managed locally or remotely (via task submission).
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDownloadWorker {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadWorker.class);

    private final Path downloadsDir = Utils.getDownloadsDir();
    private final Collection<DownloadFileItem> downloadItems;
    private final Semaphore copySemaphore;
    private Multiset<String> parentDirs = HashMultiset.create();

    public FileDownloadWorker(Collection<DownloadFileItem> downloadItems, Semaphore copySemaphore) {
        this.downloadItems = downloadItems;
        this.copySemaphore = copySemaphore;
    }

    public FileDownloadWorker(DownloadFileItem downloadItem, Semaphore copySemaphore) {
        this(Arrays.asList(downloadItem), copySemaphore);
    }

    public void startDownload() {

        List<DownloadFileItem> toConvertOnServer = new ArrayList<>();
        List<DownloadFileItem> toTransfer = new ArrayList<>();

        for (DownloadFileItem downloadItem : downloadItems) {
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
                // Stream decompression is very slow now. 7.24MB/s when I last tested on Mac. 
                // We can rely on server-side decompression until this can be addressed.
//                else if (Utils.EXTENSION_LSM.equals(targetExtension)) {
//                    // Just need to convert bz2 to lsm, which we can do locally
//                    convertOnServer = false;
//                }
            }

            if (convertOnServer) {
                toConvertOnServer.add(downloadItem);
            } else {
                toTransfer.add(downloadItem);
            }
        }

        if (toConvertOnServer.isEmpty()) {
            log.info("Will directly download {} files.", toTransfer.size());
        } else if (toTransfer.isEmpty()) {
            log.info("Will convert and download {} files.", toConvertOnServer.size());
        } else {
            log.info("Will convert and download {} files, and directly download {} files.", toConvertOnServer.size(), toTransfer.size());
        }

        for (DownloadFileItem downloadItem : toConvertOnServer) {

            try {
                String objectName = downloadItem.getDomainObject().getName();
                String targetExtension = downloadItem.getTargetExtension();
                String sourceFilePath = downloadItem.getSourceFile();

                log.info("Converting {} to {} (splitChannels={})", sourceFilePath, targetExtension, downloadItem.isSplitChannels());

                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter("filepath", sourceFilePath, null));
                taskParameters.add(new TaskParameter("output extension", targetExtension, null));
                taskParameters.add(new TaskParameter("chan spec", downloadItem.getChanspec(), null));
                if (downloadItem.isSplitChannels()) {
                    taskParameters.add(new TaskParameter("split channels", "true", null));
                }

                Task task = StateMgr.getStateMgr().submitJob("ConsoleSplitAndConvert", "Convert: " + objectName, taskParameters);

                String workerName = "Converting and downloading " + downloadItem.getTargetFile().getFileName();

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public void doStuff() throws Exception {

                        setName(workerName);
                        setStatus("Queueing job on compute cluster");

                        super.doStuff();

                        throwExceptionIfCancelled();

                        // Since there is no way to log task output vars, we use a convention where the last message
                        // will contain the output files.
                        String resultFiles = null;
                        Task task = getTask();
                        List<TaskMessage> messages = new ArrayList<>(task.getMessages());
                        if (!messages.isEmpty()) {
                            messages.sort((o1, o2) -> o2.getMessageId().compareTo(o1.getMessageId()));
                            resultFiles = messages.get(0).getMessage();
                        }

                        throwExceptionIfCancelled();

                        if (resultFiles == null) {
                            throw new Exception("No result files generated");
                        }

                        setStatus("Downloading converted files");

                        // Copy the files to the local drive
                        String[] pathAndFiles = resultFiles.split(":");
                        String path = pathAndFiles[0];
                        String[] filePaths = pathAndFiles[1].split(",");
                        for (String filePath : filePaths) {
                            String remoteFile = path + "/" + filePath;
                            String targetFile = downloadItem.getTargetFile().toString();

                            String channelSuffix = getChannelSuffix(filePath);
                            if (channelSuffix != null) {
                                targetFile = targetFile.replaceAll("#", channelSuffix);
                            }

                            File localFile = new File(targetFile);
                            copyFile(remoteFile, localFile, this, true);
                        }

                        throwExceptionIfCancelled();
                        setName(workerName.replace("Converting and downloading", "Successfully downloaded"));
                    }

                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return getDownloadSuccessCallback();
                    }
                };

                taskWorker.executeWithEvents();
            } catch (Exception e) {
                FrameworkAccess.handleExceptionQuietly(e);
            }
        }

        if (!toTransfer.isEmpty()) {
            NamedBackgroundWorker transferWorker = new NamedBackgroundWorker() {

                @Override
                public void doStuff() {

                    int errors = 0;
                    int success = 0;
                    int i = 0;

                    setName(createName(toTransfer));

                    for (DownloadFileItem downloadItem : toTransfer) {
                        String filename = downloadItem.getTargetFile().getFileName().toString();
                        try {
                            log.debug("Starting download of {} to {}", downloadItem.getSourceExtension(), downloadItem.getTargetExtension());
                            setName("Downloading file " + (i + 1) + " of " + toTransfer.size());
                            // For 2d files, we pass in a null worker here because we may be downloading a lot of small files, and if that's the case, 
                            // the rapid-fire events generated by the download would overwhelm the GUI.
                            transferAndConvertLocallyAsNeeded(downloadItem, this, downloadItem.is3d());
                            success++;
                        } catch (Exception e) {
                            errors++;
                            // If any error occurred during download, we need to delete the file which was being downloaded
                            cleanFile(downloadItem.getTargetFile().toFile());
                            if (e instanceof CancellationException) {
                                log.error("Download was cancelled: {}", filename);
                                throw (CancellationException) e;
                            } else if (e instanceof InterruptedException || e instanceof ClosedByInterruptException) {
                                log.error("Download was cancelled: {}", filename);
                                throw new CancellationException();
                            } else {
                                if (errors == 1) {
                                    // First exception is shown to the user
                                    FrameworkAccess.handleException(e);
                                } else {
                                    FrameworkAccess.handleExceptionQuietly(e);
                                }
                            }
                        }

                        setProgress(i++, toTransfer.size());
                    }

                    setName("Download " + toTransfer.size() + " items");
                    if (success == 0) {
                        setFinalStatus("Failed to download all items.");
                    } else if (errors > 0) {
                        setFinalStatus("Successfully downloaded " + success + " items. Failed to download " + errors + " items.");
                    } else {
                        setName(createName(toTransfer).replace("Download", "Successfully downloaded"));
                    }
                }

                @Override
                public Callable<Void> getSuccessCallback() {
                    return getDownloadSuccessCallback();
                }

                private void cleanFile(File file) {
                    try {
                        if (file.exists()) {
                            if (file.delete()) {
                                log.warn("Deleted partially downloaded file: {}", file);
                            }
                        }
                    } catch (Exception e) {
                        FrameworkAccess.handleExceptionQuietly("Error removing partially downloaded file", e);
                    }
                }
            };

            transferWorker.setName(createName(toTransfer));
            transferWorker.executeWithEvents();
        }
    }

    private String createName(List<DownloadFileItem> toTransfer) {
        if (toTransfer.size() == 1) {
            return "Download " + toTransfer.get(0).getTargetFile().getFileName().toString();
        } else {
            return "Download " + toTransfer.size() + " items";
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
        log.debug("Transferring {} to {}", remoteFile, localFile);
        if (worker != null) worker.throwExceptionIfCancelled();
        copyFile(remoteFile, localFile.toFile(), worker, hasProgress);
        if (worker != null) worker.throwExceptionIfCancelled();
    }

    private void copyFile(String remoteFile, File localFile, BackgroundWorker worker, boolean hasProgress) throws Exception {
        if (hasProgress && worker != null) {
            worker.setProgress(0, 100);
            worker.setStatus("Waiting to download...");
        }
        try {
            copySemaphore.acquire();
        } catch (InterruptedException e) {
            log.error("Download was cancelled: {}", localFile);
            throw new CancellationException();
        }
        try {
            if (hasProgress && worker != null) worker.setStatus("Downloading " + localFile.getName());
            Utils.copyURLToFile(remoteFile, localFile, worker, hasProgress);
        } finally {
            copySemaphore.release();
        }

    }

    private Callable<Void> getDownloadSuccessCallback() {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String maxPath = Multisets.copyHighestCountFirst(parentDirs).iterator().next();
                if (maxPath != null) {
                    DesktopApi.browse(new File(maxPath));
                } else {
                    DesktopApi.browse(downloadsDir.toFile());
                }
                return null;
            }
        };
    }
}
