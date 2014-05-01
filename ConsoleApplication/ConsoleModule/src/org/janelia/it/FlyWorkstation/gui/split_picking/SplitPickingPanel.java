package org.janelia.it.FlyWorkstation.gui.split_picking;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.MAASearchDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.PatternSearchDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.*;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;

import static com.google.common.base.Preconditions.checkNotNull;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

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

	private static final Logger log = LoggerFactory.getLogger(SplitPickingPanel.class);
	
	/** Preference properties */
    public static final String CROSS_PREFIX_PROPERTY = "SessionMgr.SplitPicker.PrefixProperty";
    public static final String LAST_WORKING_FOLDER_ID_PROPERTY = "SessionMgr.SplitPicker.LastWorkingFolderIdProperty";
    
	/** Default directory for exports */
	protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");
	
	public static final String FOLDER_NAME_SPLIT_PICKING = "Split Picking";
	public static final String FOLDER_NAME_SEARCH_RESULTS = "Search Results";
	public static final String FOLDER_NAME_SPLIT_LINES_AD = "AD";
	public static final String FOLDER_NAME_SPLIT_LINES_DBD = "DBD";
	public static final String FOLDER_NAME_CROSSES = "Crosses";
	
	private static final int STEP_PANEL_HEIGHT = 35;
	private static final int MAX_FREE_CROSSES = 1;
	private static final int MAX_TOTAL_CROSSES = 10;
	private static final int REFRESH_DELAY = 5000;
	private static final int INTERSECTION_METHOD = 0;
	private static final int KERNEL_SIZE = 3;
	
	private final JButton searchButton;
	private final JButton maaSearchButton;
	private final JButton prefixButton;
	private final JButton resultFolderButton;
	private final IconDemoPanel crossesViewer;
//	private final JTextField methodField;
//	private final JTextField blurField;
	
	private RootedEntity splitPickingFolder;
	private RootedEntity workingFolder;
	private RootedEntity searchResultsFolder;
	private RootedEntity crossFolder;
	private String crossPrefix;
	private Integer nextSuffix;
	private List<Entity> crosses;
	
	private Timer refreshTimer;
	private Set<Long> runningTasks = Collections.synchronizedSet(new HashSet<Long>());
	
	public SplitPickingPanel() {
	
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
	
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		
		JPanel prefixPanel = new JPanel();
		prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.LINE_AXIS));
		JLabel prefixLabel = new JLabel("1. Enter a personal label prefix: ");
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
		
		
		JPanel folderPanel = new JPanel();
		folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
		JLabel folderLabel = new JLabel("2. Choose a working folder: ");
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
		JLabel searchLabel = new JLabel("3. Search for screen images: ");
		searchPanel.add(searchLabel);
		searchButton = new JButton("Pattern Search");
		searchButton.setFocusable(false);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please choose a working folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						searchResultsFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_SEARCH_RESULTS, true);						
					}
					
					@Override
					protected void hadSuccess() {
						PatternSearchDialog dialog = getPatternSearchDialog();
						List<Long> sampleIds  = dialog.showDialog(true);
						createLocalGrouping(sampleIds, dialog.getSaveFolderName());
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
		
		maaSearchButton = new JButton("MAA Search");
		maaSearchButton.setFocusable(false);
		maaSearchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please choose a working folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						searchResultsFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_SEARCH_RESULTS, true);						
					}
					
					@Override
					protected void hadSuccess() {
						MAASearchDialog dialog = getMAASearchDialog();
						List<Long> sampleIds = dialog.showDialog(true);
						createLocalGrouping(sampleIds, dialog.getSaveFolderName());
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
			}
		});
		searchPanel.add(maaSearchButton);
		
		searchPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(searchPanel);
		
		
		JPanel computePanel = new JPanel();
		computePanel.setLayout(new BoxLayout(computePanel, BoxLayout.LINE_AXIS));
		JLabel computeLabel = new JLabel("4. Compute selected intersections: ");
		computePanel.add(computeLabel);
		JButton crossButton = new JButton("Compute");
		crossButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (crossPrefix==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please choose a cross prefix first", "Error", JOptionPane.ERROR_MESSAGE);
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
		JLabel exportLabel = new JLabel("5. Export results to file: ");
		exportPanel.add(exportLabel);
		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (crossFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please compute some crosses first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				exportResults();
			}
		});
		exportPanel.add(exportButton);
		exportPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(exportPanel);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.01;
		add(mainPanel, c);
		
		ViewerPane crossesPane = new ViewerPane(null, EntitySelectionModel.CATEGORY_CROSS_VIEW, false);
		crossesViewer = new IconDemoPanel(crossesPane) {
			
			@Override
			protected IconDemoToolbar createToolbar() {
				// Override to customize the toolbar
				IconDemoToolbar toolbar = super.createToolbar();
				JToolBar t = toolbar.getToolbar();
				t.removeAll();
				t.add(toolbar.getRefreshButton());
				t.addSeparator();
				t.add(toolbar.getImageSizeSlider());
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
					log.warn("entitySelected: cannot find entity with id="+rootedEntityId);
					return;
				}
				if (!rootedEntity.getEntity().getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS)) {
					return;
				}
				
				// This is a sample cross, attempt to select its parents in the other viewers

				log.warn("entitySelected: loading viewers with sample cross, id="+rootedEntityId);
				loadViewers();
			}

			@Override	
			public void setAsActive() {
				// This viewer cannot be activated
			}
		};
		crossesPane.setViewer(crossesViewer);
		
		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		add(crossesPane, c);

		ModelMgr.getModelMgr().registerOnEventBus(this);
		loadExistingCrossSimulations();
	}

	private void loadExistingCrossSimulations() {

		SimpleWorker worker = new SimpleWorker() {

			private List<Entity> crosses = new ArrayList<Entity>();
			private Integer nextSuffix = 1;
			
			@Override
			protected void doStuff() throws Exception {
				List<Entity> allCrosses = ModelMgr.getModelMgr().getOwnedEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS);
				for(Entity cross : allCrosses) {
					if (!cross.getOwnerKey().equals(SessionMgr.getSubjectKey())) continue;
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
		if (refreshing) {
			log.info("Enabling auto refresh");
		}
		else {
			log.info("Disabling auto refresh");
		}
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
							
							log.info("Waiting for {} task(s)",runningTasks.size());
							
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
									// Some tasks were completed in this iteration. Refresh the viewer to get the images.
									crossesViewer.totalRefresh();
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
		
		MAASearchDialog maaSearchDialog = getMAASearchDialog();
		maaSearchButton.setVisible(maaSearchDialog!=null && maaSearchDialog.isAccessible());
		
		final IconDemoPanel mainViewer = getMainViewer();
		final IconDemoPanel secViewer = getSecondaryViewer(); 
		
		SimpleWorker refreshWorker = new SimpleWorker() {
			
			Entity splitPickingFolderEntity;
			EntityData resultFolderEntityData;
			
			@Override
			protected void doStuff() throws Exception {
				
				for(Entity entity : ModelMgr.getModelMgr().getCommonRootEntities()) {
					if (FOLDER_NAME_SPLIT_PICKING.equals(entity.getName()) && ModelMgrUtils.hasWriteAccess(entity)) {
						splitPickingFolderEntity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
						break;
					}
				}
				
				if (splitPickingFolderEntity==null) {
					splitPickingFolderEntity = ModelMgr.getModelMgr().createCommonRoot(FOLDER_NAME_SPLIT_PICKING);
				}
			}
			
			@Override
			protected void hadSuccess() {

				splitPickingFolder = new RootedEntity(splitPickingFolderEntity);
				
				SwingUtilities.invokeLater(new Runnable() {
                    
                    @Override
                    public void run() {
                        expandEntityOutline(splitPickingFolder.getUniqueId());
                        
                        final Long lastWorkingFolderId = (Long)SessionMgr.getSessionMgr().getModelProperty(LAST_WORKING_FOLDER_ID_PROPERTY);
                        if (lastWorkingFolderId!=null) {
                            for(EntityData childEd : splitPickingFolderEntity.getEntityData()) {
                                Entity child = childEd.getChildEntity();
                                if (child!=null && child.getId().equals(lastWorkingFolderId)) {
                                    resultFolderEntityData = childEd;   
                                    break;
                                }
                            }
                        }
                        
                        if (resultFolderEntityData!=null) {
                            setWorkingFolderEntity(resultFolderEntityData);
                            
                            // Refresh the image viewer
                            crossesViewer.refresh();    

                            // Resize the viewers
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // Resize viewers
                                    //getRightSplitPane().setDividerLocation(0.66);
                                    getMainSplitPane().setDividerLocation(0.5);
                                    // Resize images to 1 per row       
                                    int fullWidth = getViewerContainer().getWidth();
                                    int padding = 100;
                                    mainViewer.getToolbar().getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
                                    secViewer.getToolbar().getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
                                    crossesViewer.getToolbar().getImageSizeSlider().setValue(crossesViewer.getWidth()-padding);

                                }
                            });
                        }   
                    }
                });
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		refreshWorker.execute();        
	}

	@Override
	public void totalRefresh() {
		// TODO: implement this with invalidate
		refresh();
	}
	
	@Subscribe 
	public void entityInvalidated(EntityInvalidationEvent event) {
		// TODO: implement this to update the entities	
	}
	
	private void showPrefixEditingDialog() {

		// Add button clicked
		final String prefix = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
				"Cross Prefix (a unique identifier will be appended to this to create each cross label):\n",
				"Set your cross prefix", JOptionPane.PLAIN_MESSAGE, null, null, crossPrefix);
		if ((prefix == null) || (prefix.length() <= 0)) {
			return;
		}

		crossPrefix = prefix;
		prefixButton.setText(crossPrefix);
		SessionMgr.getSessionMgr().setModelProperty(CROSS_PREFIX_PROPERTY, crossPrefix);
	}

	private void createLocalGrouping(final List<Long> sampleIds, final String saveFolderName) {
		
		if (sampleIds==null ||  sampleIds.isEmpty()) return;
		log.info("Creating split groups for "+sampleIds.size()+" samples");
		
		SimpleWorker groupingWorker = new SimpleWorker() {

			private RootedEntity saveFolder;
			private RootedEntity localGroupAdFolder;
			private RootedEntity localGroupDbdFolder;
			
			@Override
			protected void doStuff() throws Exception {
				getProgressMonitor().setNote("Loading data");
				
				List<Entity> samples = ModelMgr.getModelMgr().getEntityByIds(sampleIds);
				
				getProgressMonitor().setNote("Getting representative samples");
				
				log.info("Getting represented fly lines...");
				List<Entity> represented = getRepresentedFlylines(samples);
				log.info("Got {} represented fly lines ",represented.size());

				getProgressMonitor().setNote("Grouping representative samples");
				
				List<Entity> repAd = new ArrayList<Entity>();
				List<Entity> repDbd = new ArrayList<Entity>();
				for(Entity rep : represented) {
					String splitPart = rep.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
					if ("AD".equals(splitPart)) {
						repAd.add(rep);
					}
					else if ("DBD".equals(splitPart)) {
						repDbd.add(rep);
					}
				}

				getProgressMonitor().setNote("Saving split lines");
				
				Entity saveFolderEntity = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, saveFolderName);
				EntityData saveFolderEd = ModelMgr.getModelMgr().addEntityToParent(searchResultsFolder.getEntity(), saveFolderEntity);
				saveFolder = searchResultsFolder.getChild(saveFolderEd);
				
				saveRepresentedGroupings(repAd, repDbd, saveFolder);
				localGroupAdFolder = ModelMgrUtils.getChildFolder(saveFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, false);
				localGroupDbdFolder = ModelMgrUtils.getChildFolder(saveFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, false);

				getProgressMonitor().close();
			}
			
			@Override
			protected void hadSuccess() {
				SessionMgr.getBrowser().getEntityOutline().expandByUniqueId(saveFolder.getUniqueId());
				showEntityInMainViewer(localGroupAdFolder);
				showEntityInSecViewer(localGroupDbdFolder);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		IndeterminateProgressMonitor pm = new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Organizing results", "");
		pm.setMillisToDecideToPopup(0);
		pm.setMillisToPopup(0);
		groupingWorker.setProgressMonitor(pm);
		groupingWorker.execute();
	}
	
	private void createCrosses() {
		
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		final List<String> mainSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_MAIN_VIEW);		
		final List<String> secSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_SEC_VIEW);
		
		final List<Entity> samples1 = new ArrayList<Entity>();
		final List<Entity> samples2 = new ArrayList<Entity>();
		
		Viewer mainViewer = getMainViewer();
		for(RootedEntity rootedEntity : mainViewer.getRootedEntities()) {
			for(String mainSelectionId : mainSelectionIds) {
				if (rootedEntity.getId().equals(mainSelectionId)) {
					Entity sample = rootedEntity.getEntity();
					if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE) && !sample.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
						JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Not a Screen Sample or Fly Line: "+sample.getName(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					log.info("Adding sample1 "+sample.getName());
					samples1.add(sample);	
				}
			}
		}
		
		Viewer secViewer = getSecondaryViewer();
		for(RootedEntity rootedEntity : secViewer.getRootedEntities()) {
			for(String secSelectionId : secSelectionIds) {
				if (rootedEntity.getId().equals(secSelectionId)) {
					Entity sample = rootedEntity.getEntity();
					if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE) && !sample.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
						JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Not a Screen Sample or Fly Line: "+sample.getName(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					log.info("Adding sample2 "+sample.getName());
					samples2.add(sample);	
				}
			}
		}
		
		final int numCrosses = samples1.size() * samples2.size();
		if (numCrosses > MAX_TOTAL_CROSSES) {
			JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Cannot run "+numCrosses+" crosses (limited to "+MAX_TOTAL_CROSSES+" max)", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (numCrosses > MAX_FREE_CROSSES) {
			Object[] options = {"Yes", "Cancel"};
			int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getMainFrame(),
					"Are you sure you want to compute "+numCrosses+" crosses?", "Compute Crosses",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (deleteConfirmation != 0) {
				return;
			}
		}
		else if (numCrosses == 0) {
			JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Please select at least one Screen Sample or Fly Line in each viewer", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		SimpleWorker worker = new SimpleWorker() {

			final Set<String> existingCrosses = new HashSet<String>();
			
			@Override
			protected void doStuff() throws Exception {

				if (crossFolder==null) {
					setCrossFolder(ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_CROSSES, true));
				}

				if (crossesViewer.getRootedEntities()!=null) {
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
					int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getMainFrame(), message, "Recompute?",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (deleteConfirmation == 0) {
						existingCrosses.clear();
					}
				}
				
				if (existingCrosses.size()==numCrosses) return;
				
				crossesViewer.loadEntity(getCrossFolder());
				
				SimpleWorker worker2 = new SimpleWorker() {

					private Entity firstCross;
					
					@Override
					protected void doStuff() throws Exception {
						
						Entity parent = getCrossFolder().getEntity();
						
						List<Long> sampleIds1 = new ArrayList<Long>();
						List<Long> sampleIds2 = new ArrayList<Long>();
						List<Long> outputIds = new ArrayList<Long>();
						
						log.info("Processing "+numCrosses+" crosses");
						
						int i = 0;
						for(Entity sample1 : samples1) {
							for(Entity sample2 : samples2) {
								setProgress(i++, numCrosses+1);

								sample1 = ModelMgr.getModelMgr().loadLazyEntity(sample1, false);
								sample2 = ModelMgr.getModelMgr().loadLazyEntity(sample2, false);
								
								String crossName = createCrossName(sample1, sample2);
								if (existingCrosses.contains(crossName)) {
									continue;
								}
								
								Entity cross = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS, crossName);
								ModelMgr.getModelMgr().setOrUpdateValue(cross, EntityConstants.ATTRIBUTE_CROSS_LABEL, createNextCrossLabel());
								
								List<Long> childrenIds = new ArrayList<Long>();
								childrenIds.add(sample1.getId());
								childrenIds.add(sample2.getId());
								ModelMgr.getModelMgr().addChildren(cross.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
								
								if (sample1.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
									Entity rep = sample1.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
									sampleIds1.add(rep.getId());
								}
								else {
									sampleIds1.add(sample1.getId());	
								}
								
								if (sample2.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
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
						crossesViewer.refresh(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										crossesViewer.getImagesPanel().scrollToBottom();
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
				
				worker2.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Submitting jobs to the compute cluster...", "", 0, 100));
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
        Task task = ModelMgr.getModelMgr().submitJob("ScreenSampleCrossService", "Screen Sample Cross Service", taskParameters);
        
        log.info("Submitted task "+task.getDisplayName()+" id="+task.getObjectId());
        runningTasks.add(task.getObjectId());
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
			sample1 = ModelMgr.getModelMgr().loadLazyEntity(sample1, false);
			sample2 = ModelMgr.getModelMgr().loadLazyEntity(sample2, false);
			
			Entity balancedDbd = sample2.getChildByAttributeName(EntityConstants.ATTRIBUTE_BALANCED_FLYLINE);
			if (balancedDbd != null) {
				balancedDbd = ModelMgr.getModelMgr().loadLazyEntity(balancedDbd, false);
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
	
	private void setWorkingFolderEntity(final EntityData workingFolderEntityEd) {
		
		workingFolder = null;
		searchResultsFolder = null;
		crossFolder = null;
		
		workingFolder = splitPickingFolder.getChild(workingFolderEntityEd);
		expandEntityOutline(workingFolder.getUniqueId());
		
		SimpleWorker worker = new SimpleWorker() {
			@Override
			protected void doStuff() throws Exception {
				searchResultsFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_SEARCH_RESULTS, true);
				
				// Load in all search results
				searchResultsFolder.setEntity(ModelMgr.getModelMgr().loadLazyEntity(searchResultsFolder.getEntity(), false));
				for(Entity searchResultFolderEntity : searchResultsFolder.getEntity().getChildren()) {
					searchResultFolderEntity = ModelMgr.getModelMgr().loadLazyEntity(searchResultFolderEntity, false);	
					for(Entity domainFolderEntity : searchResultFolderEntity.getChildren()) {
						domainFolderEntity = ModelMgr.getModelMgr().loadLazyEntity(domainFolderEntity, false);	
					}
				}
				
				crossFolder = ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_CROSSES, true);	
				if (crossFolder!=null) {
					ModelMgr.getModelMgr().refreshEntityAndChildren(crossFolder.getEntity());
				}
			}
			
			@Override
			protected void hadSuccess() {
				resultFolderButton.setText(workingFolder.getEntity().getName());
				
				SessionMgr.getSessionMgr().setModelProperty(LAST_WORKING_FOLDER_ID_PROPERTY, workingFolder.getEntity().getId());
				
				if (crossFolder!=null) {
					log.info("Loading into cross viewer: {}",crossFolder.getId());
					crossesViewer.loadEntity(crossFolder, new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							updateViewers();
							return null;
						}
					});	
				}
				else {
					log.info("Clearing cross viewer");
					crossesViewer.clear();
					updateViewers();
				}
				
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}
	
	private void updateViewers() {

		if (splitPickingFolder==null || workingFolder==null || searchResultsFolder==null) {
			IconDemoPanel mainViewer = getMainViewer();
			IconDemoPanel secViewer = getSecondaryViewer();
			mainViewer.clear();
			if (secViewer!=null) {
				secViewer.clear();
			}
		}
		else {
			loadViewers();
		}
	}
	
	private void loadViewers() {

		log.info("Loading viewers with working folder {}",workingFolder.getUniqueId());
		
		// Open the folders in the entity outline
		if (searchResultsFolder != null) {
			expandEntityOutline(searchResultsFolder.getUniqueId());
		}
		else {
			expandEntityOutline(workingFolder.getUniqueId());	
		}

		// Get the parent lines
		Entity entity1 = null;
		Entity entity2 = null;
		List<RootedEntity> selected = crossesViewer.getSelectedEntities();
		if (selected!=null && selected.size()==1) {
			Entity crossEntity = selected.get(0).getEntity();
			for(EntityData childEd : crossEntity.getOrderedEntityData()) {
				if (!childEd.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_ENTITY)) continue;
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
			private RootedEntity adFolder;
			private RootedEntity dbdFolder;
			
			@Override
			protected void doStuff() throws Exception {
				if (sourceEntity1 != null) {
					if (!EntityUtils.isInitialized(sourceEntity1)) {
						sourceEntity1 = ModelMgr.getModelMgr().getEntityById(sourceEntity1.getId());
					}
					log.info("Parent line 1 is {}",sourceEntity1.getName());
					adFolder = findDomainFolderContainingEntity(searchResultsFolder, sourceEntity1.getId(), FOLDER_NAME_SPLIT_LINES_AD);
				}
				if (sourceEntity2 != null) {
					if (!EntityUtils.isInitialized(sourceEntity2)) {
						sourceEntity2 = ModelMgr.getModelMgr().getEntityById(sourceEntity2.getId());
					}
					log.info("Parent line 2 is {}",sourceEntity1.getName());
					dbdFolder = findDomainFolderContainingEntity(searchResultsFolder, sourceEntity2.getId(), FOLDER_NAME_SPLIT_LINES_DBD);
				}
				if (adFolder==null && dbdFolder==null) {
					log.info("No cross is selected, so selecting latest search result");
					// Nothing is selected, so just load the last search result.
					RootedEntity latestResult = searchResultsFolder.getLatestChildOfType(EntityConstants.TYPE_FOLDER);
					if (latestResult==null) return;
					adFolder = ModelMgrUtils.getChildFolder(latestResult, FOLDER_NAME_SPLIT_LINES_AD, false);
					dbdFolder = ModelMgrUtils.getChildFolder(latestResult, FOLDER_NAME_SPLIT_LINES_DBD, false);
				}
			}
			
			private RootedEntity findDomainFolderContainingEntity(RootedEntity root, long entityId, String domainName) {
				for(RootedEntity searchResultFolder : root.getRootedChildren()) {
					RootedEntity domainFolder = searchResultFolder.getChildByName(domainName);
					for(Entity flylineEntity : domainFolder.getEntity().getChildren()) {
						if (flylineEntity.getId().equals(entityId)) return domainFolder;
					}
				}
				return null;
			}
			
			@Override
			protected void hadSuccess() {
				final IconDemoPanel mainViewer = getMainViewer();
				final IconDemoPanel secViewer = getSecondaryViewer();
				
				if (adFolder!=null) {
					log.info("Got AD folder: {}",adFolder.getUniqueId());
					showEntityInMainViewer(adFolder, new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							if (sourceEntity1!=null) {
								selectAndScroll(mainViewer, sourceEntity1.getId());	
							}
							return null;
						}
					});

				}
				else {
					mainViewer.clear();
				}
				
				if (dbdFolder!=null) {
					log.info("Got DBD folder: {}",dbdFolder.getUniqueId());
					showEntityInSecViewer(dbdFolder, new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							if (sourceEntity2!=null) {
								selectAndScroll(secViewer, sourceEntity2.getId());	
							}
							return null;
						}
					});
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

	private void showPopupResultFolderMenu() {
		
		checkNotNull(splitPickingFolder, "Root folder must be defined");

		JPopupMenu chooseFolderMenu = new JPopupMenu();
		
		for(final EntityData childEd : splitPickingFolder.getEntity().getOrderedEntityData()) {
			Entity child = childEd.getChildEntity();
			if (child==null) continue;
			
			JMenuItem commonRootItem = new JMenuItem(child.getName());
			commonRootItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					try {
						setWorkingFolderEntity(childEd);
					}
					catch (Exception e) {
						SessionMgr.getSessionMgr().handleException(e);
					}
				}
			});
			chooseFolderMenu.add(commonRootItem);
		}
		
		chooseFolderMenu.addSeparator();
		
		JMenuItem createNewItem = new JMenuItem("Create New...");
		
		createNewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				// Add button clicked
				final String folderName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Folder Name:\n",
						"Create working folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
				if ((folderName == null) || (folderName.length() <= 0)) {
					return;
				}

				try {
					Entity folder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
					EntityData childEd = ModelMgr.getModelMgr().addEntityToParent(splitPickingFolder.getEntity(), folder);
					setWorkingFolderEntity(childEd);
				}
				catch (Exception e) {
					SessionMgr.getSessionMgr().handleException(e);
				}
			}
		});
		
		chooseFolderMenu.add(createNewItem);
		
		chooseFolderMenu.show(resultFolderButton, 0, resultFolderButton.getHeight());
	}

    public List<Entity> getRepresentedFlylines(List<Entity> screenSamples) throws Exception {

    	List<Long> entityIds = new ArrayList<Long>();
    	for(Entity screenSample : screenSamples) {
    		Set<Long> parentIds = ModelMgr.getModelMgr().getParentIdsForAttribute(screenSample.getId(), EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
    		if (!parentIds.isEmpty()) {
    			entityIds.addAll(parentIds);
    		}
    	}
    	
    	return ModelMgr.getModelMgr().getEntityByIds(entityIds);
    }
    
    public void saveRepresentedGroupings(List<Entity> representedAd, List<Entity> representedDbd, RootedEntity folder) throws Exception {

    	RootedEntity groupAdFolder = ModelMgrUtils.getChildFolder(folder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, true);
    	RootedEntity groupDbdFolder = ModelMgrUtils.getChildFolder(folder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, true);

    	ModelMgr.getModelMgr().updateChildIndex(groupAdFolder.getEntityData(), 0);
    	ModelMgr.getModelMgr().updateChildIndex(groupDbdFolder.getEntityData(), 1);
    	ModelMgrUtils.fixOrderIndicies(folder.getEntity(),new Comparator<EntityData>() {
			@Override
			public int compare(EntityData o1, EntityData o2) {
				return ComparisonChain.start()
				.compare(o1.getOrderIndex(), o2.getOrderIndex(), Ordering.natural().nullsLast())
					.compare(o1.getId(), o2.getId()).result();
			}
		});
    	
    	log.info("Saving {} represented AD lines to {}",representedAd.size(),groupAdFolder.getId());
    	log.info("Saving {} represented DBD lines to {}",representedDbd.size(),groupDbdFolder.getId());
    	
    	List<Long> newSplitAdIds = EntityUtils.getEntityIdList(representedAd);
    	List<Long> newSplitDbdIds = EntityUtils.getEntityIdList(representedDbd);
    	
        // Remove current 
    	ModelMgrUtils.removeAllChildren(groupAdFolder.getEntity());
    	ModelMgrUtils.removeAllChildren(groupDbdFolder.getEntity());
        
        // Add new 
    	if (!newSplitAdIds.isEmpty()) {
    		ModelMgr.getModelMgr().addChildren(groupAdFolder.getEntity().getId(), newSplitAdIds, EntityConstants.ATTRIBUTE_ENTITY);
    	}
    	if (!newSplitDbdIds.isEmpty()) {
    		ModelMgr.getModelMgr().addChildren(groupDbdFolder.getEntity().getId(), newSplitDbdIds, EntityConstants.ATTRIBUTE_ENTITY);
    	}
    
    	ModelMgr.getModelMgr().refreshEntityAndChildren(groupAdFolder.getEntity());
    	ModelMgr.getModelMgr().refreshEntityAndChildren(groupDbdFolder.getEntity());
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

        if (chooser.showDialog(SessionMgr.getMainFrame(), "OK") == FileChooser.CANCEL_OPTION) {
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


				long numTotal = crossesViewer.getRootedEntities().size();
				long numProcessed = 0;
				for(RootedEntity crossRootedEntity : crossesViewer.getRootedEntities()) {
					
					ModelMgr.getModelMgr().refreshEntityAndChildren(crossRootedEntity.getEntity());
					
					Entity crossEntity = ModelMgr.getModelMgr().getEntityTree(crossRootedEntity.getEntity().getId());
					
					Entity flylineAd = null;
					Entity flylineDbd = null;
					
					if (crossEntity.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS)) {

						for (Entity child : crossEntity.getChildren()) {
							if (child.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
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
							log.warn("Could not find AD line for screen sample "+crossEntity.getName());
							continue;
						}
							
						if (flylineDbd == null) {
							log.warn("Could not find DBD line for screen sample "+crossEntity.getName());
							continue;
						}

						flylineAd = ModelMgr.getModelMgr().loadLazyEntity(flylineAd, false);
						flylineDbd = ModelMgr.getModelMgr().loadLazyEntity(flylineDbd, false);
						
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
				int rv = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Data was successfully exported to "+destFile+". Open file in default viewer?", 
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

    	worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Exporting data", "", 0, 100));
		worker.execute();
    }

    //---------------------------------Viewer Interactions
    private void showEntityInMainViewer( RootedEntity entity ) {
        getLanesTopComponent().showEntityInMainViewer(entity);
    }
    
    private void showEntityInMainViewer( RootedEntity entity, Callable callable ) {
        getLanesTopComponent().showEntityInMainViewer(entity, callable);
    }
    
    private void showEntityInSecViewer( RootedEntity entity, Callable callable ) {
        getLanesTopComponent().showEntityInSecViewer(entity, callable);
    }
    
    private void showEntityInSecViewer( RootedEntity entity ) {
        getLanesTopComponent().showEntityInSecViewer(entity);
    }
    
    private IconDemoPanel getSecondaryViewer() {
        SplitPickingLanesTopComponent lanes = getLanesTopComponent();
        return lanes.getSecondaryPanel();
    }

    private IconDemoPanel getMainViewer() {
        SplitPickingLanesTopComponent lanes = getLanesTopComponent();
        return lanes.getMainPanel();
    }

    private ViewerSplitPanel getViewerContainer() {
        return getLanesTopComponent().getViewerSplitPanel();
    }

    private JSplitPane getMainSplitPane() {
        return getViewerContainer().getMainSplitPane();
    }

    private MAASearchDialog getMAASearchDialog() {
        return SessionMgr.getBrowser().getMAASearchDialog();
    }
	
    private PatternSearchDialog getPatternSearchDialog() {
        // Push the split picking lanes into user's face, then
        // start the pattern search.  That way, once user has
        // searched, split picking lanes show.  Then do same
        // for main wizard panel to stack the focus order.
        TopComponent tc = getLanesTopComponent();
        tc.requestActive();
        tc = getTopComponent( SplitPickingTopComponent.PREFERRED_ID );
        tc.requestActive();
        
        return SessionMgr.getBrowser().getPatternSearchDialog();
    }

    private SplitPickingLanesTopComponent getLanesTopComponent() {
        SplitPickingLanesTopComponent rtnVal = null;
        TopComponent tc = getTopComponent(
                SplitPickingLanesTopComponent.PREFERRED_ID
        );
        if ( tc instanceof SplitPickingLanesTopComponent ) {
            if ( ! tc.isOpened() ) {
                tc.open();
            }
            SplitPickingLanesTopComponent lanes = (SplitPickingLanesTopComponent) tc;
            rtnVal = lanes;
        }
        
        return rtnVal;
    }
    
    private TopComponent getTopComponent( String preferredId ) {
        return WindowManager.getDefault().findTopComponent( preferredId );
    }

	private void expandEntityOutline(final String uniqueId) {
		SessionMgr.getBrowser().getEntityOutline().expandByUniqueId(uniqueId);
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
		return searchResultsFolder;
	}

	public void setRepFolder(RootedEntity repFolder) {
		this.searchResultsFolder = repFolder;
	}

	public RootedEntity getCrossFolder() {
		return crossFolder;
	}

	public void setCrossFolder(RootedEntity crossFolder) {
		this.crossFolder = crossFolder;
	}
	
}
