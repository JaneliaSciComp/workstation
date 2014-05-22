package org.janelia.it.workstation.shared.util.filecache;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.janelia.it.workstation.shared.util.filecache.WebDavException;
import org.janelia.it.workstation.shared.util.filecache.WebDavFile;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock client instance for testing using local files for WebDAV calls.
 *
 * @author Eric Trautman
 */
public class MockWebDavClient extends org.janelia.it.workstation.shared.util.filecache.WebDavClient {

    private Map<URL, List<org.janelia.it.workstation.shared.util.filecache.WebDavFile>> urlToFileList;

    public MockWebDavClient() {
        urlToFileList = new HashMap<URL, List<org.janelia.it.workstation.shared.util.filecache.WebDavFile>>();
    }

    public void mapFileUsingDefaultUrl(File file) {
        org.janelia.it.workstation.shared.util.filecache.WebDavFile webDavFile = new org.janelia.it.workstation.shared.util.filecache.WebDavFile(null, file);
        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> list = new ArrayList<org.janelia.it.workstation.shared.util.filecache.WebDavFile>();
        list.add(webDavFile);
        urlToFileList.put(webDavFile.getUrl(), list);
    }

    public void mapFilesUsingDefaultUrl(List<File> fileList) {
        for (File file : fileList) {
            mapFileUsingDefaultUrl(file);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void mapUrlToFileList(URL url,
                                 List<File> fileList) {
        if (fileList.size() > 0) {
            List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> webDavFileList =
                    new ArrayList<org.janelia.it.workstation.shared.util.filecache.WebDavFile>(fileList.size());
            for (File file : fileList) {
                webDavFileList.add(new org.janelia.it.workstation.shared.util.filecache.WebDavFile(null, file));
            }
            urlToFileList.put(url, webDavFileList);
        }
    }

    @Override
    public void setCredentials(UsernamePasswordCredentials credentials) {
        // ignore
    }

    @Override
    public org.janelia.it.workstation.shared.util.filecache.WebDavFile findFile(URL url)
            throws org.janelia.it.workstation.shared.util.filecache.WebDavException {
        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> list = urlToFileList.get(url);
        if ((list == null) || (list.size() == 0)) {
            throw new org.janelia.it.workstation.shared.util.filecache.WebDavException("no test file registered for " + url);
        }
        return list.get(0);
    }

    @Override
    public boolean isAvailable(URL url)
            throws org.janelia.it.workstation.shared.util.filecache.WebDavException {
        return (urlToFileList.containsKey(url));
    }

    @Override
    public List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> findAllInternalFiles(URL url)
            throws org.janelia.it.workstation.shared.util.filecache.WebDavException {
        return getFiles(url, true);
    }

    @Override
    public List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> findImmediateInternalFiles(URL url)
            throws org.janelia.it.workstation.shared.util.filecache.WebDavException {
        return getFiles(url, false);
    }

    @Override
    public void createDirectory(URL directoryUrl)
            throws WebDavException {
        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> webDavFileList = new ArrayList<org.janelia.it.workstation.shared.util.filecache.WebDavFile>();
        webDavFileList.add(new org.janelia.it.workstation.shared.util.filecache.WebDavFile(null, new File(directoryUrl.getPath())));
        urlToFileList.put(directoryUrl, webDavFileList);
    }

    @Override
    public void saveFile(URL url,
                         File file) {
        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> webDavFileList = new ArrayList<org.janelia.it.workstation.shared.util.filecache.WebDavFile>();
        webDavFileList.add(new org.janelia.it.workstation.shared.util.filecache.WebDavFile(null, file));
        urlToFileList.put(url, webDavFileList);
    }

    private List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> getFiles(URL url,
                                      boolean addAll) {

        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> list = new ArrayList<org.janelia.it.workstation.shared.util.filecache.WebDavFile>();

        final String directoryPath = url.getPath();
        String path;
        File file;
        File parent;
        for (URL registeredUrl : urlToFileList.keySet()) {
            for (WebDavFile webDavFile : urlToFileList.get(registeredUrl)) {
                path = webDavFile.getUrl().getPath();
                if (addAll) {
                    if (path.startsWith(directoryPath)) {
                        list.add(webDavFile);
                    }
                } else {
                    file = new File(path);
                    parent = file.getParentFile();
                    if (directoryPath.equals(parent.getAbsolutePath() + "/")) {
                        list.add(webDavFile);
                    }
                }
            }
        }

        return list;
    }
}
