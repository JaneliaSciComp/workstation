package org.janelia.workstation.core.api;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.api.http.HttpClientManager;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConnectionEvent;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.model.ConnectionResult;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages connections to data servers.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConnectionMgr {

    private static final Logger log = LoggerFactory.getLogger(ConnectionMgr.class);

    private static final String CONNECTION_STRING_PREF = "connectionString";

    // Singleton
    private static ConnectionMgr instance;
    public static ConnectionMgr getConnectionMgr() {
        if (instance==null) {
            instance = new ConnectionMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    public String getConnectionString() {
        String systemApiGateway = System.getProperty("api.gateway");
        if (StringUtils.isNotBlank(systemApiGateway)) {
            // this takes priority because it typically comes from the command line
            log.info("Use connect string '{}' defined in system properties", systemApiGateway);
            return systemApiGateway;
        }
        String persistedApiGateway = FrameworkAccess.getLocalPreferenceValue(ConnectionMgr.class, CONNECTION_STRING_PREF, null);
        if (StringUtils.isNotBlank(persistedApiGateway)) {
            // next check if there are persisted user preferences
            log.info("Use connect string '{}' persisted in user preferences", persistedApiGateway);
            return persistedApiGateway;
        } else {
            String defaultApiGateway = ConsoleProperties.getString("api.gateway");
            log.info("Use connect string '{}' from application config", defaultApiGateway);
            return defaultApiGateway;
        }
    }

    public ConnectionResult connect(String connectionString) {

        String uri;
        try {
            URL url = new URL(connectionString);
            if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
                url = new URL(url, "/SCSW/ServiceDiscovery/v1/properties");
            }
            uri = url.toString();

        }
        catch (MalformedURLException e) {
            log.warn("Bad url entered: "+connectionString, e);
            return new ConnectionResult("Invalid connection string");
        }

        log.info("Connecting to: {}", uri);
        GetMethod method = new GetMethod(uri);

        try {
            int responseCode = HttpClientManager.getHttpClient().executeMethod(method);
            if (responseCode == HttpURLConnection.HTTP_OK) {

                Properties properties = new Properties();
                try (InputStream in = method.getResponseBodyAsStream()) {
                    properties.load(in);
                    log.info("Retrieved {} runtime properties from server", properties.size());
                    // Add the API Gateway property, but only if the retrieve was successful
                    properties.put("api.gateway", connectionString);
                }
                catch (Exception ex) {
                    log.error("Failed to load additional runtime properties", ex);
                }

                FrameworkAccess.setLocalPreferenceValue(ConnectionMgr.class, CONNECTION_STRING_PREF, connectionString);
                Events.getInstance().postOnEventBus(new ConnectionEvent(connectionString, properties));

                return new ConnectionResult(properties);
            }
            else {
                log.warn("Error connecting to server "+uri+" (HTTP "+responseCode+")");
                return new ConnectionResult("Could not connect to server (HTTP "+responseCode+")");
            }
        }
        catch (Exception ex) {
            log.error("Error connecting to server "+uri, ex);
            return new ConnectionResult("Could not connect to server ("+ex.getClass().getSimpleName()+")");
        }
        finally {
            method.releaseConnection();
        }
    }

    @Subscribe
    public void reconnected(ConnectionEvent e) {

        ConsoleProperties.reload(e.getRemoteProperties());

        String serverVersion = ConsoleProperties.getString("server.versionNumber", "");

        if (!StringUtils.isBlank(serverVersion)) {
            String clientVersion = ConsoleProperties.getString("client.versionNumber", "");
            log.info("serverVersion: {}", serverVersion);
            log.info("clientVersion: {}", clientVersion);
        }

        Events.getInstance().postOnEventBus(new ConsolePropsLoaded());
    }

}
