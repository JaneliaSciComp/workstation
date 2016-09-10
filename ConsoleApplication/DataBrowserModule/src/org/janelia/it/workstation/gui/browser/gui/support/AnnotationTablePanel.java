package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImagesPanel;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicTable;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.options.OptionConstants;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that shows a bunch of annotations in a table.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTablePanel extends JPanel implements AnnotationView {

    private static final Logger log = LoggerFactory.getLogger(AnnotationTablePanel.class);
    
    private static final String COLUMN_KEY = "Annotation Term";
    private static final String COLUMN_VALUE = "Annotation Value";

    private DynamicTable dynamicTable;
    private JLabel summaryLabel;
    private List<Annotation> annotations = new ArrayList<>();

    public AnnotationTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        refresh();
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(List<Annotation> annotations) {
        if (annotations == null) {
            this.annotations = new ArrayList<>();
        }
        else {
            this.annotations = annotations;
        }
        refresh();
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        annotations.remove(annotation);
        refresh();
    }

    @Override
    public void addAnnotation(Annotation annotation) {
        annotations.add(annotation);
        refresh();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
        if (preferredSize.height == ImagesPanel.MIN_TABLE_HEIGHT) {
            removeAll();
            add(summaryLabel, BorderLayout.CENTER);
        }
        else {
            removeAll();
            add(dynamicTable, BorderLayout.CENTER);
        }
    }

    private void refresh() {

        summaryLabel = new JLabel(annotations.size() + " annotation" + (annotations.size() > 1 ? "s" : ""));
        summaryLabel.setOpaque(false);
        summaryLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        summaryLabel.addMouseListener(new MouseHandler() {
            @Override
            protected void doubleLeftClicked(MouseEvent e) {
                SessionMgr.getSessionMgr().setModelProperty(
                        OptionConstants.ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
                e.consume();
            }

        });

        dynamicTable = new DynamicTable(false, true) {

            @Override
            public Object getValue(Object userObject, DynamicColumn column) {

                Annotation annotation = (Annotation) userObject;
                if (null != annotation) {
                    if (column.getName().equals(COLUMN_KEY)) {
                        return annotation.getKey();
                    }
                    if (column.getName().equals(COLUMN_VALUE)) {
                        return annotation.getValue();
                    }
                }
                return null;
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {

                if (dynamicTable.getCurrentRow() == null) {
                    return null;
                }

                Object userObject = dynamicTable.getCurrentRow().getUserObject();
                Annotation annotation = (Annotation) userObject;

                return getPopupMenu(e, annotation);
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                if (col != 1) {
                    return null;
                }

                // TODO: implement custom editors for each ontology term type
                return null;
            }
        };

        // This was probably in here for some reason, but I can't think of it. I'm disabling it now, 
        // because it's annoying to have the mouse wheel disabled on long lists of annotations.
//        dynamicTable.getScrollPane().setWheelScrollingEnabled(false);
//        dynamicTable.getScrollPane().addMouseWheelListener(new MouseWheelListener() {
//            @Override
//            public void mouseWheelMoved(MouseWheelEvent e) {
//                getParent().dispatchEvent(e);
//            }
//        });

        dynamicTable.getTable().addMouseListener(new MouseForwarder(this, "DynamicTable->AnnotationTablePanel"));

        dynamicTable.addColumn(COLUMN_KEY, COLUMN_KEY, true, false, false, true);
        dynamicTable.addColumn(COLUMN_VALUE, COLUMN_VALUE, true, false, false, true);

        for (Annotation annotation : annotations) {
            dynamicTable.addRow(annotation);
        }

        dynamicTable.updateTableModel();
        removeAll();
        add(dynamicTable, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void deleteAnnotation(final Annotation toDelete) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainMgr.getDomainMgr().getModel().remove(toDelete);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation",error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    private void deleteAnnotations(final List<Annotation> toDeleteList) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (Annotation toDelete : toDeleteList) {
                    DomainMgr.getDomainMgr().getModel().remove(toDelete);
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation",error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotations", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    // TODO: use the AnnotationContextMenu
    private JPopupMenu getPopupMenu(final MouseEvent e, final Annotation annotation) {

        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JTable target = (JTable) e.getSource();
        if (target.getSelectedRow() < 0) {
            return null;
        }

        JTable table = dynamicTable.getTable();

        ListSelectionModel lsm = table.getSelectionModel();
        if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {

            JMenuItem titleItem = new JMenuItem(annotation.getName());
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);

            JMenuItem copyMenuItem = new JMenuItem("  Copy to Clipboard");
            copyMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Transferable t = new StringSelection(annotation.getName());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                }
            });
            popupMenu.add(copyMenuItem);

            if (ClientDomainUtils.hasWriteAccess(annotation)) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotation");
                deleteItem.addActionListener(new ActionListener() {
                @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteAnnotation(annotation);
                    }
                });
                popupMenu.add(deleteItem);
            }

            if (annotation.getValue() != null) {
                JMenuItem editItem = new JMenuItem("  Edit Annotation");
                editItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                        dialog.setAnnotationValue(annotation.getValue());
                        dialog.setVisible(true);
                        String value = dialog.getAnnotationValue();
                        if (null == value) {
                            value = "";
                        }
                        annotation.setValue(value);
                        String tmpName = annotation.getName();
                        String namePrefix = tmpName.substring(0, tmpName.indexOf("=") + 2);
                        annotation.setName(namePrefix + value);
                        try {
                            model.save(annotation);
                        }
                        catch (Exception e1) {
                            log.error("Error editing annotation",e1);
                            SessionMgr.getSessionMgr().handleException(e1);
                        }
                    }
                });

                popupMenu.add(editItem);
            }

            JMenuItem detailsItem = new JMenuItem("  View Details");
            detailsItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // TODO: port this
                    //OntologyOutline.viewAnnotationDetails(annotation);
                }
            });
            popupMenu.add(detailsItem);

        }
        else {
            JMenuItem titleMenuItem = new JMenuItem("(Multiple Items Selected)");
            titleMenuItem.setEnabled(false);
            popupMenu.add(titleMenuItem);

            final List<Annotation> toDeleteList = new ArrayList<>();
            for (int i : table.getSelectedRows()) {
                int mi = table.convertRowIndexToModel(i);
                if (ClientDomainUtils.hasWriteAccess(annotation)) {
                    toDeleteList.add(annotations.get(mi));
                }
            }

            if (!toDeleteList.isEmpty()) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotations");
                deleteItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteAnnotations(toDeleteList);
                    }
                });
                popupMenu.add(deleteItem);
            }
        }

        return popupMenu;
    }

}
