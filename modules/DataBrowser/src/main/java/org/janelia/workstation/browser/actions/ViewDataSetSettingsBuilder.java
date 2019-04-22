package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.DataSetDialog;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=550)
public class ViewDataSetSettingsBuilder implements ContextualActionBuilder {

    private static ViewDataSetSettingsAction action = new ViewDataSetSettingsAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class ViewDataSetSettingsAction extends ViewerContextAction {

        private Sample sample;

        @Override
        public String getName() {
            return "View Data Set Settings";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();

            // reset values
            ContextualActionUtils.setVisible(this, false);
            ContextualActionUtils.setEnabled(this, true);

            if (!viewerContext.isMultiple()) {
                Object obj = viewerContext.getLastSelectedObject();
                if (obj instanceof Sample) {
                    this.sample = (Sample) obj;
                    ContextualActionUtils.setVisible(this, true);
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        ContextualActionUtils.setEnabled(this, false);
                    }
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            SimpleWorker simpleWorker = new SimpleWorker() {
                private DataSet dataSet;

                @Override
                protected void doStuff() throws Exception {
                    dataSet = DomainMgr.getDomainMgr().getModel().getDataSet(sample.getDataSet());
                }

                @Override
                protected void hadSuccess() {
                    if (dataSet==null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                                "Could not retrieve this sample's data set.", "Invalid Data Set",
                                JOptionPane.WARNING_MESSAGE);
                    }
                    else {
                        DataSetDialog dataSetDialog = new DataSetDialog(null);
                        dataSetDialog.showForDataSet(dataSet);
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            simpleWorker.execute();
        }

    }
}