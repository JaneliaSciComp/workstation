package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.OntologyElementChooser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.Enum;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

/**
 * Create a new ontology term in the current ontology. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateOntologyTermAction implements Action {
    
    private String name;
    private String className;
    
    public CreateOntologyTermAction(String className) {
        this("Add New Term", className);
    }
    
    public CreateOntologyTermAction(String name, String className) {
        this.name = name;
        this.className = className;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public void doAction() {

        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();
        final Entity ontologyRoot = ontologyOutline.getCurrentOntology();
        
        if (ontologyRoot == null) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "No ontology selected.");
        }

        final DefaultMutableTreeNode treeNode = ontologyOutline.getDynamicTree().getCurrentNode();
        final OntologyElement element = ontologyOutline.getOntologyElement(treeNode);
        final OntologyElementType childType = OntologyElementType.createTypeByName(className);

        // Add button clicked
        final String termName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Ontology Term:\n", "Adding to " + 
                ontologyOutline.getOntologyElement(treeNode).getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

        if ((termName == null) || (termName.length() <= 0)) {
            return;
        }

        if (childType instanceof Interval) {

            String lowerBoundStr = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Lower bound:\n", 
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);
            String upperBoundStr = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Upper bound:\n", 
                    "Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);

            try {
                ((Interval) childType).init(lowerBoundStr, upperBoundStr);
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
                return;
            }
        }
        else if (childType instanceof EnumText) {

            final OntologyElementChooser ontologyChooser = new OntologyElementChooser("Choose an enumeration", ontologyRoot);

            ontologyChooser.setMultipleSelection(false);
            int returnVal = ontologyChooser.showDialog(SessionMgr.getMainFrame());
            if (returnVal != OntologyElementChooser.CHOOSE_OPTION) return;
            
            List<OntologyElement> chosenElements = ontologyChooser.getChosenElements();
            if (chosenElements.size()!=1) return;
            
            OntologyElement chosenEnum = chosenElements.get(0);
            if (!(chosenEnum.getType() instanceof Enum)) {
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "You must choosen an enumeration", "Error", JOptionPane.ERROR_MESSAGE);
            }

            try {
                ((EnumText) childType).init(chosenEnum);
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
        
        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                ModelMgr.getModelMgr().createOntologyTerm(ontologyRoot.getId(), element.getId(), termName, childType, null);
            }
            
            @Override
            protected void hadSuccess() {
            }
            
            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Error creating ontology term", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        
        worker.execute();
    }
}
