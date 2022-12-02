package org.janelia.workstation.n5viewer;

import com.google.common.eventbus.Subscribe;
import org.janelia.jacsstorage.clients.api.JadeStorageService;
import org.janelia.model.domain.files.N5Container;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.bdv.N5Viewer;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Collections;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.n5viewer//BigDataViewerTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = BigDataViewerTopComponent.PREFERRED_ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.workstation.n5viewer.BigDataViewerTopComponent")
@ActionReference(path = "Menu/Window/N5 Viewer", position = 100)
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
    public static final String LABEL_TEXT = "N5 Viewer";
    private N5Viewer n5Viewer;

    public BigDataViewerTopComponent() {
        setName(LABEL_TEXT);
        setToolTipText(LABEL_TEXT);
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        setupGUI();
    }

    private void setupGUI() {
        setLayout(new BorderLayout());
        this.setDoubleBuffered(true); // Copied from BigDataViewer's ViewerFrame
    }

    public static BigDataViewerTopComponent getInstance() {
        return (BigDataViewerTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {

    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {

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

    public void loadData(N5Container n5Container, N5TreeNode n5TreeNode) {

        JadeStorageService jadeStorage = FileMgr.getFileMgr().getStorageService();
        try {
            N5Reader n5Reader = new N5JadeReader(jadeStorage, n5Container.getFilepath());
            DataSelection dataSelection = new DataSelection(n5Reader,
                    Collections.singletonList(n5TreeNode.getMetadata()));
            this.n5Viewer = new N5Viewer(null, dataSelection, false);
            removeAll();
            add(n5Viewer.getBdvSplitPanel());
            updateUI();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addData(N5Container n5Container, N5TreeNode n5TreeNode) {

        JadeStorageService jadeStorage = FileMgr.getFileMgr().getStorageService();
        try {
            N5Reader n5Reader = new N5JadeReader(jadeStorage, n5Container.getFilepath());
            DataSelection dataSelection = new DataSelection(n5Reader,
                    Collections.singletonList(n5TreeNode.getMetadata()));
            n5Viewer.addData(dataSelection);
            updateUI();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

