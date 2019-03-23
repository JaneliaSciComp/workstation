package org.janelia.it.workstation.browser.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.janelia.it.workstation.browser.api.web.SageRestClient;
import org.janelia.it.workstation.browser.model.SplitTypeInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SageResponderTest {

    private SageRestClient client;
    
    @Before
    public void setUp() throws Exception {
        client = new SageRestClient("http://sage_responder.int.janelia.org/", false);
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

    @Test
    public void testSplitHalfInfo() throws Exception {
        String frag = "BJD_109A03";
        Collection<String> frags = Arrays.asList(frag);
        Map<String, SplitTypeInfo> splitTypeInfos = client.getSplitTypeInfo(frags);
        SplitTypeInfo splitTypeInfo = splitTypeInfos.get(frag);
        Assert.assertEquals(frag, splitTypeInfo.getFragName());
        Assert.assertTrue(splitTypeInfo.hasAD());
        Assert.assertTrue(splitTypeInfo.hasDBD());
    }
}
