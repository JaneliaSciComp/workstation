package org.janelia.it.workstation.browser.filecache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import static org.junit.Assert.*;

/**
 * Tests the {@link WebDavClient} class.
 *
 * @author Eric Trautman
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AgentStorageClient.class, AbstractStorageClient.class})
@Category(TestCategories.FastTests.class)
public class AgentStorageClientTest {
    private static final String BASE_WEBDAV_URL = "http://test/webdav";

    private HttpClientProxy httpClient;
    private AgentStorageClient testWebDavClient;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        httpClient = Mockito.mock(HttpClientProxy.class);
        objectMapper = new ObjectMapper();
        testWebDavClient = new AgentStorageClient(BASE_WEBDAV_URL, httpClient, objectMapper, (t) -> {});
    }

    @Test
    public void downloadURL() throws Exception {
        String testPath = "/p1/p2/c1/c2";
        assertEquals(new URL(BASE_WEBDAV_URL + "/storage_path/data_content/" + testPath), testWebDavClient.getDownloadFileURL(testPath));
    }

    @Test
    public void findFile() throws Exception {
        String fileName = "/p1/p2/p3";
        PropFindMethod testMethod = Mockito.mock(PropFindMethod.class);

        PowerMockito.whenNew(PropFindMethod.class).withArguments(BASE_WEBDAV_URL + "/data_storage_path/" + fileName, WebDavFile.PROPERTY_NAMES, 0).thenReturn(testMethod);
        Mockito.when(httpClient.executeMethod(testMethod)).thenReturn(207);

        String returnedUrl = "http://test";
        MultiStatusResponse multiStatusResponse = new MultiStatusResponse(returnedUrl, "desc");
        MultiStatus multiStatus = new MultiStatus();
        multiStatus.addResponse(multiStatusResponse);

        Mockito.when(testMethod.getResponseBodyAsMultiStatus()).thenReturn(multiStatus);

        WebDavFile webDavFile = testWebDavClient.findFile(fileName);
        assertEquals(fileName, webDavFile.getWebdavFileKey());
        assertEquals(new URL(returnedUrl), webDavFile.getRemoteFileUrl());
    }

}
