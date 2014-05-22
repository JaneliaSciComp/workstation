/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 */
package org.janelia.it.workstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.ProgressCellRenderer;
import org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Provides a list of Annotation Sessions and allows for their selection, deselection, editing, and deletion. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionOutline extends JPanel implements Refreshable, ActivatableView {

    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_PCT_COMPLETE = "% Complete";

    private List<org.janelia.it.workstation.model.utils.AnnotationSession> sessions = new ArrayList<org.janelia.it.workstation.model.utils.AnnotationSession>();
    private org.janelia.it.workstation.model.utils.AnnotationSession currSession;
    private Component consoleFrame;
    protected final JPanel tablePanel;
    private org.janelia.it.workstation.gui.framework.table.DynamicTable dynamicTable;
    private SimpleWorker loadingWorker;
    private ModelMgrAdapter mml;
    	
    public SessionOutline(Component consoleFrame) {
        super(new BorderLayout());

        this.consoleFrame = consoleFrame;

        tablePanel = new JPanel(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);
        
        mml = new ModelMgrAdapter() {

			@Override
			public void sessionSelected(long sessionId) {
				try {
					// Wait until any loading is complete
					if (loadingWorker != null) loadingWorker.get();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				selectSessionById(sessionId);
			}

			@Override
			public void sessionDeselected() {
				selectSession(null);
			}

			@Override
			public void annotationsChanged(long entityId) {
				for(org.janelia.it.workstation.model.utils.AnnotationSession session : sessions) {
					session.clearCompletedIds();
				}
				dynamicTable.updateTableModel();
				dynamicTable.navigateToRowWithObject(currSession);
			}
			
        };
        
        loadAnnotationSessions(null);
    }

    @Override
    public void activate() {
        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().addModelMgrObserver(mml);
        refresh();
    }

    @Override
    public void deactivate() {
        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeModelMgrObserver(mml);
    }
    
    @Override
	public void refresh() {
		
        loadAnnotationSessions(new Callable<Void>() {
			public Void call() throws Exception {
				// Wait until the sessions are loaded before getting the current one and reselecting it
				org.janelia.it.workstation.model.utils.AnnotationSession session = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getCurrentAnnotationSession();
				if (session != null) {
					currSession = null;
					selectSessionById(session.getId());	
				}
				return null;
			}
        });
    }

	@Override
	public void totalRefresh() {
		refresh();
	}
	
    public void showLoadingIndicator() {
        tablePanel.removeAll();
        tablePanel.add(new JLabel(org.janelia.it.workstation.gui.util.Icons.getLoadingIcon()));
    }

    public void loadAnnotationSessions(final Callable success) {

        showLoadingIndicator();
        this.updateUI();
        sessions.clear();

        loadingWorker = new SimpleWorker() {

            private List<Task> tasks;

            protected void doStuff() throws Exception {
                tasks = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getUserTasksByType(AnnotationSessionTask.TASK_NAME);
            }

            protected void hadSuccess() {
                try {
                    initializeTable(tasks);
                    ConcurrentUtils.invokeAndHandleExceptions(success);
                }
                catch (Exception e) {
                    hadError(e);
                }
                loadingWorker = null;
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                tablePanel.removeAll();
                SessionOutline.this.updateUI();
                loadingWorker = null;
            }

        };

        loadingWorker.execute();
    }
    
	public void initializeTable(List<Task> tasks) {
		
        dynamicTable = new org.janelia.it.workstation.gui.framework.table.DynamicTable() {
        	
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {

            	org.janelia.it.workstation.model.utils.AnnotationSession session = (org.janelia.it.workstation.model.utils.AnnotationSession)userObject;
            	if (column.getName().equals(COLUMN_NAME)) {
            		return session.getName();
            	}
            	else if (column.getName().equals(COLUMN_PCT_COMPLETE)) {
            		return Math.round(session.getPercentComplete()*100)+"%";
            	}
            	
				return null;
			}

			@Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
            	JPopupMenu popupMenu = super.createPopupMenu(e);
            	
            	if (dynamicTable.getCurrentRow() == null) return popupMenu;
            	
                Object o = dynamicTable.getCurrentRow().getUserObject();

                if (o instanceof org.janelia.it.workstation.model.utils.AnnotationSession) {
                    final org.janelia.it.workstation.model.utils.AnnotationSession session = (org.janelia.it.workstation.model.utils.AnnotationSession) o;
                	selectSession(session);
                    
                    JMenuItem editMenuItem = new JMenuItem("  Edit");
                    editMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getAnnotationSessionPropertyDialog().showForSession(session);
                        }
                    });
                    popupMenu.add(editMenuItem);

                    if (session.getTask().getOwner().equals(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getUsername())) {
                        JMenuItem deleteMenuItem = new JMenuItem("  Delete");
                        deleteMenuItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent actionEvent) {
                                deleteSession(session);
                            }
                        });
                        popupMenu.add(deleteMenuItem);
                    }

                }
                
                return popupMenu;
            }

            @Override
			protected void rowClicked(int row) {
                Object o = dynamicTable.getCurrentRow().getUserObject();
                if (o instanceof org.janelia.it.workstation.model.utils.AnnotationSession) {
                    final org.janelia.it.workstation.model.utils.AnnotationSession session = (org.janelia.it.workstation.model.utils.AnnotationSession) o;
                    session.clearDerivedProperties();
                    selectSession(session);
                }
                else {
                	selectSession(null);
                }
			}

			@Override
			protected void backgroundClicked() {
				selectSession(null);
			}
        };

        dynamicTable.getTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        
        DynamicColumn nameCol = dynamicTable.addColumn(COLUMN_NAME, COLUMN_NAME, true, false, false, false);
        DynamicColumn pctCompCol = dynamicTable.addColumn(COLUMN_PCT_COMPLETE, COLUMN_PCT_COMPLETE, true, false, true, false);
        
        dynamicTable.setColumnRenderer(pctCompCol, new ProgressCellRenderer() {
			@Override
			protected int getValueAtRowIndex(int rowIndex) {
				DynamicRow row = dynamicTable.getRows().get(rowIndex);
				org.janelia.it.workstation.model.utils.AnnotationSession session = (org.janelia.it.workstation.model.utils.AnnotationSession)row.getUserObject();
				return (int)Math.round(session.getPercentComplete()*100);
			}
		});

        if (null != tasks) {
	        for (Task task : tasks) {
	            if (task.isTaskDeleted()) continue;
	            org.janelia.it.workstation.model.utils.AnnotationSession session = new org.janelia.it.workstation.model.utils.AnnotationSession((AnnotationSessionTask) task);
	            sessions.add(session);
	            dynamicTable.addRow(session);
	        }
	        
        }

        dynamicTable.updateTableModel();
        tablePanel.removeAll();
        tablePanel.add(dynamicTable);
        
        revalidate();
        repaint();
    }
	
    private void deleteSession(org.janelia.it.workstation.model.utils.AnnotationSession session) {

        if (!session.getTask().getOwner().equals(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getUsername())) {
            JOptionPane.showMessageDialog(consoleFrame, "Only the owner may delete a session", "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, "Are you sure you want to delete this session? All annotations made in this session will be lost.", "Delete Session", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) return;

        try {
            // Remove all annotations
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeAllOntologyAnnotationsForSession(session.getTask().getObjectId());

            // Remove the task
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().deleteTaskById(session.getTask().getObjectId());

            // Update Tree UI
            dynamicTable.removeRow(dynamicTable.getCurrentRow());
            SwingUtilities.updateComponentTreeUI(dynamicTable);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(consoleFrame, "Error deleting session", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public org.janelia.it.workstation.model.utils.AnnotationSession getSessionById(long taskId) {
    	for(DynamicRow row : dynamicTable.getRows()) {
            org.janelia.it.workstation.model.utils.AnnotationSession session = (org.janelia.it.workstation.model.utils.AnnotationSession) row.getUserObject();
            if (session.getTask().getObjectId().equals(taskId)) {
                return session;
            }
        }
        return null;
    }

    public void selectSessionById(long taskId) {
        selectSession(getSessionById(taskId));
    }

    public void selectSession(org.janelia.it.workstation.model.utils.AnnotationSession session) {
    	if (currSession == session) return;
		
    	currSession = session;
    	
    	if (session==null) return;
    	
		dynamicTable.navigateToRowWithObject(session);

		// TODO: update this so that it uses the ViewerManager API instead
		final IconDemoPanel panel = ((IconDemoPanel) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getViewerManager().getActiveViewer(IconDemoPanel.class));
		
		if (session != null) {
			panel.showLoadingIndicator();
			List<RootedEntity> fakeREs = new ArrayList<RootedEntity>();
			for(Entity entity : session.getEntities()) {
				EntityData ed = new EntityData();
				ed.setChildEntity(entity);
				fakeREs.add(new RootedEntity(null, ed));
			}
			panel.loadImageEntities(fakeREs);
		}
		
		org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().setCurrentAnnotationSession(session);
    }
}
