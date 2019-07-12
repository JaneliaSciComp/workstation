package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.model.domain.ontology.*;
import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.workstation.browser.gui.support.NodeChooser;
import org.janelia.workstation.browser.nodes.OntologyNode;
import org.janelia.workstation.browser.nodes.OntologyTermNode;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "OntologyAddTermAction"
)
@ActionRegistration(
        displayName = "#CTL_OntologyAddTermAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 145)
})
@NbBundle.Messages("CTL_OntologyAddTermAction=Add Item")
public class OntologyAddTermAction extends BaseContextualPopupAction {

    private final static Logger log = LoggerFactory.getLogger(OntologyAddTermAction.class);

    private OntologyTerm selectedTerm;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(OntologyTerm.class)) {
            selectedTerm = getNodeContext().getSingleObjectOfType(OntologyTerm.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(selectedTerm.getOntology()));
        }
        else {
            selectedTerm = null;
            setEnabledAndVisible(false);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected List<JComponent> getItems() {

        final OntologyTerm term = selectedTerm;

        List<JComponent> items = new ArrayList<>();
        if (term==null) return items;
        
        if (term instanceof org.janelia.model.domain.ontology.Enum) {
            // Alternative "Add" menu for enumeration nodes
            JMenuItem smi = new JMenuItem("Item");
            smi.addActionListener(e -> createTerm(term, EnumItem.class));
            items.add(smi);
        }
        else if (term.allowsChildren() || term instanceof Tag) {

            Class[] nodeTypes = {Category.class, Tag.class, org.janelia.model.domain.ontology.Enum.class, EnumText.class, Interval.class, Text.class, Accumulation.class, Custom.class};
            for (final Class<? extends OntologyTerm> nodeType : nodeTypes) {
                try {
                    JMenuItem smi = new JMenuItem(nodeType.newInstance().getTypeName());
                    smi.addActionListener(e -> createTerm(term, nodeType));
                    items.add(smi);
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException(ex);
                }
            }
        }

        return items;
    }

    private void createTerm(final OntologyTerm parentTerm, Class<? extends OntologyTerm> termClass) {

        final OntologyTerm ontologyTerm = createTypeByName(termClass);
        ActivityLogHelper.logUserAction("OntologyAddTermAction.createTerm", ontologyTerm.getTypeName());

        final String termName = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Ontology Term:\n",
                "Adding to " + parentTerm.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

        if ((termName == null) || (termName.length() <= 0)) {
            return;
        }

        ontologyTerm.setName(termName);

        if (ontologyTerm instanceof Interval) {

            String lowerBoundStr = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Lower bound:\n",
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);
            String upperBoundStr = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Upper bound:\n",
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);

            try {
                Long lowerBound = new Long(lowerBoundStr);
                Long upperBound = new Long(upperBoundStr);
                ((Interval) ontologyTerm).init(lowerBound, upperBound);
            }
            catch (NumberFormatException ex) {
                FrameworkAccess.handleException(ex);
                return;
            }
        }
        else if (ontologyTerm instanceof EnumText) {

            OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
            NodeChooser nodeChooser = new NodeChooser(new OntologyNode(parentTerm.getOntology()), "Choose an enumeration");
            int returnVal = nodeChooser.showDialog(explorer);
            if (returnVal != NodeChooser.CHOOSE_OPTION) return;
            if (nodeChooser.getChosenElements().isEmpty()) return;

            List<Node> chosenElements = nodeChooser.getChosenElements();
            if (chosenElements.size()!=1) return;

            OntologyTermNode chosenEnumNode = (OntologyTermNode)chosenElements.get(0);
            OntologyTerm chosenTerm = chosenEnumNode.getOntologyTerm();

            if (!(chosenTerm instanceof org.janelia.model.domain.ontology.Enum)) {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "You must choosen an enumeration", "Error", JOptionPane.ERROR_MESSAGE);
            }

            try {
                ((EnumText) ontologyTerm).init(chosenTerm.getId());
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                Ontology ontology = parentTerm.getOntology();
                model.addOntologyTerm(ontology.getId(), parentTerm.getId(), ontologyTerm);
                log.info("Added term {} to ontology {}", ontologyTerm.getName(), ontology.getId());
            }

            @Override
            protected void hadSuccess() {
                // UI updated by events
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private OntologyTerm createTypeByName(Class<? extends OntologyTerm> termClass) {
        try {
            return termClass.newInstance();
        }
        catch (Exception e) {
            log.error("Could not create ontology term of type "+termClass,e);
        }
        return null;
    }
}
