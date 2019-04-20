package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.model.domain.ontology.*;
import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.workstation.browser.gui.support.NodeChooser;
import org.janelia.workstation.browser.nodes.OntologyNode;
import org.janelia.workstation.browser.nodes.OntologyTermNode;
import org.janelia.workstation.common.actions.NodePresenterAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=510)
public class OntologyAddTermBuilder implements ContextualActionBuilder {

    private final static Logger log = LoggerFactory.getLogger(OntologyAddTermBuilder.class);

    private static final OntologyAddTermAction action = new OntologyAddTermAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof OntologyTerm;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return null;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class OntologyAddTermAction extends NodePresenterAction {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public JMenuItem getPopupPresenter() {

            List<Node> selectedNodes = getSelectedNodes();

            assert !selectedNodes.isEmpty() : "No nodes are selected";

            JMenu addMenuPopup = new JMenu("Add");

            if (selectedNodes.size()>1) {
                addMenuPopup.setEnabled(false);
                return addMenuPopup;
            }

            Node selectedNode = selectedNodes.get(0);
            final OntologyTermNode termNode = (OntologyTermNode)selectedNode;

            final Ontology ontology = termNode.getOntology();
            final OntologyTerm term = termNode.getOntologyTerm();

            if (term instanceof org.janelia.model.domain.ontology.Enum) {
                // Alternative "Add" menu for enumeration nodes
                JMenuItem smi = new JMenuItem("Item");
                smi.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        createTerm(termNode, ontology, EnumItem.class);
                    }
                });
                addMenuPopup.add(smi);
            }
            else if (term.allowsChildren() || term instanceof Tag) {

                Class[] nodeTypes = {Category.class, Tag.class, org.janelia.model.domain.ontology.Enum.class, EnumText.class, Interval.class, Text.class, Accumulation.class, Custom.class};
                for (final Class<? extends OntologyTerm> nodeType : nodeTypes) {
                    try {
                        JMenuItem smi = new JMenuItem(nodeType.newInstance().getTypeName());
                        smi.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                createTerm(termNode, ontology, nodeType);
                            }
                        });
                        addMenuPopup.add(smi);
                    }
                    catch (Exception ex) {
                        FrameworkAccess.handleException(ex);
                    }
                }
            }

            if (!ClientDomainUtils.hasWriteAccess(ontology)) {
                addMenuPopup.setEnabled(false);
            }

            return addMenuPopup;
        }

        private void createTerm(final OntologyTermNode parentNode, Ontology ontology, Class<? extends OntologyTerm> termClass) {

            final OntologyTerm ontologyTerm = createTypeByName(termClass);
            ActivityLogHelper.logUserAction("OntologyAddTermBuilder.createTerm", ontologyTerm.getTypeName());

            final String termName = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Ontology Term:\n",
                    "Adding to " + parentNode.getDisplayName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

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
                NodeChooser nodeChooser = new NodeChooser(new OntologyNode(ontology), "Choose an enumeration");
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
                    Ontology ontology = parentNode.getOntology();
                    OntologyTerm parentTerm = parentNode.getOntologyTerm();
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

        static OntologyTerm createTypeByName(Class<? extends OntologyTerm> termClass) {
            try {
                return termClass.newInstance();
            }
            catch (Exception e) {
                log.error("Could not create ontology term of type "+termClass,e);
            }
            return null;
        }
    }
}
