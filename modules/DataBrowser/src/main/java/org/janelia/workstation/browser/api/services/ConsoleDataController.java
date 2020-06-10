package org.janelia.workstation.browser.api.services;

import java.util.List;

import org.janelia.model.access.domain.TimebasedIdentifierGenerator;
import org.janelia.workstation.integration.api.DataController;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implements the data controller.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DataController.class, position=100)
public class ConsoleDataController implements DataController {

    // This context is the same for all clients, but hopefully different than our servers
    private static final int ID_CONTEXT = 10;

    /**
     * It's important that we use loopback here, otherwise there could be a significant delay in constructing this
     * generator, which may unexpectedly block the EDT. The IP address component isn't very important for GUID's
     * generated on the client.
     */
    private TimebasedIdentifierGenerator generator = new TimebasedIdentifierGenerator(ID_CONTEXT, true);

    @Override
    public List<Long> generateGUIDs(int count) {
        return generator.generateIdList(count);
    }
}
