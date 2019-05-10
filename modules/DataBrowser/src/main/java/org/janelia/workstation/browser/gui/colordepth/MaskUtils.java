package org.janelia.workstation.browser.gui.colordepth;

import org.janelia.model.access.domain.TimebasedIdentifierGenerator;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.filecache.RemoteLocation;
import org.janelia.workstation.core.filecache.WebDavUploader;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MaskUtils {

    private static final Logger log = LoggerFactory.getLogger(MaskUtils.class);

    private static final String IMPORT_STORAGE_DEFAULT_TAGS = ConsoleProperties.getString("console.upload.StorageTags.nrs");

    /**
     * Upload the given local file to the remote storage location, and return its real path.
     * @param localFile
     * @return
     */
    static String uploadMask(File localFile) {

        WebDavUploader uploader = FileMgr.getFileMgr().getFileUploader();
        String uploadContext = uploader.createUploadContext("WorkstationFileUpload",
                AccessManager.getSubjectName(),
                IMPORT_STORAGE_DEFAULT_TAGS);

        Long guid = TimebasedIdentifierGenerator.generateIdList(1).get(0);
        RemoteLocation location = uploader.uploadFile("UserGeneratedMask_"+guid, uploadContext, IMPORT_STORAGE_DEFAULT_TAGS, localFile);
        String uploadPath = location.getRealFilePath();
        log.info("Uploaded mask to: "+uploadPath);
        
        return uploadPath;
    }

    static String getFormattedScorePct(ColorDepthMatch match) {
        return String.format("%2.0f%%", match.getScorePercent()*100);
    }
}
