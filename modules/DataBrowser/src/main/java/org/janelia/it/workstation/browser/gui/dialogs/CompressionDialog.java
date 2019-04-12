package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for viewing and editing sample compression strategy.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CompressionDialog extends ModalDialog {

    private final static Logger log = LoggerFactory.getLogger(CompressionDialog.class);
    
    private JPanel attrPanel;

    private JRadioButton unalignedLLCheckbox;
    private JRadioButton unalignedVLCheckbox;
    private JRadioButton alignedLLCheckbox;
    private JRadioButton alignedVLCheckbox;
    private JRadioButton sepLLCheckbox;
    private JRadioButton sepVLCheckbox;
    
    private Collection<Sample> samples;
    private DataSet dataSet;

    public CompressionDialog() {
        init();
    }
     
    public CompressionDialog(Dialog parent) {
        super(parent);
        init();
    }
     
    private final void init() {
        setTitle("Change Sample Compression Strategy");

        String desc = "<html>Lossless files are expensive to store in the long term.<br>"
                + "By default, the Workstation stores visually lossless files encoded using the H5J format.<br>"
                + "This can be adjusted at the data set level, or for an individual sample or set of samples.<br>"
                + "Switching from visually lossless to lossless causes all the secondary data to be regenerated.</html>";
        
        attrPanel = new JPanel(new MigLayout("wrap 3, ins 20", "[]20[]20[]"));
        add(attrPanel, BorderLayout.CENTER);

        attrPanel.add(new JLabel(desc), "span 3, gapbottom 20");
        
        unalignedLLCheckbox = new JRadioButton();
        unalignedLLCheckbox.setFocusable(false);
        
        unalignedVLCheckbox = new JRadioButton();
        unalignedVLCheckbox.setFocusable(false);

        alignedLLCheckbox = new JRadioButton();
        alignedLLCheckbox.setFocusable(false);
        
        alignedVLCheckbox = new JRadioButton();
        alignedVLCheckbox.setFocusable(false);
        
        sepLLCheckbox = new JRadioButton();
        sepLLCheckbox.setFocusable(false);
        
        sepVLCheckbox = new JRadioButton();
        sepVLCheckbox.setFocusable(false);

        ButtonGroup stacksGroup = new ButtonGroup();
        stacksGroup.add(unalignedLLCheckbox);
        stacksGroup.add(unalignedVLCheckbox);

        ButtonGroup alignedGroup = new ButtonGroup();
        alignedGroup.add(alignedLLCheckbox);
        alignedGroup.add(alignedVLCheckbox);
        
        ButtonGroup sepGroup = new ButtonGroup();
        sepGroup.add(sepLLCheckbox);
        sepGroup.add(sepVLCheckbox);
        
        attrPanel.add(Box.createGlue());
        attrPanel.add(new JLabel("Lossless (PBD format)"), "");
        attrPanel.add(new JLabel("Visually Lossless (H5J format)"), "");

        attrPanel.add(new JLabel("Unaligned 3d Stacks"), "");
        attrPanel.add(unalignedLLCheckbox, "");
        attrPanel.add(unalignedVLCheckbox, "");

        attrPanel.add(new JLabel("Aligned 3d Stacks"), "");
        attrPanel.add(alignedLLCheckbox, "");
        attrPanel.add(alignedVLCheckbox, "");
        
        attrPanel.add(new JLabel("Neuron Separations"), "");
        attrPanel.add(sepLLCheckbox, "");
        attrPanel.add(sepVLCheckbox, "");
        
        add(attrPanel, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("Apply");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });
        
        getRootPane().setDefaultButton(okButton);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForSamples(Collection<Sample> samples) {

        this.samples = samples;
        this.dataSet = null;
        
        if (samples.size()>1) {
            setTitle("Change Sample Compression Strategy for "+samples.size()+" Selected Samples");
        }
        
        int unalignedll = 0;
        int unalignedvl = 0;
        int alignedll = 0;
        int alignedvl = 0;
        int sepll = 0;
        int sepvl = 0;
        
        for(Sample sample : samples) {
            
            if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(sample.getUnalignedCompressionType())) {
                unalignedll++;
            }
            else if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getUnalignedCompressionType())) {
                unalignedvl++;
            }

            if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(sample.getAlignedCompressionType())) {
                alignedll++;
            }
            else if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getAlignedCompressionType())) {
                alignedvl++;
            }
            
            if (DomainConstants.VALUE_COMPRESSION_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                sepll++;
            }
            else if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                sepvl++;
            }
            
        }
        
        if (unalignedll > 0 && unalignedvl == 0) {
            unalignedLLCheckbox.setSelected(true);
        }
        else if (unalignedvl > 0 && unalignedll == 0) {
            unalignedVLCheckbox.setSelected(true);
        }

        if (alignedll > 0 && alignedvl == 0) {
            alignedLLCheckbox.setSelected(true);
        }
        else if (alignedvl > 0 && alignedll == 0) {
            alignedVLCheckbox.setSelected(true);
        }
        
        if (sepll > 0 && sepvl == 0) {
            sepLLCheckbox.setSelected(true);
        }
        else if (sepvl > 0 && sepll == 0) {
            sepVLCheckbox.setSelected(true);
        }

        ActivityLogHelper.logUserAction("CompressionDialog.showForSample");
        packAndShow();
    }

    public void showForDataSet(DataSet dataSet) {

        this.samples = null;
        this.dataSet = dataSet;
        
        setTitle("Change Default Sample Compression Strategy for "+dataSet.getName());
        
        if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(dataSet.getUnalignedCompressionType())) {
            unalignedLLCheckbox.setSelected(true);
        }
        else {
            unalignedVLCheckbox.setSelected(true);
        }

        if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(dataSet.getAlignedCompressionType())) {
            alignedLLCheckbox.setSelected(true);
        }
        else {
            alignedVLCheckbox.setSelected(true);
        }
        
        if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(dataSet.getSeparationCompressionType())) {
            sepLLCheckbox.setSelected(true);
        }
        else {
            sepVLCheckbox.setSelected(true);
        }

        ActivityLogHelper.logUserAction("CompressionDialog.showForDataSet");
        packAndShow();
    }
    
    private void saveAndClose() {

        ActivityLogHelper.logUserAction("CompressionDialog.saveAndClose");

        if (samples != null) {
            if (!saveSamples()) {
                return;
            }
        }
        else if (dataSet != null) {
            if (!saveDataSet()) {
                return;
            }
        }
        else {
            throw new IllegalStateException("Both samples and dataSet cannot be null");
        }

        setVisible(false);
    }
    
    private boolean saveSamples() {

        String targetUnalignedCompression =  unalignedLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        String targetAlignedCompression =  alignedLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        String targetSeparationCompression =  sepLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;

        Collection<Sample> dirtySamples = samples;
        
//        for(Sample sample : samples) {
//            
//            String currUnalignedCompression = SampleUtils.getUnalignedCompression(dataSet, sample);
//            String currAlignedCompression = SampleUtils.getAlignedCompression(dataSet, sample);
//            String currSepCompression = SampleUtils.getSeparationCompression(dataSet, sample);
//
//            if (!targetUnalignedCompression.equals(currUnalignedCompression) || !targetAlignedCompression.equals(currAlignedCompression)  || !targetSeparationCompression.equals(currSepCompression)) {
//                dirtySamples.add(sample);
//            }
//        }
        
        if (dirtySamples.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All the selected samples already have the selected compression strategy",  "Change Sample Compression Strategy", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        else {
            final String samplesText = dirtySamples.size()>1?dirtySamples.size()+" Samples":"1 Sample";
            String message = "Are you sure you want to change the compression strategy for "+samplesText+"?";
            int result = JOptionPane.showConfirmDialog(this, message,  "Change Sample Compression Strategy", JOptionPane.OK_CANCEL_OPTION);
            if (result != 0) return false;
        }
        
        StringBuilder sampleIdBuf = new StringBuilder();
        for(final Sample sample : dirtySamples) {
            if (sampleIdBuf.length()>0) sampleIdBuf.append(",");
            sampleIdBuf.append(sample.getId());
        }
        
        if (sampleIdBuf.length()==0) return false;

        SimpleWorker worker = new SimpleWorker() {
            
            Task task;

            @Override
            protected void doStuff() throws Exception {
                try {
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
                    taskParameters.add(new TaskParameter("target unaligned compression", targetUnalignedCompression, null));
                    taskParameters.add(new TaskParameter("target aligned compression", targetAlignedCompression, null));
                    taskParameters.add(new TaskParameter("target separation compression", targetSeparationCompression, null));
                    task = StateMgr.getStateMgr().submitJob("ConsoleSampleCompression", "Console Sample Compression", taskParameters);
                    if (task==null) {
                        throw new IllegalStateException("Task could not be submitted");
                    }
                }
                catch (Exception e) {
                    FrameworkImplProvider.handleException(e);
                    return;
                }
            }

            @Override
            protected void hadSuccess() {
                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Changing Compression Strategy for "+dirtySamples.size()+" Samples";
                    }

                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return () -> {

                            DomainExplorerTopComponent.getInstance().refresh(true, true, () -> {

                                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), 
                                        "<html>Sample compression strategy has been updated for "+dirtySamples.size()+" samples.<br>"
                                            + "Any samples needing reprocessing have been scheduled.<br> "
                                            + "Results will be available once the pipeline has completed.</html>",
                                        "Samples updated successfully", JOptionPane.INFORMATION_MESSAGE);
                                
                                return null;
                            });
                            
                            return null;
                        };
                    }
                };

                taskWorker.executeWithEvents();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
        
        return true;
    }
    
    private boolean saveDataSet() {

        String targetUnalignedCompression =  unalignedLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        String targetAlignedCompression =  alignedLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        String targetSeparationCompression =  sepLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        
        String message = "<html>Do you want to apply the new compression strategy to all existing samples in data set, or only new samples added in the future?</html>";
        
        String[] buttons = { "All Existing and Future Samples", "Only Future Samples", "Cancel" };
        int selectedOption = JOptionPane.showOptionDialog(this, message, 
                "Apply changes", JOptionPane.INFORMATION_MESSAGE, 0, null, buttons, buttons[0]);

        boolean applyToExisting;
        
        if (selectedOption == 0) {
            log.info("User chose to apply changes to all existing and future samples");
            applyToExisting = true;
            message = "<html>Are you sure you want to process all samples in this data set? This operation may be compute intensive and costly.</html>";
            int result = JOptionPane.showConfirmDialog(this, message,  "Change Sample Compression Strategy", JOptionPane.OK_CANCEL_OPTION);
            if (result != 0) return false;
        }
        else if (selectedOption == 1) {
            log.info("User chose to apply changes to future samples only");
            applyToExisting = false;
        }
        else {
            log.info("User chose to cancel");
            return false;
        }

        SimpleWorker worker = new SimpleWorker() {
            
            Task task;

            @Override
            protected void doStuff() throws Exception {
                try {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    dataSet.setUnalignedCompressionType(targetUnalignedCompression);
                    dataSet.setAlignedCompressionType(targetAlignedCompression);
                    dataSet.setSeparationCompressionType(targetSeparationCompression);
                    model.save(dataSet);
                    
                    if (applyToExisting) {
                        HashSet<TaskParameter> taskParameters = new HashSet<>();
                        taskParameters.add(new TaskParameter("data set identifier", dataSet.getIdentifier(), null));
                        taskParameters.add(new TaskParameter("target unaligned compression", targetUnalignedCompression, null));
                        taskParameters.add(new TaskParameter("target aligned compression", targetAlignedCompression, null));
                        taskParameters.add(new TaskParameter("target separation compression", targetSeparationCompression, null));
                        task = StateMgr.getStateMgr().submitJob("ConsoleDataSetCompression", "Console Data Set Compression", taskParameters);
                        if (task==null) {
                            throw new IllegalStateException("Task could not be submitted");
                        }
                    }
                }
                catch (Exception e) {
                    FrameworkImplProvider.handleException(e);
                    return;
                }
            }

            @Override
            protected void hadSuccess() {
                if (task==null) return;
                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Changing Compression Strategy for "+dataSet.getIdentifier();
                    }

                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return () -> {

                            DomainExplorerTopComponent.getInstance().refresh(true, true, () -> {

                                if (applyToExisting) {
                                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), 
                                            "<html>Sample compression strategy has been updated for "+dataSet.getIdentifier()
                                                + ".<br> Any samples needing reprocessing have been scheduled.<br> "
                                                + "Results will be available once the pipeline has completed.</html>",
                                            "Data set updated successfully", JOptionPane.INFORMATION_MESSAGE);
                                }
                                else {
                                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), 
                                            "<html>Sample compression strategy has been updated for "+dataSet.getIdentifier()
                                                +".<br> Future samples will be processed to the given compression.</html>",
                                            "Data set updated successfully", JOptionPane.INFORMATION_MESSAGE);
                                }
                                
                                return null;
                            });
                            
                            return null;
                        };
                    }
                };

                taskWorker.executeWithEvents();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
        
        return true;
    }
}
