package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.FolderUtils;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.screen.ScreenEvalConstants;
import org.janelia.it.jacs.shared.screen.ScreenEvalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for searching Arnim's MAA annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MAASearchDialog extends ModalDialog implements Accessibility, ActionListener {

	private static final Logger log = LoggerFactory.getLogger(MAASearchDialog.class);

	private Map<String, List<JCheckBox>> intCheckBoxMap = new HashMap<String, List<JCheckBox>>();
	private Map<String, List<JCheckBox>> distCheckBoxMap = new HashMap<String, List<JCheckBox>>();
	private Map<String, JLabel> compCountLabelMap = new HashMap<String, JLabel>();

	private Map<String, Entity> compEntityMap = new LinkedHashMap<String, Entity>();
	private Map<String, Integer> countMap;
	private Map<String, Entity> folderMap;
	private Map<String, List<Long>> cachedSampleEvals = new HashMap<String, List<Long>>();

	private JScrollPane scrollPane;
	private JPanel scorePanel;
	private JLabel selectionLabel;
	private JButton resetButton;
	private JTextField folderNameField;
	private JButton okButton;

	private RootedEntity outputFolder;
	private Browser browser;
	private RootedEntity saveFolder;
	private boolean returnInsteadOfSaving = false;

	public MAASearchDialog(Browser browser) {

		this.browser = browser;

		setTitle("MAA Screen Search");
		setPreferredSize(new Dimension(800, 800));
		setLayout(new BorderLayout());

		scorePanel = new JPanel(new MigLayout("wrap 4, ins 20", "[left][center][center][left]"));
		scrollPane = new JScrollPane();
		scrollPane.setViewportView(scorePanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(50);
		add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		selectionLabel = new JLabel("");
		buttonPane.add(selectionLabel);

		this.resetButton = new JButton("Reset");
		resetButton.setVisible(false);
		resetButton.setToolTipText("Reset all checkboxes");
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetCheckboxes();
			}
		});
		buttonPane.add(resetButton);

		buttonPane.add(Box.createHorizontalGlue());

		JLabel folderNameLabel = new JLabel("Save selected objects in folder: ");
		buttonPane.add(folderNameLabel);

		folderNameField = new JTextField(10);
		folderNameField.setToolTipText("Enter the folder name to save the results in");
		folderNameField.setMaximumSize(new Dimension(400, 20));
		buttonPane.add(folderNameField);

		this.okButton = new JButton("Save");
		okButton.setEnabled(false);
		okButton.setToolTipText("Save the results");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveResults();
			}
		});
		buttonPane.add(okButton);

		JButton cancelButton = new JButton("Close");
		cancelButton.setToolTipText("Close this dialog without saving results");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		buttonPane.add(cancelButton);

		add(buttonPane, BorderLayout.SOUTH);
		init();
	}

	public void showDialog() {

		if (!isAccessible()) return;
		
		this.outputFolder = null;
		this.returnInsteadOfSaving = false;
		packAndShow();
	}

	public RootedEntity showDialog(RootedEntity outputFolder) {
		
		if (!isAccessible()) return null;
		
		this.outputFolder = outputFolder;
		this.saveFolder = null;
		this.returnInsteadOfSaving = false;
		packAndShow();
		return saveFolder;
	}
	
	public List<Long> showDialog(boolean returnInsteadOfSaving) {

		if (!isAccessible())
			return new ArrayList<Long>();
		
		this.outputFolder = null;
		this.saveFolder = null;
		this.returnInsteadOfSaving = true;
		packAndShow();
		try {
			return getSelectedSamples();
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return new ArrayList<Long>();
		}
	}

	public String getSaveFolderName() {
		return folderNameField.getText();
	}
	
	public void init() {

		log.info("Begin loading");

		SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {

				Entity topLevelFolder = null;
				for (Entity entity : ModelMgr.getModelMgr().getCommonRootEntities()) {
					if (entity.getName().equals(ScreenEvalConstants.TOP_LEVEL_FOLDER_NAME)
							&& ModelMgrUtils.isOwner(entity)) {
						topLevelFolder = entity;
					}
				}

				if (topLevelFolder == null) {
					return;
				}

				topLevelFolder = ModelMgr.getModelMgr().loadLazyEntity(topLevelFolder, false);
				for (Entity child : topLevelFolder.getOrderedChildren()) {
					compEntityMap.put(child.getName(), child);
				}
			}

			@Override
			protected void hadSuccess() {

				if (countMap == null || folderMap == null) {
					loadCounts();
				}

				scorePanel.removeAll();
				scorePanel.add(new JLabel("Compartment"));
				scorePanel.add(new JLabel("Intensity"));
				scorePanel.add(new JLabel("Distribution"));
				scorePanel.add(new JLabel("Selected"));

				for (String compartment : compEntityMap.keySet()) {

					JLabel label = new JLabel(compartment);
					scorePanel.add(label);

					List<JCheckBox> intCheckBoxes = new ArrayList<JCheckBox>();
					JPanel intCheckboxPanel = new JPanel();
					for (int i = 0; i <= 5; i++) {
						JCheckBox checkBox = new JCheckBox("" + i);
						checkBox.addActionListener(MAASearchDialog.this);
						intCheckboxPanel.add(checkBox);
						intCheckBoxes.add(checkBox);

					}
					scorePanel.add(intCheckboxPanel);
					intCheckBoxMap.put(compartment, intCheckBoxes);

					List<JCheckBox> distCheckBoxes = new ArrayList<JCheckBox>();
					JPanel distCheckboxPanel = new JPanel();
					for (int d = 0; d <= 5; d++) {
						JCheckBox checkBox = new JCheckBox("" + d);
						checkBox.addActionListener(MAASearchDialog.this);
						distCheckboxPanel.add(checkBox);
						distCheckBoxes.add(checkBox);
					}
					scorePanel.add(distCheckboxPanel);
					distCheckBoxMap.put(compartment, distCheckBoxes);

					JLabel countLabel = new JLabel();
					scorePanel.add(countLabel, "gapleft 5lp");
					compCountLabelMap.put(compartment, countLabel);
				}

				scrollPane.revalidate();
				scrollPane.repaint();
			}

			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		worker.execute();
	}

	public void loadCounts() {

		selectionLabel.setText("Loading sample counts...");

		SimpleWorker worker = new SimpleWorker() {

			private Map<String, Integer> countMap = new HashMap<String, Integer>();
			private Map<String, Entity> folderMap = new HashMap<String, Entity>();

			@Override
			protected void doStuff() throws Exception {

				for (String compartment : compEntityMap.keySet()) {
					Entity compEntity = compEntityMap.get(compartment);
					compEntity = ModelMgr.getModelMgr().loadLazyEntity(compEntity, false);

					for (Entity intFolder : compEntity.getOrderedChildren()) {
						intFolder = ModelMgr.getModelMgr().loadLazyEntity(intFolder, false);
						int i = ScreenEvalUtils.getValueFromFolderName(intFolder);

						for (Entity distFolder : intFolder.getOrderedChildren()) {
							int d = ScreenEvalUtils.getValueFromFolderName(distFolder);
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							countMap.put(key, distFolder.getChildren().size());
							folderMap.put(key, distFolder);
						}
					}
				}
			}

			@Override
			protected void hadSuccess() {
				MAASearchDialog.this.countMap = this.countMap;
				MAASearchDialog.this.folderMap = this.folderMap;
				okButton.setEnabled(true);
				updateSampleCount();
				log.info("Completed loading, " + countMap.size() + " counts, dialog is ready");
			}

			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		worker.execute();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updateSampleCount();
	}

	private void updateSampleCount() {
		if (countMap == null)
			return;

		Integer min = null;

		for (String compartment : compEntityMap.keySet()) {
			List<JCheckBox> intCheckboxes = intCheckBoxMap.get(compartment);
			boolean intChecked = false;
			boolean compChecked = false;
			int compCount = 0;
			for (int i = 0; i < intCheckboxes.size(); i++) {
				JCheckBox intCheckbox = intCheckboxes.get(i);
				if (intCheckbox.isSelected()) {
					compChecked = intChecked = true;
					List<JCheckBox> distCheckboxes = distCheckBoxMap.get(compartment);
					boolean distChecked = false;
					for (int d = 0; d < distCheckboxes.size(); d++) {
						JCheckBox distCheckbox = distCheckboxes.get(d);
						if (distCheckbox.isSelected()) {
							compChecked = distChecked = true;
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compCount += countMap.get(key);
						}
					}
					if (!distChecked) {
						for (int d = 0; d < distCheckboxes.size(); d++) {
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compCount += countMap.get(key);
						}
					}
				}
			}

			if (!intChecked) {
				for (int i = 0; i < intCheckboxes.size(); i++) {
					List<JCheckBox> distCheckboxes = distCheckBoxMap.get(compartment);
					for (int d = 0; d < distCheckboxes.size(); d++) {
						JCheckBox distCheckbox = distCheckboxes.get(d);
						if (distCheckbox.isSelected()) {
							compChecked = true;
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compCount += countMap.get(key);
						}
					}
				}
			}

			compCountLabelMap.get(compartment).setText(compChecked ? compCount + "" : "");

			if (compChecked) {
				if (min == null || compCount < min) {
					min = compCount;
				}
			}
		}

		resetButton.setVisible(min != null);
		selectionLabel.setText(min == null ? "" : "At most " + min + " sample" + (min == 1 ? "" : "s"));
	}

	private void resetCheckboxes() {

		for (String compartment : compEntityMap.keySet()) {
			List<JCheckBox> intCheckboxes = intCheckBoxMap.get(compartment);
			for (int i = 0; i < intCheckboxes.size(); i++) {
				JCheckBox intCheckbox = intCheckboxes.get(i);
				intCheckbox.setSelected(false);
				List<JCheckBox> distCheckboxes = distCheckBoxMap.get(compartment);
				for (int d = 0; d < distCheckboxes.size(); d++) {
					JCheckBox distCheckbox = distCheckboxes.get(d);
					distCheckbox.setSelected(false);
				}
			}
		}

		updateSampleCount();
	}

	protected synchronized void saveResults() {

		if (returnInsteadOfSaving) {
			setVisible(false);
			return;
		}
		
		if (countMap == null || folderMap == null) {
			throw new IllegalStateException("Cannot save results before entity load is complete");
		}

		SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				saveFolder = FolderUtils.saveEntitiesToFolder(outputFolder == null ? null : outputFolder,
						folderNameField.getText(), getSelectedSamples());
			}

			@Override
			protected void hadSuccess() {
				final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
				entityOutline.totalRefresh(true, new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(
								EntitySelectionModel.CATEGORY_OUTLINE, saveFolder.getUniqueId(), true);
						Utils.setDefaultCursor(MAASearchDialog.this);
						setVisible(false);
						return null;
					}
				});
			}

			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				Utils.setDefaultCursor(MAASearchDialog.this);
			}
		};

		Utils.setWaitingCursor(MAASearchDialog.this);
		worker.execute();
	}

	private List<Long> getSelectedSamples() throws Exception {

		Set<Long> consensus = new HashSet<Long>();

		for (String compartment : compEntityMap.keySet()) {
			Set<Long> compSampleIds = new LinkedHashSet<Long>();
			boolean compChecked = false;

			List<JCheckBox> intCheckboxes = intCheckBoxMap.get(compartment);
			boolean intChecked = false;
			for (int i = 0; i < intCheckboxes.size(); i++) {
				JCheckBox intCheckbox = intCheckboxes.get(i);
				if (intCheckbox.isSelected()) {
					compChecked = intChecked = true;
					List<JCheckBox> distCheckboxes = distCheckBoxMap.get(compartment);
					boolean distChecked = false;
					for (int d = 0; d < distCheckboxes.size(); d++) {
						JCheckBox distCheckbox = distCheckboxes.get(d);
						if (distCheckbox.isSelected()) {
							compChecked = distChecked = true;
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compSampleIds.addAll(getSampleEvals(key));
						}
					}
					if (!distChecked) {
						for (int d = 0; d < distCheckboxes.size(); d++) {
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compSampleIds.addAll(getSampleEvals(key));
						}
					}
				}
			}

			if (!intChecked) {
				for (int i = 0; i < intCheckboxes.size(); i++) {
					List<JCheckBox> distCheckboxes = distCheckBoxMap.get(compartment);
					for (int d = 0; d < distCheckboxes.size(); d++) {
						JCheckBox distCheckbox = distCheckboxes.get(d);
						if (distCheckbox.isSelected()) {
							compChecked = true;
							String key = ScreenEvalUtils.getKey(compartment, i, d);
							compSampleIds.addAll(getSampleEvals(key));
						}
					}
				}
			}

			if (compChecked) {
				if (consensus.isEmpty()) {
					consensus.addAll(compSampleIds);
					log.info("Consensus is now: " + consensus.size());
				} else {
					consensus.retainAll(compSampleIds);
					log.info("Consensus is now: " + consensus.size() + " (filtered by " + compSampleIds.size() + ")");
				}
			}
		}

		return new ArrayList<Long>(new LinkedHashSet<Long>(consensus));
	}

	private List<Long> getSampleEvals(String key) throws Exception {

		List<Long> samples = cachedSampleEvals.get(key);

		if (samples == null) {
			samples = new ArrayList<Long>();

			Entity distFolder = folderMap.get(key);
			List<Long> maskIds = new ArrayList<Long>();
			for (EntityData ed : distFolder.getEntityData()) {
				if (ed.getChildEntity() != null) {
					maskIds.add(ed.getChildEntity().getId());
				}
			}

			if (!maskIds.isEmpty()) {
				List<String> upMapping = new ArrayList<String>();
				List<String> downMapping = new ArrayList<String>();
				upMapping.add(EntityConstants.TYPE_FOLDER);
				// TODO: this will be necessary (along with other changes) once Sean's entity restructuring is complete
				//upMapping.add(EntityConstants.TYPE_FOLDER);
				upMapping.add(EntityConstants.TYPE_SCREEN_SAMPLE);
				log.trace("Got " + maskIds.size() + " masks for " + key);
				List<MappedId> mappedIds = ModelMgr.getModelMgr().getProjectedResults(maskIds, upMapping, downMapping);
				for (MappedId mappedId : mappedIds) {
					samples.add(mappedId.getMappedId());
				}
			}

			cachedSampleEvals.put(key, samples);
			log.info("Got " + samples.size() + " samples for " + key);
		} else {
			log.info("Got " + samples.size() + " samples for " + key + " (cached)");
		}

		return samples;
	}

	public boolean isAccessible() {
		if (!"user:jenetta".equals(SessionMgr.getSubjectKey())) {
			log.info("User "+SessionMgr.getSubjectKey()+" is not user:jenetta");
			return false;
		}
		return true;
	}
}
