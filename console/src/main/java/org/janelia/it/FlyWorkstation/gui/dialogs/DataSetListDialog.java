package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
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
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.*;

/**
 * A dialog for viewing all the data sets that a user has access to.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetListDialog extends ModalDialog implements Accessibility {

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
                    	if (EntityConstants.ATTRIBUTE_MERGE_ALGORITHMS.equals(column.getName())) {
                    		return decodeEnumList(MergeAlgorithm.class, value);
                    	}
                    	else if (EntityConstants.ATTRIBUTE_STITCH_ALGORITHMS.equals(column.getName())) {
                    		return decodeEnumList(StitchAlgorithm.class, value);
                    	}
                    	else if (EntityConstants.ATTRIBUTE_ALIGNMENT_ALGORITHMS.equals(column.getName())) {
                    		return decodeEnumList(AlignmentAlgorithm.class, value);
                    	}
                    	else if (EntityConstants.ATTRIBUTE_ANALYSIS_ALGORITHMS.equals(column.getName())) {
                    		return decodeEnumList(AnalysisAlgorithm.class, value);
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
            		if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) { 
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
        								ModelMgr.getModelMgr().deleteEntityById(dataSetEntity.getId());
        							}
        							
        							@Override
        							protected void hadSuccess() {
        								Utils.setDefaultCursor(DataSetListDialog.this);
        								reloadData();
        							}
        							
        							@Override
        							protected void hadError(Throwable error) {
        								SessionMgr.getSessionMgr().handleException(error);
        								Utils.setDefaultCursor(DataSetListDialog.this);
        								reloadData();
        							}
        						};
        						worker.execute();
        					}
        				});
        		        menu.add(deleteItem);
            		}
        		}
        		
        		return menu;
        	}
        };
        
        dynamicTable.addColumn("Name");
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_MAGNIFICATION);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_MERGE_ALGORITHMS);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_STITCH_ALGORITHMS);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_ALIGNMENT_ALGORITHMS);
        dynamicTable.addColumn(EntityConstants.ATTRIBUTE_ANALYSIS_ALGORITHMS);

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

    	reloadData();

		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.5),(int)(browser.getHeight()*0.5)));
		
        // Show dialog and wait
        packAndShow();
    }
    
    public void reloadData() {

    	mainPanel.removeAll();
    	mainPanel.add(loadingLabel, BorderLayout.CENTER);
    	
        SimpleWorker worker = new SimpleWorker() {

        	private List<Entity> dataSetEntities = new ArrayList<Entity>();
        	
			@Override
			protected void doStuff() throws Exception {
				for(Entity dataSetEntity : ModelMgr.getModelMgr().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
					if (ModelMgrUtils.isOwner(dataSetEntity)) {
						dataSetEntities.add(dataSetEntity);
					}
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
    
    public boolean isAccessible() {
    	return true;
    }
}
