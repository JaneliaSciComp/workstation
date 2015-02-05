package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.Viewer;
import org.janelia.it.workstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog to migrate annotations and references to a set of entities. This
 * dialog is designed to be invoked when the user selects migration sources 
 * in the Left Pane and targets in the Right Pane.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MigrateEntitiesDialog extends ModalDialog {
    
    private static final Logger log = LoggerFactory.getLogger(MigrateEntitiesDialog.class);
    
    // If this is true, then we won't write to the database. 
    private static final boolean DEBUG = false;
    
    private static final String HELP_TEXT = 
            "To use this tool, open collections in both Left and Right Panes, "
            + "and then select entities so that the entities selected in the Left Pane can be migrated to the corresponding "
            + "entities in the Right Pane. All annotations will be moved from Left to Right, and then all Folder References to the "
            + "entities in the Left Pane will be replaced with references to corresponding entities in the Right Pane.";
            
    // UI Elements
    private final JPanel mainPanel;
    private final JButton cancelButton;
    private final JButton okButton;
    private final JTextPane textPane;
    
    // Migration sources/targets
    private List<RootedEntity> mainSelected;
    private List<RootedEntity> secSelected;
    private Map<Long,List<EntityData>> refLists = new HashMap<>();
    private Map<Long,List<Entity>> annotationLists = new HashMap<>();

    private boolean done = false;
    
    public MigrateEntitiesDialog() {

        setTitle("Migrate References and Annotations");
        setModalityType(ModalityType.MODELESS);
    
        this.textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");

        JLabel loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        mainPanel = new JPanel(new BorderLayout());
    	mainPanel.add(loadingLabel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        this.cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        this.okButton = new JButton("Migrate");
        okButton.setEnabled(false);
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (done) {
                    setVisible(false);
                    SessionMgr.getBrowser().getEntityOutline().refresh(true, true, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            Viewer mainViewer = SessionMgr.getBrowser().getViewerManager().getMainViewer();
                            if (mainViewer!=null) mainViewer.refresh();
                            Viewer secViewer = SessionMgr.getBrowser().getViewerManager().getSecViewer();
                            if (secViewer!=null) secViewer.refresh();
                            return null;
                        }
                    });
                }
                else {
                    performMigration();
                }
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
        
        loadSelectionState();
        
        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.5),(int)(mainFrame.getHeight()*0.8)));
        // Show dialog and wait
        packAndShow();
    }
    
    private void loadSelectionState() {

        SimpleWorker worker = new SimpleWorker() {

            private String error;
            
            @Override
            protected void doStuff() throws Exception {
                
                ViewerPane mainPane = SessionMgr.getBrowser().getViewerManager().getMainViewerPane();
                ViewerPane secPane = SessionMgr.getBrowser().getViewerManager().getSecViewerPane();
                                
                if (secPane.getViewer()==null) {
                    this.error = "Left Pane is not open";
                    return;
                }
                
                mainSelected = mainPane.getViewer().getSelectedEntities();
                secSelected = secPane.getViewer().getSelectedEntities();
                
                if (mainSelected.isEmpty()) {
                    this.error = "no entities selected in Right Pane";
                    return;
                }
                
                if (secSelected.isEmpty()) {
                    this.error = "no entities selected in Left Pane";
                    return;
                }
                
                
                if (mainSelected.size()!=secSelected.size()) {
                    this.error = "different numbers of entities are selected in Left and Right Panes";
                    return;
                }
                
                for(RootedEntity re : mainSelected) {
                    refLists.put(re.getEntityId(), ModelMgr.getModelMgr().getAllParentEntityDatas(re.getEntityId()));
                    annotationLists.put(re.getEntityId(), ModelMgr.getModelMgr().getAnnotationsForEntity(re.getEntityId()));
                }
                
            }

            @Override
            protected void hadSuccess() {
                mainPanel.removeAll();
                StringBuilder sb = new StringBuilder();
                
                if (error!=null) {
                    sb.append(HELP_TEXT).append("<br><br>Currently the following issue is preventing migration: ").append(error);
                }
                else {
                    sb.append("Will migrate annotations and replace references to ").append(mainSelected.size()).append(" entities as follows:<br><ul>");
                    
                    int c = 0;
                    for(RootedEntity sourceRe : mainSelected) {
                        RootedEntity targetRe = secSelected.get(c);
                        Entity source = sourceRe.getEntity();
                        Entity target = targetRe.getEntity();
                        
                        sb.append("<li><b>").append(source.getName()).append(" -> ").append(target.getName()).append("</b><ul>");
                        
                        boolean migrate = false;
                        
                        List<Entity> annotations = annotationLists.get(source.getId());
                        if (annotations.isEmpty()) {
                            sb.append("<li>No annotations</li>");
                        }
                        else {
                            sb.append("<li>Annotations: <ul>");
                            for(Entity annotation : annotations) {
                                migrate = true;
                                sb.append("<li>").append(annotation.getName()).append("</li>");
                            }
                            sb.append("</ul></li>"); 
                        }
                        
                        List<EntityData> refs = refLists.get(source.getId());
                        if (refs.isEmpty()) {
                            sb.append("<li>No folder references</li>");
                        }
                        else {
                            int numAlignedItemRefs = 0;
                            sb.append("<li>References: <ul>");
                            for(EntityData refEd : refs) {
                                String refType = refEd.getParentEntity().getEntityTypeName();
                                if (refType.equals(EntityConstants.TYPE_ALIGNED_ITEM)) {
                                    numAlignedItemRefs++;
                                }
                                if (!refType.equals(EntityConstants.TYPE_FOLDER)) {
                                    continue;
                                }
                                String issue = "";
                                migrate = true;
                                sb.append("<li>").append(refEd.getParentEntity().getName()).append(" (").append(refType).append(")").append(issue).append("</li>");
                            }
                            if (numAlignedItemRefs>0) { 
                                sb.append("<li><font color=red>").append(numAlignedItemRefs).append(" aligned items reference this neuron, but cannot be migrated.").append("</font></li>");
                            }
                            sb.append("</ul></li>"); 
                        }
                        
                        if (!migrate) {
                            sb.append("<li><font color=red>There is nothing that can be migrated for this neuron fragment.</font></li>");
                        }
                        
                        sb.append("</ul></li>"); 
                        
                        c++;
                    }
                    
                    sb.append("</ul>"); 
                    
                    okButton.setEnabled(true);
                }
                
                textPane.setText("<html>"+sb.toString()+"</html>");
                JScrollPane scrollPane = new JScrollPane(textPane);
                mainPanel.add(scrollPane, BorderLayout.CENTER);
                mainPanel.updateUI();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
    
    private void performMigration() {
       
        textPane.setText("<html>Performing Migration...</html>");
        mainPanel.updateUI();
        
        final StringBuilder sb = new StringBuilder();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                int c = 0;
                for(RootedEntity sourceRe : mainSelected) {
                    RootedEntity targetRe = secSelected.get(c);
                    Entity source = sourceRe.getEntity();
                    Entity target = targetRe.getEntity();
                    
                    sb.append("<li>Migrating <b>").append(source.getName()).append(" -> ").append(target.getName()).append("</b><ul>");
                        
                    for(final Entity annotation : annotationLists.get(source.getId())) {
                        // migrate annotation to point to new target
                        String message;
                        try {
                            if (!DEBUG) ModelMgr.getModelMgr().setOrUpdateValue(annotation, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, target.getId().toString());
                            message = "Migrated annotation "+annotation.getName();
                        }
                        catch (Exception e) {
                            log.error("Error migrating neuron fragment annotation",e);
                            message = "<font color=red>Error migrating annotation "+annotation.getName()+": "+e.getMessage()+"</font>";
                        }
                        final String html = "<li>"+message+"</li>";
                        sb.append(html);
                    }

                    for(final EntityData refEd : refLists.get(source.getId())) {
                        String refType = refEd.getParentEntity().getEntityTypeName();
                        if (refType.equals(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)) {
                            continue;
                        }
                        if (!refType.equals(EntityConstants.TYPE_ALIGNED_ITEM)) {
                            // migrate reference to point to a new target
                            String message;
                            try {
                                refEd.setChildEntity(target);
                                if (!DEBUG) ModelMgr.getModelMgr().saveOrUpdateEntityData(refEd);  
                                message = "Migrated reference "+refEd.getParentEntity().getName();
                            }
                            catch (Exception e) {
                                log.error("Error migrating neuron fragment reference",e);
                                message = "<font color=red>Error migrating reference "+refEd.getParentEntity().getName()+": "+e.getMessage()+"</font>";
                            }
                            final String html = "<li>"+message+"</li>";
                            sb.append(html);
                        }
                    }
                    
                    sb.append("</ul></li>");
                    
                    c++;
                }
            }

            @Override
            protected void hadSuccess() {
                textPane.setText("<html>Completed Migration:<ul>"+sb.toString()+"</ul></html>");
                mainPanel.updateUI();
                done = true;
                cancelButton.setVisible(false);
                okButton.setText("Close");
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
}