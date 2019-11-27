package org.janelia.workstation.browser.gui.colordepth;

import java.io.File;

import org.janelia.model.access.domain.TimebasedIdentifierGenerator;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.filecache.RemoteLocation;
import org.janelia.workstation.core.filecache.WebDavUploader;
import org.janelia.workstation.core.util.ConsoleProperties;

public class MaskUtils {

    /**
     * Upload the given local file to the remote storage location, and return its real path.
     * @param localFile
     * @return
     */
    static String uploadMask(File localFile) {

        String importStorageDefaultTags = ConsoleProperties.getString("console.upload.StorageTags.nrs");

        WebDavUploader uploader = FileMgr.getFileMgr().getFileUploader();
        String uploadContext = uploader.createUploadContext("WorkstationFileUpload",
                AccessManager.getSubjectName(),
                importStorageDefaultTags);

        Long guid = TimebasedIdentifierGenerator.generateIdList(1).get(0);
        RemoteLocation location = uploader.uploadFile("UserGeneratedMask_"+guid, uploadContext, importStorageDefaultTags, localFile);
        return location.getRealFilePath();
    }

    static String getFormattedScorePct(ColorDepthMatch match) {
        return String.format("%.0f%%", match.getScorePercent()*100);
    }
}
