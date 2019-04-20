package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 220)
public class OpenInNeuronAnnotatorBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof NeuronFragment;
    }

    @Override
    public Action getAction(Object obj) {
        return new OpenInNeuronAnnotatorAction((NeuronFragment)obj);
    }
}
