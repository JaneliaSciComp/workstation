package org.janelia.it.FlyWorkstation.gui.framework.bookmark;

import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextArea;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextField;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Title:        Genome Browser Client
 * Description:  This project is for JBuilder 4.0
 * @author
 * @version $Id: BookmarkPropertyDialog.java,v 1.2 2011/03/08 16:16:48 saffordt Exp $
 */

public class BookmarkPropertyDialog extends JDialog {
  Border border1;
  JLabel idLabel = new JLabel();
  JLabel searchLabel = new JLabel();
  JLabel typeLabel = new JLabel();
  JLabel speciesLabel = new JLabel();
  JLabel urlLabel = new JLabel();
  JLabel commentsLabel = new JLabel();
  JPanel bookmarkPanel = new JPanel();
  Border border2;
  TitledBorder titledBorder2;
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  StandardTextField oidTextField = new StandardTextField();
  StandardTextField searchTextField = new StandardTextField();
  StandardTextField typeTextField = new StandardTextField();
  StandardTextField speciesTextField = new StandardTextField();
  StandardTextField urlTextField = new StandardTextField();
  private BookmarkInfo targetInfo;
  private JFrame parentFrame;
  JScrollPane jScrollPane1 = new JScrollPane();
  StandardTextArea commentsTextArea = new StandardTextArea();

  public BookmarkPropertyDialog(JFrame owner, String title, boolean modal,
      BookmarkInfo targetInfo) {
    super(owner, title, modal);
    this.parentFrame = owner;
    this.targetInfo = targetInfo;
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
    oidTextField.setText(targetInfo.getId().toString());
    searchTextField.setText(targetInfo.getSearchValue());
    typeTextField.setText(targetInfo.getBookmarkType());
    speciesTextField.setText(targetInfo.getSpecies());
    urlTextField.setText(targetInfo.getBookmarkURLText());
    urlTextField.setCaretPosition(0);
    commentsTextArea.setText(targetInfo.getComments());
  }


  private void jbInit() throws Exception {
    border2 = new EtchedBorder(EtchedBorder.RAISED,Color.white,new Color(134, 134, 134));
    titledBorder2 = new TitledBorder(border2,"Bookmark Properties");
    idLabel.setText("OID:");
    idLabel.setBounds(new Rectangle(19, 28, 94, 27));
    border1 = new EtchedBorder(EtchedBorder.RAISED,Color.white,new Color(134, 134, 134));
    this.getContentPane().setLayout(null);
    searchLabel.setBounds(new Rectangle(19, 65, 94, 27));
    searchLabel.setText("Search Value:");
    typeLabel.setBounds(new Rectangle(19, 103, 94, 27));
    typeLabel.setText("Type:");
    speciesLabel.setBounds(new Rectangle(19, 139, 94, 27));
    speciesLabel.setText("Species:");
    urlLabel.setBounds(new Rectangle(19, 174, 94, 27));
    urlLabel.setText("URL:");
    commentsLabel.setText("Comments:");
    commentsLabel.setBounds(new Rectangle(19, 205, 94, 27));
    commentsTextArea.setLineWrap(true);
    commentsTextArea.setWrapStyleWord(true);
    commentsTextArea.setRows(4);

    bookmarkPanel.setBorder(titledBorder2);
    bookmarkPanel.setBounds(new Rectangle(6, 5, 324, 316));
    bookmarkPanel.setLayout(null);
    okButton.setText("OK");
    okButton.setBounds(new Rectangle(42, 329, 99, 31));
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        targetInfo.setComments(commentsTextArea.getText().trim());
        BookmarkPropertyDialog.this.dispose();
      }
    });
    cancelButton.setBounds(new Rectangle(185, 329, 99, 31));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        BookmarkPropertyDialog.this.dispose();
      }
    });
    oidTextField.setEditable(false);
    oidTextField.setBounds(new Rectangle(118, 28, 191, 27));
    searchTextField.setEditable(false);
    searchTextField.setBounds(new Rectangle(118, 65, 191, 27));
    typeTextField.setEditable(false);
    typeTextField.setBounds(new Rectangle(118, 103, 191, 27));
    speciesTextField.setEditable(false);
    speciesTextField.setBounds(new Rectangle(118, 139, 191, 27));
    urlTextField.setEditable(false);
    urlTextField.setBounds(new Rectangle(118, 174, 191, 27));
    jScrollPane1.setBounds(new Rectangle(18, 228, 293, 79));
    this.getContentPane().add(bookmarkPanel, null);
    bookmarkPanel.add(speciesLabel, null);
    bookmarkPanel.add(searchLabel, null);
    bookmarkPanel.add(typeLabel, null);
    bookmarkPanel.add(idLabel, null);
    bookmarkPanel.add(urlLabel, null);
    bookmarkPanel.add(oidTextField, null);
    bookmarkPanel.add(searchTextField, null);
    bookmarkPanel.add(typeTextField, null);
    bookmarkPanel.add(speciesTextField, null);
    bookmarkPanel.add(urlTextField, null);
    bookmarkPanel.add(commentsLabel, null);
    bookmarkPanel.add(jScrollPane1, null);
    jScrollPane1.getViewport().add(commentsTextArea, null);
    this.getContentPane().add(okButton, null);
    this.getContentPane().add(cancelButton, null);
    this.setSize(345, 397);
    this.setLocationRelativeTo(parentFrame);
  }
}