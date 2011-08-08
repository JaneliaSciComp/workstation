/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The main frame for the dataviewer assembles all the subcomponents.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewFrame extends JFrame {

    private static final double realEstatePercent = 0.7;

    private EntityTypePane entityTypePane;
    private EntityListPane entityListPane;
    private EntityDataPane entityParentsPane;
    private EntityDataPane entityChildrenPane;
    private JPanel progressPanel;
    private SimpleWorker loadTask;

    public DataviewFrame() {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (screenSize.width * realEstatePercent), (int) (screenSize.height * realEstatePercent)));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        progressPanel.add(new JLabel("Loading..."), c);
        c.gridy = 1;
        progressPanel.add(progressBar, c);

        setLayout(new BorderLayout());

        setJMenuBar(new DataviewMenuBar(this));

        initUI();
        initData();
    }

    public EntityListPane getEntityListPane() {
        return entityListPane;
    }

    private void initUI() {

        entityTypePane = new EntityTypePane();

        entityParentsPane = new EntityDataPane("Entity Data: Parents", true, false) {

            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getParentEntity() != null) {
                    entityListPane.showEntity(entityData.getParentEntity());
                }
            }

        };

        entityChildrenPane = new EntityDataPane("Entity Data: Children", false, true) {

            @Override
            protected void doubleClick(EntityData entityData) {
                if (entityData.getChildEntity() != null) {
                    entityListPane.showEntity(entityData.getChildEntity());
                }
            }

        };

        entityListPane = new EntityListPane(entityParentsPane, entityChildrenPane);


        double frameHeight = (double) DataviewFrame.this.getPreferredSize().height - 30;

        JSplitPane splitPaneVerticalInner = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, entityParentsPane, entityListPane);
        splitPaneVerticalInner.setDividerLocation((int) (frameHeight * 1 / 4));

        JSplitPane splitPaneVerticalOuter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, splitPaneVerticalInner, entityChildrenPane);
        splitPaneVerticalOuter.setDividerLocation((int) (frameHeight * 3 / 4));

        JSplitPane splitPaneHorizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, entityTypePane, splitPaneVerticalOuter);
        splitPaneHorizontal.setDividerLocation(300);
        getContentPane().add(splitPaneHorizontal, BorderLayout.CENTER);
    }

    private void initData() {
        entityTypePane.refresh();
    }

    private class EntityTypePane extends JScrollPane {

        private JTree tree;

        public EntityTypePane() {
            tree = new JTree(new DefaultMutableTreeNode("Loading..."));
            setViewportView(tree);

            tree.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {

                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row >= 0) {
                        if (e.isPopupTrigger()) {
                            // Right click
                        }
                        else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                            // Double click
                        }
                        else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                            // Single click
                            TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                            if (node.getUserObject() instanceof EntityType) {
                                entityListPane.showEntities((EntityType) node.getUserObject());
                                tree.setSelectionPath(path);
                            }
                            else if (node.getUserObject() instanceof EntityAttribute) {
                                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                                entityListPane.showEntities((EntityType) parent.getUserObject());
                                tree.setSelectionPath(path.getParentPath());
                            }
                        }
                    }
                }
            });
        }

        public void refresh() {

            loadTask = new SimpleWorker() {

                private TreeModel model;

                @Override
                protected void doStuff() throws Exception {
                    List<EntityType> entityTypes = ModelMgr.getModelMgr().getEntityTypes();

                    DefaultMutableTreeNode root = new DefaultMutableTreeNode("EntityType");

                    for (EntityType entityType : entityTypes) {
                        DefaultMutableTreeNode entityTypeNode = new DefaultMutableTreeNode(entityType) {
                            @Override
                            public String toString() {
                                return ((EntityType) getUserObject()).getName();
                            }
                        };
                        root.add(entityTypeNode);

                        for (EntityAttribute entityAttribute : entityType.getOrderedAttributes()) {
                            DefaultMutableTreeNode entityAttrNode = new DefaultMutableTreeNode(entityAttribute) {
                                @Override
                                public String toString() {
                                    return ((EntityAttribute) getUserObject()).getName();
                                }
                            };
                            entityTypeNode.add(entityAttrNode);
                        }
                    }
                    model = new DefaultTreeModel(root);
                }

                @Override
                protected void hadSuccess() {
                    tree.setModel(model);
                    expandAll();
                }

                @Override
                protected void hadError(Throwable error) {
                    error.printStackTrace();
                }

            };

            loadTask.execute();
        }

        public void expandAll() {
            int row = 0;
            while (row < tree.getRowCount()) {
                tree.expandRow(row);
                row++;
            }
        }

    }

}
