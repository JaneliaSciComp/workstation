/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewFrame extends JFrame {

    private static final double realEstatePercent = 0.7;

    private EntityTypePane entityTypePane;
    private EntityPane entityPane;
    private EntityListPane entityListPane;
    private EntityDetailPane entityDetailPane;
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
        progressPanel.add(new JLabel("Loading..."),c);
        c.gridy = 1;
        progressPanel.add(progressBar,c);

        setLayout(new BorderLayout());
        initUI();
        initData();

    }

    private void initUI() {

        entityTypePane = new EntityTypePane();
        entityDetailPane = new EntityDetailPane();
        entityListPane = new EntityListPane(entityDetailPane);
        entityPane = new EntityPane(entityListPane, entityDetailPane);

        JSplitPane splitPaneHorizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, entityTypePane, entityPane);
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
                        else if (e.getClickCount()==2 
                        		&& e.getButton()==MouseEvent.BUTTON1 
                        		&& (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        	// Double click
                        }
                        else if (e.getClickCount()==1 
                        		&& e.getButton()==MouseEvent.BUTTON1 ) {
                        	// Single click
                        	TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                        	DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                            if (node.getUserObject() instanceof EntityType) {
                                entityListPane.showEntities((EntityType)node.getUserObject());
                                tree.setSelectionPath(path);
                            }
                            else if (node.getUserObject() instanceof EntityAttribute) {
                            	DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
                                entityListPane.showEntities((EntityType)parent.getUserObject());
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
                    List<EntityType> entityTypes = EJBFactory.getRemoteAnnotationBean().getEntityTypes();

                    DefaultMutableTreeNode root = new DefaultMutableTreeNode("EntityType");

                    for(EntityType entityType : entityTypes) {
                        DefaultMutableTreeNode entityTypeNode = new DefaultMutableTreeNode(entityType) {
                            @Override
                            public String toString() {
                                return ((EntityType)getUserObject()).getName();
                            }
                        };
                        root.add(entityTypeNode);

                        for(EntityAttribute entityAttribute : entityType.getOrderedAttributes()) {
                            DefaultMutableTreeNode entityAttrNode = new DefaultMutableTreeNode(entityAttribute) {
                                @Override
                                public String toString() {
                                    return ((EntityAttribute)getUserObject()).getName();
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

    private class EntityPane extends JPanel {

        private EntityListPane entityListPane;
        private EntityDetailPane entityDetailPane;

        public EntityPane(EntityListPane entityListPane, EntityDetailPane entityDetailPane) {
            this.entityListPane = entityListPane;
            this.entityDetailPane = entityDetailPane;
            setLayout(new BorderLayout());
            JSplitPane splitPaneVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, entityListPane, entityDetailPane);
            splitPaneVertical.setDividerLocation(DataviewFrame.this.getPreferredSize().height/2);
            add(splitPaneVertical, BorderLayout.CENTER);
        }

    }

    private class EntityListPane extends JPanel {

        private final EntityDetailPane entityDetailPane;
        private final List<String> staticColumns = new ArrayList<String>();
        private final JTable table;
        private final JLabel titleLabel;
        private final JScrollPane scrollPane;
        private List<Entity> entities;
        private TableModel tableModel;
        private SimpleWorker loadTask;
        
        public EntityListPane(final EntityDetailPane entityDetailPane) {

            this.entityDetailPane = entityDetailPane;

            staticColumns.add("Id");
            staticColumns.add("User");
            //staticColumns.add("Status");
            staticColumns.add("Creation Date");
            staticColumns.add("Updated Date");
            staticColumns.add("Name");

            ListSelectionListener listener = new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int row = table.getSelectedRow();
                        if (row >= 0 && row<entities.size()) {
                            entityDetailPane.showEntity(entities.get(row));
                        }
                    }
                }
            };

            table = new JTable();
            table.setFillsViewportHeight(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setColumnSelectionAllowed(false);
            table.setRowSelectionAllowed(true);
            table.getSelectionModel().addListSelectionListener(listener);
            table.getColumnModel().getSelectionModel().addListSelectionListener(listener);

            scrollPane = new JScrollPane();
            scrollPane.setViewportView(table);

            titleLabel = new JLabel("Entity");

            setLayout(new BorderLayout());
            add(titleLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void showLoading() {
            remove(scrollPane);
            add(progressPanel,BorderLayout.CENTER);
            repaint();
        }

        /**
         * Async method for loading and displaying entities of a given type. 
         */
        public void showEntities(final EntityType entityType) {

        	if (loadTask != null && !loadTask.isDone()) {
        		System.out.println("Cancel current entity type load");
        		loadTask.cancel(true);
        	}

    		System.out.println("Loading entities of type "+entityType.getName());
    		
            titleLabel.setText("Entity: "+entityType.getName());
            showLoading();

            loadTask = new SimpleWorker() {

				@Override
				protected void doStuff() throws Exception {
            		List<Entity> entities = EJBFactory.getRemoteAnnotationBean().getEntitiesByType(entityType.getId());
            		if (isCancelled()) return;
            		tableModel = updateTableModel(entities);
				}
				
				@Override
				protected void hadSuccess() {
                    table.setModel(tableModel);
                    Utils.autoResizeColWidth(table);
                    remove(progressPanel);
                    add(scrollPane, BorderLayout.CENTER);
                    entityDetailPane.showEmpty();
				}
				
				@Override
				protected void hadError(Throwable error) {
					error.printStackTrace();
				}
				
			};

            loadTask.execute();
        }

        public void showEntity(final Entity entity) {

        	if (loadTask != null && !loadTask.isDone()) {
        		System.out.println("Cancel current entity load");
        		loadTask.cancel(true);
        	}

    		System.out.println("Loading entity "+entity.getName());
    		showLoading();
    		 
            titleLabel.setText("Entity: "+entity.getEntityType().getName()+" ("+entity.getName()+")");
            List<Entity> entities = new ArrayList<Entity>();
            entities.add(entity);
    		tableModel = updateTableModel(entities);
            table.setModel(tableModel);
            Utils.autoResizeColWidth(table);
            remove(progressPanel);
            add(scrollPane, BorderLayout.CENTER);
            entityDetailPane.showEmpty();
            table.getSelectionModel().setSelectionInterval(0, 0);
        }

        /**
         * Synchronous method for updating the JTable model. Should be called from the EDT.
         */
        private TableModel updateTableModel(List<Entity> entities) {

            this.entities = entities;

            Collections.sort(entities, new Comparator<Entity>() {
                @Override
                public int compare(Entity o1, Entity o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });

            // Data formatted for the JTable
            Vector<String> columnNames = new Vector<String>(staticColumns);
            Vector<Vector<String>> data = new Vector<Vector<String>>();
            	
            // Build the data in column order
            for(Entity entity : entities) {
                Vector<String> rowData = new Vector<String>();
                rowData.add(entity.getId().toString());
                rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
                //rowData.add((entity.getEntityStatus() == null) ? "" : entity.getEntityStatus().getName());
                rowData.add((entity.getCreationDate() == null) ? "" : entity.getCreationDate().toString());
                rowData.add((entity.getUpdatedDate() == null) ? "" : entity.getUpdatedDate().toString());
                rowData.add((entity.getName() == null) ? "(unnamed)" : entity.getName().toString());
                data.add(rowData);
            }
            
            return new DefaultTableModel(data, columnNames) {
                public boolean isCellEditable(int rowIndex, int mColIndex) {
                    return false;
                }
            };
        }

    }

    private class EntityDetailPane extends JPanel {

        private final List<String> staticColumns = new ArrayList<String>();
        private final JTable table;
        private final JLabel titleLabel;
        private final JScrollPane scrollPane;
        private List<EntityData> datas;
        private TableModel tableModel;
        private SimpleWorker loadTask;

        public EntityDetailPane() {

            staticColumns.add("Attribute Name");
            //staticColumns.add("Attribute Description");
            staticColumns.add("Id");
            staticColumns.add("User");
            staticColumns.add("Updated Date");
            staticColumns.add("Order");
            staticColumns.add("Child");
            staticColumns.add("Value");
            
            table = new JTable();
            table.setFillsViewportHeight(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setColumnSelectionAllowed(false);
            table.setRowSelectionAllowed(true);

            table.addMouseListener(new MouseAdapter(){
                public void mouseClicked(MouseEvent e){
                    if (e.getClickCount() == 2) {
                        JTable target = (JTable)e.getSource();
                        int row = target.getSelectedRow();
                        if (row >= 0 && row<datas.size()) {
                            EntityData entityData = datas.get(row);
                            if (entityData.getChildEntity() != null) {
                                entityListPane.showEntity(entityData.getChildEntity());
                            }
                        }
                    }
                }
            });

            scrollPane = new JScrollPane();
            scrollPane.setViewportView(table);

            titleLabel = new JLabel("EntityData");

            setLayout(new BorderLayout());
            add(titleLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void showLoading() {
            remove(scrollPane);
            add(progressPanel,BorderLayout.CENTER);
            repaint();
        }

        public void showEmpty() {
            titleLabel.setText("EntityData");
            remove(scrollPane);
            repaint();
        }

        /**
         * Async method for loading and displaying entities of a given type.
         */
        public void showEntity(final Entity entity) {

        	if (loadTask != null && !loadTask.isDone()) {
        		System.out.println("Cancel current entity data load");
        		loadTask.cancel(true);
        	}
        	
        	System.out.println("Loading data for entity "+entity.getName());
        	
            titleLabel.setText("EntityData: "+entity.getName());
            showLoading();

            loadTask = new SimpleWorker() {

				@Override
				protected void doStuff() throws Exception {
                	List<EntityData> datas = entity.getOrderedEntityData();

                	for(EntityData data : datas) {
                		Entity child = data.getChildEntity();
                		if (child != null) {
                			data.setChildEntity(EJBFactory.getRemoteAnnotationBean().getEntityById(child.getId().toString()));	
                		}
                		if (isCancelled()) return;
                	}
            	    
                    tableModel = updateTableModel(datas);
				}
				
				@Override
				protected void hadSuccess() {
                    if (tableModel == null) {
                        System.out.println("TableModel was null");
                        return;
                    }
                    else {
                        table.setModel(tableModel);
                        Utils.autoResizeColWidth(table);
                    }
                    remove(progressPanel);
                    add(scrollPane, BorderLayout.CENTER);
				}
				
				@Override
				protected void hadError(Throwable error) {
					error.printStackTrace();
				}
				
			};

            loadTask.execute();
        }


        /**
         *
         *
         * Synchronous method for updating the JTable model. Should be called from the EDT.
         */
        private TableModel updateTableModel(List<EntityData> dataSet) {

            datas = dataSet;

            // Data formatted for the JTable
            Vector<String> columnNames = new Vector<String>();
            Vector<Vector<String>> data = new Vector<Vector<String>>();

            // Prepend the static columns
            columnNames.addAll(staticColumns);

            // Build the data in column order
            for(EntityData entityData : datas) {
                Vector<String> rowData = new Vector<String>();
                rowData.add((entityData.getEntityAttribute() == null) ? "" : entityData.getEntityAttribute().getName());
                //rowData.add((entityData.getEntityAttribute() == null) ? "" : entityData.getEntityAttribute().getDescription());
                rowData.add(entityData.getId().toString());
                rowData.add((entityData.getUser() == null) ? "" : entityData.getUser().getUserLogin());
                rowData.add((entityData.getUpdatedDate() == null) ? "" : entityData.getUpdatedDate().toString());
                rowData.add((entityData.getOrderIndex() == null) ? "" : entityData.getOrderIndex().toString());
                rowData.add((entityData.getChildEntity() == null) ? "" : (entityData.getChildEntity().getName() == null) ? "(unnamed)" : entityData.getChildEntity().getName().toString());
                rowData.add(entityData.getValue());
                data.add(rowData);
            }

            return new DefaultTableModel(data, columnNames) {
                public boolean isCellEditable(int rowIndex, int mColIndex) {
                    return false;
                }
            };
        }

    }


}
