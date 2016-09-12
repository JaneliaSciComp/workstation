package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.OntologyNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 * Created with IntelliJ IDEA. User: kimmelr Date: 6/28/12 Time: 3:45 PM
 */
public class AnnotationBuilderDialog extends JDialog {

    private JPanel annotationPanel = new JPanel();
    private JTextField annotationTextField;
    private String originalAnnotationText;
    private StringBuilder annotationValue = new StringBuilder();
    private boolean cancelled = false;

    public AnnotationBuilderDialog() {
        super(SessionMgr.getMainFrame(), "Edit Value", true);
        OntologyNode ontologyNode = OntologyExplorerTopComponent.getInstance().getOntologyNode();
        Ontology currOntology = ontologyNode == null ? null : ontologyNode.getOntology();
        TreeSet<String> terms = currOntology==null?new TreeSet<String>():DomainMgr.getDomainMgr().getModel().getOntologyTermSet(currOntology);
        final JComboBox comboBox = new JComboBox(terms.toArray());
        comboBox.setEditable(true);
        comboBox.setSelectedItem(null);
        AutoCompleteDecorator.decorate(comboBox);
        annotationTextField = new JTextField();

        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.PAGE_AXIS));

        final JButton doneButton = new JButton("Save and Close");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder(annotationTextField.getText());
                if (annotationValue.toString().endsWith(" - ")) {
                    int lastIndex = annotationValue.lastIndexOf(" - ");
                    annotationValue = annotationValue.delete(lastIndex, lastIndex + 3);
                }
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });

        final JButton addButton = new JButton("Add Term");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationValue = new StringBuilder(annotationTextField.getText());
                int position = annotationTextField.getCaretPosition();
                if (null != comboBox.getSelectedItem()) {
                    annotationValue.insert(position, comboBox.getSelectedItem().toString() + " - ");
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

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                AnnotationBuilderDialog.this.setVisible(false);
            }
        });
        // Add the scroll pane to this panel.
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
        buttonPanel.add(cancelButton);
        annotationPanel.add(textBox);
        annotationPanel.add(comboBoxPanel);
        annotationPanel.add(buttonPanel);

        createAndShowGUI();
        comboBox.requestFocus();
    }

    private void createAndShowGUI() {
        // Create and set up the window.
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(SessionMgr.getBrowser().getIconImage());
        this.setLocationRelativeTo(SessionMgr.getMainFrame());
        annotationPanel.setOpaque(true); // content panes must be opaque
        this.setContentPane(annotationPanel);

        // Display the window.
        this.pack();
    }

    public String getAnnotationValue() {
        return cancelled?null:annotationValue.toString();
    }

    public void setAnnotationValue(String newAnnotationValue) {
        if (null == newAnnotationValue) {
            newAnnotationValue = "";
        }
        originalAnnotationText = newAnnotationValue;
        annotationValue = new StringBuilder(newAnnotationValue);
        annotationTextField.setText(newAnnotationValue);
    }

    public String getOriginalAnnotationText() {
        return originalAnnotationText;
    }
}
