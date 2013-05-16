package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.apache.commons.httpclient.UsernamePasswordCredentials;

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
public class MockWebDavClient extends WebDavClient {

    private Map<URL, List<WebDavFile>> urlToFileList;

    public MockWebDavClient() {
        urlToFileList = new HashMap<URL, List<WebDavFile>>();
    }

    public void mapFileUsingDefaultUrl(File file) {
        WebDavFile webDavFile = new WebDavFile(null, file);
        List<WebDavFile> list = new ArrayList<WebDavFile>();
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
            List<WebDavFile> webDavFileList =
                    new ArrayList<WebDavFile>(fileList.size());
            for (File file : fileList) {
                webDavFileList.add(new WebDavFile(null, file));
            }
            urlToFileList.put(url, webDavFileList);
        }
    }

    @Override
    public boolean hasCredentials() {
        return true;
    }

    @Override
    public void setCredentials(UsernamePasswordCredentials credentials) {
        // ignore
    }

    @Override
    public WebDavFile findFile(URL url)
            throws WebDavException {
        List<WebDavFile> list = urlToFileList.get(url);
        if ((list == null) || (list.size() == 0)) {
            throw new WebDavException("no test file registered for " + url);
        }
        return list.get(0);
    }

    @Override
    public boolean isAvailable(URL url)
            throws WebDavException {
        return (urlToFileList.containsKey(url));
    }

    @Override
    public List<WebDavFile> findAllInternalFiles(URL url)
            throws WebDavException {
        return getFiles(url, true);
    }

    @Override
    public List<WebDavFile> findImmediateInternalFiles(URL url)
            throws WebDavException {
        return getFiles(url, false);
    }

    @Override
    public void createDirectory(URL directoryUrl)
            throws WebDavException {
        List<WebDavFile> webDavFileList = new ArrayList<WebDavFile>();
        webDavFileList.add(new WebDavFile(null, new File(directoryUrl.getPath())));
        urlToFileList.put(directoryUrl, webDavFileList);
    }

    @Override
    public void saveFile(URL url,
                         File file) {
        List<WebDavFile> webDavFileList = new ArrayList<WebDavFile>();
        webDavFileList.add(new WebDavFile(null, file));
        urlToFileList.put(url, webDavFileList);
    }

    private List<WebDavFile> getFiles(URL url,
                                      boolean addAll) {

        List<WebDavFile> list = new ArrayList<WebDavFile>();

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
