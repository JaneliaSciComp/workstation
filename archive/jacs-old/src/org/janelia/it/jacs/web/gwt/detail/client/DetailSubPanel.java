
package org.janelia.it.jacs.web.gwt.detail.client;

import java.awt.Panel;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;

/**
 * Base-class for all DetailSubPanels.  It contains the data and operations needed to
 * render the panel
 *
 * @author Tareq Nabeel
 */
public abstract class DetailSubPanel extends VerticalPanel {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel");

    public static String DETAIL_HELP_URL_PROP = "DetailPage.HelpURL";

    private TitledBox mainTitledBox;
    private FlexTable mainDataTable;
    private LoadingLabel mainLoadingLabel;
    private String acc;
    private org.janelia.it.jacs.web.gwt.detail.client.DetailPanel parentPanel;
    private ActionLink previousPanelLink;

    public DetailSubPanel() {
        super();
    }

    public String getAcc() {
        return acc;
    }

    public void setAcc(String acc) {
        this.acc = acc;
    }

    public TitledBox getMainTitledBox() {
        return mainTitledBox;
    }

    public void setMainTitledBox(TitledBox mainTitledBox) {
        this.mainTitledBox = mainTitledBox;
    }

    public FlexTable getMainDataTable() {
        return mainDataTable;
    }

    public void setMainDataTable(FlexTable mainDataTable) {
        this.mainDataTable = mainDataTable;
    }

    public LoadingLabel getMainLoadingLabel() {
        return mainLoadingLabel;
    }

    public void setMainLoadingLabel(LoadingLabel mainLoadingLabel) {
        this.mainLoadingLabel = mainLoadingLabel;
    }

    public org.janelia.it.jacs.web.gwt.detail.client.DetailPanel getParentPanel() {
        return parentPanel;
    }

    /**
     * Retrieves the link to the previous panel
     *
     * @return previousPanelLink
     */
    public ActionLink getPreviousPanelLink() {
        return previousPanelLink;
    }

    /**
     * Can be used to store any user preferences for example before sub panel is blow away
     */
    public void preInit() {
    }

    /**
     * Can be used to restore any user preferences
     */
    public void postInit() {
    }

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public abstract String getDetailTypeLabel();

    /**
     * Returns the specific detail object
     *
     * @return BaseSequenceEntity instance for ReadPanel,
     *         Project instance for ProjectPanel,
     *         Publication instance for Publication panel, etc.
     */
    public abstract Object getEntity();

    /**
     * @param obj BaseSequenceEntity instance for ReadPanel,
     *            Project instance for ProjectPanel,
     *            Publication instance for Publication panel, etc.
     *            ProteinCluster instance for Cluster panel, etc.
     */
    public abstract void setEntity(Object obj);

    /**
     * Used to set error messages when entity retrieval fails
     */
    public void setServiceErrorMessage() {
        MessageUtil.setServiceErrorMessage(getMainTitledBox(), getDetailTypeLabel(), getEntityKeyLabel() + ": " + getAcc());
    }

    /**
     * Use to post error messages when entity cannot be found in database
     */
    public void setNotFoundErrorMessage() {
        MessageUtil.setNotFoundErrorMessage(getMainTitledBox(), getDetailTypeLabel(), getEntityKeyLabel() + ": " + getAcc());
    }

    /**
     * Adds entity data to the panel using the entity model instance retrieved through
     * the service call. This method is called after successful detailservice callback.
     */
    abstract public void displayData();

    /**
     * Optional link e.g. "back to job details" link on ReadPanel
     *
     * @param actionLink ActionLink instance
     */
    public void addActionLink(ActionLink actionLink) {
        logger.debug("DetailSubPanel addActionLink...");
        if (actionLink != null) {
            getMainTitledBox().addActionLink(actionLink);
        }
        previousPanelLink = actionLink;
    }

    /**
     * The method creates a link to a target entity
     * <p/>
     * The precondition for this method is parentEntity != null
     *
     * @param currentAcc        accession number of the current entity
     * @param currentPanelLabel brief description of the current detail panel
     * @param targetAcc         accession number of the target entity
     * @return the widget for accessing the target entity
     */
    public Widget getTargetAccessionWidget(final String currentAcc,
                                           final String currentPanelLabel,
                                           final String targetAcc) {

        Widget targetAccessWidget = new Link(targetAcc, new ClickListener() {

            ActionLink prevBackLink = getPreviousPanelLink();
            String pageTokenWhenCreated = History.getToken();
            // for now we do not handle history events ("back" and "forward")
            // however the clicks on the action links should add the corresponding history tokens
            BackActionLink backToCurrentDetailLink = new BackActionLink(
                    "back to " + currentPanelLabel,
                    new ClickListener() {
                        public void onClick(Widget widget) {
                            getParentPanel().rebuildPanel(currentAcc, pageTokenWhenCreated, prevBackLink);
                        }
                    }, pageTokenWhenCreated);

            public void onClick(Widget widget) {
                String currentPageTokenWhenClicked = History.getToken();
                String nextPageToken = null;
                if (currentPageTokenWhenClicked != null && currentPageTokenWhenClicked.length() > 0) {
                    nextPageToken = currentPageTokenWhenClicked.replaceAll(currentAcc, targetAcc);
                }
                getParentPanel().rebuildPanel(targetAcc, nextPageToken, backToCurrentDetailLink);
                getParentPanel().setCurrentBackLink(backToCurrentDetailLink);
                getParentPanel().setPreviousBackLink(prevBackLink);
            }
        });
        return targetAccessWidget;
    }

    protected void createDetailSpecificPanels() {
    }

    /**
     * Creates and adds the loading labels to the mainTitleBox.
     *
     * @see org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanel for example override
     */
    protected void createAndAddLoadingLabels() {
        logger.debug("DetailSubPanel createAndAddLoadingLabels...");
        mainLoadingLabel = new LoadingLabel("Loading " + getDetailTypeLabel() + " data...", true);
        mainTitledBox.add(mainLoadingLabel);
    }

    /**
     * This method creates the skeleton of the DetailSubPanel with loading messages
     * before data is retrieved from server asynchronously
     */
    protected void createSkeleton() {
        logger.debug("DetailSubPanel createSkeleton...");
        mainTitledBox = new TitledBox(getDetailTypeLabel() + " Details", true);
        mainTitledBox.setStyleName("detailMainPanel");

        // remove the default hide/show action link and add a help link
        mainTitledBox.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(mainTitledBox, new HelpActionLink("help"), DETAIL_HELP_URL_PROP);

        mainDataTable = new FlexTable();
        mainTitledBox.add(mainDataTable);
        //mainDataTable.setBorderWidth(1);
        add(mainTitledBox);

        createAndAddLoadingLabels();
    }

    /**
     * The label to display for entity key e.g. Camera Acc
     *
     * @return The label to use for entity id in Title box and logging/error messages
     */
    protected String getEntityKeyLabel() {
        return "Camera Acc";
    }

    protected void setParentPanel(org.janelia.it.jacs.web.gwt.detail.client.DetailPanel parentPanel) {
        this.parentPanel = parentPanel;
    }


    protected void addSpacer(Panel panel) {
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
    }

    protected void addSmallSpacer(Panel panel) {
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
    }
}
