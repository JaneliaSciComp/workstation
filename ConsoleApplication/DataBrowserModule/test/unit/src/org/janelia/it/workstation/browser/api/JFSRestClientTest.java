package org.janelia.it.workstation.browser.api;

import java.util.Map;

import org.janelia.it.workstation.browser.api.web.JFSRestClient;
import org.janelia.it.workstation.browser.api.web.QuotaUsage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JFSRestClientTest {

    private JFSRestClient client;
    
    @Before
    public void setUp() throws Exception {
        client = new JFSRestClient();
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testDiskQuota() throws Exception {
        
        String username = "rokickik";
        Map<String, QuotaUsage> diskQuotas = client.getDiskQuota("user:"+username);

        Assert.assertTrue(diskQuotas.size()==1);
        System.out.println(">> "+diskQuotas);
        Assert.assertTrue(diskQuotas.containsKey(username));
        
        QuotaUsage quotaUsage = diskQuotas.get(username);
        Assert.assertEquals("scicomp", quotaUsage.getLab());
        Assert.assertNotNull(quotaUsage.getPercentUsage());
        Assert.assertNotNull(quotaUsage.getSpaceUsedTB());
        Assert.assertNotNull(quotaUsage.getTotalSpaceTB());
        Assert.assertNotNull(quotaUsage.getTotalFiles());
       
    }
}
