package org.janelia.workstation.core.api;

import java.io.InputStream;
import java.net.HttpURLConnection;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.janelia.workstation.core.api.http.HttpClientManager;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConnectionEvent;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
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

    private static final Logger log = LoggerFactory.getLogger(DomainMgr.class);

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
        String defaultValue = ConsoleProperties.getString("api.gateway", null);
        return FrameworkAccess.getLocalPreferenceValue(ConnectionMgr.class, CONNECTION_STRING_PREF, defaultValue);
    }

    public void setConnectionString(String connectionString) {
        FrameworkAccess.setLocalPreferenceValue(ConnectionMgr.class, CONNECTION_STRING_PREF, connectionString);
        Events.getInstance().postOnEventBus(new ConnectionEvent(connectionString));
    }
/*


 */
    @Subscribe
    public void reconnected(ConnectionEvent e) {

        String url = e.getConnectionString()+"/SCSW/ServiceDiscovery/v1/properties";
        log.info("Connecting to: {}", url);
        GetMethod method = new GetMethod(url);

        try {
            int responseCode = HttpClientManager.getHttpClient().executeMethod(method);
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // Prepend the API Gateway property, which is just the bare connection string
                String properties = "api.gateway="+e.getConnectionString()+"\n"+method.getResponseBodyAsString();

                // This isn't exactly right, because it counts commented properties, and = symbols inside property values
                int count = properties.split("=").length;
                log.info("Retrieved {} runtime properties", count-1);

                ConsoleProperties.reload(properties);
                Events.getInstance().postOnEventBus(new ConsolePropsLoaded());
            }
            else {
                log.warn("Error connecting to server "+url+" (HTTP "+responseCode+")");
                JOptionPane.showMessageDialog(
                        FrameworkAccess.getMainFrame(),
                        "Could not connect to "+e.getConnectionString()+" (HTTP "+responseCode+")",
                        "Error connecting to server",
                        JOptionPane.ERROR_MESSAGE,
                        null
                );
            }
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
        finally {
            method.releaseConnection();
        }
    }

}
