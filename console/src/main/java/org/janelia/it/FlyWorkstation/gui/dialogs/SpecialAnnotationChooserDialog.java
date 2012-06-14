package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
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

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/12/12
 * Time: 10:28 AM
 */


public class SpecialAnnotationChooserDialog extends JDialog{

    private static JPanel annotationPanel = new JPanel();
    private List<OntologyElement> ontologyElements = new ArrayList<OntologyElement>();
    private DefaultTableModel model;
    JComboBox comboBox;
    public static JFrame frame = new JFrame("Special Annotation Session");
    private static SpecialAnnotationChooserDialog dialog = new SpecialAnnotationChooserDialog();

    private SpecialAnnotationChooserDialog() {
        super( SessionMgr.getBrowser(),"Special Annotation Session", false);
        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        model = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int column) {
                if(0 == column){
                    return false;
                }
                else{
                    return true;
                }
            }
        };
        model.addColumn("Annotation");
        model.addColumn("Enumeration");

        JTable table = new JTable(model);
        TableColumn comboColumn = table.getColumnModel().getColumn(1);

        List<OntologyElement> list = ModelMgr.getModelMgr().getCurrentOntology().getChildren();
        iterateAndAddRows(list);
        comboColumn.setCellEditor(new DefaultCellEditor(comboBox));
        for(int i = 0; i < model.getRowCount(); i++){
            model.setValueAt("undetermined",i,1);
        }

        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrObserver() {
            @Override
            public void ontologySelected(long rootId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void ontologyChanged(long rootId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void entitySelected(String category, String entityId, boolean clearAll) {
                if(!category.equals("outline") && clearAll){
                    List<OntologyAnnotation> annotations = ((IconDemoPanel)SessionMgr.getBrowser().getActiveViewer()).getAnnotations().getAnnotations();
                    int i = 0;
                    for(OntologyAnnotation annotation:annotations){
                        if(model.getValueAt(i,0).equals(annotation.getKeyString())){
                            model.setValueAt(annotation.getValueString(),i,1);
                        }
                        i++;
                    }
                }
            }

            @Override
            public void entityDeselected(String category, String entityId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void entityChanged(long entityId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void entityRemoved(long entityId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void entityDataRemoved(long entityDataId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void entityViewRequested(long entityId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void annotationsChanged(long entityId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void sessionSelected(long sessionId) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void sessionDeselected() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        table.setFillsViewportHeight(true);
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        JButton annotateButton = new JButton("Annotate");
        annotateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                                    action.doAnnotation(rootedEntity.getEntity(),element,model.getValueAt(i,1));
                                    i++;
                                }
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {

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
        //Add the scroll pane to this panel.
        annotationPanel.add(scrollPane);
        JPanel buttonPanel = new JPanel(new GridLayout(1,0));
        buttonPanel.add(annotateButton);
        annotationPanel.add(buttonPanel);

        createAndShowGUI();

    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.

        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setResizable(false);
        frame.setIconImage(SessionMgr.getBrowser().getIconImage());
        frame.setAlwaysOnTop(true);
        //Create and set up the content pane.

        annotationPanel.setOpaque(true); //content panes must be opaque
        frame.setContentPane(annotationPanel);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private void iterateAndAddRows(List<OntologyElement> list){

        for(OntologyElement element:list){
            if(element.getType() instanceof EnumText){
                model.addRow(new Object[]{element.getName()});
                OntologyElement valueEnum = ((EnumText) element.getType()).getValueEnum();

                if (valueEnum==null) {
                    Exception error = new Exception(element.getName()+" has no supporting enumeration.");
                    SessionMgr.getSessionMgr().handleException(error);
                    return;
                }

                List<OntologyElement> children = valueEnum.getChildren();

                int i = 0;
                Object[] selectionValues = new Object[children.size()];
                for(OntologyElement child : children) {
                    selectionValues[i++] = child;
                }

                comboBox = new JComboBox(selectionValues);
                ontologyElements.add(element);
            }

            if(null!=element.getChildren()){
                iterateAndAddRows(element.getChildren());
            }
        }
    }


}
