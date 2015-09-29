package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for viewing the list of accessible fly line releases, editing them, and adding new ones. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleaseDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(Browser.class);
    
    private static Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    private FlyLineReleaseListDialog parentDialog;
	
    private JPanel attrPanel;
    private JTextField nameInput;
    private SubjectListPanel annotatorsPanel;
    private SubjectListPanel subscribersPanel;
    
    private Entity releaseEntity;
   
    public FlyLineReleaseDialog(FlyLineReleaseListDialog parentDialog) {

    	this.parentDialog = parentDialog;
    	
        setTitle("Fly Line Release Definition");
    	
        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        
        add(attrPanel, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAndClose();
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public void showForNewDataSet() {
    	showForRelease(null);
    }
    
    public void showForRelease(final Entity releaseEntity) {

        this.releaseEntity = releaseEntity;

        attrPanel.removeAll();

        addSeparator(attrPanel, "Release Attributes", true);

        final JLabel nameLabel = new JLabel("Release Name: ");
        nameInput = new JTextField(40);
                
        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel, "gap para");
        attrPanel.add(nameInput);

        JPanel bottomPanel = new JPanel(new MigLayout("wrap 2"));
        
        annotatorsPanel = new SubjectListPanel("Annotators");
        bottomPanel.add(annotatorsPanel);

        subscribersPanel = new SubjectListPanel("Subscribers");
        bottomPanel.add(subscribersPanel);

        attrPanel.add(bottomPanel, "span 2");
        
        Utils.setWaitingCursor(FlyLineReleaseDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            private List<Subject> subjects;

            @Override
            protected void doStuff() throws Exception {
                this.subjects = ModelMgr.getModelMgr().getSubjects();
            }

            @Override
            protected void hadSuccess() {
                annotatorsPanel.init(subjects);
                subscribersPanel.init(subjects);
                
                // TODO: load jlists from the release entity
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.4),(int)(mainFrame.getHeight()*0.4)));
        
        packAndShow();
    }
    
    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span"+(first?"":", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }
    
    private void saveAndClose() {

        Utils.setWaitingCursor(FlyLineReleaseDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                if (releaseEntity == null) {
                    releaseEntity = ModelMgr.getModelMgr().createFlyLineRelease(nameInput.getText());
                } else {
                    releaseEntity.setName(nameInput.getText());
                }
                
                // TODO: save jlists to release entity
            }

            @Override
            protected void hadSuccess() {
                parentDialog.refresh();
                Utils.setDefaultCursor(FlyLineReleaseDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(FlyLineReleaseDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }
    
    public class SubjectListPanel extends JPanel {

        private DefaultListModel<Subject> model;
        private JList<Subject> subjectList;
        private DefaultComboBoxModel<Subject> comboBoxModel;
        private JComboBox<Subject> subjectCombobox;
        
        public SubjectListPanel(String title) {
            
            setLayout(new BorderLayout());
            
            this.model = new DefaultListModel<>();
            this.subjectList = new JList<>(model);
            subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            subjectList.setLayoutOrientation(JList.VERTICAL);
            subjectList.setCellRenderer(new SubjectComboBoxRenderer());
            subjectList.setVisibleRowCount(-1);
            JScrollPane scrollPane = new JScrollPane(subjectList);
            scrollPane.setPreferredSize(new Dimension(200, 200));
            add(new JLabel(title), BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            this.comboBoxModel = new DefaultComboBoxModel<>();
            this.subjectCombobox = new JComboBox<>(comboBoxModel);
            subjectCombobox.setEditable(false);
            subjectCombobox.setToolTipText("Choose a user or group");
            subjectCombobox.setRenderer(new SubjectComboBoxRenderer());
            subjectCombobox.setMaximumRowCount(20);
            subjectCombobox.setPreferredSize(new Dimension(150,25));
            
            JButton addButton = new JButton("Add");
            addButton.setToolTipText("Add the selected user or group");
            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Subject selected = (Subject)comboBoxModel.getSelectedItem();
                    if (selected != null) {
                        addSubject(selected);
                        comboBoxModel.removeElement(selected);
                        revalidate();
                        repaint();
                    }
                }
            });
            
            JPanel addPane = new JPanel();
            addPane.setLayout(new BoxLayout(addPane, BoxLayout.LINE_AXIS));
//            addPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            addPane.add(subjectCombobox);
            addPane.add(addButton);
            addPane.add(Box.createHorizontalGlue());
            add(addPane, BorderLayout.SOUTH);
        }
        
        public void init(List<Subject> subjects) {
            for(Subject subject : subjects) {
                if (!SessionMgr.getSubjectKey().equals(subject.getKey())) {
                    comboBoxModel.addElement(subject);
                }
            }
        }
        
        public void addSubject(Subject subject) {
            model.addElement(subject);
        }
        
        public void removeSubject(Subject subject) {
            model.removeElement(subject);
        }
        
        public List<Subject> getSubjects() {
            return Collections.list(model.elements());
        }
    }
}
