
package org.janelia.it.jacs.web.gwt.status.client.wizard;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.URLUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.status.client.JobResultsData;

/**
 * @author Michael Press
 */
public class DetailsWizardPage extends JobResultsWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.wizard.DetailsWizardPage");

    public static final String HISTORY_TOKEN = "DetailPage";
//    private DetailPanel _detailPanel;

    public DetailsWizardPage(JobResultsData data, WizardController controller) {
        super(data, controller);
        init();
    }

    public DetailsWizardPage(JobResultsData data, WizardController controller, boolean showButtons) {
        super(data, controller, showButtons);
        init();
    }

    private void init() {
        _detailPanel = new DetailPanel(getController());
    }

    public String getPageToken() // used for history
    {
        String pageToken = HISTORY_TOKEN;
        if (getData().getDetailAcc() != null) {
            pageToken = URLUtils.addParameter(pageToken, DetailPanel.ACC_PARAM, getData().getDetailAcc());
        }
        return pageToken;
    }

    public Widget getMainPanel() {
        return _detailPanel;
    }

    public String getPageTitle() {
        return Constants.JOBS_SEQUENCE_DETAILS_LABEL;
    }

    protected void preProcess(Integer priorPageNumber) {
        _logger.debug("DetailsWizardPage.preProcess()");

        // Add a "back" link to the main panel since it's removed after the reset()
        int currentPage = getController().getCurrentPageIndex();
        String backPageToken = getController().getPageTokenAt(currentPage - 1);
        BackActionLink backLink = new BackActionLink("back to job details", new ClickListener() {
            public void onClick(Widget widget) {
                getController().back();
            }
        });
        backLink.setTargetHistoryToken(backPageToken);

        _detailPanel.rebuildPanel(getData().getDetailAcc(), null /* page token */, backLink);
    }

    protected void setupButtons() {
        super.setupButtons(); // start with defaults;

        RoundedButton backButton = getButtonManager().getBackButton();
        backButton.setVisible(true);
        backButton.setEnabled(true);
        backButton.setText("Back to Job Details");

        getButtonManager().getNextButton().setVisible(false);
    }
}
