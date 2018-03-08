package org.janelia.it.workstation.browser.api;

import org.janelia.it.workstation.browser.util.SimpleJwtParser;
import org.junit.Test;

import org.junit.Assert;

public class JwtParserTest {

    @Test
    public void testToken() throws Exception {
        String t = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1MjA1MzE0MDUsInVzZXJfbmFtZSI6InJva2lja2lrIiwiZnVsbF9uYW1lIjoiS29ucmFkIFJva2lja2kiLCJtYWlsIjoicm9raWNraWtAamFuZWxpYS5oaG1pLm9yZyJ9.-QYI2L6hUwRPUNYWZ9AL6SGEEv_dndFpykaX7LG2Whw";
        SimpleJwtParser parser = new SimpleJwtParser(t);
        
        Assert.assertEquals("JWT", parser.getTyp());
        Assert.assertEquals("HS256", parser.getAlg());
        Assert.assertEquals("1520531405", parser.getExp());
        Assert.assertEquals("rokickik", parser.getUsername());
        Assert.assertEquals("Konrad Rokicki", parser.getFullname());
        Assert.assertEquals("rokickik@janelia.hhmi.org", parser.getMail());
        
    }
}
