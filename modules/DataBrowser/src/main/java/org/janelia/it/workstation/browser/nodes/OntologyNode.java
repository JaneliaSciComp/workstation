package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddOntologyTermAction;
import org.janelia.it.workstation.browser.nb_action.OntologyExportAction;
import org.janelia.it.workstation.browser.nb_action.OntologyImportAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RenameAction;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.workstation.common.nodes.DomainObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root node of an ontology. Manages all the nodes in the ontology, including
 * inter-ontology references and actions. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyNode extends OntologyTermNode implements DomainObjectNode<Ontology> {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyNode.class);
    
    public OntologyNode(Ontology ontology) {
        super(null, ontology, ontology);
    }
    
    @Override
    public Ontology getDomainObject() {
        return getOntology();
    }

    @Override
    public void update(Ontology ontology) {
        String oldName = getName();
        String oldDisplayName = getDisplayName();
        log.debug("Updating node with: {}",ontology.getName());
        getLookupContents().remove(getDomainObject());
        getLookupContents().add(ontology);
        fireCookieChange();
        fireNameChange(oldName, getName());
        log.debug("Display name changed {} -> {}",oldDisplayName, getDisplayName());
        fireDisplayNameChange(oldDisplayName, getDisplayName());
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder.png").getImage();
    }
    
    @Override
    public boolean canRename() {
        return true;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(RenameAction.get());
        actions.add(new RemoveAction());
        actions.add(null);
        actions.add(OntologyImportAction.get());
        actions.add(OntologyExportAction.get());
        actions.add(null);
        actions.add(AddOntologyTermAction.get());
        return actions.toArray(new Action[actions.size()]);
    }
    
    @Override
    public void setName(final String newName) {
        final Ontology ontology = getOntology();
        final String oldName = ontology.getName();
        ontology.setName(newName);
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                log.trace("Changing name from " + oldName + " to: " + newName);
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.updateProperty(ontology, "name", newName);
            }
            @Override
            protected void hadSuccess() {
                log.trace("Fire name change from" + oldName + " to: " + newName);
                fireDisplayNameChange(oldName, newName); 
            }
            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };
        worker.execute();
    }
    
    protected final class ViewDetailsAction extends AbstractAction {

        public ViewDetailsAction() {
            putValue(NAME, "View Details");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DomainDetailsDialog().showForDomainObject(getOntology());
        }
    }

    protected final class ChangePermissionsAction extends AbstractAction {

        public ChangePermissionsAction() {
            putValue(NAME, "Change Permissions");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DomainDetailsDialog().showForDomainObject(getOntology(), DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }

        @Override
        public boolean isEnabled() {
            return ClientDomainUtils.isOwner(getOntology());
        }
    }
}
