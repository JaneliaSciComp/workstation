package org.janelia.workstation.core.filecache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.janelia.workstation.core.filecache.AbstractStorageClient;
import org.janelia.workstation.core.filecache.MasterStorageClient;
import org.janelia.workstation.core.filecache.WebDavFile;
import org.janelia.workstation.core.filecache.WebDavStorage;
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
@PrepareForTest({MasterStorageClient.class, AbstractStorageClient.class})
@Category(TestCategories.FastTests.class)
public class MasterStorageClientTest {
    private static final String BASE_WEBDAV_URL = "http://test/webdav";

    private HttpClientProxy httpClient;
    private MasterStorageClient testWebDavClient;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        httpClient = Mockito.mock(HttpClientProxy.class);
        objectMapper = new ObjectMapper();
        testWebDavClient = new MasterStorageClient(BASE_WEBDAV_URL, httpClient, objectMapper);
    }

    @Test
    public void findStorage() throws Exception {
        String storagePrefix = "/p1/p2";
        PropFindMethod testMethod = Mockito.mock(PropFindMethod.class);

        PowerMockito.whenNew(PropFindMethod.class).withArguments(BASE_WEBDAV_URL + "/data_storage_path/" + storagePrefix, WebDavFile.PROPERTY_NAMES, 0)
                .thenReturn(testMethod);
        Mockito.when(httpClient.executeMethod(testMethod)).thenReturn(207);

        String returnedUrl = "http://test";
        MultiStatusResponse multiStatusResponse = new MultiStatusResponse(returnedUrl, 200, "desc");
        MultiStatus multiStatus = new MultiStatus();
        multiStatus.addResponse(multiStatusResponse);

        Mockito.when(testMethod.getResponseBodyAsMultiStatus()).thenReturn(multiStatus);

        WebDavStorage webDavFile = testWebDavClient.findStorage(storagePrefix);
        assertEquals(storagePrefix, webDavFile.getWebdavFileKey());
        assertEquals(new URL(returnedUrl), webDavFile.getRemoteFileUrl());
    }

}
