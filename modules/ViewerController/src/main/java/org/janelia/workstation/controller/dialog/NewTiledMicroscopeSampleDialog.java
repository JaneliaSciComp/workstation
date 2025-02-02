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
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.controller.action.SaveTiledMicroscopeSampleAction;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.jdesktop.swingx.VerticalLayout;

public class NewTiledMicroscopeSampleDialog extends ModalDialog {
	
	private final JTextField nameTextField = new JTextField(40);
	private final JTextField pathToOctreeTextField = new JTextField(40);
	private final JTextField pathToKTXTextField = new JTextField(40);
	private final JTextField pathToOmeZarrFormatTextField = new JTextField(40);
	private final JCheckBox storageCredentialsRequiredCheckbox = new JCheckBox();
	private final JTextField storageAccessKeyTextField = new JTextField(40);
	private final JTextField storageSecretKeyTextField = new JTextField(40);
	private final JTextField storageRegionTextField = new JTextField(40);
	private final JCheckBox rawCompressedField = new JCheckBox();
	private final JComboBox<String> sampleFormat = new JComboBox<>(new String[] {ktxSample, zarrSample});

	private static final String ktxSample = "KTX Sample";
	private static final String zarrSample ="OME-Zarr Sample";

	private TmSample sample;
	GridBagConstraints c;
	JPanel attrPanel;
	JLabel pathToOctreeLabel = new JLabel("Path To Render Folder:");
	JLabel pathToKTXLabel = new JLabel("Path To KTX Folder (optional):");
	JLabel pathToOmeZarrFormatLabel = new JLabel("Path To OME-Zarr Fileset:");
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
		c.anchor = GridBagConstraints.WEST;
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

		// Zarr Selections
		c.gridx = 0;
		c.gridy = 4;
		attrPanel.add(pathToOmeZarrFormatLabel, c);
		c.gridx = 1;
		attrPanel.add(pathToOmeZarrFormatTextField, c);

		c.gridx = 0;
		c.gridy = 5;
		JLabel storageCredentialsRequiredLabel = new JLabel("Storage requires credentials");
		attrPanel.add(storageCredentialsRequiredLabel, c);
		c.gridx = 1;
		attrPanel.add(storageCredentialsRequiredCheckbox, c);

		JLabel accessKeyLabel = new JLabel("Storage Access Key:");
		accessKeyLabel.setLabelFor(storageAccessKeyTextField);
		c.gridx = 0;
		c.gridy = 6;
		attrPanel.add(accessKeyLabel, c);
		c.gridx = 1;
		attrPanel.add(storageAccessKeyTextField, c);
		accessKeyLabel.setVisible(false);
		storageAccessKeyTextField.setVisible(false);

		JLabel secretKeyLabel = new JLabel("Storage Secret Key:");
		secretKeyLabel.setLabelFor(storageSecretKeyTextField);
		c.gridx = 0;
		c.gridy = 7;
		attrPanel.add(secretKeyLabel, c);
		c.gridx = 1;
		attrPanel.add(storageSecretKeyTextField, c);
		secretKeyLabel.setVisible(false);
		storageSecretKeyTextField.setVisible(false);

		JLabel storageRegionLabel = new JLabel("Storage Region:");
		storageRegionLabel.setLabelFor(storageRegionTextField);
		c.gridx = 0;
		c.gridy = 8;
		attrPanel.add(storageRegionLabel, c);
		c.gridx = 1;
		attrPanel.add(storageRegionTextField, c);
		storageRegionLabel.setVisible(false);
		storageRegionTextField.setVisible(false);

		storageCredentialsRequiredCheckbox.addActionListener(e -> {
            if (storageCredentialsRequiredCheckbox.isSelected()) {
                accessKeyLabel.setVisible(true);
				storageAccessKeyTextField.setVisible(true);
                secretKeyLabel.setVisible(true);
				storageSecretKeyTextField.setVisible(true);
				storageRegionLabel.setVisible(true);
				storageRegionTextField.setVisible(true);
			} else {
                accessKeyLabel.setVisible(false);
				storageAccessKeyTextField.setVisible(false);
                secretKeyLabel.setVisible(false);
				storageSecretKeyTextField.setVisible(false);
				storageRegionLabel.setVisible(false);
				storageRegionTextField.setVisible(false);
            }
        });

		// Zarr selections
		sampleFormat.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
						String formatSelected = (String)itemEvent.getItem();
						switch (formatSelected) {
							case ktxSample:
								pathToOctreeLabel.setVisible(true);
								pathToOctreeTextField.setVisible(true);
								pathToKTXLabel.setVisible(true);
								pathToKTXTextField.setVisible(true);
								pathToOmeZarrFormatLabel.setVisible(false);
								pathToOmeZarrFormatTextField.setVisible(false);
							    break;
							case zarrSample:
								pathToOctreeLabel.setVisible(false);
								pathToOctreeTextField.setVisible(false);
								pathToKTXLabel.setVisible(false);
								pathToKTXTextField.setVisible(false);
								pathToOmeZarrFormatLabel.setVisible(true);
								pathToOmeZarrFormatTextField.setVisible(true);

								// Figure out the user path preference

								mainPanel.repaint();
								mainPanel.revalidate();
								break;
						}
					}
				}
		});

        // default is to not show Zarr field
		pathToOmeZarrFormatLabel.setVisible(false);
		pathToOmeZarrFormatTextField.setVisible(false);

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
			pathToOmeZarrFormatTextField.setText(altPath);
			sampleFormat.setSelectedItem(zarrSample);
		}

		packAndShow();
	}

	private void save() {

		String name = nameTextField.getText();
		String octree = pathToOctreeTextField.getText();
		String ktx = StringUtils.isBlank(pathToKTXTextField.getText()) ? null : pathToKTXTextField.getText();
		String alt = StringUtils.isBlank(pathToOmeZarrFormatTextField.getText()) ? null : pathToOmeZarrFormatTextField.getText();
		String accessKey = StringUtils.isBlank(storageAccessKeyTextField.getText()) ? null : storageAccessKeyTextField.getText();
		String secretKey = StringUtils.isBlank(storageSecretKeyTextField.getText()) ? null : storageSecretKeyTextField.getText();
		String storageRegion = StringUtils.isBlank(storageRegionTextField.getText()) ? null : storageRegionTextField.getText();
		if (sampleFormat.getSelectedItem()==ktxSample && octree.isEmpty()) {
			JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
					"You must specify both a sample name and location!",
					"Missing values",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (sampleFormat.getSelectedItem()==zarrSample && alt.isEmpty()) {
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

		Action action = new SaveTiledMicroscopeSampleAction(
				sample, name, octree, ktx, alt, rawCompressedField.isSelected(), accessKey, secretKey, storageRegion
		);
		action.actionPerformed(null);
		NewTiledMicroscopeSampleDialog.this.dispose();
	}
}