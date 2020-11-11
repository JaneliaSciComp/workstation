package org.janelia.horta.nodes;

import org.janelia.workstation.controller.widgets.SimpleIcons;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * this widget displays a list of neurons in a workspace
 *
 * djo, 12/13
 */
public class ObjectMeshPanel extends JPanel {
    private JTable objectMeshTable;
    private ObjectMeshTableModel objectMeshTableModel;

    private NeuronManager annotationModel;
    private TmModelManager modelManager;

    private static final int NARROW_COLUNN_WIDTH = 50;
    private JLabel meshLabel;

    public ObjectMeshPanel() {
        setupUI();
    }
    
    private void setupUI() {
        annotationModel = NeuronManager.getInstance();
        modelManager = TmModelManager.getInstance();
        setLayout(new BorderLayout());

        // list of meshes
        meshLabel =new JLabel("Object Meshes", JLabel.LEADING);
        add(meshLabel, BorderLayout.NORTH);

        // neuron table
        objectMeshTableModel = new ObjectMeshTableModel();
        objectMeshTableModel.setAnnotationModel(annotationModel);
        objectMeshTable = new JTable(objectMeshTableModel){
            // likewise:
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {};
            }
        };

        // we want the name column to be the only one that expands
        objectMeshTable.getColumnModel().getColumn(objectMeshTableModel.COLUMN_NAME).setPreferredWidth(125);
        objectMeshTable.getColumnModel().getColumn(objectMeshTableModel.COLUMN_PATH).setPreferredWidth(100);
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        objectMeshTable.getColumnModel().getColumn(objectMeshTableModel.COLUMN_NAME).setCellRenderer(leftRenderer);

        // fixed width, narrow columns:
        objectMeshTable.getColumnModel().getColumn(objectMeshTableModel.COLUMN_VISIBILITY).setPreferredWidth(NARROW_COLUNN_WIDTH);
        objectMeshTable.getColumnModel().getColumn(objectMeshTableModel.COLUMN_VISIBILITY).setMaxWidth(NARROW_COLUNN_WIDTH);

        objectMeshTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        objectMeshTable.addMouseListener(new MouseHandler() {

            private void selectMesh(TmObjectMesh mesh) {
                List<TmObjectMesh> meshList = new ArrayList<>();
                meshList.add(mesh);
                SelectionMeshEvent event = new SelectionMeshEvent(meshList, true, false);
                ViewerEventBus.postEvent(event);
            }

            @Override
            protected void popupTriggered(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = objectMeshTable.convertRowIndexToModel(viewRow);
                    TmObjectMesh selectedMesh = objectMeshTableModel.getValueAt(modelRow);
                    // select neuron
                    selectMesh(selectedMesh);

                    // show popup menu for the selected neuron
                    JPopupMenu popupMenu = createPopupMenu(selectedMesh);
                    if (popupMenu!=null) {
                        popupMenu.show(me.getComponent(), me.getX(), me.getY());
                    }
                    me.consume();
                }
            }

            @Override
            protected void singleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = objectMeshTable.convertRowIndexToModel(viewRow);
                    TmObjectMesh selectedMesh = objectMeshTableModel.getValueAt(modelRow);
                    // which column?
                    int viewColumn = table.columnAtPoint(me.getPoint());
                    int modelColumn = objectMeshTable.convertColumnIndexToModel(viewColumn);
                    if (modelColumn == objectMeshTableModel.COLUMN_NAME) {
                        // single click name, select neuron
                        selectMesh(selectedMesh);
                    } else if (modelColumn == objectMeshTableModel.COLUMN_VISIBILITY) {
                        // single click visibility = toggle visibility
                        boolean vis = modelManager.getCurrentView().toggleHidden(selectedMesh);
                        objectMeshTableModel.updateMeshes(selectedMesh);
                        MeshVisibilityEvent updateEvent = new MeshVisibilityEvent(selectedMesh, vis);
                        ViewerEventBus.postEvent(updateEvent);
                    }
                }
                me.consume();
            }

            @Override
            protected void doubleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                me.consume();
            }
        });

        JScrollPane scrollPane = new JScrollPane(objectMeshTable);
        objectMeshTable.setFillsViewportHeight(true);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPopupMenu createPopupMenu (TmObjectMesh mesh) {
        JPopupMenu menu = new JPopupMenu();
        Action deleteObjectMeshAction = new AbstractAction("Delete Object Mesh") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SimpleWorker updater = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        modelManager.getCurrentWorkspace().removeObjectMesh(mesh);
                        TmViewState viewState = TmModelManager.getInstance().getCurrentView();
                        if (viewState.isHidden(mesh.getName())) {
                            viewState.removeMeshFromHidden(mesh.getName());
                        }
                        objectMeshTableModel.deleteMesh(mesh.getName());
                        modelManager.saveWorkspace(modelManager.getCurrentWorkspace());
                        MeshDeleteEvent event = new MeshDeleteEvent(mesh);
                        ViewerEventBus.postEvent(event);
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing to see here
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(
                                "Error deleting object mesh " + mesh.getName(),
                                error);
                    }
                };
                updater.execute();
            }
        };

        Action changeNameAction = new AbstractAction("Change Object Mesh Name") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String oldName = mesh.getName();
                String newName = JOptionPane.showInputDialog(null,
                        "Enter new mesh name '" + oldName + "':",
                        oldName);
                if (newName == null)
                    return; // User pressed "Cancel"
                if (newName.length() < 1)
                    return; // We don't name things with the empty string
                if (newName.equals(oldName))
                    return; // Nothing changed

                SimpleWorker changer = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        mesh.setName(newName);
                        modelManager.saveWorkspace(modelManager.getCurrentWorkspace());
                        MeshUpdateEvent event = new MeshUpdateEvent(mesh, oldName, MeshUpdateEvent.PROPERTY.NAME);
                        ViewerEventBus.postEvent(event);
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing to see here
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(
                                "Error updated object mesh " + mesh.getName(),
                                error);
                    }
                };
                changer.execute();
            }
        };

        Action changeMeshPathAction = new AbstractAction("Change Path To Object Mesh") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String oldPath = mesh.getPathToObjFile();
                String newPath = JOptionPane.showInputDialog(null,
                        "Enter path for mesh '" + oldPath + "':",
                        oldPath);
                if (newPath == null)
                    return; // User pressed "Cancel"
                if (newPath.length() < 1)
                    return; // We don't name things with the empty string
                if (newPath.equals(oldPath))
                    return; // Nothing changed

                SimpleWorker changer = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        mesh.setPathToObjFile(newPath);
                        modelManager.saveWorkspace(modelManager.getCurrentWorkspace());
                    }

                    @Override
                    protected void hadSuccess() {
                        // nothing to see here
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(
                                "Error updated object mesh " + mesh.getName(),
                                error);
                    }
                };
                changer.execute();
            }
        };
        menu.add(new JMenuItem(deleteObjectMeshAction));
        menu.add(new JMenuItem(changeNameAction));
        menu.add(new JMenuItem(changeMeshPathAction));
        return menu;
    }

    /**
     * populate the UI with info from the input workspace
     */
    public void loadWorkspace(TmWorkspace workspace) {
        objectMeshTableModel.clear();
        if (workspace != null) {
            objectMeshTableModel.addMeshes(workspace.getObjectMeshList());
        }
    }

    /**
     * add a new Neuron into the list
     */
    public void addObjectMeshToTable(TmObjectMesh objectMesh) {
        List meshList = new ArrayList();
        meshList.add(objectMesh);
        objectMeshTableModel.addMeshes(meshList);
    }
}

class ObjectMeshTableModel extends AbstractTableModel {
    // note: creation date column and owner name column will be hidden
    // column names, tooltips, and associated methods should probably be static
    private String[] columnNames = {"Name", "V", "Path"};
    private String[] columnTooltips = {"Name", "Visibility", "Path"};

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_VISIBILITY = 1;
    public static final int COLUMN_PATH = 2;

    private ArrayList<TmObjectMesh> meshes = new ArrayList<>();

    // need this to retrieve colors, tags
    private NeuronManager annotationModel;

    // icons
    private ImageIcon visibleIcon;
    private ImageIcon invisibleIcon;
    private TmModelManager modelManager;

    public void setAnnotationModel(NeuronManager annotationModel) {
        this.annotationModel = annotationModel;
        modelManager = TmModelManager.getInstance();
        // set up icons; yes, we have multiple storage places for icons...
        visibleIcon = SimpleIcons.getIcon("eye.png");
        invisibleIcon = SimpleIcons.getIcon("closed_eye.png");
    }

    public void clear() {
        meshes.clear();
        fireTableDataChanged();
    }

    public void addMeshes(Collection<TmObjectMesh> meshList) {
        meshes.addAll(meshList);
        fireTableDataChanged();
    }

    public void deleteMesh(String name) {
        TmObjectMesh foundMesh = null;
        for (TmObjectMesh objectMesh: meshes) {
            if (objectMesh.getName().equals(name)) {
                foundMesh = objectMesh;
                break;
            }
        }
        if (foundMesh != null) {
            meshes.remove(foundMesh);
            fireTableDataChanged();
        }
    }

    public void updateMeshes (TmObjectMesh mesh) {
        fireTableDataChanged();
    }

    // boilerplate stuff
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return meshes.size();
    }

    // needed to get color to work right; make sure classes match what getValueAt() returns!
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case COLUMN_NAME:
                // neuron
                return Integer.class;
            case COLUMN_VISIBILITY:
                // visibility; we use an image to indicate that:
                return ImageIcon.class;
            case COLUMN_PATH:
                return String.class;
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
        }
    }

    public TmObjectMesh getValueAt(int row) {
        return meshes.get(row);
    }

    public Object getValueAt(int row, int column) {
        TmObjectMesh targetMesh = meshes.get(row);
        switch (column) {
            case COLUMN_NAME:
                // neuron name
                return targetMesh.getName();
            case COLUMN_VISIBILITY:
                if (!modelManager.getCurrentView().isHidden(targetMesh.getName())) {
                    return visibleIcon;
                } else {
                    return invisibleIcon;
                }
            case COLUMN_PATH:
                return targetMesh.getPathToObjFile();
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
        }
    }

}