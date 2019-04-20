package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builds an action to assign a shortcut to the selected ontology term.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=620)
public class OntologyShortcutBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Assign Shortcut...";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof OntologyTerm;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
        OntologyExplorerTopComponent.getInstance().showKeyBindDialog((OntologyTerm)obj);
    }
}
