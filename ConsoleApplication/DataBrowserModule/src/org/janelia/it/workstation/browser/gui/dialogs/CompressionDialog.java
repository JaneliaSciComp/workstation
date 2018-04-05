package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
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

import org.apache.commons.lang.StringUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.OpenInNeuronAnnotatorAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
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
    
    private final JPanel attrPanel;

    private JRadioButton stacksLLCheckbox;
    private JRadioButton stacksVLCheckbox;
    private JRadioButton sepLLCheckbox;
    private JRadioButton sepVLCheckbox;
    
    private Collection<Sample> samples;
    private DataSet dataSet;
    
    public CompressionDialog() {

        setTitle("Change Sample Compression Strategy");

        String desc = "<html>Lossless files are expensive to store in the long term.<br>"
                + "By default, the Workstation stores visually lossless files encoded using the H5J format.<br>"
                + "This can be adjusted at the data set level, or for an individual sample or set of samples.<br>"
                + "Switching from visually lossless to lossless causes all the secondary data to be regenerated.</html>";
        
        attrPanel = new JPanel(new MigLayout("wrap 3, ins 20", "[]20[]20[]"));
        add(attrPanel, BorderLayout.CENTER);

        attrPanel.add(new JLabel(desc), "span 3, gapbottom 20");
        
        stacksLLCheckbox = new JRadioButton();
        stacksLLCheckbox.setFocusable(false);
        stacksLLCheckbox.addActionListener((e) -> {
           updateDerivedState();
        });
        
        stacksVLCheckbox = new JRadioButton();
        stacksVLCheckbox.setFocusable(false);
        stacksVLCheckbox.addActionListener((e) -> {
            updateDerivedState();
         });
        sepLLCheckbox = new JRadioButton();
        sepLLCheckbox.setFocusable(false);
        
        sepVLCheckbox = new JRadioButton();
        sepVLCheckbox.setFocusable(false);

        ButtonGroup stacksGroup = new ButtonGroup();
        stacksGroup.add(stacksLLCheckbox);
        stacksGroup.add(stacksVLCheckbox);

        ButtonGroup sepGroup = new ButtonGroup();
        sepGroup.add(sepLLCheckbox);
        sepGroup.add(sepVLCheckbox);
        
        attrPanel.add(Box.createGlue());
        attrPanel.add(new JLabel("Lossless (PBD format)"), "");
        attrPanel.add(new JLabel("Visually Lossless (H5J format)"), "");

        attrPanel.add(new JLabel("Image Stacks"), "");
        attrPanel.add(stacksLLCheckbox, "");
        attrPanel.add(stacksVLCheckbox, "");

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
    
    private void updateDerivedState() {
        if  (stacksLLCheckbox.isSelected() || !stacksVLCheckbox.isSelected()) {
            sepLLCheckbox.setSelected(true);
            sepLLCheckbox.setEnabled(false);
            sepVLCheckbox.setEnabled(false);
        }
        else {
            sepLLCheckbox.setEnabled(true);
            sepVLCheckbox.setEnabled(true);
        }   
    }

    public void showForSamples(Collection<Sample> samples) {

        this.samples = samples;
        this.dataSet = null;
        
        if (samples.size()>1) {
            setTitle("Change Sample Compression Strategy for "+samples.size()+" Selected Samples");
        }
        
        int stackll = 0;
        int stackvl = 0;
        int sepll = 0;
        int sepvl = 0;
        
        for(Sample sample : samples) {
            if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(sample.getCompressionType())) {
                stackll++;
            }
            else if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getCompressionType())) {
                stackvl++;
            }

            if (DomainConstants.VALUE_COMPRESSION_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                sepll++;
            }
            else if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                sepvl++;
            }
        }
        
        if (stackvl == 0) {
            stacksLLCheckbox.setSelected(true);
        }
        else if (stackll == 0) {
            stacksVLCheckbox.setSelected(true);
        }
        else {
            // use default, if all samples don't agree
            stacksVLCheckbox.setSelected(true);
        }
        
        if (sepvl == 0) {
            sepLLCheckbox.setSelected(true);
        }
        else if (sepll == 0) {
            sepVLCheckbox.setSelected(true);
        }
        else {
            // use default, if all samples don't agree
            sepLLCheckbox.setSelected(true);
        }

        updateDerivedState();

        ActivityLogHelper.logUserAction("CompressionDialog.showForSample");
        packAndShow();
    }

    public void showForDataSet(DataSet dataSet) {

        this.samples = null;
        this.dataSet = dataSet;
        
        setTitle("Change Default Sample Compression Strategy for "+dataSet.getName());
        
        if (DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J.equals(dataSet.getDefaultCompressionType())) {
            stacksLLCheckbox.setSelected(true);
        }
        else {
            stacksVLCheckbox.setSelected(true);
        }
        
        if (DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(dataSet.getDefaultSeparationCompressionType())) {
            sepVLCheckbox.setSelected(true);
        }
        else {
            sepLLCheckbox.setSelected(true);
        }

        updateDerivedState();

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

        final String samplesText = samples.size()>1?samples.size()+" Samples":"1 Sample";
        String targetSampleCompression =  stacksLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
        String targetSeparationCompression =  sepLLCheckbox.isSelected() ? DomainConstants.VALUE_COMPRESSION_LOSSLESS : DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;

        boolean dirty = false;
        
        for(Sample sample : samples) {
            
            String currSampleCompression = sample.getCompressionType();
            String currSepCompression = sample.getSeparationCompressionType();

            if (StringUtils.isBlank(currSampleCompression)) {
                currSampleCompression = DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
            }
            if (StringUtils.isBlank(currSepCompression)) {
                currSepCompression = DomainConstants.VALUE_COMPRESSION_LOSSLESS;
            }
            
            if (!targetSampleCompression.equals(currSampleCompression) || !targetSeparationCompression.equals(currSepCompression)) {
                dirty = true;
            }
        }
        
        if (dirty) {
            String message = "Are you sure you want to change the compression strategy for "+samplesText+"?";
            int result = JOptionPane.showConfirmDialog(this, message,  "Change Sample Compression Strategy", JOptionPane.OK_CANCEL_OPTION);
            if (result != 0) return false;
        }
        
        StringBuilder sampleIdBuf = new StringBuilder();
        for(final Sample sample : samples) {
            // Target is Visually Lossless, just run the compression service
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
                    taskParameters.add(new TaskParameter("target sample compression", targetSampleCompression, null));
                    taskParameters.add(new TaskParameter("target separation compression", targetSeparationCompression, null));
                    task = StateMgr.getStateMgr().submitJob("ConsoleSampleCompression", "Console Sample Compression", taskParameters);
                    if (task==null) {
                        throw new IllegalStateException("Task could not be submitted");
                    }
                }
                catch (Exception e) {
                    ConsoleApp.handleException(e);
                    return;
                }
            }

            @Override
            protected void hadSuccess() {
                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Changing Compression Strategy for "+samples.size()+" Samples";
                    }

                    @Override
                    public Callable<Void> getSuccessCallback() {
                        return new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                
                                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), 
                                        "Sample compression strategy has been updated for "+samples.size()+" samples. "
                                            + "Any samples needed reprocessing have been dispatched. "
                                            + "Results will be available once the pipeline has completed.",
                                        "Samples updated successfully", JOptionPane.INFORMATION_MESSAGE);
                                
                                return null;
                            }
                        };
                    }
                };

                taskWorker.executeWithEvents();
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
        
        return true;
    }
    
    private boolean saveDataSet() {

        String message = "<html>Apply compression strategy to all existing samples in data set, or only new samples added in the future?</html>";
        
        String[] buttons = { "All Existing and Future Samples", "Only Future Samples", "Cancel" };
        int selectedOption = JOptionPane.showOptionDialog(this, message, 
                "Apply changes", JOptionPane.INFORMATION_MESSAGE, 0, null, buttons, buttons[0]);

        if (selectedOption == 0) {
            log.info("User chose to apply changes to all existing and future samples");

            message = "<html>Are you sure you want to process all samples in this data set? This operation may be compute intensive and costly.</html>";
            int result = JOptionPane.showConfirmDialog(this, message,  "Change Sample Compression Strategy", JOptionPane.OK_CANCEL_OPTION);
            if (result != 0) return false;
        }
        else if (selectedOption == 1) {
            log.info("User chose to apply changes to future samples only");
        }
        else {
            log.info("User chose to cancel save");
            return false;
        }
        
        // TODO: finish this later
        throw new IllegalStateException("This operation is not yet implemented");
        
//        return true;
    }
}
