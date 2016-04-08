package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for viewing all the fly line releases that a user has access to.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LineReleaseListDialog extends ModalDialog implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(LineReleaseListDialog.class);
    
    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd");

    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_RELEASE_DATE = "Release Date";
    private static final String COLUMN_DATA_SETS = "Data Sets";
    private static final String COLUMN_SAGE_SYNC = "SAGE Sync";

    private final JLabel loadingLabel;
    private final JPanel mainPanel;
    private final DynamicTable dynamicTable;
    private final LineReleaseDialog releaseDialog;
    private List<LineRelease> releases;

    public LineReleaseListDialog() {

        setTitle("My Fly Line Releases");

        releaseDialog = new LineReleaseDialog(this);

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(loadingLabel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        dynamicTable = new DynamicTable(true, false) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                LineRelease release = (LineRelease) userObject;
                if (release != null) {
                    if (COLUMN_NAME.equals(column.getName())) {
                        return release.getName();
                    }
                    else if (COLUMN_RELEASE_DATE.equals(column.getName())) {
                        Date date = release.getReleaseDate();
                        return date == null ? null : df.format(date);
                    }
                    else if (COLUMN_DATA_SETS.equals(column.getName())) {
                        List<String> value = release.getDataSets();
                        return value == null ? null : Task.csvStringFromCollection(value).replaceAll(",", ", ");
                    }
                    else if (COLUMN_SAGE_SYNC.equals(column.getName())) {
                        return release.isSageSync();
                    }
                    else {
                        throw new IllegalStateException("No such column: "+column.getName());
                    }
                }
                return null;
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                JPopupMenu menu = super.createPopupMenu(e);

                if (menu != null) {
                    JTable table = getTable();
                    ListSelectionModel lsm = table.getSelectionModel();
                    if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                        return menu;
                    }

                    final LineRelease release = (LineRelease) getRows().get(table.getSelectedRow()).getUserObject();

                    JMenuItem editItem = new JMenuItem("  Edit");
                    editItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            releaseDialog.showForRelease(release);
                        }
                    });
                    menu.add(editItem);

                    JMenuItem deleteItem = new JMenuItem("  Delete");
                    deleteItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            int result = JOptionPane.showConfirmDialog(LineReleaseListDialog.this, "Are you sure you want to delete release '"
                                    + release.getName() + "'? This will not remove anything already published to the web.",
                                    "Delete Release", JOptionPane.OK_CANCEL_OPTION);
                            if (result != 0) {
                                return;
                            }

                            Utils.setWaitingCursor(LineReleaseListDialog.this);

                            SimpleWorker worker = new SimpleWorker() {

                                @Override
                                protected void doStuff() throws Exception {
                                    DomainMgr.getDomainMgr().getModel().remove(release);
                                }

                                @Override
                                protected void hadSuccess() {
                                    Utils.setDefaultCursor(LineReleaseListDialog.this);
                                    loadReleases();
                                }

                                @Override
                                protected void hadError(Throwable error) {
                                    SessionMgr.getSessionMgr().handleException(error);
                                    Utils.setDefaultCursor(LineReleaseListDialog.this);
                                    loadReleases();
                                }
                            };
                            worker.execute();
                        }
                    });
                    menu.add(deleteItem);
                }

                return menu;
            }

            @Override
            protected void rowDoubleClicked(int row) {
                final LineRelease release = (LineRelease) getRows().get(row).getUserObject();
                releaseDialog.showForRelease(release);
            }

            @Override
            public Class<?> getColumnClass(int column) {
                DynamicColumn dc = getColumns().get(column);
                if (dc.getName().equals(COLUMN_SAGE_SYNC)) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }

            @Override
            protected void valueChanged(DynamicColumn dc, int row, Object data) {
                if (dc.getName().equals(COLUMN_SAGE_SYNC)) {
                    final Boolean selected = data == null ? Boolean.FALSE : (Boolean) data;
                    DynamicRow dr = getRows().get(row);
                    final LineRelease release = (LineRelease) dr.getUserObject();
                    SimpleWorker worker = new SimpleWorker() {

                        @Override
                        protected void doStuff() throws Exception {
                            release.setSageSync(selected);
                            DomainMgr.getDomainMgr().getModel().update(release);
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
            }
        };

        dynamicTable.addColumn(COLUMN_NAME);
        dynamicTable.addColumn(COLUMN_RELEASE_DATE);
        dynamicTable.addColumn(COLUMN_DATA_SETS);
        dynamicTable.addColumn(COLUMN_SAGE_SYNC).setEditable(true);

        JButton addButton = new JButton("Add new");
        addButton.setToolTipText("Add a new fly line release definition");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                releaseDialog.showForNewRelease();
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close this dialog");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(addButton);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showDialog() {

        loadReleases();

        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.4), (int) (mainFrame.getHeight() * 0.4)));

        // Show dialog and wait
        packAndShow();
    }

    private void loadReleases() {

        mainPanel.removeAll();
        mainPanel.add(loadingLabel, BorderLayout.CENTER);

        this.releases = new ArrayList<>();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (LineRelease releaseEntity : DomainMgr.getDomainMgr().getModel().getLineReleases()) {
                    releases.add(releaseEntity);
                }
            }

            @Override
            protected void hadSuccess() {

                // Update the attribute table
                dynamicTable.removeAllRows();
                for (LineRelease releaseEntity : releases) {
                    dynamicTable.addRow(releaseEntity);
                }

                dynamicTable.updateTableModel();
                mainPanel.removeAll();
                mainPanel.add(dynamicTable, BorderLayout.CENTER);
                mainPanel.revalidate();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                mainPanel.removeAll();
                mainPanel.add(dynamicTable, BorderLayout.CENTER);
                mainPanel.revalidate();
            }
        };
        worker.execute();
    }

    public void refresh() {
        loadReleases();
    }

    public void totalRefresh() {
        throw new UnsupportedOperationException();
    }

    List<LineRelease> getReleases() {
        return releases;
    }

}
