package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.model.domain.ontology.Ontology;
import org.janelia.workstation.browser.gui.components.DomainObjectAcceptorHelper;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
        Collection<Action> actions = DomainObjectAcceptorHelper.getNodeContextMenuItems(getDomainObject());
        return actions.toArray(new Action[0]);
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
                FrameworkAccess.handleException(error);
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
