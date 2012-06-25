package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Tag;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/25/12
 * Time: 9:54 AM
 */
public class AnnotationBuilderDialog extends JFrame{

    private static JPanel annotationPanel = new JPanel();
    private List<OntologyElement> ontologyElements = new ArrayList<OntologyElement>();
    JComboBox comboBox;
    private static AnnotationBuilderDialog  dialog = new AnnotationBuilderDialog();

    private AnnotationBuilderDialog() {
        super("Annotation Builder");
        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        List<OntologyElement> list = ModelMgr.getModelMgr().getCurrentOntology().getChildren();
        iterateAndAddRows(list, 0);

        final JButton annotateButton = new JButton("Annotate");
        annotateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotateButton.setEnabled(false);
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {

                        final List<RootedEntity> selectedEntities = ((IconDemoPanel)SessionMgr.getBrowser().getActiveViewer()).getSelectedEntities();
                        for(RootedEntity rootedEntity: selectedEntities){
                            if(null!=rootedEntity){
                                List<Entity> annotations = ModelMgr.getModelMgr().getAnnotationsForEntity(rootedEntity.getEntity().getId());
                                if(null!=annotations){
                                    for(Entity annotation:annotations){

                                        for(OntologyElement element: ontologyElements){
                                            if(annotation.getName().contains(element.getName()) && null!=annotation.getId()){
                                                ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
                                            }
                                        }
                                    }
                                }
                                int i = 0;
                                for(OntologyElement element: ontologyElements){
                                    AnnotateAction action = new AnnotateAction();
                                    action.init(element);
//                                    action.doAnnotation(rootedEntity.getEntity(),element,model.getValueAt(i,1));
                                    i++;
                                }
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        annotateButton.setEnabled(true);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Adding annotations", "", 0, 100));
                worker.execute();
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(annotateButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        annotationPanel.add(buttonPanel);

        createAndShowGUI();

    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createAndShowGUI() {
        //Create and set up the window.

        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(SessionMgr.getBrowser().getIconImage());
        this.setAlwaysOnTop(true);
        //Create and set up the content pane.

        annotationPanel.setOpaque(true); //content panes must be opaque
        this.setContentPane(annotationPanel);

        //Display the window.
        this.pack();
        this.setVisible(true);
    }

    private void iterateAndAddRows(List<OntologyElement> list, int recursionLevel){
        for(OntologyElement element:list){
            if(element.getType() instanceof Tag){
                ontologyElements.add(element);
            }

            if(null!=element.getChildren()){
                iterateAndAddRows(element.getChildren(), recursionLevel+1);
            }
        }
        comboBox = new JComboBox((Vector<?>) ontologyElements);
    }

    public static AnnotationBuilderDialog getDialog(){
        return dialog;
    }


}

