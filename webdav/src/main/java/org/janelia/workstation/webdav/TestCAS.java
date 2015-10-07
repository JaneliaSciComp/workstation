package org.janelia.workstation.webdav;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpMethod;
import org.janelia.workstation.webdav.exception.FileNotFoundException;

import java.io.IOException;

/**
 * Created by schauderd on 9/28/15.
 */
public class TestCAS {
    public static void main (String[] args) {
        HttpClient client = new HttpClient();
        PostMethod login = new PostMethod("https://schauderd-ws1.janelia.priv:8443/cas/login");

        PostMethod method = new PostMethod("https://schauderd-ws1.janelia.priv:8443/cas/v1/tickets");
        NameValuePair[] data = {
                new NameValuePair("username", "schauderd"),
                new NameValuePair("password", "Doubloon!1")
        };
        login.setRequestBody(data);
        method.setRequestBody(data);
        try {
            int responseCode = client.executeMethod(login);
            System.out.println (responseCode);
            String content = method.getResponseBodyAsString();
            System.out.println(content);
            responseCode = client.executeMethod(method);
            System.out.println (responseCode);
            content = method.getResponseBodyAsString();
            System.out.println(content);
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
}
