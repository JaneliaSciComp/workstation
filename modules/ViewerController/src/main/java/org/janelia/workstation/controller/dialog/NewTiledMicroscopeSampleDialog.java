package org.janelia.workstation.controller.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.controller.action.SaveTiledMicroscopeSampleAction;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.jdesktop.swingx.VerticalLayout;

public class NewTiledMicroscopeSampleDialog extends ModalDialog {
	
	private JTextField nameTextField = new JTextField(40);
	private JTextField pathToOctreeTextField = new JTextField(40);
	private JTextField pathToKTXTextField = new JTextField(40);
	private JTextField pathToAltFormatTextField = new JTextField(40);
	private JCheckBox rawCompressedField = new JCheckBox();
	private JComboBox sampleFormat = new JComboBox<>(new String[] {"KTX Sample", "Zarr Sample"});


	private TmSample sample;
	GridBagConstraints c;
	JPanel attrPanel;
	JLabel pathToOctreeLabel = new JLabel("Path To Render Folder:");
	JLabel pathToKTXLabel = new JLabel("Path To KTX Folder (optional):");
	JLabel pathToAltFormatLabel = new JLabel("Path To Sample Top Folder:");
	JPanel mainPanel;

	public NewTiledMicroscopeSampleDialog() {
        c = new GridBagConstraints();
		setTitle("Add Horta Sample");

		mainPanel = new JPanel();
		mainPanel.setLayout(new VerticalLayout(5));

		attrPanel = new JPanel();
		attrPanel.setLayout(new GridBagLayout());

		JLabel sampleNameLabel = new JLabel("Sample Name:");
		sampleNameLabel.setLabelFor(nameTextField);
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		attrPanel.add(sampleNameLabel, c);
		c.gridx = 1;
		attrPanel.add(nameTextField, c);

		JLabel sampleFormatLabel = new JLabel("Choose Sample Format:");
		sampleFormatLabel.setLabelFor(sampleFormat);
		c.gridx = 0;
		c.gridy = 1;
		attrPanel.add(sampleFormatLabel, c);
		c.gridx = 1;
		attrPanel.add(sampleFormat, c);

		// KTX Selections
		c.gridx = 0;
		c.gridy = 2;
		attrPanel.add(pathToOctreeLabel, c);
		c.gridx = 1;
		attrPanel.add(pathToOctreeTextField, c);
		c.gridx = 0;
		c.gridy = 3;
		attrPanel.add(pathToKTXLabel, c);
		c.gridx = 1;
		attrPanel.add(pathToKTXTextField, c);

		c.gridx = 0;
		c.gridy = 4;
		attrPanel.add(pathToAltFormatLabel, c);
		c.gridx = 1;
		attrPanel.add(pathToAltFormatTextField, c);

		// Zarr selections
		sampleFormat.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
						String formatSelected = (String)itemEvent.getItem();
						switch (formatSelected) {
							case "KTX Sample":
								pathToOctreeLabel.setVisible(true);
								pathToOctreeTextField.setVisible(true);
								pathToKTXLabel.setVisible(true);
								pathToKTXTextField.setVisible(true);
								pathToAltFormatLabel.setVisible(false);
								pathToAltFormatTextField.setVisible(false);
							    break;
							case "Zarr Sample":
								pathToOctreeLabel.setVisible(false);
								pathToOctreeTextField.setVisible(false);
								pathToKTXLabel.setVisible(false);
								pathToKTXTextField.setVisible(false);
								pathToAltFormatLabel.setVisible(true);
								pathToAltFormatTextField.setVisible(true);

								// Figure out the user path preference

								mainPanel.repaint();
								mainPanel.revalidate();
								break;
						}
					}
				}
		});

		mainPanel.add(attrPanel);
		add(mainPanel, BorderLayout.CENTER);

		JButton okButton = new JButton("Save Sample");
		okButton.addActionListener(e -> save());

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setToolTipText("Cancel and close this dialog");
		cancelButton.addActionListener(e -> setVisible(false));

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(cancelButton);

		add(buttonPane, BorderLayout.SOUTH);
	}

	public void showForNewSample() {
		sample = new TmSample();

		packAndShow();
	}

	public void showForSample(TmSample sample) {
		this.sample = sample;

		nameTextField.setText(sample.getName());

		Map<FileType,String> filepaths = sample.getFiles();

		String octreePath = filepaths.get(FileType.LargeVolumeOctree);
		String ktxPath = filepaths.get(FileType.LargeVolumeKTX);
		String altPath = filepaths.get(FileType.LargeVolumeZarr);

		if (octreePath!=null) {
			pathToOctreeTextField.setText(octreePath);
		}

		if (ktxPath!=null) {
			pathToKTXTextField.setText(ktxPath);
		}

		if (altPath!=null) {
			pathToAltFormatTextField.setText(altPath);
		}

		packAndShow();
	}

	private void save() {

		String name = nameTextField.getText();
		String octree = pathToOctreeTextField.getText();
		String ktx = StringUtils.isBlank(pathToKTXTextField.getText()) ? null : pathToKTXTextField.getText();
		String alt = StringUtils.isBlank(pathToAltFormatTextField.getText()) ? null : pathToAltFormatTextField.getText();
		if (octree.isEmpty()) {
			JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
					"You must specify both a sample name and location!",
					"Missing values",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (name.isEmpty()) {
			File file = new File(octree);
			name = file.getName();
		}

		Action action = new SaveTiledMicroscopeSampleAction(sample, name, octree, ktx, alt, rawCompressedField.isSelected());
		action.actionPerformed(null);
		NewTiledMicroscopeSampleDialog.this.dispose();
	}
}