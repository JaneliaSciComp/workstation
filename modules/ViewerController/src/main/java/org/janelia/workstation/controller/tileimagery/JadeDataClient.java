package org.janelia.workstation.controller.tileimagery;

import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.util.ConsoleProperties;

public class JadeDataClient {
    static JadeDataClient instance;
    JadeServiceClient client;

    public static JadeDataClient getInstance() {
        if (instance==null) {
            instance = new JadeDataClient();
        }
        return instance;
    }

    public JadeDataClient() {
        client = new JadeServiceClient(
                ConsoleProperties.getString("jadestorage.rest.url"),
                () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
        );
    }

    public JadeServiceClient getClient() {
        return client;
    }

    public void setClient(JadeServiceClient client) {
        this.client = client;
    }
}
