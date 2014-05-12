package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;

/**
 * A dialog for viewing all the data sets that a user has access to.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetListDialog extends ModalDialog implements Accessibility, Refreshable {

    private JLabel loadingLabel;
    private JPanel mainPanel;
    private DynamicTable dynamicTable;
    private DataSetDialog dataSetDialog;
    
    public DataSetListDialog() {

        setTitle("My Data Sets");
        
        dataSetDialog = new DataSetDialog(this);
        
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
            	Entity dataSetEntity = (Entity)userObject;
                if (dataSetEntity!=null) {
                    if ("Name".equals(column.getName())) {
                        return dataSetEntity.getName();
                    }
                    else {
                    	String value = dataSetEntity.getValueByAttributeName(column.getName());
                    	if (EntityConstants.ATTRIBUTE_PIPELINE_PROCESS.equals(column.getName())) {
                    		return decodeEnumList(PipelineProcess.class, value);
                    	}
                    	else if (EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN.equals(column.getName())) {
                            return value;
                        }
                    	else if (EntityConstants.ATTRIBUTE_SAGE_SYNC.equals(column.getName())) {
                            return new Boolean(value!=null);
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
        		
        		if (menu!=null) {
        			JTable table = getTable();
        			ListSelectionModel lsm = table.getSelectionModel();
            		if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) return menu;
            		
        			final Entity dataSetEntity = (Entity)getRows().get(table.getSelectedRow()).getUserObject();
    		        
        			JMenuItem editItem = new JMenuItem("  Edit");
    		        editItem.addActionListener(new ActionListener() {
    					@Override
    					public void actionPerformed(ActionEvent e) {
    						dataSetDialog.showForDataSet(dataSetEntity);
    					}
    				});
    		        menu.add(editItem);
    		        
    		        JMenuItem deleteItem = new JMenuItem("  Delete");
    		        deleteItem.addActionListener(new ActionListener() {
    					@Override
    					public void actionPerformed(ActionEvent e) {
    						
    						Utils.setWaitingCursor(DataSetListDialog.this);

    				        SimpleWorker worker = new SimpleWorker() {

    							@Override
    							protected void doStuff() throws Exception {
    								ModelMgr.getModelMgr().deleteEntityTree(dataSetEntity.getId());
    							}
    							
    							@Override
    							protected void hadSuccess() {
    								Utils.setDefaultCursor(DataSetListDialog.this);
    								loadDataSets();
    							}
    							
    							@Override
    							protected void hadError(Throwable error) {
    								SessionMgr.getSessionMgr().handleException(error);
    								Utils.setDefaultCursor(DataSetListDialog.this);
    								loadDataSets();
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
    			final Entity dataSetEntity = (Entity)getRows().get(row).getUserObject();
				dataSetDialog.showForDataSet(dataSetEntity);
			}

            @Override
            public Class<?> getColumnClass(int column) {
                DynamicColumn dc = getColumns().get(column);
                if (dc.getName().equals(EntityConstants.ATTRIBUTE_SAGE_SYNC)) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }

            @Override
            protected void valueChanged(DynamicColumn dc, int row, Object data) {
                if (dc.getName().equals(EntityConstants.ATTRIBUTE_SAGE_SYNC)) {
                    final Boolean selected = data==null? Boolean.FALSE : (Boolean)data;
                    DynamicRow dr = getRows().get(row);
                    final Entity dataSetEntity = (Entity)dr.getUserObject();
                    SimpleWorker worker = new SimpleWorker() {
                        
                        @Override
                        protected void doStuff() throws Exception {
                            if (selected) {
                                ModelMgr.getModelMgr().setAttributeAsTag(dataSetEntity, EntityConstants.ATTRIBUTE_SAGE_SYNC);
                            }
                            else {
                                EntityData sageSyncEd = dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC);
                                if (sageSyncEd!=null) {
                                    dataSetEntity.getEntityData().remove(sageSyncEd);
                                    ModelMgr.getModelMgr().removeEntityData(sageSyncEd);
                                }
                            }
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
        
        dynamicTable.addColumn("Name");
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_SAGE_SYNC).setEditable(true);
        
        JButton addButton = new JButton("Add new");
        addButton.setToolTipText("Add a new data set definition");
        addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataSetDialog.showForNewDataSet();
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

    	loadDataSets();

		Component mainFrame = SessionMgr.getMainFrame();
		setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.4)));
		
        // Show dialog and wait
        packAndShow();
    }
    
    private void loadDataSets() {

    	mainPanel.removeAll();
    	mainPanel.add(loadingLabel, BorderLayout.CENTER);
    	
        SimpleWorker worker = new SimpleWorker() {

        	private List<Entity> dataSetEntities = new ArrayList<Entity>();
        	
			@Override
			protected void doStuff() throws Exception {
				for(Entity dataSetEntity : ModelMgr.getModelMgr().getDataSets()) {
					dataSetEntities.add(dataSetEntity);
				}
			}
			
			@Override
			protected void hadSuccess() {

		        // Update the attribute table
		        dynamicTable.removeAllRows();
		        for(Entity dataSetEntity : dataSetEntities) {
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

    private String decodeEnumList(Class enumType, String list) {
    	
    	StringBuffer buf = new StringBuffer();
    	for(String key : list.split(",")) {
    		if (key.isEmpty()) continue;
    		try {
    			String value = ((NamedEnum)Enum.valueOf(enumType, key)).getName();
    			if (buf.length()>0) buf.append(", ");
    			buf.append(value);
    		}
    		catch (Exception e) {
    			SessionMgr.getSessionMgr().handleException(new Exception("Unrecognized enumerated value: "+key));
    		}
    	}
    	return buf.toString();
    }

    public void refresh() {
    	loadDataSets();
    }

    public void totalRefresh() {
    	throw new UnsupportedOperationException();
    }
    
    public boolean isAccessible() {
    	return true;
    }
}
