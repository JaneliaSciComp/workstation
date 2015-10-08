package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * A dialog for viewing all the fly line releases that a user has access to.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleaseListDialog extends ModalDialog implements Refreshable {

    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
    
    private final JLabel loadingLabel;
    private final JPanel mainPanel;
    private final DynamicTable dynamicTable;
    private final FlyLineReleaseDialog releaseDialog;
    private List<Entity> releases;

    public FlyLineReleaseListDialog() {

        setTitle("My Fly Line Releases");

        releaseDialog = new FlyLineReleaseDialog(this);

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
                Entity releaseEntity = (Entity) userObject;
                if (releaseEntity != null) {
                    if ("Name".equals(column.getName())) {
                        return releaseEntity.getName();
                    }
                    String value = releaseEntity.getValueByAttributeName(column.getName());
                    if (value != null) {
                        if (EntityConstants.ATTRIBUTE_RELEASE_DATE.equals(column.getName())) {
                            return df.format(ISO8601Utils.parse(value));
                        }
                        else if (EntityConstants.ATTRIBUTE_DATA_SETS.equals(column.getName())) {
                            return value.replaceAll(",", ", ");
                        }
                        else {
                            return value;
                        }
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

                    final Entity releaseEntity = (Entity) getRows().get(table.getSelectedRow()).getUserObject();

                    JMenuItem editItem = new JMenuItem("  Edit");
                    editItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            releaseDialog.showForRelease(releaseEntity);
                        }
                    });
                    menu.add(editItem);

                    JMenuItem deleteItem = new JMenuItem("  Delete");
                    deleteItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            int result = JOptionPane.showConfirmDialog(FlyLineReleaseListDialog.this, "Are you sure you want to delete release '" + 
                                    releaseEntity.getName() + "'? This will not remove anything already published to the web.",
                                    "Delete Release", JOptionPane.OK_CANCEL_OPTION);
                            if (result != 0) return;
                            
                            Utils.setWaitingCursor(FlyLineReleaseListDialog.this);

                            SimpleWorker worker = new SimpleWorker() {

                                @Override
                                protected void doStuff() throws Exception {
                                    ModelMgr.getModelMgr().deleteEntityTree(releaseEntity.getId());
                                }

                                @Override
                                protected void hadSuccess() {
                                    Utils.setDefaultCursor(FlyLineReleaseListDialog.this);
                                    loadReleases();
                                }

                                @Override
                                protected void hadError(Throwable error) {
                                    SessionMgr.getSessionMgr().handleException(error);
                                    Utils.setDefaultCursor(FlyLineReleaseListDialog.this);
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
                final Entity dataSetEntity = (Entity) getRows().get(row).getUserObject();
                releaseDialog.showForRelease(dataSetEntity);
            }
        };

        dynamicTable.addColumn("Name");
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_RELEASE_DATE);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_DATA_SETS);

        JButton addButton = new JButton("Add new");
        addButton.setToolTipText("Add a new fly line release definition");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                releaseDialog.showForNewDataSet();
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
        
        this.releases = new ArrayList<Entity>();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (Entity releaseEntity : ModelMgr.getModelMgr().getFlyLineReleases()) {
                    releases.add(releaseEntity);
                }
            }

            @Override
            protected void hadSuccess() {

                // Update the attribute table
                dynamicTable.removeAllRows();
                for (Entity dataSetEntity : releases) {
                    dynamicTable.addRow(dataSetEntity);
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

    List<Entity> getReleases() {
        return releases;
    }
    
}
