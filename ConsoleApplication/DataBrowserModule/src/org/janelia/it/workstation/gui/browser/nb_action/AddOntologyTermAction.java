package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Custom;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.Tag;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.gui.support.NodeChooser;
import org.janelia.it.workstation.gui.browser.nodes.OntologyNode;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: rename Enum term to something else that doesn't conflict with java.lang
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddOntologyTermAction extends NodePresenterAction {

    private final static Logger log = LoggerFactory.getLogger(AddOntologyTermAction.class);
    
    protected final Component mainFrame = SessionMgr.getMainFrame();

    private final static AddOntologyTermAction singleton = new AddOntologyTermAction();
    public static AddOntologyTermAction get() {
        return singleton;
    }
    
    private AddOntologyTermAction() {
    }
    
    @Override
    public synchronized JMenuItem getPopupPresenter() {

        List<Node> selectedNodes = getSelectedNodes();
        
        assert !selectedNodes.isEmpty() : "No nodes are selected";
        
        JMenu addMenuPopup = new JMenu("Add...");
        
        if (selectedNodes.size()>1) {
            addMenuPopup.setEnabled(false);
            return addMenuPopup;
        }
        
        Node selectedNode = selectedNodes.get(0);
        final OntologyTermNode termNode = (OntologyTermNode)selectedNode;
        
        final Ontology ontology = termNode.getOntology();
        final OntologyTerm term = termNode.getOntologyTerm();
        
        if (term instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
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

            Class[] nodeTypes = {Category.class, Tag.class, org.janelia.it.jacs.model.domain.ontology.Enum.class, EnumText.class, Interval.class, Text.class, Custom.class};
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
                    SessionMgr.getSessionMgr().handleException(ex);
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
        ActivityLogHelper.logUserAction("AddOntologyTermAction.createTerm", ontologyTerm.getTypeName());

        final String termName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Ontology Term:\n", 
                "Adding to " + parentNode.getDisplayName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

        if ((termName == null) || (termName.length() <= 0)) {
            return;
        }
        
        ontologyTerm.setName(termName);

        if (ontologyTerm instanceof Interval) {

            String lowerBoundStr = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Lower bound:\n", 
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);
            String upperBoundStr = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Upper bound:\n", 
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);

            try {
                Long lowerBound = new Long(lowerBoundStr);
                Long upperBound = new Long(upperBoundStr);
                ((Interval) ontologyTerm).init(lowerBound, upperBound);
            }
            catch (NumberFormatException ex) {
                SessionMgr.getSessionMgr().handleException(ex);
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
            
            if (!(chosenTerm instanceof org.janelia.it.jacs.model.domain.ontology.Enum)) {
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "You must choosen an enumeration", "Error", JOptionPane.ERROR_MESSAGE);
            }

            try {
                ((EnumText) ontologyTerm).init(chosenTerm.getId());
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
        
        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                Ontology ontology = parentNode.getOntology();
                OntologyTerm parentTerm = parentNode.getOntologyTerm();
                model.addOntologyTerm(ontology.getId(), parentTerm.getId(), ontologyTerm);
            }
            
            @Override
            protected void hadSuccess() {
                // UI updated by events
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
    }
    
    public static OntologyTerm createTypeByName(Class<? extends OntologyTerm> termClass) {
        try {
            return termClass.newInstance();
        }
        catch (Exception e) {
            log.error("Could not create ontology term of type "+termClass,e);
        }
        return null;
    }
}
;