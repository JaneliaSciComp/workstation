package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.SplitGroupingDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A panel that may be inserted into the right-most view pane and serves as a workflow driver for the GAL4 split line 
 * picking task. The top part of the panel describes the step-wise workflow and provides buttons for the user to
 * access various aspects of the workflow (search, grouping, crossing). The bottom part of the panel is an abbreviated 
 * image viewer which shows the results of cross simulations. It is synchronized with the other two image viewer panels
 * in that selecting a cross result selects its two inputs in the other two panels. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitPickingPanel extends JPanel implements Refreshable {

	/** Preference properties */
    public static final String CROSS_PREFIX_PROPERTY = "SessionMgr.SplitPicker.PrefixProperty";
    public static final String LAST_WORKING_FOLDER_ID_PROPERTY = "SessionMgr.SplitPicker.LastWorkingFolderIdProperty";
    
	/** Default directory for exports */
	protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");
	
	public static final String FOLDER_NAME_REPRESENTATIVES = "Representatives";
	public static final String FOLDER_NAME_SPLIT_LINES = "Split Lines";
	public static final String FOLDER_NAME_SPLIT_LINES_AD = "AD";
	public static final String FOLDER_NAME_SPLIT_LINES_DBD = "DBD";
	public static final String FOLDER_NAME_CROSSES = "Crosses";
	
	private static final int STEP_PANEL_HEIGHT = 35;
	private static final int MAX_FREE_CROSSES = 1;
	private static final int MAX_TOTAL_CROSSES = 10;
	private static final int REFRESH_DELAY = 5000;
	private static final int INTERSECTION_METHOD = 0;
	private static final int KERNEL_SIZE = 3;
	
	private final SplitGroupingDialog splitGroupingDialog;
	private final JButton searchButton;
	private final JButton groupButton;
	private final JButton prefixButton;
	private final JButton resultFolderButton;
	private final IconDemoPanel crossesPanel;
//	private final JTextField methodField;
//	private final JTextField blurField;
	
	private RootedEntity workingFolder;
	private RootedEntity repFolder;
	private RootedEntity splitLinesFolder;
	private RootedEntity groupAdFolder;
	private RootedEntity groupDbdFolder;
	private RootedEntity crossFolder;
	private String crossPrefix;
	private Integer nextSuffix;
	private List<Entity> crosses;
	
	private Timer refreshTimer;
	private Set<Long> runningTasks = Collections.synchronizedSet(new HashSet<Long>());
	
	public SplitPickingPanel() {
	
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
	
		this.splitGroupingDialog = new SplitGroupingDialog(this);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		
		JPanel folderPanel = new JPanel();
		folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
		JLabel folderLabel = new JLabel("1. Choose a working folder: ");
		folderPanel.add(folderLabel);

		resultFolderButton = new JButton("Choose result folder");
		resultFolderButton.setFocusable(false);
		resultFolderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupResultFolderMenu();
			}
		});
		folderPanel.add(resultFolderButton);
		folderPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		folderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(folderPanel);
		
		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));
		JLabel searchLabel = new JLabel("2. Search for screen images: ");
		searchPanel.add(searchLabel);
		searchButton = new JButton("Search");
		searchButton.setFocusable(false);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a working folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						repFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_REPRESENTATIVES, true);
					}
					
					@Override
					protected void hadSuccess() {
						SessionMgr.getBrowser().getEntityOutline().selectEntityByUniqueId(repFolder.getUniqueId());
						SessionMgr.getSessionMgr().getActiveBrowser().getPatternSearchDialog().showDialog(repFolder);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
			}
		});
		searchPanel.add(searchButton);
		searchPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(searchPanel);
		

		JPanel groupPanel = new JPanel();
		groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.LINE_AXIS));
		JLabel groupLabel = new JLabel("3. Group by split lines:");
		groupPanel.add(groupLabel);
		groupButton = new JButton("Group");
		groupButton.setFocusable(false);
		groupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getWorkingFolder()==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (getRepFolder()==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please search for representatives first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				splitGroupingDialog.showDialog();	
			}
		});
		groupPanel.add(groupButton);
		groupPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(groupPanel);


		JPanel prefixPanel = new JPanel();
		prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.LINE_AXIS));
		JLabel prefixLabel = new JLabel("4. Enter a label prefix: ");
		prefixPanel.add(prefixLabel);


		crossPrefix = (String)SessionMgr.getSessionMgr().getModelProperty(CROSS_PREFIX_PROPERTY);
		prefixButton = new JButton(crossPrefix==null?"Enter prefix":crossPrefix);
		prefixButton.setFocusable(false);
		prefixButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPrefixEditingDialog();
			}
		});
		prefixPanel.add(prefixButton);
		prefixPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		prefixPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(prefixPanel);
		
		
		JPanel computePanel = new JPanel();
		computePanel.setLayout(new BoxLayout(computePanel, BoxLayout.LINE_AXIS));
		JLabel computeLabel = new JLabel("5. Compute selected intersections: ");
		computePanel.add(computeLabel);
		JButton crossButton = new JButton("Compute");
		crossButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (crossPrefix==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a cross prefix first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				createCrosses();
			}
		});
		computePanel.add(crossButton);
		computePanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		computePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(computePanel);

//		JPanel param1Panel = new JPanel();
//		param1Panel.setLayout(new BoxLayout(param1Panel, BoxLayout.LINE_AXIS));
//		JLabel param1Label = new JLabel("Method (0=minimum value, 1=geometric mean, 2=scaled product): ");
//		param1Panel.add(Box.createHorizontalStrut(50));
//		param1Panel.add(param1Label);		
//		methodField = new JTextField("0");
//		methodField.setMaximumSize(new Dimension(50, STEP_PANEL_HEIGHT));
//		param1Panel.add(methodField);
//		param1Panel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
//		param1Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
//		mainPanel.add(param1Panel);
//		
//
//		JPanel param2Panel = new JPanel();
//		param2Panel.setLayout(new BoxLayout(param2Panel, BoxLayout.LINE_AXIS));
//		JLabel param2Label = new JLabel("Gaussian kernel size for blur: ");
//		param2Panel.add(Box.createHorizontalStrut(50));
//		param2Panel.add(param2Label);
//		blurField = new JTextField("3");
//		blurField.setMaximumSize(new Dimension(50, STEP_PANEL_HEIGHT));
//		param2Panel.add(blurField);
//		param2Panel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
//		param2Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
//		mainPanel.add(param2Panel);
		

		
		JPanel exportPanel = new JPanel();
		exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.LINE_AXIS));
		JLabel exportLabel = new JLabel("6. Export results to file: ");
		exportPanel.add(exportLabel);
		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (crossFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please compute some crosses first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				exportResults();
			}
		});
		exportPanel.add(exportButton);
		exportPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(exportPanel);
		
		
		add(mainPanel, BorderLayout.NORTH);
		
		crossesPanel = new IconDemoPanel(EntitySelectionModel.CATEGORY_CROSS_VIEW) {
			
			@Override
			protected JToolBar createToolbar() {
				// Override to customize the toolbar
				JToolBar toolbar = super.createToolbar();
				toolbar.removeAll();
				
				toolbar.add(refreshButton);

				toolbar.addSeparator();
				toolbar.add(imageSizeSlider);
				return toolbar;
			}
			
			@Override
			protected void buttonDrillDown(AnnotatedImageButton button) {
				// Do nothing, this panel does not support drill down
			}
			
			@Override
			protected void entitySelected(String rootedEntityId, boolean clearAll) {
				super.entitySelected(rootedEntityId, clearAll);
				
				final RootedEntity rootedEntity = getRootedEntityById(rootedEntityId);
				if (rootedEntity == null) {
					System.out.println("SplitPickingPanel.entitySelected: cannot find entity with id="+rootedEntityId);
					return;
				}
				if (!rootedEntity.getEntity().getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS)) {
					return;
				}
				
				// This is a sample cross, attempt to select its parents in the other viewers

				loadViewers();
			}
		};
		
		add(crossesPanel, BorderLayout.CENTER);
		
		loadExistingCrossSimulations();
	}
	
	private void loadExistingCrossSimulations() {

		SimpleWorker worker = new SimpleWorker() {

			private List<Entity> crosses = new ArrayList<Entity>();
			private Integer nextSuffix = 1;
			
			@Override
			protected void doStuff() throws Exception {
				List<Entity> allCrosses = ModelMgr.getModelMgr().getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS);
				for(Entity cross : allCrosses) {
					if (!cross.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
					crosses.add(cross);
					String label = cross.getValueByAttributeName(EntityConstants.ATTRIBUTE_CROSS_LABEL);
					if (label==null) continue;
					int di = label.lastIndexOf("-");
					if (di>=0 && di<label.length()-2) {
						String suffix = label.substring(di+1);
						try {
							int s = Integer.parseInt(suffix);
							if (s>nextSuffix) {
								nextSuffix = s;
							}
						}
						catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			@Override
			protected void hadSuccess() {
				SplitPickingPanel.this.crosses = crosses;
				SplitPickingPanel.this.nextSuffix = nextSuffix;
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();

	}

	private void setRefreshing(final boolean refreshing) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (refreshTimer!=null) {
					refreshTimer.stop();
					refreshTimer = null;
				}
				
				if (refreshing) {
					refreshTimer = new Timer(REFRESH_DELAY, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							
							synchronized (runningTasks) {
								if (runningTasks.isEmpty()) {
									refreshTimer.stop();
									refreshTimer = null;
									return;
								}
								
								Set<Long> doneTasks = new HashSet<Long>();
								for(Long taskId : runningTasks) {
									try {
										Task task = ModelMgr.getModelMgr().getTaskById(taskId);
										if (task!= null) {
											Event lastEvent = task.getLastNonDeletedEvent();
											if (lastEvent != null) {
												if (Task.isDone(lastEvent.getEventType())) {
													doneTasks.add(taskId);
												}
											}
										}
									}
									catch (Exception ex) {
										SessionMgr.getSessionMgr().handleException(ex);
									}
								}
								
								if (!doneTasks.isEmpty()) {
									crossesPanel.refresh();
								}
								
								for(Long doneTaskId : doneTasks) {
									runningTasks.remove(doneTaskId);
								}
							}
						}
					});
					refreshTimer.setInitialDelay(0);
					refreshTimer.start();	
				} 
			}
		});
		
	}
	
	/**
	 * Select the given entity in the given viewer, and then scroll it into view.
	 * @param viewer
	 * @param entityId
	 */
	private void selectAndScroll(final IconDemoPanel viewer, final Long entityId) {
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(viewer.getSelectionCategory(), ""+entityId, true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewer.getImagesPanel().scrollSelectedEntitiesToTop();
			}
		});	
	}
	
	@Override
	public void refresh() {

		final Long lastWorkingFolderId = (Long)SessionMgr.getSessionMgr().getModelProperty(LAST_WORKING_FOLDER_ID_PROPERTY);
		if (lastWorkingFolderId!=null) {
			SimpleWorker workingFolderLoadingWorker = new SimpleWorker() {
				Entity folder;
				@Override
				protected void doStuff() throws Exception {
					folder = ModelMgr.getModelMgr().getEntityById(""+lastWorkingFolderId);
				}
				
				@Override
				protected void hadSuccess() {
					if (folder!= null) {
						setResultFolder(folder);
					}
				}
				
				@Override
				protected void hadError(Throwable error) {
					SessionMgr.getSessionMgr().handleException(error);
				}
			};
			workingFolderLoadingWorker.execute();
		}
		
		final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
		final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer(); 
		
		SessionMgr.getBrowser().getCenterRightHorizontalSplitPane().setDividerLocation(0.66);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SessionMgr.getBrowser().getViewersPanel().getMainSplitPane().setDividerLocation(0.5);
				
				// Refresh the image viewer
				crossesPanel.refresh();
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						// Resize images to 1 per row		
						int fullWidth = SessionMgr.getBrowser().getViewersPanel().getWidth();
						int padding = 100;
						mainViewer.getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
						secViewer.getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
						crossesPanel.getImageSizeSlider().setValue(crossesPanel.getWidth()-padding);						
					}
				});
			}
		});
	}

	private void showPrefixEditingDialog() {

		// Add button clicked
		final String prefix = (String) JOptionPane.showInputDialog(SessionMgr.getBrowser(), 
				"Cross Prefix (a unique identifier will be appended to this to create each cross label):\n",
				"Set your cross prefix", JOptionPane.PLAIN_MESSAGE, null, null, crossPrefix);
		if ((prefix == null) || (prefix.length() <= 0)) {
			return;
		}

		crossPrefix = prefix;
		prefixButton.setText(crossPrefix);
		SessionMgr.getSessionMgr().setModelProperty(CROSS_PREFIX_PROPERTY, crossPrefix);
	}
	
	private void createCrosses() {
		
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		final List<String> mainSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_MAIN_VIEW);		
		final List<String> secSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_SEC_VIEW);
		
		final List<Entity> samples1 = new ArrayList<Entity>();
		final List<Entity> samples2 = new ArrayList<Entity>();

		for(String mainSelectionId : mainSelectionIds) {
			Entity sample1 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW).getEntityById(mainSelectionId);
			if (!sample1.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE) && !sample1.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a Screen Sample or Fly Line: "+sample1.getName(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			samples1.add(sample1);
		}

		for(String secSelectionId : secSelectionIds) {
			Entity sample2 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_SEC_VIEW).getEntityById(secSelectionId);
			if (!sample2.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE) && !sample2.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a Screen Sample or Fly Line: "+sample2.getName(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			samples2.add(sample2);
		}
		
		final int numCrosses = samples1.size() * samples2.size();
		if (numCrosses > MAX_TOTAL_CROSSES) {
			JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Cannot run "+numCrosses+" crosses (limited to "+MAX_TOTAL_CROSSES+" max)", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (numCrosses > MAX_FREE_CROSSES) {
			Object[] options = {"Yes", "Cancel"};
			int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getBrowser(),
					"Are you sure you want to compute "+numCrosses+" crosses?", "Compute Crosses",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (deleteConfirmation != 0) {
				return;
			}
		}
		else if (numCrosses == 0) {
			JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please select at least one Screen Sample or Fly Line in each viewer", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		SimpleWorker worker = new SimpleWorker() {

			final Set<String> existingCrosses = new HashSet<String>();
			
			@Override
			protected void doStuff() throws Exception {

				if (crossFolder==null) {
					setCrossFolder(ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_CROSSES, true));
				}

				if (crossesPanel.getRootedEntities()!=null) {
					for(Entity sample1 : samples1) {
						for(Entity sample2 : samples2) {
							String c1 = createCrossName(sample1, sample2);
							String c2 = createCrossName(sample2, sample1);
							for(Entity crossEntity : crosses) {
								String crossName = crossEntity.getName();
								if (crossName.equals(c1) || crossName.equals(c2)) {
									existingCrosses.add(crossName);
								}
							}
						}
					}
				}
			}
			
			@Override
			protected void hadSuccess() {
				
				final int numExisting = existingCrosses.size();
				if (numExisting > 0) {
					Object[] options = {"Yes", "No"};
					String message = numCrosses==1 ? 
							"You have already simulated this cross before. Recompute it?" : 
							"You have already simulated "+numExisting+" out of these "+numCrosses+" crosses. Recompute them?";
					int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getBrowser(), message, "Recompute?",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (deleteConfirmation == 0) {
						existingCrosses.clear();
					}
				}
				
				if (existingCrosses.size()==numCrosses) return;
				
				crossesPanel.loadEntity(getCrossFolder());
				
				SimpleWorker worker2 = new SimpleWorker() {

					private Entity firstCross;
					
					@Override
					protected void doStuff() throws Exception {
						
						Entity parent = getCrossFolder().getEntity();
						
						List<Long> sampleIds1 = new ArrayList<Long>();
						List<Long> sampleIds2 = new ArrayList<Long>();
						List<Long> outputIds = new ArrayList<Long>();
						
						System.out.println("Processing "+numCrosses+" crosses");
						
						int i = 0;
						for(Entity sample1 : samples1) {
							for(Entity sample2 : samples2) {
								setProgress(i++, numCrosses+1);

								ModelMgrUtils.loadLazyEntity(sample1, false);
								ModelMgrUtils.loadLazyEntity(sample2, false);
								
								String crossName = createCrossName(sample1, sample2);
								if (existingCrosses.contains(crossName)) {
									continue;
								}
								
								Entity cross = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS, crossName);
								cross.setValueByAttributeName(EntityConstants.ATTRIBUTE_CROSS_LABEL, createNextCrossLabel());
								ModelMgr.getModelMgr().saveOrUpdateEntity(cross);
								
								List<Long> childrenIds = new ArrayList<Long>();
								childrenIds.add(sample1.getId());
								childrenIds.add(sample2.getId());
								ModelMgr.getModelMgr().addChildren(cross.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
								
								if (sample1.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
									Entity rep = sample1.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
									sampleIds1.add(rep.getId());
								}
								else {
									sampleIds1.add(sample1.getId());	
								}
								
								if (sample2.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
									Entity rep = sample2.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
									sampleIds2.add(rep.getId());
								}
								else {
									sampleIds2.add(sample2.getId());	
								}
								
								outputIds.add(cross.getId());
								
								if (firstCross==null) {
									firstCross = cross;
								}
								
								crosses.add(cross);
							}
						}
						
						ModelMgr.getModelMgr().addChildren(parent.getId(), outputIds, EntityConstants.ATTRIBUTE_ENTITY);
						startIntersections(sampleIds1, sampleIds2, outputIds);
					}
					
					@Override
					protected void hadSuccess() {
						SessionMgr.getBrowser().getEntityOutline().refresh();
						crossesPanel.refresh(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										crossesPanel.scrollToBottom();
									}
								});
								return null;
							}
						});
						setRefreshing(true);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker2.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Submitting jobs to the compute cluster...", "", 0, 100));
				worker2.execute();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}

    private void startIntersections(List<Long> sampleIdList1, List<Long> sampleIdList2, List<Long> outputIdList3) throws Exception {

    	String idList1Str = commafy(sampleIdList1);
    	String idList2Str = commafy(sampleIdList2);
    	String idList3Str = commafy(outputIdList3);
    	String method = ""+INTERSECTION_METHOD;//methodField.getText();
    	String kernelSize = ""+KERNEL_SIZE;//blurField.getText();
    	
    	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
    	taskParameters.add(new TaskParameter("screen sample 1 id list", idList1Str, null));
    	taskParameters.add(new TaskParameter("screen sample 2 id list", idList2Str, null));
    	taskParameters.add(new TaskParameter("output entity id_list", idList3Str, null));
    	taskParameters.add(new TaskParameter("intersection method", method, null));
    	taskParameters.add(new TaskParameter("kernel size", kernelSize, null));
    	
    	Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(), 
    			taskParameters, "screenSampleCrossService", "Screen Sample Cross Service");
        task.setJobName("Screen Sample Cross Service");
        task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
        
        System.out.println("Submitting task "+task.getDisplayName()+" id="+task.getObjectId());
        
        runningTasks.add(task.getObjectId());
        ModelMgr.getModelMgr().submitJob("ScreenSampleCrossService", task.getObjectId());
    }
    
    private String createNextCrossLabel() {
    	if (crossPrefix == null || nextSuffix==null) {
    		throw new IllegalStateException("Error creating cross label");
    	}
    	String crossLabel = crossPrefix+"-"+String.format("%05d", nextSuffix);
    	nextSuffix++;
    	return crossLabel;
    }

	private String createCrossName(Entity sample1, Entity sample2) {

		try {
			ModelMgrUtils.loadLazyEntity(sample1, false);
			ModelMgrUtils.loadLazyEntity(sample2, false);
			
			Entity balancedDbd = sample2.getChildByAttributeName(EntityConstants.ATTRIBUTE_BALANCED_FLYLINE);
			if (balancedDbd != null) {
					ModelMgrUtils.loadLazyEntity(balancedDbd, false);
					sample2 = balancedDbd;
			}
	
			String[] parts1 = sample1.getName().split("-");
			String[] parts2 = sample2.getName().split("-");
			return parts1[0]+"-x-"+parts2[0];
		}
		catch (Exception e) {
			throw new IllegalStateException("Error creating cross name",e);
		}
	}
	
	private void setResultFolder(Entity entity) {
		
		resultFolderButton.setText(entity.getName());
		workingFolder = new RootedEntity(entity);
		repFolder = null;
		splitLinesFolder = null;
		groupAdFolder = null;
		groupDbdFolder = null;
		crossFolder = null;
		
		SimpleWorker worker = new SimpleWorker() {
			@Override
			protected void doStuff() throws Exception {
				repFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_REPRESENTATIVES, false);
				splitLinesFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_SPLIT_LINES, false);	
				if (splitLinesFolder!=null) {
					groupAdFolder = ModelMgrUtils.getChildFolder(splitLinesFolder, FOLDER_NAME_SPLIT_LINES_AD, false);
					if (groupAdFolder!=null) {
						ModelMgrUtils.refreshEntityAndChildren(groupAdFolder.getEntity());
					}
					groupDbdFolder = ModelMgrUtils.getChildFolder(splitLinesFolder, FOLDER_NAME_SPLIT_LINES_DBD, false);	
					if (groupDbdFolder!=null) {
						ModelMgrUtils.refreshEntityAndChildren(groupDbdFolder.getEntity());
					}
				}
				crossFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_CROSSES, false);	
				if (crossFolder!=null) {
					ModelMgrUtils.refreshEntityAndChildren(crossFolder.getEntity());
				}
			}
			
			@Override
			protected void hadSuccess() {
				SessionMgr.getSessionMgr().setModelProperty(LAST_WORKING_FOLDER_ID_PROPERTY, workingFolder.getEntity().getId());
				
				final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
				if (groupAdFolder==null && groupDbdFolder==null) {
					if (repFolder!=null) {
						expandEntityOutline(repFolder.getUniqueId());
						mainViewer.loadEntity(repFolder);
					}
					else {
						expandEntityOutline(workingFolder.getUniqueId());
						mainViewer.clear();
					}
					IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_SEC_VIEW);
					if (secViewer!=null) {
						secViewer.clear();
					}
				}
				else {
					loadViewers();
				}
				
				if (crossFolder!=null) {
					crossesPanel.loadEntity(crossFolder);	
				}
				else {
					crossesPanel.clear();
					revalidate();
					repaint();
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}

	private void loadViewers() {

		// Open the folders in the entity outline
		if (splitLinesFolder != null) {
			expandEntityOutline(splitLinesFolder.getUniqueId());
		}
		else if (repFolder != null) {
			expandEntityOutline(repFolder.getUniqueId());
		}
		else {
			expandEntityOutline(workingFolder.getUniqueId());	
		}

		// Get the parent lines
		Entity entity1 = null;
		Entity entity2 = null;
		List<RootedEntity> selected = crossesPanel.getSelectedEntities();
		if (selected!=null && selected.size()==1) {
			Entity crossEntity = selected.get(0).getEntity();
			for(EntityData childEd : crossEntity.getOrderedEntityData()) {
				if (!childEd.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) continue;
				if (entity1 == null) {
					entity1 = childEd.getChildEntity();
				}
				else {
					entity2 = childEd.getChildEntity();
					break;
				}
			}
		}
		
		final Entity finalEntity1 = entity1;
		final Entity finalEntity2 = entity2;
		SimpleWorker worker = new SimpleWorker() {

			private Entity sourceEntity1 = finalEntity1;
			private Entity sourceEntity2 = finalEntity2;
			
			@Override
			protected void doStuff() throws Exception {
				if (sourceEntity1 != null && !EntityUtils.isInitialized(sourceEntity1)) {
					sourceEntity1 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity1.getId());
				}
				if (sourceEntity2 != null && !EntityUtils.isInitialized(sourceEntity2)) {
					sourceEntity2 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity2.getId());
				}
			}
			
			@Override
			protected void hadSuccess() {
				final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
				final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer();
				
				if (groupAdFolder!=null) {
					if (mainViewer.getContextRootedEntity()!=null && mainViewer.getContextRootedEntity().getEntityId().equals(groupAdFolder.getEntityId())) {
						selectAndScroll(mainViewer, finalEntity1.getId());	
					}
					else {
						mainViewer.loadEntity(groupAdFolder, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								if (sourceEntity1!=null) {
									selectAndScroll(mainViewer, finalEntity1.getId());	
								}
								return null;
							}
						});
					}

				}
				else {
					mainViewer.clear();
				}
				
				if (groupDbdFolder!=null) {
					if (secViewer.getContextRootedEntity()!=null && secViewer.getContextRootedEntity().getEntityId().equals(groupDbdFolder.getEntityId())) {
						selectAndScroll(secViewer, finalEntity2.getId());	
					}
					else {
						secViewer.loadEntity(groupDbdFolder, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								if (sourceEntity2!=null) {
									selectAndScroll(secViewer, finalEntity2.getId());	
								}
								return null;
							}
						});
					}
				}
				else {
					secViewer.clear();
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}

	private void expandEntityOutline(String uniqueId) {
		final EntityOutline entityOutline = SessionMgr.getBrowser().getEntityOutline();
		DefaultMutableTreeNode node = entityOutline.getNodeByUniqueId(uniqueId);
		entityOutline.getDynamicTree().navigateToNode(node);
	}
	
	
	private void showPopupResultFolderMenu() {

		JPopupMenu chooseFolderMenu = new JPopupMenu();
		
		List<EntityData> rootEds = SessionMgr.getBrowser().getEntityOutline().getRootEntity().getOrderedEntityData();
		
		for(EntityData rootEd : rootEds) {
			final Entity commonRoot = rootEd.getChildEntity();
			if (!commonRoot.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
			
			JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
			commonRootItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					setResultFolder(commonRoot);
				}
			});
			
			chooseFolderMenu.add(commonRootItem);
		}
		
		chooseFolderMenu.addSeparator();
		
		JMenuItem createNewItem = new JMenuItem("Create new...");
		
		createNewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				// Add button clicked
				final String folderName = (String) JOptionPane.showInputDialog(SessionMgr.getBrowser(), "Folder Name:\n",
						"Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
				if ((folderName == null) || (folderName.length() <= 0)) {
					return;
				}

				try {
					setResultFolder(ModelMgrUtils.createNewCommonRoot(folderName));	
				}
				catch (Exception e) {
					SessionMgr.getSessionMgr().handleException(e);
				}
			}
		});
		
		chooseFolderMenu.add(createNewItem);
		
		chooseFolderMenu.show(resultFolderButton, 0, resultFolderButton.getHeight());
	}
	
    protected synchronized void exportResults() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = new File(DEFAULT_EXPORT_DIR,"SplitPickingResults.xls");
        
        int i = 1;
        while (defaultFile.exists() && i<10000) {
        	defaultFile = new File(DEFAULT_EXPORT_DIR,"SplitPickingResults_"+i+".xls");
        	i++;
        }
        
        chooser.setSelectedFile(defaultFile);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public String getDescription() {
				return "Tab-delimited Files (*.xls, *.txt)";
			}
			@Override
			public boolean accept(File f) {
				return !f.isDirectory();
			}
		});

        if (chooser.showDialog(SessionMgr.getBrowser(), "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if ((destFile == null) || destFile.equals("")) {
            return;
        }
        
        final String[] exportColumns = { "FlyStoreParent1", "FlyStoreParent2", "FlyStoreLabel",
        								 "CrossIdentifier",
        							     "OriginalADTransformantId", "OriginalADRobotId", 
        								 "OriginalDBDTransformantId","OriginalDBDRobotId",
        								 "BalancedADTransformantId", "BalancedADRobotId", 
        								 "BalancedDBDTransformantId", "BalancedDBDRobotId" };
        
    	SimpleWorker worker = new SimpleWorker() {
    		
			@Override
			protected void doStuff() throws Exception {
				FileWriter writer = new FileWriter(destFile);

				StringBuffer buf = new StringBuffer();
				for(String column : exportColumns) {
					buf.append(column);
					buf.append("\t");
				}
				buf.append("\n");
				writer.write(buf.toString());


				long numTotal = crossesPanel.getRootedEntities().size();
				long numProcessed = 0;
				for(RootedEntity crossRootedEntity : crossesPanel.getRootedEntities()) {
					
					ModelMgrUtils.refreshEntityAndChildren(crossRootedEntity.getEntity());
					
					Entity crossEntity = ModelMgr.getModelMgr().getEntityTree(crossRootedEntity.getEntity().getId());
					
					Entity flylineAd = null;
					Entity flylineDbd = null;
					
					if (crossEntity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS)) {

						for (Entity child : crossEntity.getChildren()) {
							if (child.getEntityType().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
								String splitPart = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
								if ("AD".equals(splitPart)) {
									flylineAd = child;
								}
								else if ("DBD".equals(splitPart)) {
									flylineDbd = child;
								}
							}
						}
						
						if (flylineAd == null) {
							System.out.println("Could not find AD line for screen sample "+crossEntity.getName());
							continue;
						}
							
						if (flylineDbd == null) {
							System.out.println("Could not find DBD line for screen sample "+crossEntity.getName());
							continue;
						}

						ModelMgrUtils.loadLazyEntity(flylineAd, false);
						ModelMgrUtils.loadLazyEntity(flylineDbd, false);
						
						String crossLabel = crossEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CROSS_LABEL);
						Entity balancedAd = flylineAd.getChildByAttributeName(EntityConstants.ATTRIBUTE_BALANCED_FLYLINE);
						Entity balancedDbd = flylineDbd.getChildByAttributeName(EntityConstants.ATTRIBUTE_BALANCED_FLYLINE);
						String adRobotId = flylineAd.getValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
						String dbdRobotId = flylineDbd.getValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
						String balancedAdRobotId = balancedAd==null?"":balancedAd.getValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
						String balancedDbdRobotId = balancedDbd==null?"":balancedDbd.getValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
						
						StringBuffer buf2 = new StringBuffer();
						
						// 3 standard columns for FlyStore import
						buf2.append(adRobotId==null?"":adRobotId);
						buf2.append("\t");
						buf2.append(balancedDbdRobotId==null?"":balancedDbdRobotId);
						buf2.append("\t");
						buf2.append(crossLabel==null?"":crossLabel);
						buf2.append("\t");
						
						// Other supporting columns
						buf2.append(crossEntity.getName());
						buf2.append("\t");
						buf2.append(flylineAd.getName());
						buf2.append("\t");
						buf2.append(adRobotId==null?"":adRobotId);
						buf2.append("\t");
						buf2.append(flylineDbd.getName());
						buf2.append("\t");
						buf2.append(dbdRobotId==null?"":dbdRobotId);
						buf2.append("\t");
						buf2.append(balancedAd==null?"":balancedAd.getName());	
						buf2.append("\t");
						buf2.append(balancedAdRobotId==null?"":balancedAdRobotId);	
						buf2.append("\t");
						buf2.append(balancedDbd==null?"":balancedDbd.getName());	
						buf2.append("\t");
						buf2.append(balancedDbdRobotId==null?"":balancedDbdRobotId);
						buf2.append("\n");
						writer.write(buf2.toString());
					}

					setProgress((int)++numProcessed, (int)numTotal);
				}
				
				writer.close();
			}
			
			@Override
			protected void hadSuccess() {
				int rv = JOptionPane.showConfirmDialog(SessionMgr.getBrowser(), "Data was successfully exported to "+destFile+". Open file in default viewer?", 
						"Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if (rv==JOptionPane.YES_OPTION) {
					OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
					openAction.doAction();
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

    	worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getBrowser(), "Exporting data", "", 0, 100));
		worker.execute();
    }
    
	private String commafy(List<Long> list) {
		StringBuffer sb = new StringBuffer();
		for(Long l : list) {
			if (sb.length()>0) sb.append(",");
			sb.append(l);
		}
		return sb.toString();
	}
	
	public RootedEntity getWorkingFolder() {
		return workingFolder;
	}

	public void setWorkingFolder(RootedEntity workingFolder) {
		this.workingFolder = workingFolder;
	}

	public RootedEntity getRepFolder() {
		return repFolder;
	}

	public void setRepFolder(RootedEntity repFolder) {
		this.repFolder = repFolder;
	}

	public RootedEntity getSplitLinesFolder() {
		return splitLinesFolder;
	}

	public void setSplitLinesFolder(RootedEntity splitLinesFolder) {
		this.splitLinesFolder = splitLinesFolder;
	}

	public RootedEntity getGroupAdFolder() {
		return groupAdFolder;
	}

	public void setGroupAdFolder(RootedEntity groupAdFolder) {
		this.groupAdFolder = groupAdFolder;
	}

	public RootedEntity getGroupDbdFolder() {
		return groupDbdFolder;
	}

	public void setGroupDbdFolder(RootedEntity groupDbdFolder) {
		this.groupDbdFolder = groupDbdFolder;
	}

	public RootedEntity getCrossFolder() {
		return crossFolder;
	}

	public void setCrossFolder(RootedEntity crossFolder) {
		this.crossFolder = crossFolder;
	}
	
}
