package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasImageStack;
import org.janelia.model.domain.sample.*;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.FileCallable;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Given a neuron separation, open it using the Neuron Annotator mode of VVD.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInVvdNAPluginActionListener implements ActionListener {

    private final static Logger log = LoggerFactory.getLogger(OpenInVvdNAPluginActionListener.class);

    private NeuronFragment fragment;
    private NeuronSeparation separation;

    public OpenInVvdNAPluginActionListener(NeuronFragment fragment) {
        this.fragment = fragment;
    }

    public OpenInVvdNAPluginActionListener(NeuronSeparation separation) {
        this.separation = separation;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("OpenInVvdNAPluginActionListener.doAction", fragment==null?separation:fragment.getSeparationId());

        if (separation!=null) {
            openSeparation();
            return;
        }

        SimpleWorker worker = new SimpleWorker() {
            Sample sample;

            @Override
            protected void doStuff() throws Exception {
                if (fragment==null) {
                    throw new IllegalStateException("Both fragment and separation were null");
                }
                sample = DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());
                if (sample != null) {
                    separation = SampleUtils.getNeuronSeparation(sample, fragment);
                }
            }

            @Override
            protected void hadSuccess() {
                if (sample==null) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                }
                else if (separation==null) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    openSeparation();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private void openSeparation() {
        if (separation.getParentRun()!=null && separation.getParentRun().getParent().getParent().isSamplePurged()) {
            int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), "This sample was previously purged, and may not load correctly in VVD. Continue with load?",  "Data missing", JOptionPane.OK_CANCEL_OPTION);
            if (result==JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        try {
            log.debug("Requesting view of separation {} in VVD NA Plugin: ", separation.getId());

            /*
                Usage example:
                   VVDViewer -p "NAPlugin -l \"/path/to/ConsolidatedLabel.v3dpbd\"
                    -v \"/path/to/stack.h5j\" -c sssr -s 1.00x1.00x1.20"
            */

            PipelineResult result = separation.getParentResult();
            String llFilepath = DomainUtils.getFilepath(result, FileType.LosslessStack);
            String vllFilepath = DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack);
            String stackFilepath = llFilepath == null ? vllFilepath : llFilepath;
            String consolidatedLabelFilepath = separation.getFilepath()+"/ConsolidatedLabel.v3dpbd";
            String opticalRes = getOpticalResolution(result);
            String chanSpec = getChannelSpec(result);
            String sampleName = result.getParentRun().getParent().getParent().getName();

            // Fetch both the stack and the label
            log.info("Fetching stack: {}", stackFilepath);
            Utils.processStandardFilepath(stackFilepath, new FileCallable() {
                @Override
                public void call(File stackFile) throws Exception {
                    log.info("Fetching label: {}", consolidatedLabelFilepath);
                    Utils.processStandardFilepath(consolidatedLabelFilepath, new FileCallable() {
                        @Override
                        public void call(File consolidatedLabelFile) throws Exception {

                            StringBuilder pluginArgs = new StringBuilder("NAPlugin ");
                            pluginArgs.append("-l \"").append(consolidatedLabelFile.getAbsolutePath()).append("\" ");
                            pluginArgs.append("-v \"").append(stackFile.getAbsolutePath()).append("\" ");
                            pluginArgs.append("-c ").append(chanSpec).append(" ");
                            pluginArgs.append("-s ").append(opticalRes).append(" ");

                            List<String> arguments = new ArrayList<>();
                            arguments.add("-p");
                            arguments.add(pluginArgs.toString());
                            arguments.add("--desc");
                            arguments.add(sampleName);

                            ToolMgr.runTool(FrameworkAccess.getMainFrame(), ToolMgr.TOOL_VVD, arguments);
                        }
                    });
                }
            });
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    private String getChannelSpec(PipelineResult result) {
        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult sr = (SampleProcessingResult)result;
            return sr.getChannelSpec();
        }
        else if (result instanceof SampleAlignmentResult) {
            HasImageStack sr = (HasImageStack)result;
            return sr.getChannelSpec();
        }
        return null;
    }

    private String getOpticalResolution(PipelineResult result) {
        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult sr = (SampleProcessingResult)result;
            return sr.getOpticalResolution();
        }
        else if (result instanceof SampleAlignmentResult) {
            HasImageStack sr = (HasImageStack)result;
            return sr.getOpticalResolution();
        }
        return null;
    }
}
