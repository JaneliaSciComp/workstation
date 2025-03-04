package org.janelia.workstation.admin;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.domain.tiledMicroscope.TmWorkspaceInfo;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class DatabaseCleanupPanel extends JPanel implements Refreshable {
    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupPanel.class);
    private final AdministrationTopComponent parent;
    private JTable workspaceTable;
    private WorkspaceTableModel tableModel;

    public DatabaseCleanupPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        refresh();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        removeAll();

        JPanel titlePanel = new TitlePanel("Manage Workspaces", "Return To Top Menu",
                event -> refresh(),
                event -> returnHome());
        add(titlePanel, BorderLayout.NORTH);

        tableModel = new WorkspaceTableModel();
        workspaceTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(workspaceTable);
        add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Load Largest Workspaces");
        refreshButton.addActionListener(e -> loadLargestWorkspaces());

        JButton deleteButton = new JButton("Delete Selected Workspaces");
        deleteButton.addActionListener(e -> deleteSelectedWorkspaces());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        revalidate();
    }

    private void loadLargestWorkspaces() {
        SimpleWorker worker = new SimpleWorker() {
            private List<TmWorkspaceInfo> workspaceResults;

            @Override
            protected void doStuff() {
                TiledMicroscopeDomainMgr domainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                workspaceResults = domainMgr.getLargestWorkspaces();
            }

            @Override
            protected void hadSuccess() {
                tableModel.clear();
                tableModel.setWorkspaces(workspaceResults);
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error loading workspaces", error);
                JOptionPane.showMessageDialog(DatabaseCleanupPanel.this,
                        "Failed to load workspaces: " + error.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        worker.execute();
    }

    private void deleteSelectedWorkspaces() {
        List<Long> selectedWorkspaces = tableModel.getSelectedWorkspaces();
        if (selectedWorkspaces.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one workspace to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected workspaces?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {

            BackgroundWorker deleter = new BackgroundWorker() {
                @Override
                public String getName() {
                    return "Deleting TmWorkspaces";
                }

                @Override
                protected void doStuff() {
                    TiledMicroscopeDomainMgr domainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                    domainMgr.removeWorkspaces(selectedWorkspaces);
                }

                @Override
                protected void hadSuccess() {
                    loadLargestWorkspaces();
                    JOptionPane.showMessageDialog(DatabaseCleanupPanel.this,
                            "Selected workspaces deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }

                @Override
                protected void hadError(Throwable error) {
                    log.error("Error deleting workspaces", error);
                    JOptionPane.showMessageDialog(DatabaseCleanupPanel.this,
                            "Failed to delete workspaces: " + error.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            };
            deleter.executeWithEvents();
        }
    }

    @Override
    public void refresh() {
        setupUI();
    }

    private void returnHome() {
        parent.viewTopMenu();
    }
}

class WorkspaceTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Select", "Name", "Owner", "Date Created", "Size"};
    private List<TmWorkspaceInfo> workspaceResults = new ArrayList<>();
    private final List<Boolean> selected = new ArrayList<>();

    public void setWorkspaces(List<TmWorkspaceInfo> workspaceResults) {
        this.workspaceResults = workspaceResults;
        selected.clear();
        for (int i = 0; i < workspaceResults.size(); i++) {
            selected.add(false);  // Initialize selection state
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return workspaceResults.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TmWorkspaceInfo workspaceInfo = workspaceResults.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return selected.get(rowIndex);  // Boolean checkbox
            case 1:
                return workspaceInfo.getWorkspaceName();
            case 2:
                return workspaceInfo.getOwnerKey();
            case 3:
                return workspaceInfo.getDateCreated();
            case 4:
                return workspaceInfo.getTotalSize();
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;  // Allow only checkboxes to be editable
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && aValue instanceof Boolean) {
            selected.set(rowIndex, (Boolean) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);  // Notify JTable of the change
        }
    }

    public void clear() {
        workspaceResults.clear();
        selected.clear();
        fireTableDataChanged(); // Notify the table UI that the data has changed
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    public List<Long> getSelectedWorkspaces() {
        List<Long> selectedWorkspaces = new ArrayList<>();
        for (int i = 0; i < workspaceResults.size(); i++) {
            if (selected.get(i)) {
                selectedWorkspaces.add(workspaceResults.get(i).getWorkspaceId());
            }
        }
        return selectedWorkspaces;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}

