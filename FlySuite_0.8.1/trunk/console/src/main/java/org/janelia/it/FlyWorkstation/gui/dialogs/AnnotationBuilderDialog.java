package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 6/28/12
 * Time: 3:45 PM
 */
public class AnnotationBuilderDialog extends JDialog{

    private JPanel annotationPanel = new JPanel();
    private JTextField pathText;
    private StringBuilder pathString = new StringBuilder();

    public AnnotationBuilderDialog(){
        super(SessionMgr.getBrowser(),"Path Annotation", true);
        TreeSet<String> ontologyTermSet = ModelMgr.getModelMgr().getOntologyTermSet(ModelMgr.getModelMgr().getCurrentOntology());
        final JComboBox comboBox = new JComboBox(ontologyTermSet.toArray());
        comboBox.setEditable(true);
        comboBox.setSelectedItem(null);
        AutoCompleteDecorator.decorate(comboBox);
        pathText = new JTextField();

        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pathString = new StringBuilder(pathText.getText());
                if(pathString.toString().endsWith(" - ")){
                    int lastIndex = pathString.lastIndexOf(" - ");
                    pathString = pathString.delete(lastIndex,lastIndex+3);
                }
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });

        final JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pathString = new StringBuilder(pathText.getText());
                int position = pathText.getCaretPosition();
                if(null!=comboBox.getSelectedItem()){
                    pathString.insert(position, comboBox.getSelectedItem().toString()+" - ");
                }
                comboBox.setSelectedItem(null);
                pathText.setText(pathString.toString());
            }
        });

        comboBox.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char keyPressed = e.getKeyChar();
                if (keyPressed == KeyEvent.VK_ENTER) {
                    addButton.doClick();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pathString = new StringBuilder();
                comboBox.setSelectedItem(null);
                pathText.setText(pathString.toString());
            }
        });

        JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pathString = new StringBuilder();
                pathText.setText("");
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });
        //Add the scroll pane to this panel.
        JPanel textBox = new JPanel();
        JPanel comboBoxPanel = new JPanel();
        TitledBorder titledBorder1 = BorderFactory.createTitledBorder("Annotation Text");
        TitledBorder titledBorder2 = BorderFactory.createTitledBorder("Term to be Added");
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.PAGE_AXIS));
        comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.PAGE_AXIS));
        textBox.add(pathText);
        textBox.setBorder(titledBorder1);
        comboBoxPanel.add(comboBox);
        comboBoxPanel.setBorder(titledBorder2);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(addButton);
        buttonPanel.add(doneButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        annotationPanel.add(textBox);
        annotationPanel.add(comboBoxPanel);
        annotationPanel.add(buttonPanel);

        createAndShowGUI();
        comboBox.requestFocus();
    }

    private void createAndShowGUI() {
        //Create and set up the window.

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(SessionMgr.getBrowser().getIconImage());
        this.setLocationRelativeTo(SessionMgr.getBrowser());
        annotationPanel.setOpaque(true); //content panes must be opaque
        this.setContentPane(annotationPanel);

        //Display the window.
        this.pack();
    }

    public String getPathString(){
        return pathString.toString();
    }

    public void setPathString(String pathString1){
        pathString = new StringBuilder(pathString1);
    }

    public void setPathText(String pathText1){
        pathText.setText(pathText1);
    }
}
