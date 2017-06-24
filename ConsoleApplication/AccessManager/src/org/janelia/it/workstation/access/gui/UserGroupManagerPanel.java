package org.janelia.it.workstation.access.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.access.gui.editor.GroupEditorPanel;
import org.janelia.it.workstation.access.gui.editor.UserEditorPanel;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Swing-based panel for managing users and groups. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserGroupManagerPanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(UserGroupManagerPanel.class);

    private final DefaultListModel<Subject> subjectListModel;
    private final JList<Subject> subjectList;
    private final JPanel editorPanel;
    private final List<Subject> subjects = new ArrayList<>();
    
    public UserGroupManagerPanel() {
        
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.4);

        this.subjectListModel = new DefaultListModel<>();
        this.subjectList = new JList<>(subjectListModel);
        subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectList.setLayoutOrientation(JList.VERTICAL);
        subjectList.setVisibleRowCount(-1);
        subjectList.setCellRenderer(new SubjectComboBoxRenderer());

//        subjectList.addMouseListener(new MouseHandler() {
//            @Override
//            protected void singleLeftClicked(MouseEvent e) {
//                if (e.isConsumed()) {
//                    return;
//                }
//                int row = subjectList.locationToIndex(e.getPoint());
//                subjectList.setSelectedIndex(row);
//                subjectSelected(subjectListModel.get(row));
//            }
//        });

        final ListSelectionModel listSelectionModel = subjectList.getSelectionModel();
        listSelectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int firstIndex = listSelectionModel.getMinSelectionIndex();
                subjectSelected(subjectListModel.get(firstIndex));
            }
        });
            
        JScrollPane scrollPane = new JScrollPane(subjectList);
        splitPane.setLeftComponent(scrollPane);
        
        editorPanel = new JPanel(new BorderLayout());
        splitPane.setRightComponent(editorPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    protected void subjectSelected(Subject subject) {
    
        JPanel editor = null;
        
        log.info("Loading subject in editor: {}",subject);
        
        if (subject instanceof Group) {
            editor = new GroupEditorPanel(subjects);
            ((GroupEditorPanel)editor).loadGroup((Group)subject);
        }
        else if (subject instanceof User) {
            editor = new UserEditorPanel(subjects);
            ((UserEditorPanel)editor).loadUser((User)subject);
        }
        else {
            throw new IllegalStateException("Subject must be a user or a group");
        }
        
        editorPanel.removeAll();
        editorPanel.add(editor, BorderLayout.CENTER);   
        editorPanel.updateUI();
    }

    public void refresh() throws Exception {

        SimpleWorker worker = new SimpleWorker() {

            private List<Subject> allSubjects;
            private List<Subject> mySubjects = new ArrayList<>();
            
            @Override
            protected void doStuff() throws Exception {

                this.allSubjects = DomainMgr.getDomainMgr().getSubjects();
                
                for (Subject subject : allSubjects) {
                    if (AccessManager.isAdmin()) {
                        // Admin users can manipulate all other users and groups
                        mySubjects.add(subject);
                    }
                    else {
                        // Non-admin users can only manipulate themselves and the groups they are admins for
                        User currUser = (User)AccessManager.getAccessManager().getAuthenticatedSubject();
                        if (subject.getId().equals(currUser.getId())) {
                            mySubjects.add(subject);    
                        }
                        else if (subject instanceof Group) {
                            Group group = (Group)subject;
                            UserGroupRole ugr = currUser.getRole(group.getKey());
                            if (ugr!=null && ugr.getRole().isAdmin()) {
                                mySubjects.add(subject);
                            }
                        }
                    }
                }
                
                DomainUtils.sortSubjects(subjects);
            }

            @Override
            protected void hadSuccess() {
                
                subjects.clear();
                subjects.addAll(allSubjects);
                
                // Refresh subject list 
                subjectListModel.clear();
                for(Subject subject : mySubjects) {
                    subjectListModel.addElement(subject);
                }
                
                // Clear editor
                editorPanel.removeAll();
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
}
