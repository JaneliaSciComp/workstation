package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to upload files to the server via WebDAV.
 *
 * @author Eric Trautman
 */
public class WebDavUploader {
    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploader.class);

    private final StorageClientMgr storageClientMgr;

    /**
     * Constructs an uploader.
     *
     * @param storageClientMgr WebDAV client manager for the current session
     */
    public WebDavUploader(StorageClientMgr storageClientMgr) {
        this.storageClientMgr = storageClientMgr;
    }

    /**
     * Uploads the specified file to the server.
     *
     * @param  storageName user assigned storage name.
     * @param  storageContext storage path context.
     * @param  storageTags tags used for selecting the storage.
     * @param  file  file to upload.
     *
     * @return the server path for the parent directory of the uploaded file.
     *
     * @throws WebDavException
     *   if the file cannot be uploaded.
     */
    public RemoteLocation uploadFile(String storageName, String storageContext, String storageTags, File file) throws WebDavException {
        String storageURL = storageClientMgr.createStorage(storageName, storageContext, storageTags);
        RemoteLocation remoteFile = storageClientMgr.uploadFile(file, storageURL, storageClientMgr.urlEncodeComp(file.getName()));
        LOG.info("uploaded {} to {} - {}", file, storageURL, remoteFile);
        return remoteFile;
    }

    /**
     * Uploads the specified files to the server.
     *
     * @param  storageName         user assigned storage name
     * @param  fileList            list of local files to upload.
     * @param  storageContext storage path context.
     * @param  storageTags tags used for selecting the storage.
     * @param  localRootDirectory  a common parent of all listed files that is used to determine
     *                             the relative path for each file on the server.
     *                             For example given files /a/b/f1.txt and /a/b/c/f2.txt
     *                             and a localRootDirectory of /a, the server paths would be:
     *                             [upload-dir]/b/f1.txt and [upload-dir]/b/c/f2.txt.
     *                             This parameter is optional.
     *                             Specifiy null if the relative paths are not important AND
     *                             all file names are unique.
     *
     * @return the list of uploaded files.
     *
     * @throws IllegalArgumentException
     *   if the server paths cannot be derived or are not unique.
     *
     * @throws WebDavException
     *   if the files cannot be uploaded.
     */
    public List<RemoteLocation> uploadFiles(String storageName, String storageContext, String storageTags, List<File> fileList, File localRootDirectory)
            throws IllegalArgumentException, WebDavException {

        String storageURL = storageClientMgr.createStorage(storageName, storageContext, storageTags);

        // need to go through the entire fileList and create the directory hierarchy
        // and then upload the file content
        class PathComps {
            final Path lastSubPath;
            final List<Path> subPaths = new ArrayList<>();

            private PathComps() {
                this(null);
            }

            private PathComps(Path lastSubPath) {
                this.lastSubPath = lastSubPath;
                if (lastSubPath != null) {
                    this.subPaths.add(lastSubPath);
                }
            }

            private PathComps append(Path pathComp) {
                PathComps res;
                if (lastSubPath == null || pathComp == null) {
                    res = new PathComps(pathComp);
                } else {
                    res = new PathComps(lastSubPath.resolve(pathComp));
                    res.subPaths.addAll(subPaths);
                }
                return res;
            }

            private PathComps append(PathComps pathComps) {
                PathComps res = this;
                for (Path pc : pathComps.subPaths) {
                    res = res.append(pc);
                }
                return res;
            }
        }
        Path localRootPath = localRootDirectory.toPath();
        Set<Path> filePathHierarchy = fileList.stream()
                .filter(f -> f.isFile())
                .map(f -> localRootPath.relativize(f.toPath()))
                .flatMap(fp -> {
                    int nPathComponents = fp.getNameCount();
                    return IntStream.range(0, nPathComponents - 1)
                            .mapToObj(i -> fp.getName(i))
                            .map(p -> p.toString())
                            .map(p -> storageClientMgr.urlEncodeComp(p))
                            .map(pc -> Paths.get(pc))
                            .reduce(new PathComps(),
                                    (pathList, pc)-> pathList.append(pc),
                                    (pl1, pl2) -> pl1.append(pl2))
                            .subPaths.stream();
                })
                .map(fp -> localRootPath.resolve(fp))
                .sorted()
                .collect(Collectors.toSet());

        filePathHierarchy.forEach(fp -> storageClientMgr.createDirectory(storageURL, localRootPath.relativize(fp).toString()));
        List<RemoteLocation> remoteFileList = fileList.stream()
                .filter(f -> f.isFile())
                .map(f -> {
                    RemoteLocation remoteFile = storageClientMgr.uploadFile(f, storageURL, storageClientMgr.urlEncodeComps(localRootPath.relativize(f.toPath()).toString()));
                    LOG.info("uploaded {} to {} - {}", f, storageURL, remoteFile);
                    return remoteFile;
                })
                .collect(Collectors.toList());

        LOG.info("uploaded {} files to {}", fileList.size(), storageURL);
        return remoteFileList;
    }

}
