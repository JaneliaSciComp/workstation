package org.janelia.workstation.site.jrc.nb_action;

import javax.swing.Action;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=540)
public class StageForPublishingBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return SystemAction.get(StageForPublishingAction.class);
    }
}
