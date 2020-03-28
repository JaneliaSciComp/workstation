package org.janelia.workstation.browser.actions.context;

import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.DataSetDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ViewDataSetSettingsAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewDataSetSettingsAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 550)
})
@NbBundle.Messages("CTL_ViewDataSetSettingsAction=View Data Set Settings")
public class ViewDataSetSettingsAction extends BaseContextualNodeAction {

    private DomainObject domainObject;

    @Override
    protected void processContext() {
        this.domainObject = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(Sample.class);
        }
        else if (getNodeContext().isSingleObjectOfType(DataSet.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(DataSet.class);
        }
        if (domainObject != null) {
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(domainObject));
        }
    }

    @Override
    public String getName() {
        return "View Data Set Settings";
    }

    @Override
    public void performAction() {

        DomainObject domainObject = this.domainObject;

        SimpleWorker simpleWorker = new SimpleWorker() {
            private DataSet dataSet;

            @Override
            protected void doStuff() throws Exception {
                if (domainObject instanceof DataSet) {
                    dataSet = (DataSet)domainObject;
                }
                else if (domainObject instanceof Sample) {
                    Sample sample = (Sample)domainObject;
                    dataSet = DomainMgr.getDomainMgr().getModel().getDataSet(sample.getDataSet());
                }
                else {
                    throw new IllegalStateException("Cannot accept class: "+domainObject.getClass());
                }
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