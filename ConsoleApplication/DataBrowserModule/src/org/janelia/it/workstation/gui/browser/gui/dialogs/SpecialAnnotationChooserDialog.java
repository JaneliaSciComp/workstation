package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/12/12
 * Time: 10:28 AM
 */
public class SpecialAnnotationChooserDialog extends JFrame{

    private static JPanel annotationPanel = new JPanel();
    private Ontology ontology;
    private List<OntologyTerm> OntologyTerms = new ArrayList<>();
    private DefaultTableModel model;
    private JComboBox comboBox;
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

        Long ontologyId = StateMgr.getStateMgr().getCurrentOntologyId();
        this.ontology = DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, ontologyId);

        iterateAndAddRows(ontology.getTerms(), 0);

        comboColumn.setCellEditor(new DefaultCellEditor(comboBox));
        for(int i = 0; i < model.getRowCount(); i++){
            model.setValueAt("undetermined",i,1);
        }

        tableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            OntologyTerm child = OntologyTerms.get(table.getSelectedRow());
            OntologyTerm parent = child.getParent();
            if(parent instanceof EnumText &&
                    !model.getValueAt(table.getSelectedRow(),1).equals(model.getValueAt(OntologyTerms.indexOf(parent), 1))){
                model.setValueAt( model.getValueAt(table.getSelectedRow(), 1),
                        OntologyTerms.indexOf(parent), 1);
                }
            }
        };
        model.addTableModelListener(tableModelListener);

        // TODO: port this to use a selection model
//        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
//
//            @Override
//            public void entitySelected(String category, String entityId, boolean clearAll) {
//                try {
//                    model.removeTableModelListener(tableModelListener);
//                    String[] splitResult = entityId.split("/e_");
//                    String splitlast = splitResult[splitResult.length-1];
//                    SpecialAnnotationChooserDialog.this.setTitle(ModelMgr.getModelMgr().getEntityById(splitlast).getName());
//
//                    Long iD = ModelMgr.getModelMgr().getEntityById(splitlast).getId();
//                    List<Entity> annotations = ModelMgr.getModelMgr().getAnnotationsForEntity(iD);
//
//                    if(!category.equals("outline") && clearAll && annotations.size()!=0){
//
//                        List<OntologyAnnotation> annotations1 = ((IconDemoPanel)SessionMgr.getBrowser().getViewerManager().getActiveViewer(IconDemoPanel.class)).getAnnotations().getAnnotations();
//                        int i = 0;
//                        for(OntologyAnnotation annotation:annotations1){
//                            if(model.getValueAt(i,0).toString().trim().equals(annotation.getKeyString())){
//                                model.setValueAt(annotation.getValueString(),i,1);
//                            }
//                            i++;
//                        }
//                    }
//                    else{
//                        for(int i = 0; i < model.getRowCount(); i++){
//                            model.setValueAt("undetermined",i,1);
//                        }
//                    }
//                    model.addTableModelListener(tableModelListener);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });

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

                        // TODO: port this to use the selection model
                        List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
                        for(Reference selectedId : selectedIds){
                            if(null!=selectedId){
                                List<Annotation> annotations = DomainMgr.getDomainMgr().getModel().getAnnotations(Arrays.asList(selectedId));
                                if(null!=annotations){
                                    for(Annotation annotation:annotations){
                                        for(OntologyTerm element: OntologyTerms){
                                            if(annotation.getName().contains(element.getName()) && null!=annotation.getId()){
                                                DomainMgr.getDomainMgr().getModel().remove(annotation);
                                            }
                                        }
                                    }
                                }
                                int i = 0;
                                DomainObject target = DomainMgr.getDomainMgr().getModel().getDomainObject(selectedId);
                                for(OntologyTerm element: OntologyTerms){
                                    ApplyAnnotationAction action = ApplyAnnotationAction.get();
                                    action.doAnnotation(target, element, model.getValueAt(i,1));
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

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Adding annotations", "", 0, 100));
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

    private void iterateAndAddRows(List<OntologyTerm> list, int recursionLevel){
        StringBuilder tabString = new StringBuilder("");
        for(int i = 0; i<recursionLevel; i++){
            tabString.append("   ");
        }


        for(OntologyTerm element : list){
            
            if(element instanceof EnumText){
                model.addRow(new Object[]{tabString + element.getName()});
                Long valueEnumId = ((EnumText) element).getValueEnumId();
                OntologyTerm valueEnum = DomainMgr.getDomainMgr().getModel().getOntologyTermByReference(new OntologyTermReference(ontology.getId(), valueEnumId));

                if (valueEnum==null) {
                    Exception error = new Exception(element.getName()+" has no supporting enumeration.");
                    SessionMgr.getSessionMgr().handleException(error);
                    return;
                }

                List<OntologyTerm> children = valueEnum.getTerms();

                int i = 0;
                Object[] selectionValues = new Object[children.size()];
                for(OntologyTerm child : children) {
                    selectionValues[i++] = child;
                }

                comboBox = new JComboBox(selectionValues);
                OntologyTerms.add(element);

            }

            if(null!=element.getTerms()){
                iterateAndAddRows(element.getTerms(), recursionLevel+1);
            }
        }

    }

    public static SpecialAnnotationChooserDialog getDialog(){
        return dialog;
    }


}
