package org.janelia.workstation.browser.gui.components;

import java.awt.BorderLayout;
import java.lang.ref.WeakReference;

import javax.swing.SwingUtilities;

import org.janelia.workstation.browser.gui.editor.StartPage;
import org.janelia.workstation.core.api.lifecycle.ConsoleState;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Most of this code is taken from NetBeans' WelcomeComponent.
 * 
 * @author  Richard Gregor, S. Aubrecht
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.browser.components//StartPage//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = StartPageTopComponent.TC_NAME,
        iconBase = "org/janelia/workstation/browser/images/application_go.png",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
)
@TopComponent.Registration(mode = "editor", openAtStartup = false, position = 0) // We do our own "openAtStartup" in the ShowingHook module.
@ActionID(category = "Window", id = "org.janelia.workstation.browser.components.StartPageTopComponent")
@ActionReference(path = "Menu/Window/Core", position = 40)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_StartPageAction",
        preferredID = StartPageTopComponent.TC_NAME
)
@Messages({
    "CTL_StartPageAction=Start Page",
    "CTL_StartPageTopComponent=Start Page"
})
public class StartPageTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);

    public static final String TC_NAME = "StartPageTopComponent";
    public static final String TC_VERSION = "1.0";

    private static WeakReference<StartPageTopComponent> component = new WeakReference<StartPageTopComponent>(null); 
    private StartPage startPage;

    private boolean initialized = false;
    
    private StartPageTopComponent(){
        setLayout(new BorderLayout());
        setName("Start Page");
        startPage = null;
        initialized = false;
    }

    void writeProperties(java.util.Properties p) {
    }

    void readProperties(java.util.Properties p) {
    }
    
    /**
     * #38900 - lazy addition of GUI components
     */
    private void doInitialize() {
        if (null == startPage) {
            startPage = new StartPage();
            add(startPage, BorderLayout.CENTER);
            //setFocusable(false);
        }
    }

    /**
     * Singleton accessor. As StartPageTopComponent is persistent singleton this
     * accessor makes sure that StartPageTopComponent is deserialized by window
     * system. Uses known unique TopComponent ID "Welcome" to get
     * StartPageTopComponent instance from window system. "Welcome" is name of
     * settings file defined in module layer.
     */
    public static StartPageTopComponent findComp() {
        StartPageTopComponent wc = component.get();
        if (wc == null) {
            TopComponent tc = WindowManager.getDefault().findTopComponent(TC_NAME); // NOI18N
            if (tc != null) {
                if (tc instanceof StartPageTopComponent) {
                    wc = (StartPageTopComponent) tc;
                    component = new WeakReference<StartPageTopComponent>(wc);
                }
                else {
                    // Incorrect settings file?
                    log.error("Incorrect settings file. Unexpected class returned for StartPageTopComponent");
                    wc = StartPageTopComponent.createComp();
                }
            }
            else {
                // Component cannot be deserialized
                // Fallback to accessor reserved for window system.
                wc = StartPageTopComponent.createComp();
            }
        }
        return wc;
    }

    /**
     * Singleton accessor reserved for window system ONLY. Used by window system
     * to create StartPageTopComponent instance from settings file when method
     * is given. Use <code>findComp</code> to get correctly deserialized
     * instance of StartPageTopComponent.
     */
    public static StartPageTopComponent createComp() {
        StartPageTopComponent wc = component.get();
        if (wc == null) {
            wc = new StartPageTopComponent();
            component = new WeakReference<StartPageTopComponent>(wc);
        }
        return wc;
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
        if (ConsoleState.getCurrState() < ConsoleState.WINDOW_SHOWN) {
            if (!ApplicationOptions.getInstance().isShowStartPageOnStartup()) {
                log.info("Closing start page because 'Show On Startup' is disabled");
                close();
                return;
            }
        }
        if (!initialized) {
            initialized = true;
            doInitialize();
        }
        if (startPage!=null) {
            Events.getInstance().registerOnEventBus(startPage);
        }
        ApplicationOptions.getInstance().addPropertyChangeListener(startPage);
    }

    @Override
    protected void componentShowing() {
        super.componentShowing();
        if (null != startPage && getComponentCount() == 0) {
            add(startPage, BorderLayout.CENTER);
        }
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        if (startPage!=null) {
            Events.getInstance().unregisterOnEventBus(startPage);
        }
        ApplicationOptions.getInstance().removePropertyChangeListener(startPage);
    }
    
    @Override 
    protected void componentHidden() {
        super.componentHidden();
        if (null != startPage) {
            remove(startPage);
        }
    }

    @Override
    protected void componentActivated() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                requestFocusInWindow();
            }
        });
    }
    
    @Override
    protected void componentDeactivated() {
    }

    @Override
    public void requestFocus() {
        if (null != startPage) {
            startPage.requestFocus();
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        if (null != startPage) {
            return startPage.requestFocusInWindow();
        }
        return super.requestFocusInWindow();
    }
}
