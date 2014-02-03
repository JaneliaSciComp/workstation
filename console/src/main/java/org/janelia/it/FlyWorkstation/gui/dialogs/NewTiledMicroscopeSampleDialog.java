package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.gui.framework.actions.CreateTiledMicroscopeSampleAction;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextField;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewTiledMicroscopeSampleDialog extends JDialog {
  Border border1;
  JLabel nameLabel = new JLabel();
  JLabel pathToRenderLabel = new JLabel();
  JPanel samplePanel = new JPanel();
  Border border2;
  TitledBorder titledBorder2;
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  StandardTextField nameTextField = new StandardTextField();
  StandardTextField pathToRenderFolderTextField = new StandardTextField();
  private JFrame parentFrame;

  public NewTiledMicroscopeSampleDialog(JFrame owner, String title, boolean modal) {
    super(owner, title, modal);
    this.parentFrame = owner;
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    setUpValues();
    this.setVisible(true);
  }


  private void setUpValues() {
    nameTextField.setText("");
    pathToRenderFolderTextField.setText("");
  }


  private void jbInit() throws Exception {
    border2 = new EtchedBorder(EtchedBorder.RAISED,Color.white,new Color(134, 134, 134));
    titledBorder2 = new TitledBorder(border2,"Add Sample");
    nameLabel.setText("Sample Name:");
    nameLabel.setBounds(new Rectangle(19, 28, 94, 27));
    border1 = new EtchedBorder(EtchedBorder.RAISED,Color.white,new Color(134, 134, 134));
    this.getContentPane().setLayout(null);
    pathToRenderLabel.setBounds(new Rectangle(19, 65, 94, 27));
    pathToRenderLabel.setText("Path to Render Data Folder:");

    samplePanel.setBorder(titledBorder2);
    samplePanel.setBounds(new Rectangle(6, 5, 324, 316));
    samplePanel.setLayout(null);
    okButton.setText("OK");
    okButton.setBounds(new Rectangle(42, 329, 99, 31));
    okButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            new CreateTiledMicroscopeSampleAction(nameTextField.getText(), pathToRenderFolderTextField.getText()).doAction();
            NewTiledMicroscopeSampleDialog.this.dispose();
        }
    });
    cancelButton.setBounds(new Rectangle(185, 329, 99, 31));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            NewTiledMicroscopeSampleDialog.this.dispose();
        }
    });
    nameTextField.setEditable(false);
    nameTextField.setBounds(new Rectangle(118, 28, 191, 27));
    pathToRenderFolderTextField.setEditable(false);
    pathToRenderFolderTextField.setBounds(new Rectangle(118, 65, 191, 27));
    this.getContentPane().add(samplePanel, null);
    samplePanel.add(nameTextField, null);
    samplePanel.add(pathToRenderFolderTextField, null);
    this.getContentPane().add(okButton, null);
    this.getContentPane().add(cancelButton, null);
    this.setSize(345, 397);
    this.setLocationRelativeTo(parentFrame);
  }
}