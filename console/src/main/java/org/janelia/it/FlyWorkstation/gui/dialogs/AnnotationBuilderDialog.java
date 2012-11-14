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
    private JTextField annotationTextField;
    private StringBuilder annotationValue = new StringBuilder();

    public AnnotationBuilderDialog(){
        super(SessionMgr.getBrowser(),"Edit Value", true);
        TreeSet<String> ontologyTermSet = ModelMgr.getModelMgr().getOntologyTermSet(ModelMgr.getModelMgr().getCurrentOntology());
        final JComboBox comboBox = new JComboBox(ontologyTermSet.toArray());
        comboBox.setEditable(true);
        comboBox.setSelectedItem(null);
        AutoCompleteDecorator.decorate(comboBox);
        annotationTextField = new JTextField();

        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder(annotationTextField.getText());
                if(annotationValue.toString().endsWith(" - ")){
                    int lastIndex = annotationValue.lastIndexOf(" - ");
                    annotationValue = annotationValue.delete(lastIndex,lastIndex+3);
                }
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });

        final JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder(annotationTextField.getText());
                int position = annotationTextField.getCaretPosition();
                if(null!=comboBox.getSelectedItem()){
                    annotationValue.insert(position, comboBox.getSelectedItem().toString()+" - ");
                }
                comboBox.setSelectedItem(null);
                annotationTextField.setText(annotationValue.toString());
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
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder();
                comboBox.setSelectedItem(null);
                annotationTextField.setText(annotationValue.toString());
            }
        });

        JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder();
                annotationTextField.setText("");
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
        textBox.add(annotationTextField);
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

    public String getAnnotationValue(){
        return annotationValue.toString();
    }

    public void setAnnotationValue(String pathString1){
        annotationValue = new StringBuilder(pathString1);
    }

    public void setAnnotationTextField(String pathText1){
        annotationTextField.setText(pathText1);
    }
}
