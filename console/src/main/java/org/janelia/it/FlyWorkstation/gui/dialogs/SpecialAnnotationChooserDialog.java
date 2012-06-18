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

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
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


public class SpecialAnnotationChooserDialog extends JFrame{

    private static JPanel annotationPanel = new JPanel();
    private List<OntologyElement> ontologyElements = new ArrayList<OntologyElement>();
    private DefaultTableModel model;
    JComboBox comboBox;
    private static SpecialAnnotationChooserDialog  dialog = new SpecialAnnotationChooserDialog();
    private TableModelListener tableModelListener=null;

    private SpecialAnnotationChooserDialog() {
        super("Special Annotation Session");
        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        model = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int column) {
                return column!=0;
            }
        };
        model.addColumn("Annotation");
        model.addColumn("Enumeration");

        final JTable table = new JTable(model);
        TableColumn comboColumn = table.getColumnModel().getColumn(1);

        List<OntologyElement> list = ModelMgr.getModelMgr().getCurrentOntology().getChildren();
        iterateAndAddRows(list, 0);

        comboColumn.setCellEditor(new DefaultCellEditor(comboBox));
        for(int i = 0; i < model.getRowCount(); i++){
            model.setValueAt("undetermined",i,1);
        }

        tableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            OntologyElement child = ontologyElements.get(table.getSelectedRow());
            OntologyElement parent = child.getParent();
            if(parent.getType() instanceof EnumText &&
                    !model.getValueAt(table.getSelectedRow(),1).equals(model.getValueAt(ontologyElements.indexOf(parent), 1))){
                model.setValueAt( model.getValueAt(table.getSelectedRow(), 1),
                        ontologyElements.indexOf(parent), 1);
                }
            }
        };
        model.addTableModelListener(tableModelListener);

        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrObserver() {
            @Override
            public void ontologySelected(long rootId) {}

            @Override
            public void ontologyChanged(long rootId) {}

            @Override
            public void entitySelected(String category, String entityId, boolean clearAll) {
                try {
                    model.removeTableModelListener(tableModelListener);
                    String[] splitResult = entityId.split("/e_");
                    String splitlast = splitResult[splitResult.length-1];
                    SpecialAnnotationChooserDialog.this.setTitle(ModelMgr.getModelMgr().getEntityById(splitlast).getName());

                    Long iD = ModelMgr.getModelMgr().getEntityById(splitlast).getId();
                    List<Entity> annotations = ModelMgr.getModelMgr().getAnnotationsForEntity(iD);

                    if(!category.equals("outline") && clearAll && annotations.size()!=0){

                        List<OntologyAnnotation> annotations1 = ((IconDemoPanel)SessionMgr.getBrowser().getActiveViewer()).getAnnotations().getAnnotations();
                        int i = 0;
                        for(OntologyAnnotation annotation:annotations1){
                            if(model.getValueAt(i,0).toString().trim().equals(annotation.getKeyString())){
                                model.setValueAt(annotation.getValueString(),i,1);
                            }
                            i++;
                        }
                    }
                    else{
                        for(int i = 0; i < model.getRowCount(); i++){
                            model.setValueAt("undetermined",i,1);
                        }
                    }
                    model.addTableModelListener(tableModelListener);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void entityDeselected(String category, String entityId) {}

            @Override
            public void entityChanged(long entityId) {}

            @Override
            public void entityRemoved(long entityId) {}

            @Override
            public void entityDataRemoved(long entityDataId) {}

            @Override
            public void entityViewRequested(long entityId) {}

            @Override
            public void annotationsChanged(long entityId) {}

            @Override
            public void sessionSelected(long sessionId) {}

            @Override
            public void sessionDeselected() {}
        });

        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        table.setFillsViewportHeight(true);
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
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
                                    action.doAnnotation(rootedEntity.getEntity(),element,model.getValueAt(i,1));
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
                for(int i = 0; i < model.getRowCount(); i++){
                    model.setValueAt("undetermined",i,1);
                }
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SpecialAnnotationChooserDialog.this.setVisible(false);
            }
        });
        //Add the scroll pane to this panel.
        annotationPanel.add(scrollPane);
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
        StringBuilder tabString = new StringBuilder("");
        for(int i = 0; i<recursionLevel; i++){
            tabString.append("   ");
        }


        for(OntologyElement element:list){
            if(element.getType() instanceof EnumText){
                model.addRow(new Object[]{tabString + element.getName()});
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
                iterateAndAddRows(element.getChildren(), recursionLevel+1);
            }
        }

    }

    public static SpecialAnnotationChooserDialog getDialog(){
        return dialog;
    }


}
