
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Sample;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanelBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.DetailServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.DetailSubPanel;
import org.janelia.it.jacs.web.gwt.detail.client.util.MessageUtil;
import org.janelia.it.jacs.web.gwt.map.client.panel.MapBox;

import java.util.List;

/**
 * Responsible for controlling the sequence and timing of operations need to build a BSEntityPanel
 *
 * @author Tareq Nabeel
 */
public class BSEntityPanelBuilder extends DetailPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanelBuilder");

    private static BSEntityServiceAsync bsEntityService = (BSEntityServiceAsync) GWT.create(BSEntityService.class);

    static {
        ((ServiceDefTarget) bsEntityService).setServiceEntryPoint("bsDetail.srv");
    }

    // This timer class is necessary to get Firefox to properly render the wrapper titlebox for
    // site/sample metadata.
    protected class MapBoxVisibilityTimer extends Timer {
        static final int START_COUNT = 20; // 10 seconds
        int sanityCount = START_COUNT;

        public MapBoxVisibilityTimer() {
            scheduleRepeating(500); // milliseconds
        }

        public void run() {
            BSEntityPanel bsePanel = (BSEntityPanel) getSubPanel();
            if (sanityCount < START_COUNT && bsePanel.getSiteManager().getSampleDataPanel().isSamplesPopulated()) {
                MapBox mapBox = bsePanel.getSiteManager().getMapPanel().getGoogleMapBox();
                mapBox.setVisible(true);
                bsePanel.getSiteManager().addMapBox();
                cancel();
            }
            else if (sanityCount <= 0) {
                cancel();
            }
            sanityCount--;
        }
    }

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public BSEntityPanelBuilder(DetailSubPanel subPanel) {
        super(subPanel);
    }

    /**
     * Retrieves the data needed to build the BSEntityPanel
     */
    protected void retrieveData() {
        logger.debug("BSEntityPanelBuilder retrieveData...");
        super.retrieveData();
    }

    /**
     * This method overrides the more generic method in DetailPanelBuilder
     *
     * @return
     */
    protected AsyncCallback
    getRetrieveEntityCallBack() {
        return new GetBSEntityCallback();
    }

    /**
     * Data handler for the result of the invokeRetrieveData
     */
    private class GetBSEntityCallback implements AsyncCallback {
        public void onFailure(Throwable throwable) {
            logger.error("DetailPanelBuilder GetEntityCallback failed: ", throwable);
            getSubPanel().setServiceErrorMessage();
        }

        public void onSuccess(Object result) {
            logger.debug("DetailPanelBuilder GetEntityCallback succeeded ");
            try {
                logger.debug("class of subpanel =" + getSubPanel().getClass().getName());
                getSubPanel().getMainLoadingLabel().setVisible(false);
                getSubPanel().setEntity(result);
                if (result != null) {
                    if (result instanceof BaseSequenceEntity) {
                        BaseSequenceEntity bse = (BaseSequenceEntity) result;
                        if (bse.getTaxonId() == null) {
                            setupPanelsAfterEntity();
                        }
                        else {
                            if (getSubPanel() instanceof BSEntityPanel) {
                                bsEntityService.getTaxonSynonyms(bse.getTaxonId(), new GetTaxonSynonymsCallback());
                            }
                            else {
                                setupPanelsAfterEntity();
                            }
                        }
                    }
                    else {
                        setupPanelsAfterEntity();
                    }
                }
                else {
                    getSubPanel().setNotFoundErrorMessage();
                }
            }
            catch (RuntimeException e) {
                logger.error("DetailPanelBuilder GetEntityCallback onSuccess caught exception", e);
                throw e;
            }
        }
    }

    private class GetTaxonSynonymsCallback implements AsyncCallback {
        public void onFailure(Throwable throwable) {
            logger.error("DetailPanelBuilder GetTaxonSynonymsCallback failed: ", throwable);
            // still need to setup panels regardless of failure
            setupPanelsAfterEntity();
        }

        public void onSuccess(Object result) {
            logger.debug("DetailPanelBuilder GetTaxonSynonymsCallback succeeded");
            try {
                List synonymList = (List) result;
                BSEntityPanel bp = (BSEntityPanel) getSubPanel();
                SequenceDetailsTableBuilder stb = bp.getBaseEntityTableBuilder();
                stb.setTaxonSynonyms(synonymList);
            }
            catch (RuntimeException e) {
                logger.error("DetailPanelBuilder GetTaxonSynonymsCallback on Success caught exception", e);
                // ignore error
            }
            setupPanelsAfterEntity();
        }
    }

    /**
     * Gets called on successful completion of retrieveData
     */
    protected void populatePanel() {
        try {
            logger.debug("BSEntityPanelBuilder populatePanel...");
            // start displaying entity data
            getBSEntityPanel().displayData();
            // display the IP notice
            if (getBSEntityPanel().isEntitySampleAvailable()) {
                getBSEntityPanel().displayIntellectualPropertyNotice();
                getBSEntityPanel().retrieveAndBuildSampleSiteMapData();
            }
            else if (getBSEntityPanel().hasSample()) {
                // the entity has associated sample with it but the sample data must be retrieved from the server
                retrieveEntitySample(getAcc());
                getBSEntityPanel().retrieveAndBuildSampleSiteMapData();
            }
            if (getBSEntityPanel().isDisplayDeflineFlag()) {
                getBSEntityPanel().displayEntityDefline();
            }
            // display the sequence
            if (getBSEntityPanel().isSequenceTooBigToDisplay()) {
                // if the sequence is too large don't even try to retrieve it and start displaying it
                getBSEntityPanel().displayEntitySequence(true);
            }
            else {
                // otherwise start downloading it and display it once we've got it
                // in order to avoid sequence data appearing before entity data
                retrieveSequenceData();
            }
            // retrieve other panel specific data
            retrieveAndPopulatePanelData();
        }
        catch (RuntimeException e) {
            logger.error("BSEntityPanelBuilder populatePanel caught exception " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the panel specific data
     */
    protected void retrieveAndPopulatePanelData() {
        logger.debug("retrieve and populate specific entity data");
        BSEntityPanel bsePanel = (BSEntityPanel) getSubPanel();
        if (bsePanel.hasSample()) {
            bsePanel.getSiteManager().getMapPanel().getGoogleMapBox().setVisible(false);
            new MapBoxVisibilityTimer();
        }
    }

    public static BSEntityServiceAsync getBaseEntityService() {
        return bsEntityService;
    }

    protected BSEntityPanel getBSEntityPanel() {
        return (BSEntityPanel) getSubPanel();
    }

    protected DetailServiceAsync getDetailService() {
        return bsEntityService;
    }

    private void retrieveEntitySample(String acc) {
        logger.debug("retrieve the intellectual property notice");
        getBaseEntityService().getEntitySampleByAcc(acc, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                logger.error("BSEntityService.getEntity failed", throwable);
            }

            public void onSuccess(Object object) {
                logger.debug("BSEntityService.getEntity return successfully");
                try {
                    Sample sample = (Sample) object;
                    getBSEntityPanel().displayIntellectualPropertyNotice(sample.getIntellectualPropertyNotice());
                }
                catch (Exception e) {
                    logger.error("Exception raised while processing the returned entity in retrieveIntellectualPropertyNoticeByReadAccession", e);
                }
            }
        });
    }

    /**
     * Retrieves sequence data
     */
    private void retrieveSequenceData() {
        logger.debug("BSEntityPanelBuilder retrieveSequenceData...");
        // Timer needed to give labels time to display .... very goofy
        new SequenceDataRetrievalTimer().schedule(1000);
    }

    /**
     * Delay needed to give UI the chance to make async call for retrieve sequence
     */
    private class SequenceDataRetrievalTimer extends Timer {
        public void run() {
            getBaseEntityService().getSequenceUIData(getAcc(),
                    getBSEntityPanel().getClearRangeBegin(),
                    getBSEntityPanel().getClearRangeEnd(),
                    getBSEntityPanel().SEQUENCE_CHARS_PER_LINE,
                    new GetSequenceDataCallback());
        }
    }

    /**
     * This method retrieves and displays the data needed for a BioSequence for the camera
     * accession supplied to DetailPanel
     */
    private class GetSequenceDataCallback implements AsyncCallback {
        public void onFailure(Throwable throwable) {
            logger.error("BSEntityPanelBuilder GetSequenceDataCallback failed: " + throwable.getMessage());
            getBSEntityPanel().getSequenceLoadingLabel().setVisible(false);
            MessageUtil.addServiceErrorMessage(getSubPanel().getMainTitledBox(),
                    "Sequence",
                    "Entity ID: " + getSubPanel().getAcc());
        }

        public void onSuccess(Object result) {
            try {
                logger.debug("BSEntityPanelBuilder GetSequenceDataCallback succeeded ");
                getBSEntityPanel().getSequenceLoadingLabel().setVisible(false);
                if (result != null) {
                    getBSEntityPanel().setSequenceUIData((SequenceUIData) result);
                    // Now that we have the sequence data, display it
                    getBSEntityPanel().displayEntitySequence(false);
                }
                else {
                    MessageUtil.addNotFoundErrorMessage(getSubPanel().getMainTitledBox(),
                            "Sequence",
                            "Entity ID: " + getSubPanel().getAcc());
                }
            }
            catch (RuntimeException e) {
                logger.error("BSEntityPanelBuilder GetSequenceDataCallback onSuccess caught exception:" + e.getMessage(), e);
                throw e;
            }
        }
    }
}
