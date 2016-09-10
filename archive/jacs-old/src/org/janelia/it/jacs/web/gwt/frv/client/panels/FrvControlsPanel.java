
package org.janelia.it.jacs.web.gwt.frv.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobStatusTimer;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SmallRoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;
import org.janelia.it.jacs.web.gwt.frv.client.panels.tabs.*;
import org.janelia.it.jacs.web.gwt.frv.client.popups.FrvSaveWorkPopup;

import java.util.ArrayList;

public class FrvControlsPanel extends TitledBox implements IsJobSettable {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.FrvControlsPanel");

    private RoundedTabPanel _tabs;
    private ArrayList<FrvControlsPanelTab> panels = new ArrayList<FrvControlsPanelTab>();
    private static final int DB_MAX_SIZE = 80;
    //RecruitmentViewer.HelpURL=http://www.janelia.org
    private static final String HELP_LINK_URL_PROPERTY_NAME = "RecruitmentViewer.HelpURL";
    private RecruitableJobInfo _originalJob;
    private JobSelectionListener listener;
    private LoadingLabel _loadingLabel = new LoadingLabel();
    private RoundedButton submitButton, resetButton, saveButton;

    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }


    public FrvControlsPanel(String title, JobSelectionListener listener) {
        super(title);
        init(listener);
    }

    private void init(JobSelectionListener listener) {
        this.listener = listener;
        this.setWidth("400px");
        _loadingLabel = new LoadingLabel("Parsing data and regenerating images...", false);
        // Add Help and About action links
        TitledBoxActionLinkUtils.addHelpActionLink(this, new HelpActionLink("help"), HELP_LINK_URL_PROPERTY_NAME);

        // Add control tabs - This ordering is very important for display
        panels.add(new FrvControlsPanelDataTab(listener));
        panels.add(new FrvControlsPanelSampleTab(listener));
        panels.add(new FrvControlsPanelMateTab(listener));
        panels.add(new FrvControlsPanelAnnotationTab(listener));

        _tabs = new RoundedTabPanel();
        _tabs.add(HtmlUtils.getHtml("temp", "comingSoon"), "temp"); // this will get replaced by the real panel in setJob()
        _tabs.add(HtmlUtils.getHtml("coming soon", "comingSoon"), "Sample Filters");
        _tabs.add(HtmlUtils.getHtml("coming soon", "comingSoon"), "Mate Filters");
//        _tabs.add(HtmlUtils.getHtml("coming soon", "comingSoon"), "Metadata Filters");
//        _tabs.add(HtmlUtils.getHtml("coming soon", "comingSoon"), "Cluster Filters");
        _tabs.add(HtmlUtils.getHtml("coming soon", "comingSoon"), "Annotation Filters");
        _tabs.selectTab(0);
        _tabs.setWidth("100%");

        add(_tabs);
        add(HtmlUtils.getHtml("<br>", "text"));
        add(getSubmissionButtonPanel());
        add(_loadingLabel);
    }

    public void setJob(RecruitableJobInfo job) {
        // Set this as the original job
        _originalJob = job;
        int previouslySelectedTab = _tabs.getTabBar().getSelectedTab();
        if (previouslySelectedTab < 0) {
            previouslySelectedTab = 0;
        }

        // Manage the tabs
        if (job == null || job.getClass().getName().endsWith("RecruitableJobInfo")) {
            for (int i = 0; i < panels.size(); i++) {
                FrvControlsPanelTab tmpTab = panels.get(i);
                _tabs.remove(i);
                _tabs.insert(tmpTab.getPanel(), tmpTab.getTabLabel(), i);
                tmpTab.setJob(job);
            }
        }
        else if (job.getClass().getName().endsWith("BlastJobInfo")) {
            FrvControlsPanelTab tmpTab = panels.get(0);
            _tabs.remove(0);
            _tabs.insert(tmpTab.getPanel(), tmpTab.getTabLabel(), 0);
            tmpTab.setJob(job);
        }
        else
            _logger.error("Unknown task type");

        _tabs.selectTab(previouslySelectedTab);
        setActiveState(false);
    }

    protected Widget getSubjectDbWidget(RecruitableJobInfo job) {
        if (job == null || job.getSubjectName() != null)
            return HtmlUtils.getHtml("&nbsp;", "text");
        else
            return new FulltextPopperUpperHTML(job.getSubjectName(), DB_MAX_SIZE);
    }

    public Panel getSubmissionButtonPanel() {
        CenteredWidgetHorizontalPanel masterButtonPanel = new CenteredWidgetHorizontalPanel();
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", "text"));
        submitButton = new SmallRoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        buttonPanel.add(submitButton);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        resetButton = new SmallRoundedButton("Reset", new ClickListener() {
            public void onClick(Widget sender) {
                setJob(_originalJob);
            }
        });
        buttonPanel.add(resetButton);
        buttonPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        saveButton = new SmallRoundedButton("Save My Work", new ClickListener() {
            public void onClick(Widget sender) {
                saveJob();
            }
        });
        buttonPanel.add(saveButton);
        masterButtonPanel.add(buttonPanel);
        return masterButtonPanel;
    }

    private void saveJob() {
        // Popup and get the name of the job
        FrvSaveWorkPopup saveWorkPopup = new FrvSaveWorkPopup(_originalJob, saveButton);
        saveWorkPopup.center();
        new PopupAtRelativePixelLauncher(saveWorkPopup, 0, 0).showPopup(saveButton);
    }

    private void submitJob() {
        boolean hasChanged = false;
        for (Object panel : panels) {
            FrvControlsPanelTab frvControlsPanelTab = (FrvControlsPanelTab) panel;
            if (frvControlsPanelTab.updateJobChanges()) {
                //Window.alert(frvControlsPanelTab.getTabLabel()+" has a change");
                hasChanged = true;
            }
        }
        if (hasChanged) {
            _logger.debug("Running recruitment task");
            setActiveState(true);
            _recruitmentService.runRecruitmentJob(_originalJob, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("Could not run Recruitment job\n" + caught.getMessage());
                    ErrorPopupPanel popup = new ErrorPopupPanel("Unable to process your request at this time.");
                    new PopupCenteredLauncher(popup, 250).showPopup(submitButton);
                    setActiveState(false);
                }

                public void onSuccess(Object result) {
                    // A null result essentially means that nothing actually changed even though hasChanged said it did.
                    // The above case would need to be fixed.
                    if (null == result) {
                        _logger.warn("FRV said the info object hasChanged but the back end is returning null (no change).\t  This needs to be corrected.");
                        return;
                    }
                    String taskId = (String) result;
                    createJobStatusTimer(taskId);
                }
            });
        }
        else {
            InfoPopupPanel popup = new InfoPopupPanel("There are no changes to submit.");
            new PopupAboveLauncher(popup, 250).showPopup(submitButton);
        }
    }

    /**
     * This method uses the JobStatusTimer to monitor the job.
     *
     * @param jobNumber - grid job number
     */
    private void createJobStatusTimer(final String jobNumber) {
        new JobStatusTimer(jobNumber, 2000, new JobStatusListener() {
            public void onJobRunning(JobInfo ignore) {
                // ignore, timer still running
            }

            // timer cancels itself
            public void onJobFinished(JobInfo newJobInfo) {
                _logger.debug("Job " + newJobInfo.getJobId() + " completed, status = " + newJobInfo.getStatus());
                setActiveState(false);
                listener.onSelect(newJobInfo);
                //setJob((RecruitableJobInfo)newJobInfo);
            }

            // timer cancels itself
            public void onCommunicationError() {
                postJobStatusError(jobNumber, null);
                setActiveState(false);
            }
        });
    }

    private void setActiveState(boolean jobRunning) {
        submitButton.setEnabled(!jobRunning);
        resetButton.setEnabled(!jobRunning);
        if (_originalJob.getUsername().equalsIgnoreCase(User.SYSTEM_USER_LOGIN)) {
            saveButton.setEnabled(false);
        }
        else {
            saveButton.setEnabled(!jobRunning);
        }
        _loadingLabel.setVisible(jobRunning);
    }

    private void postJobStatusError(String jobNumber, Throwable e) {
        postError("An error occurred during status check for job number " + jobNumber, e);
    }

    /**
     * This method posts an error message to the UI and logs it and the exception as well
     *
     * @param msg       error message
     * @param throwable the error being reported
     */
    private void postError(String msg, Throwable throwable) {
        _logger.error(msg, throwable);
        ErrorPopupPanel popup = new ErrorPopupPanel("Unable to process your request at this time.");
        new PopupCenteredLauncher(popup, 250).showPopup(submitButton);
    }

}
