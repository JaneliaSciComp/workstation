package org.janelia.workstation.ndviewer;

import bdv.util.*;
import bdv.util.volatiles.VolatileViews;
import com.google.common.eventbus.Subscribe;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.files.N5Container;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.bdv.N5Viewer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;
import java.io.IOException;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.ndviewer//BigDataViewerTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = BigDataViewerTopComponent.PREFERRED_ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.workstation.ndviewer.BigDataViewerTopComponent")
@ActionReference(path = "Menu/Window")
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BigDataViewerTopComponentAction",
        preferredID = BigDataViewerTopComponent.PREFERRED_ID
)
@Messages({
        "CTL_BigDataViewerTopComponentAction=BigDataViewer",
        "CTL_BigDataViewerTopComponent=" + BigDataViewerTopComponent.LABEL_TEXT,
        "HINT_BigDataViewerTopComponent=BigDataViewer"
})
public final class BigDataViewerTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(BigDataViewerTopComponent.class);

    public static final String PREFERRED_ID = "BigDataViewerTopComponent";
    public static final String LABEL_TEXT = "BigDataViewer";

    private Refreshable currentView;
    private BdvHandlePanel bdv;

    public BigDataViewerTopComponent() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        setName(LABEL_TEXT);
        setToolTipText(LABEL_TEXT);
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        //bdv = new BdvHandlePanel(topFrame, Bdv.options());
        setupGUI();
    }

    public static boolean isSupported(DomainObject domainObject) {
        return domainObject instanceof N5Container;
    }

//    static boolean isNavigateOnClick() {
//        return FrameworkAccess.getModelProperty(OptionConstants.NAVIGATE_ON_CLICK, true);
//    }

    private void setupGUI() {
        setLayout(new GridLayout(1, 2));

        this.setDoubleBuffered(true); // Copied from BigDataViewer's ViewerFrame
    }

    public void add(RandomAccessibleInterval<FloatType> img) {
        BdvFunctions.show(img, "img", Bdv.options().addTo(bdv));
        revalidate();
        repaint();
    }

    public static BigDataViewerTopComponent getInstance() {
        return (BigDataViewerTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }


    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        if (currentView != null) currentView.refresh();
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            if (currentView != null) currentView.refresh();
        }
    }

    @Override
    public void componentOpened() {
        Events.getInstance().registerOnEventBus(this);
    }

    @Override
    public void componentClosed() {
        Events.getInstance().unregisterOnEventBus(this);
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    static boolean isNavigateOnClick() {
        return FrameworkAccess.getModelProperty(OptionConstants.NAVIGATE_ON_CLICK, true);
    }

    public void loadDomainObject(DomainObject domainObject) {
        N5Container container = ((N5Container) domainObject);
//        String filepath = ((N5Container) domainObject).getFilepath();
//        String filepath = "/groups/cellmap/cellmap/data/aic_desmosome-1/aic_desmosome-1.n5";
        String filepath = "/groups/cellmap/cellmap/data/jrc_mus-kidney/jrc_mus-kidney.n5";
        log.info("\nLoading into BDV: {}", filepath);

            SimpleWorker worker = new SimpleWorker() {
                N5Reader reader;
                RandomAccessibleInterval img;

                Img<?> img2;
                @Override
                protected void doStuff() throws Exception {
                    // TODO: need mapping or N5FS implementation via Jade
                    String localFilepath = filepath.replaceFirst("/groups/cellmap/cellmap", "/Volumes/cellmap");
                    log.info("\nLoading into BDV: {}", localFilepath);

                    reader = new N5FSReader(localFilepath) {
                        public Version getVersion() throws IOException {
                            // Disable version checking because the POM reading functionality doesn't work with the NetBeans module system
                            return new Version("0.0.0");
                        }
                    };

                    log.info("Exists /volumes: {}", reader.exists("/volumes"));
                    log.info("Exists /volumes/raw: {}", reader.exists("/volumes/raw"));
                    log.info("List /volumes/raw: {}", String.join(",", reader.list("/volumes/raw")));
                    log.info("DatasetAttributes /volumes/raw: {}", reader.getDatasetAttributes("/volumes/raw"));
                    CachedCellImg ts = N5Utils.openVolatile(reader, "/volumes/raw/s4");
                    img = VolatileViews.wrapAsVolatile(ts);
//                    System.out.println("igyg img.cursor().next().getClass() = " + img.cursor().next().getClass());

                    //img2 = N5Utils.open(reader, "volumes/masks/foreground");

//                    if (reader.exists("/volumes/raw/ch0") && reader.exists("/volumes/raw/ch1")) {
//                        log.info("\nLoading into BDV: {}/volumes/raw/ch0", localFilepath);
//                        log.info("\nLoading into BDV: {}/volumes/raw/ch1", localFilepath);
//                        final DatasetAttributes attributes = reader.getDatasetAttributes("/volumes/raw/ch0");
//                        log.info("\nData set attributes: {}", attributes);
//                        img = N5Utils.open(reader, "/volumes/raw/ch0");
//                        img2 = N5Utils.open(reader, "/volumes/raw/ch1");
//                    }
//                    else {
//                        log.info("\nLoading into BDV: {}/volumes/raw", localFilepath);
//                        try {
//                            DatasetAttributes attributes = reader.getDatasetAttributes("/volumes/raw");
//                            log.info("\nData set attributes: {}", attributes);
//                        } catch (NullPointerException e) {
//                            log.error("Error loading attributes: ", e);
//                        }
//                        img = N5Utils.open(reader, "/volumes/raw");
//                    }
                }

                @Override
                protected void hadSuccess() {
                    log.info("Showing in BDV");
                    // TODO: should be automatic, based on the data
//                    FinalInterval minMax = Intervals.createMinMax(100, 100, 100, 200, 200, 150);
//                    IntervalView view = Views.zeroMin(Views.interval(img, minMax));

                    // TODO: how to target a specific BDV instance?
                    //BdvFunctions.show(view, container.getName(), BdvOptions.options().addTo(bdv));

//                    if (img!= null) {
////                        System.out.println("img.cursor().next().getClass() = " + img.cursor().next().getClass());
//                        BdvStackSource<?> bss = BdvFunctions.show(img, container.getName(), BdvOptions.options().addTo(bdv));
//                    }
//                    if (img2!= null) {
//                        BdvStackSource<?> bss2 = BdvFunctions.show(img2, container.getName(), BdvOptions.options().addTo(bdv));
//                    }
//                    if (img2 == null && img==null) {
//                        log.error("No data loaded");
//                    }
                    //bss.setDisplayRange(0, 1024);
                    //bss2.setDisplayRange(0, 1024);

                    DataSelection dataSelection = null;//new DataSelection(reader, metadataList);
                    try {
                        N5Viewer n5Viewer = new N5Viewer(null, dataSelection, false);
                        BigDataViewerTopComponent.this.add(bdv.getSplitPanel());

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            worker.execute();


    }
}

