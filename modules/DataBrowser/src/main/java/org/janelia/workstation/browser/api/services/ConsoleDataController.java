package org.janelia.workstation.browser.api.services;

import org.janelia.model.util.TimebasedIdentifierGenerator;
import org.janelia.workstation.integration.api.DataController;
import org.openide.util.lookup.ServiceProvider;

import java.util.List;

/**
 * Implements the data controller.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DataController.class, position=100)
public class ConsoleDataController implements DataController {

    // This context is the same for all clients, but hopefully different than our servers
    private static final int ID_CONTEXT = 10;

    private TimebasedIdentifierGenerator generator = new TimebasedIdentifierGenerator(ID_CONTEXT);

    @Override
    public List<Number> generateGUIDs(long count) {
        return generator.generateIdList(count);
    }
}
