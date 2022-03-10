package org.janelia.workstation.ndviewer;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import com.google.common.eventbus.Subscribe;
import java.awt.*;
import javax.swing.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.util.Refreshable;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@TopComponent.Registration(mode = "editor", openAtStartup = true)
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
    final BdvHandlePanel bdv;

    public BigDataViewerTopComponent() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        setName(LABEL_TEXT);
        setToolTipText(LABEL_TEXT);
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        bdv = new BdvHandlePanel(topFrame, Bdv.options());
        setupGUI();
    }

    private void setupGUI() {
        setLayout(new GridLayout(1, 1));
        this.add(bdv.getViewerPanel());
        // TODO to be added to side explorer
        //  this.add(bdv.getCardPanel().getComponent());
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

}

