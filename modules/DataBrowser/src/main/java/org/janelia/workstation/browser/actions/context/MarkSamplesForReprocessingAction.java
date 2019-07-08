package org.janelia.workstation.browser.actions.context;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.dto.SampleReprocessingRequest;
import org.janelia.model.domain.enums.PipelineStatus;
import org.janelia.model.domain.enums.SubjectRole;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "MarkSamplesForReprocessingAction"
)
@ActionRegistration(
        displayName = "#CTL_MarkSamplesForReprocessingAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 510)
})
@NbBundle.Messages("CTL_MarkSamplesForReprocessingAction=Mark Samples for Reprocessing")
public class MarkSamplesForReprocessingAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(MarkSamplesForReprocessingAction.class);
    
    private static final int MAX_SAMPLE_RERUN_COUNT = 10;
    private Collection<Sample> samples = new ArrayList<>();

    @Override
    protected void processContext() {
        samples.clear();
        if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
            for (Sample sample : getNodeContext().getOnlyObjectsOfType(Sample.class)) {
                boolean canWrite = ClientDomainUtils.hasWriteAccess(sample);
                boolean canRerun = (!PipelineStatus.Processing.toString().equals(sample.getStatus())  &&
                        !PipelineStatus.Scheduled.toString().equals(sample.getStatus()));

                if ((canWrite && canRerun) || AccessManager.getAccessManager().isAdmin()) {
                    samples.add(sample);
                }
            }
        }
        setEnabledAndVisible(!samples.isEmpty());
    }

    @Override
    public String getName() {
        if (samples!=null && samples.size()>1) {
            return "Mark "+samples.size()+" Samples for Reprocessing";
        }
        return super.getName();
    }

    @Override
    public void performAction() {
        
        if (samples.size() > MAX_SAMPLE_RERUN_COUNT && !AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin)) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "You cannot submit more than "+MAX_SAMPLE_RERUN_COUNT+" samples for reprocessing at a time.",
                    "Too many samples selected", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int numBlocked = 0;
        for (Sample sample : samples) {
            if (sample.isSampleBlocked()) {
                numBlocked++;
            }
        }
        
        StringBuilder sampleText = new StringBuilder();
        if (samples.size() == 1) {
            sampleText.append("sample");
        }
        else {
            sampleText.append(samples.size());
            sampleText.append(" samples");
        }
        
        ReprocessingDialog dialog = new ReprocessingDialog("Reprocess "+sampleText);
        if (!dialog.showDialog()) return;
        
        if (numBlocked>0) {
            int result2 = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), "You have selected "+numBlocked+" blocked samples for reprocessing. Continue with unblocking and reprocessing?",
                    "Blocked Samples Selected", JOptionPane.OK_CANCEL_OPTION);
            if (result2 != 0) return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (Sample sample : samples) {
                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.markForReprocessing", sample);    
                }
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                
                SampleReprocessingRequest request = new SampleReprocessingRequest();
                request.setSampleReferences(DomainUtils.getReferences(samples));
                request.setProcessLabel("User Requested Reprocessing");
                request.setReuseSummary(dialog.isReuseSummary());
                request.setReuseProcessing(dialog.isReuseProcessing());
                request.setReusePost(dialog.isReusePost());
                request.setReuseAlignment(dialog.isReuseAlignment());
                request.setReuseSeparation(dialog.isReuseSeparation());
                request.setKeepExistingResults(dialog.isKeepExistingResults());
                request.setExtraOptions(dialog.getExtraOptions());
                
                log.info("Dispatching {} samples", samples.size());
                model.dispatchSamples(request);
            }

            @Override
            protected void hadSuccess() {
                log.info("Successfully dispatched {} samples.", samples.size());
                DomainExplorerTopComponent.getInstance().refresh(true, true, null);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
            
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Marking samples for reprocessing", ""));
        worker.execute();
    }
    

    private class ReprocessingDialog extends ModalDialog {

        private final GroupedKeyValuePanel mainPanel;
        private final JCheckBox reuseSummaryCheckbox;
        private final JCheckBox reuseProcessingCheckbox; 
        private final JCheckBox reusePostCheckbox; 
        private final JCheckBox reuseAlignmentCheckbox;
        private final JCheckBox reuseSeparationCheckbox;
        private final JCheckBox keepResultsCheckbox;
        private final JTextField extraOptionsField;
        
        private boolean returnValue;
        
        ReprocessingDialog(String okButtonName) {
            
            setTitle("Reprocess Samples");
            setLayout(new BorderLayout());

            this.mainPanel = new GroupedKeyValuePanel();
            mainPanel.addSeparator("Reuse existing results");
            
            this.reuseSummaryCheckbox = new JCheckBox();
            reuseSummaryCheckbox.setSelected(true);
            mainPanel.addItem("LSM Summary", reuseSummaryCheckbox);
            
            this.reuseProcessingCheckbox = new JCheckBox();
            reuseProcessingCheckbox.setSelected(true);
            mainPanel.addItem("Sample Processing", reuseProcessingCheckbox);
            
            this.reusePostCheckbox = new JCheckBox();
            reusePostCheckbox.setSelected(true);
            mainPanel.addItem("Post-Processing", reusePostCheckbox);
            
            this.reuseAlignmentCheckbox = new JCheckBox();
            reuseAlignmentCheckbox.setSelected(true);
            mainPanel.addItem("Alignment", reuseAlignmentCheckbox);

            this.reuseSeparationCheckbox = new JCheckBox();
            reuseSeparationCheckbox.setSelected(true);
            mainPanel.addItem("Separation", reuseSeparationCheckbox);
            
            mainPanel.addSeparator("Other options");
            this.keepResultsCheckbox = new JCheckBox();
            mainPanel.addItem("Keep previous results", keepResultsCheckbox);
            
            this.extraOptionsField = new JTextField();
            extraOptionsField.setColumns(20);
            if (AccessManager.getAccessManager().isAdmin()) {
                mainPanel.addItem("Extra options", extraOptionsField);
            }
            
            add(mainPanel, BorderLayout.CENTER);
            
            JButton okButton = new JButton(okButtonName);
            okButton.setToolTipText("Schedule selected samples for reprocessing");
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    returnValue = true;
                    setVisible(false);
                }
            });

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText("Cancel without reprocessing");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    returnValue = false;
                    setVisible(false);
                }
            });

            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(okButton);
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPane.add(cancelButton);
            add(buttonPane, BorderLayout.SOUTH);

            getRootPane().setDefaultButton(okButton);
            
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    returnValue = false;
                }
            });
        }
        
        public boolean showDialog() {
            packAndShow();
            // Blocks until dialog is no longer visible, and then:
            removeAll();
            dispose();
            log.info("Returning value: "+returnValue);
            return returnValue;
        }


        public boolean isReuseSummary() {
            return reuseSummaryCheckbox.isSelected();
        }


        public boolean isReuseProcessing() {
            return reuseProcessingCheckbox.isSelected();
        }


        public boolean isReusePost() {
            return reusePostCheckbox.isSelected();
        }


        public boolean isReuseAlignment() {
            return reuseAlignmentCheckbox.isSelected();
        }
        
        public boolean isReuseSeparation() {
            return reuseSeparationCheckbox.isSelected();
        }

        public boolean isKeepExistingResults() {
            return keepResultsCheckbox.isSelected();
        }


        public String getExtraOptions() {
            return extraOptionsField.getText();
        }
    }
}
