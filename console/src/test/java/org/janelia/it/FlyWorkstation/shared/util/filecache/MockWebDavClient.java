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
        WebDavFile webDavFile = new WebDavFile(file);
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
                webDavFileList.add(new WebDavFile(file));
            }
            urlToFileList.put(url, webDavFileList);
        }
    }

    @Override
    public void setCredentials(UsernamePasswordCredentials credentials) {
        // ignore
    }

    @Override
    public WebDavFile findFile(URL url)
            throws WebDavRetrievalException {
        List<WebDavFile> list = urlToFileList.get(url);
        if ((list == null) || (list.size() == 0)) {
            throw new WebDavRetrievalException("no test file registered for " + url);
        }
        return list.get(0);
    }

    @Override
    public List<WebDavFile> findAllInternalFiles(URL url)
            throws WebDavRetrievalException {
        return getFiles(url, true);
    }

    @Override
    public List<WebDavFile> findImmediateInternalFiles(URL url)
            throws WebDavRetrievalException {
        return getFiles(url, false);
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
