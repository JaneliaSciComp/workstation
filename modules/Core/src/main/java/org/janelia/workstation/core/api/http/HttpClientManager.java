package org.janelia.workstation.core.api.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

public class HttpClientManager {

    private static final int HTTP_TIMEOUT_SEC = 5;

    // Singleton
    private static HttpClientManager instance;

    private static HttpClientManager getInstance() {
        if (instance==null) {
            instance = new HttpClientManager();
        }
        return instance;
    }

    private final HttpClient httpClient;

    private HttpClientManager() {

        HttpConnectionParams httpParams = new HttpConnectionParams();
        httpParams.setConnectionTimeout(HTTP_TIMEOUT_SEC*1000);
        httpParams.setSoTimeout(HTTP_TIMEOUT_SEC*1000);

        HttpClientParams clientParams = new HttpClientParams(httpParams);

        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);

        multiThreadedHttpConnectionManager.setParams(params);

        Protocol.registerProtocol("https",
                new Protocol("https",
                        new ProtocolSocketFactory() {
                            private final SSLContext sslcontext = createSSLContext();

                            @Override
                            public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
                                return sslcontext.getSocketFactory().createSocket(
                                        host,
                                        port,
                                        localAddress,
                                        localPort);
                            }

                            @Override
                            public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
                                if (params == null) {
                                    throw new IllegalArgumentException("Parameters may not be null");
                                }
                                int timeout = params.getConnectionTimeout();
                                SocketFactory socketfactory = sslcontext.getSocketFactory();
                                if (timeout == 0) {
                                    return socketfactory.createSocket(host, port, localAddress, localPort);
                                } else {
                                    Socket socket = socketfactory.createSocket();
                                    SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
                                    SocketAddress remoteaddr = new InetSocketAddress(host, port);
                                    socket.bind(localaddr);
                                    socket.connect(remoteaddr, timeout);
                                    return socket;
                                }
                            }

                            @Override
                            public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                                return sslcontext.getSocketFactory().createSocket(
                                        host,
                                        port);
                            }
                        },
                        443));

        httpClient = new HttpClient(clientParams, multiThreadedHttpConnectionManager);
    }

    public static HttpClient getHttpClient() {
        return getInstance().httpClient;
    }    

    static SSLContext createSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            TrustManager[] trustManagers = {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                            // Everyone is trusted
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                            // Everyone is trusted
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Error initilizing SSL context", e);
        }
    }
}
