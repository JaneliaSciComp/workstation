package org.janelia.it.workstation.gui.dataview;

import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.search.SearchResultsPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.TreeMap;
import org.janelia.it.workstation.gui.util.WindowLocator;

/**
 * The left-hand panel which lists the Entity types and their attributes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTypePane extends JScrollPane {

    private SimpleWorker loadTask;
    private final JTree tree;

    public EntityTypePane() {

        tree = new JTree(new DefaultMutableTreeNode("Loading..."));
        setViewportView(tree);

        tree.addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                tree.setSelectionRow(tree.getRowForLocation(e.getX(), e.getY()));
                showPopupMenu(e);
            }
        });
    }

    private void showPopupMenu(MouseEvent e) {

        TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        if (node.isRoot()) {

            JMenuItem addTypeMenuItem = new JMenuItem("Add Entity Type");
            addTypeMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String typeName = (String) JOptionPane.showInputDialog(SessionMgr.getBrowser().getMainComponent(), "Name:\n", "Add Entity Type", JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if (StringUtils.isEmpty(typeName)) {
                        return;
                    }

                    try {
                        ModelMgr.getModelMgr().createEntityType(typeName);
                        refresh();
                    }
                    catch (Exception x) {
                        x.printStackTrace();
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser().getMainComponent(), "Error adding type " + x.getMessage());
                    }
                }
            });
            popupMenu.add(addTypeMenuItem);
        }
        else if (node.getUserObject() instanceof EntityType) {

            final EntityType entityType = (EntityType) node.getUserObject();

            JMenuItem addAttrMenuItem = new JMenuItem("Add Attribute");
            addAttrMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String attrName = (String) JOptionPane.showInputDialog(SessionMgr.getBrowser().getMainComponent(), "Name:\n", "Add Entity Attribute", JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if (StringUtils.isEmpty(attrName)) {
                        return;
                    }

                    try {
                        ModelMgr.getModelMgr().createEntityAttribute(entityType.getName(), attrName);
                        refresh();
                    }
                    catch (Exception x) {
                        x.printStackTrace();
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser().getMainComponent(), "Error adding attribute " + x.getMessage());
                    }
                }
            });
            popupMenu.add(addAttrMenuItem);

            JMenuItem searchItem = new JMenuItem("Search for this type");
            searchItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EntityPane entityPane = getEntityPane();
                    SearchPane searchPane = entityPane.getSearchPane();
                    SearchResultsPanel searchResultsPanel = entityPane.getSearchResultsPanel();
                    searchPane.setTabIndex(1);
                    searchPane.getSolrPanel().setSearchString("+entity_type:\"" + entityType.getName() + "\"");
                    searchPane.performSolrSearch(true);
                }
            });
            popupMenu.add(searchItem);
        }
        else if (node.getUserObject() instanceof EntityAttribute) {

            final EntityAttribute entityAttr = (EntityAttribute) node.getUserObject();

            JMenuItem searchItem = new JMenuItem("Search for this attribute");
            searchItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EntityPane entityPane = getEntityPane();
                    SearchPane searchPane = entityPane.getSearchPane();
                    SearchResultsPanel searchResultsPanel = entityPane.getSearchResultsPanel();
                    searchPane.setTabIndex(1);
                    String attrName = SolrUtils.getDynamicFieldName(entityAttr.getName());
                    searchPane.getSolrPanel().setSearchString("+" + attrName + ":*");
                    searchResultsPanel.setColumnVisibility(attrName, true);
                    searchPane.performSolrSearch(true);
                }
            });
            popupMenu.add(searchItem);
        }

        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }
    
    private EntityPane getEntityPane() { 
        DataViewerTopComponent tc = (DataViewerTopComponent)WindowLocator.getByName(DataViewerTopComponent.PREFERRED_ID);
        if (tc!=null) {
            tc.open();
            tc.requestActive();
            return tc.getDataViewer().getEntityPane();
        }
        return null;
    }

    public void refresh() {

        loadTask = new SimpleWorker() {

            private TreeModel model;

            @Override
            protected void doStuff() throws Exception {

                List<EntityType> entityTypes = ModelMgr.getModelMgr().getEntityTypes();
                TreeMap<String, EntityType> sortedCollection = new TreeMap<String, EntityType>();
                for (EntityType entityType : entityTypes) {
                    sortedCollection.put(entityType.getName(), entityType);
                }

                DefaultMutableTreeNode root = new DefaultMutableTreeNode("EntityType");

                for (EntityType entityType : sortedCollection.values()) {
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
