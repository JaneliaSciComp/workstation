package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=660)
public class RemoveAnnotationBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof OntologyTerm;
    }

    @Override
    public Action getAction(Object obj) {
        return null;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return RemoveAnnotationByTermAction.get();
    }

}
