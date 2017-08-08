package org.janelia.it.workstation.browser.api;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.workstation.browser.api.web.SageRestClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SageResponderTest {

    private SageRestClient client;
    
    @Before
    public void setUp() throws Exception {
        client = new SageRestClient();
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testPublishingName() throws Exception {
        String lineName = "GMR_SS01399";
        Set<String> pubNames = new HashSet<>();
        for(String sagePubName : client.getPublishingNames(lineName)) {
            pubNames.add(sagePubName);
        }
        Assert.assertTrue(pubNames.contains("SS01399"));
        Assert.assertTrue(pubNames.contains("GMR_MB630B"));   
    }
}
