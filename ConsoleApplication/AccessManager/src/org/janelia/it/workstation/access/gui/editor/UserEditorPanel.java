package org.janelia.it.workstation.access.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.ComboMembershipListPanel;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

public class UserEditorPanel extends JPanel {

    private final JLabel keyInput;
    private final JTextField nameInput;
    private final JTextField emailInput;
    private final ComboMembershipListPanel<Subject> groupsPanel;
    
    private List<Subject> subjects;
    private User user;
    
    public UserEditorPanel(List<Subject> subjects) {

        this.subjects = subjects;
        
        setLayout(new BorderLayout());

        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();
        
        attrPanel.addSeparator("User Attributes");
        
        keyInput = new JLabel();
        attrPanel.addItem("User Name", keyInput);
        
        nameInput = new JTextField(30);
        attrPanel.addItem("Full Name", nameInput);

        emailInput = new JTextField(30);
        attrPanel.addItem("Email", emailInput);
        
        attrPanel.addSeparator("Group Membership");
        
        groupsPanel = new ComboMembershipListPanel<>("Groups", SubjectComboBoxRenderer.class);
        attrPanel.addItem(groupsPanel);
        
        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Save changes");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(saveButton);
        buttonPane.add(Box.createHorizontalGlue());
        
        add(attrPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        
    }
    
    public void loadUser(final User user) {

        this.user = user;
        Utils.setWaitingCursor(UserEditorPanel.this);

        SimpleWorker worker = new SimpleWorker() {

            private final List<Subject> allGroups = new ArrayList<>();
            private final List<Subject> currGroups = new ArrayList<>();
            
            @Override
            protected void doStuff() throws Exception {
                
                for (Subject subject : DomainMgr.getDomainMgr().getSubjects()) {
                    if (subject.getKey().startsWith("group:")) {
                        allGroups.add(subject);
                        if (user.getGroups().contains(subject.getKey())) {
                            currGroups.add(subject);    
                        }
                    }
                }

                DomainUtils.sortSubjects(allGroups);
                DomainUtils.sortSubjects(currGroups);
            }

            @Override
            protected void hadSuccess() {
                
                keyInput.setText(user.getKey());
                nameInput.setText(user.getFullName());
                emailInput.setText(user.getEmail());
                groupsPanel.setEditable(AccessManager.isAdmin());
                groupsPanel.initItemsInCombo(allGroups);
                for (Subject subject : currGroups) {
                    groupsPanel.addItemToList(subject);
                }
                
                Utils.setDefaultCursor(UserEditorPanel.this);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(UserEditorPanel.this);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();        
    }

    private void refresh() {
        
        
    }
    
    private void saveChanges() {
        
        user.setFullName(nameInput.getText());
        user.setEmail(emailInput.getText());
        
        user.getGroups().clear();
        for(Subject group : groupsPanel.getItemsInList()) {
            user.getGroups().add(group.getKey());
        }
        
        // TODO: persist user
    }
}
