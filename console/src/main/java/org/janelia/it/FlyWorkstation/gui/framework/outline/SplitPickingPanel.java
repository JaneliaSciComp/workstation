package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.SplitGroupingDialog;
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
	
	public static final String FOLDER_NAME_REPRESENTATIVES = "Representatives";
	public static final String FOLDER_NAME_SPLIT_LINES = "Split Lines";
	public static final String FOLDER_NAME_SPLIT_LINES_AD = "AD";
	public static final String FOLDER_NAME_SPLIT_LINES_DBD = "DBD";
	public static final String FOLDER_NAME_CROSSES = "Crosses";
	
	private static final int STEP_PANEL_HEIGHT = 35;
	private static final int MAX_FREE_CROSSES = 1;
	private static final int MAX_TOTAL_CROSSES = 10;
	
	private final SplitGroupingDialog splitGroupingDialog;
	private final JButton searchButton;
	private final JButton groupButton;
	private final JButton resultFolderButton;
	private final IconDemoPanel crossesPanel;
	private final JTextField methodField;
	private final JTextField blurField;

	private RootedEntity workingFolder;
	private RootedEntity repFolder;
	private RootedEntity splitLinesFolder;
	private RootedEntity groupAdFolder;
	private RootedEntity groupDbdFolder;
	private RootedEntity crossFolder;
	
	public SplitPickingPanel() {
	
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
	
		this.splitGroupingDialog = new SplitGroupingDialog(this);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
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

		
		JPanel computePanel = new JPanel();
		computePanel.setLayout(new BoxLayout(computePanel, BoxLayout.LINE_AXIS));
		JLabel computeLabel = new JLabel("4. Compute selected intersections: ");
		computePanel.add(computeLabel);
		JButton crossButton = new JButton("Compute");
		crossButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (workingFolder==null) {
					JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						if (crossFolder==null) {
							setCrossFolder(ModelMgrUtils.getChildFolder(workingFolder, FOLDER_NAME_CROSSES, true));
						}
					}
					
					@Override
					protected void hadSuccess() {
						crossesPanel.loadEntity(getCrossFolder());
						createCrosses(getCrossFolder());
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
			}
		});
		computePanel.add(crossButton);
		computePanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		computePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(computePanel);
		
		
		JPanel param1Panel = new JPanel();
		param1Panel.setLayout(new BoxLayout(param1Panel, BoxLayout.LINE_AXIS));
		JLabel param1Label = new JLabel("Method (0=minimum value, 1=geometric mean, 2=scaled product): ");
		param1Panel.add(Box.createHorizontalStrut(50));
		param1Panel.add(param1Label);		
		methodField = new JTextField("0");
		methodField.setMaximumSize(new Dimension(50, STEP_PANEL_HEIGHT));
		param1Panel.add(methodField);
		param1Panel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		param1Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(param1Panel);
		

		JPanel param2Panel = new JPanel();
		param2Panel.setLayout(new BoxLayout(param2Panel, BoxLayout.LINE_AXIS));
		JLabel param2Label = new JLabel("Gaussian kernel size for blur: ");
		param2Panel.add(Box.createHorizontalStrut(50));
		param2Panel.add(param2Label);
		blurField = new JTextField("3");
		blurField.setMaximumSize(new Dimension(50, STEP_PANEL_HEIGHT));
		param2Panel.add(blurField);
		param2Panel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		param2Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		mainPanel.add(param2Panel);
		
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
				
				Entity crossEntity = rootedEntity.getEntity();
				Entity sourceEntity1 = null;
				Entity sourceEntity2 = null;
				
				for(EntityData childEd : crossEntity.getOrderedEntityData()) {
					if (!childEd.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) continue;
					if (sourceEntity1 == null) {
						sourceEntity1 = childEd.getChildEntity();
					}
					else {
						sourceEntity2 = childEd.getChildEntity();
						break;
					}
				}

				if (sourceEntity1==null || sourceEntity2==null) {
					System.out.println("Error: incomplete sample cross entity: "+rootedEntityId);
					return;
				}
				
				final Entity finalEntity1 = sourceEntity1;
				final Entity finalEntity2 = sourceEntity2;
				
				SimpleWorker worker = new SimpleWorker() {

					private Entity sourceEntity1 = finalEntity1;
					private Entity sourceEntity2 = finalEntity2;
					
					@Override
					protected void doStuff() throws Exception {
						if (!EntityUtils.isInitialized(sourceEntity1)) {
							sourceEntity1 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity1.getId());
						}
						if (!EntityUtils.isInitialized(sourceEntity2)) {
							sourceEntity2 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity2.getId());
						}
					}
					
					@Override
					protected void hadSuccess() {
						final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
						if (mainViewer.getEntityById(""+sourceEntity1.getId()) == null) {
							mainViewer.loadEntity(rootedEntity, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									selectAndScroll(mainViewer, sourceEntity1.getId());	
									return null;
								}
							});
						}
						else {
							selectAndScroll(mainViewer, sourceEntity1.getId());	
						}
						
						final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer();
						if (secViewer.getEntityById(""+sourceEntity2.getId()) == null) {
							secViewer.loadEntity(rootedEntity, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									selectAndScroll(secViewer, sourceEntity2.getId());
									return null;
								}
							});
						}
						else {
							selectAndScroll(secViewer, sourceEntity2.getId());
						}
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
				
			}
		};
		
		add(crossesPanel, BorderLayout.CENTER);
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

	private void createCrosses(final RootedEntity resultFolder) {
		
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		final List<String> mainSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_MAIN_VIEW);		
		final List<String> secSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_SEC_VIEW);
		
		final List<Entity> samples1 = new ArrayList<Entity>();
		final List<Entity> samples2 = new ArrayList<Entity>();

		for(String mainSelectionId : mainSelectionIds) {
			Entity sample1 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW).getEntityById(mainSelectionId);
			if (!sample1.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a screen sample: "+sample1.getName(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			samples1.add(sample1);
		}

		for(String secSelectionId : secSelectionIds) {
			Entity sample2 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_SEC_VIEW).getEntityById(secSelectionId);
			if (!sample2.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a screen sample: "+sample2.getName(), "Error", JOptionPane.ERROR_MESSAGE);
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
			JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please select at least one Screen Sample in each viewer", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final Set<String> existingCrosses = new HashSet<String>();
		for(Entity sample1 : samples1) {
			for(Entity sample2 : samples2) {
				for(RootedEntity rootedEntity : crossesPanel.getRootedEntities()) {
					String crossName = rootedEntity.getEntity().getName();
					String c1 = createCrossName(sample1, sample2);
					String c2 = createCrossName(sample2, sample1);
					if (crossName.equals(c1) || crossName.equals(c2)) {
						existingCrosses.add(c1);
					}
				}
			}
		}
		
		final int numExisting = existingCrosses.size();
		if (numExisting > 0) {
			Object[] options = {"Yes", "No"};
			String message = numCrosses==1 ? 
					"The cross you have selected already exists in the result folder. Recompute it?" : 
					numExisting+" out of "+numCrosses+" crosses already exist in the result folder. Recompute them?";
			int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getBrowser(), message, "Recompute?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (deleteConfirmation == 0) {
				existingCrosses.clear();
			}
		}
		
		if (existingCrosses.size()==numCrosses) return;
		
		SimpleWorker worker = new SimpleWorker() {

			private Entity firstCross;
			
			@Override
			protected void doStuff() throws Exception {
				
				Entity parent = resultFolder.getEntity();
				
				List<Long> sampleIds1 = new ArrayList<Long>();
				List<Long> sampleIds2 = new ArrayList<Long>();
				List<Long> outputIds = new ArrayList<Long>();
				
				System.out.println("Processing "+numCrosses+" crosses");
				
				int i = 0;
				for(Entity sample1 : samples1) {
					for(Entity sample2 : samples2) {
						setProgress(i++, numCrosses+1);
						
						String crossName = createCrossName(sample1, sample2);
						if (existingCrosses.contains(crossName)) {
							continue;
						}
						
						Entity cross = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS, crossName);
						
						List<Long> childrenIds = new ArrayList<Long>();
						childrenIds.add(sample1.getId());
						childrenIds.add(sample2.getId());
						ModelMgr.getModelMgr().addChildren(cross.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
						
						sampleIds1.add(sample1.getId());
						sampleIds2.add(sample2.getId());
						outputIds.add(cross.getId());

						if (firstCross==null) {
							firstCross = cross;
						}
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
						selectAndScroll(crossesPanel, firstCross.getId());
						return null;
					}
				});
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Submitting jobs to the compute cluster...", "", 0, 100));
		worker.execute();
	}

    private void startIntersections(List<Long> sampleIdList1, List<Long> sampleIdList2, List<Long> outputIdList3) throws Exception {

    	String idList1Str = commafy(sampleIdList1);
    	String idList2Str = commafy(sampleIdList2);
    	String idList3Str = commafy(outputIdList3);
    	String method = methodField.getText();
    	String kernelSize = blurField.getText();
    	
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
        
        ModelMgr.getModelMgr().submitJob("ScreenSampleCrossService", task.getObjectId());
    }

	private String createCrossName(Entity sample1, Entity sample2) {
		String[] parts1 = sample1.getName().split("-");
		String[] parts2 = sample2.getName().split("-");
		return parts1[0]+"+"+parts2[0];
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
				final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
				if (groupAdFolder==null && groupDbdFolder==null) {
					if (repFolder!=null) {
						mainViewer.loadEntity(repFolder);
					}
					else {
						mainViewer.clear();
					}
					IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_SEC_VIEW);
					if (secViewer!=null) {
						secViewer.clear();
					}
				}
				else {
					final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer();
					if (groupAdFolder!=null) {
	    				mainViewer.loadEntity(groupAdFolder);
					}
					else {
						mainViewer.clear();
					}
					
					if (groupDbdFolder!=null) {
	    				secViewer.loadEntity(groupDbdFolder);
					}
					else {
						secViewer.clear();
					}
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

	private void showPopupResultFolderMenu() {

		JPopupMenu chooseFolderMenu = new JPopupMenu();
		
		List<EntityData> rootEds = SessionMgr.getBrowser().getEntityOutline().getRootEntity().getOrderedEntityData();
		
		for(EntityData rootEd : rootEds) {
			final Entity commonRoot = rootEd.getChildEntity();
			if (!commonRoot.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
			
			JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
			commonRootItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					SimpleWorker worker = new SimpleWorker() {
						@Override
						protected void doStuff() throws Exception {
							setResultFolder(commonRoot);							
						}
						@Override
						protected void hadSuccess() {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									SessionMgr.getBrowser().getEntityOutline().selectEntityByUniqueId(workingFolder.getUniqueId());
								}
							});
						}
						@Override
						protected void hadError(Throwable error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					};
					worker.execute();
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

				SimpleWorker worker = new SimpleWorker() {
					@Override
					protected void doStuff() throws Exception {
						setResultFolder(ModelMgrUtils.createNewCommonRoot(folderName));
					}
					@Override
					protected void hadSuccess() {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								SessionMgr.getBrowser().getEntityOutline().selectEntityByUniqueId(workingFolder.getUniqueId());
							}
						});
					}
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				worker.execute();
			}
		});
		
		chooseFolderMenu.add(createNewItem);
		
		chooseFolderMenu.show(resultFolderButton, 0, resultFolderButton.getHeight());
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
