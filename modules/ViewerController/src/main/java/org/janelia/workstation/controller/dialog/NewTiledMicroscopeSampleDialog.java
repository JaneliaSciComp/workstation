package org.janelia.workstation.controller.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.controller.action.SaveTiledMicroscopeSampleAction;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.jdesktop.swingx.VerticalLayout;

public class NewTiledMicroscopeSampleDialog extends ModalDialog {
	
	private JTextField nameTextField = new JTextField(40);
	private JTextField pathToOctreeTextField = new JTextField(40);
	private JTextField pathToKTXTextField = new JTextField(40);
	private JTextField pathToRawTextField = new JTextField(40);

	private TmSample sample;

	public NewTiledMicroscopeSampleDialog() {

		setTitle("Add Tiled Microscope Sample");

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new VerticalLayout(5));

		JPanel attrPanel = new JPanel();
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

		// Figure out the user path preference
		c.gridx = 0;
		c.gridy = 1;
		attrPanel.add(new JLabel("Path To Render Folder:"), c);
		c.gridx = 1;
		attrPanel.add(pathToOctreeTextField, c);

		// Figure out the user path preference
		c.gridx = 0;
		c.gridy = 2;
		attrPanel.add(new JLabel("Path To KTX Folder (optional):"), c);
		c.gridx = 1;
		attrPanel.add(pathToKTXTextField, c);

		// Figure out the user path preference
		c.gridx = 0;
		c.gridy = 3;
		attrPanel.add(new JLabel("Path To RAW Folder (optional):"), c);
		c.gridx = 1;
		attrPanel.add(pathToRawTextField, c);

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

		String octreePath = sample.getLargeVolumeOctreeFilepath();
		String ktxPath = sample.getLargeVolumeKTXFilepath();
		String rawPath = sample.getTwoPhotonAcquisitionFilepath();

		if (octreePath!=null) {
			pathToOctreeTextField.setText(octreePath);
		}

		if (ktxPath!=null) {
			pathToKTXTextField.setText(ktxPath);
		}

		if (rawPath!=null) {
			pathToRawTextField.setText(rawPath);
		}

		packAndShow();
	}

	private void save() {

		String name = nameTextField.getText();
		String octree = pathToOctreeTextField.getText();
		String ktx = StringUtils.isBlank(pathToKTXTextField.getText()) ? null : pathToKTXTextField.getText();
		String raw = StringUtils.isBlank(pathToRawTextField.getText()) ? null : pathToRawTextField.getText();

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

		Action action = new SaveTiledMicroscopeSampleAction(sample, name, octree, ktx, raw);
		action.actionPerformed(null);
		NewTiledMicroscopeSampleDialog.this.dispose();
	}
}