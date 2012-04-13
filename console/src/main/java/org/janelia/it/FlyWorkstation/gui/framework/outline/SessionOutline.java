/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.table.ProgressCellRenderer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Provides a list of Annotation Sessions and allows for their selection, deselection, editing, and deletion. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionOutline extends JPanel implements Refreshable {

    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_PCT_COMPLETE = "% Complete";

    private List<AnnotationSession> sessions = new ArrayList<AnnotationSession>();
    private AnnotationSession currSession;
    private Browser consoleFrame;
    protected final JPanel tablePanel;
    private DynamicTable dynamicTable;
    private SimpleWorker loadingWorker;
    	
    public SessionOutline(Browser consoleFrame) {
        super(new BorderLayout());

        this.consoleFrame = consoleFrame;

        tablePanel = new JPanel(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);
        
        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

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
				for(AnnotationSession session : sessions) {
					session.clearCompletedIds();
				}
				dynamicTable.updateTableModel();
				dynamicTable.navigateToRowWithObject(currSession);
			}
			
        });
        
        loadAnnotationSessions(null);
    }
    
    @Override
	public void refresh() {
		
        loadAnnotationSessions(new Callable<Void>() {
			public Void call() throws Exception {
				// Wait until the sessions are loaded before getting the current one and reselecting it
				AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
				if (session != null) {
					currSession = null;
					selectSessionById(session.getId());	
				}
				return null;
			}
        });
    }

    public void showLoadingIndicator() {
        tablePanel.removeAll();
        tablePanel.add(new JLabel(Icons.getLoadingIcon()));
    }

    public void loadAnnotationSessions(final Callable success) {

        showLoadingIndicator();
        this.updateUI();
        sessions.clear();

        loadingWorker = new SimpleWorker() {

            private List<Task> tasks;

            protected void doStuff() throws Exception {
                tasks = ModelMgr.getModelMgr().getUserTasksByType(AnnotationSessionTask.TASK_NAME);
            }

            protected void hadSuccess() {
                try {
                    initializeTable(tasks);
                    if (success!=null) success.call();
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
		
        dynamicTable = new DynamicTable() {
        	
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {

            	AnnotationSession session = (AnnotationSession)userObject;
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

                if (o instanceof AnnotationSession) {
                    final AnnotationSession session = (AnnotationSession) o;
                	selectSession(session);
                    
                    JMenuItem editMenuItem = new JMenuItem("  Edit");
                    editMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyDialog().showForSession(session);
                        }
                    });
                    popupMenu.add(editMenuItem);

                    if (session.getTask().getOwner().equals(SessionMgr.getUsername())) {
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
                if (o instanceof AnnotationSession) {
                    final AnnotationSession session = (AnnotationSession) o;
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
				AnnotationSession session = (AnnotationSession)row.getUserObject();
				return (int)Math.round(session.getPercentComplete()*100);
			}
		});

        if (null != tasks) {
	        for (Task task : tasks) {
	            if (task.isTaskDeleted()) continue;
	            AnnotationSession session = new AnnotationSession((AnnotationSessionTask) task);
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
	
    private void deleteSession(AnnotationSession session) {

        if (!session.getTask().getOwner().equals(SessionMgr.getUsername())) {
            JOptionPane.showMessageDialog(consoleFrame, "Only the owner may delete a session", "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, "Are you sure you want to delete this session? All annotations made in this session will be lost.", "Delete Session", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) return;

        try {
            // Remove all annotations
            ModelMgr.getModelMgr().removeAllOntologyAnnotationsForSession(session.getTask().getObjectId());

            // Remove the task
            ModelMgr.getModelMgr().deleteTaskById(session.getTask().getObjectId());

            // Update Tree UI
            dynamicTable.removeRow(dynamicTable.getCurrentRow());
            SwingUtilities.updateComponentTreeUI(dynamicTable);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(consoleFrame, "Error deleting session", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public AnnotationSession getSessionById(long taskId) {
    	for(DynamicRow row : dynamicTable.getRows()) {
            AnnotationSession session = (AnnotationSession) row.getUserObject();
            if (session.getTask().getObjectId().equals(taskId)) {
                return session;
            }
        }
        return null;
    }

    public void selectSessionById(long taskId) {
        selectSession(getSessionById(taskId));
    }

    public void selectSession(AnnotationSession session) {
    	if (currSession == session) return;
		
    	currSession = session;
		dynamicTable.navigateToRowWithObject(session);

		final IconDemoPanel panel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
		panel.clear();
		
		if (session != null) {
			panel.showLoadingIndicator();
			panel.loadImageEntities(session.getEntities());
		}
		
		ModelMgr.getModelMgr().setCurrentAnnotationSession(session);
    }
}
