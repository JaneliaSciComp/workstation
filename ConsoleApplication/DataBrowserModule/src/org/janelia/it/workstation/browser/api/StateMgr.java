package org.janelia.it.workstation.browser.api;

import java.awt.Color;
import java.awt.IllegalComponentStateException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.state.NavigationHistory;
import org.janelia.it.workstation.browser.api.state.UserColorMapping;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.selection.OntologySelectionEvent;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBind;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.browser.util.RendererType2D;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel;

/**
 * Singleton for tracking and restoring the current state of the GUI. The
 * state may be tracked on a per-user or per-installation basis, but nothing 
 * here will ever modify the actual data. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StateMgr {

    private static final Logger log = LoggerFactory.getLogger(StateMgr.class);
    
    // Singleton
    private static StateMgr instance;
    public static synchronized StateMgr getStateMgr() {
        if (instance==null) {
            instance = new StateMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }
    
    public static final String AUTO_SHARE_TEMPLATE = "Browser.AutoShareTemplate";
    public final static String RECENTLY_OPENED_HISTORY = "Browser.RecentlyOpenedHistory";
    public static final int MAX_RECENTLY_OPENED_HISTORY = 10;
    public static final String ADD_TO_FOLDER_HISTORY = "ADD_TO_FOLDER_HISTORY";
    public static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    
    private final Map<TopComponent,NavigationHistory> navigationHistoryMap = new HashMap<>();
    private final UserColorMapping userColorMapping = new UserColorMapping();
    
    private Annotation currentSelectedOntologyAnnotation;
    private OntologyTerm errorOntology;
    private PermissionTemplate autoShareTemplate;

    public static boolean isDarkLook = false;
    
    private StateMgr() {     
        log.info("Initializing State Manager");
        try {
            this.autoShareTemplate = (PermissionTemplate)ConsoleApp.getConsoleApp().getModelProperty(AUTO_SHARE_TEMPLATE);
            
            if (ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.UNLOAD_IMAGES_PROPERTY) == null) {
                ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.UNLOAD_IMAGES_PROPERTY, false);
            }
    
            if (ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.DISPLAY_RENDERER_2D) == null) {
                ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.DISPLAY_RENDERER_2D, RendererType2D.IMAGE_IO.toString());
            }
            
            log.debug("Using 2d renderer: {}", ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.DISPLAY_RENDERER_2D));
        }
        catch (Throwable e) {
            // Catch all exceptions, because anything failing to init here cannot be allowed to prevent the Workstation from starting
            ConsoleApp.handleException(e);
        }
    }

    public void initLAF() {
        String[] li = {"Licensee=HHMI", "LicenseRegistrationNumber=122030", "Product=Synthetica", "LicenseType=Single Application License", "ExpireDate=--.--.----", "MaxVersion=2.20.999"};
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", "9A519ECE-5BB55629-B2E1233E-9E3E72DB-19992C5D");

        String[] li2 = {"Licensee=HHMI", "LicenseRegistrationNumber=142016", "Product=SyntheticaAddons", "LicenseType=Single Application License", "ExpireDate=--.--.----", "MaxVersion=1.10.999"};
        UIManager.put("SyntheticaAddons.license.info", li2);
        UIManager.put("SyntheticaAddons.license.key", "43BF31CE-59317732-9D0D5584-654D216F-7806C681");

        // Ensure the synthetical choices are all available.
        UIManager.installLookAndFeel("Synthetica AluOxide Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaAluOxideLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackEye Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackStar Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueIce Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueLight Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueLightLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueSteel Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel");
        UIManager.installLookAndFeel("Synthetica Classy Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaClassyLookAndFeel");
        UIManager.installLookAndFeel("Synthetica GreenDream Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel");
        UIManager.installLookAndFeel("Synthetica MauveMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica OrangeMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica SilverMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica Simple2D Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel");
        UIManager.installLookAndFeel("Synthetica SkyMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica WhiteVision Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel");
        LookAndFeelInfo[] installedInfos = UIManager.getInstalledLookAndFeels();

        String lafName = (String) ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.DISPLAY_LOOK_AND_FEEL);
        LookAndFeel currentLaf = UIManager.getLookAndFeel();
        LookAndFeelInfo currentLafInfo = null;
        if (lafName==null) lafName = "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel";
        try {
            boolean installed = false;
            for (LookAndFeelInfo lafInfo : installedInfos) {
                if (lafInfo.getClassName().equals(lafName)) {
                    installed = true;
                }
                if (lafInfo.getName().equals(currentLaf.getName())) {
                    currentLafInfo = lafInfo;
                }
            }
            if (installed) {
                setLookAndFeel(lafName);
            }
            else if (currentLafInfo != null) {
                setLookAndFeel(currentLafInfo.getName());
                ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.DISPLAY_LOOK_AND_FEEL, currentLafInfo.getClassName());
            }
            else {
                log.error("Could not set Look and Feel: {}",lafName);
            }
        }
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }

    private void setLookAndFeel(String lookAndFeelClassName) {
        try {
            if (lookAndFeelClassName.contains("BlackEye")) {
                isDarkLook = true;
                try {
                    
                    UIManager.setLookAndFeel(new SyntheticaBlackEyeLookAndFeel() {
                        @Override
                        protected void loadCustomXML() throws ParseException {
                            loadXMLConfig("/SyntheticaBlackEyeLookAndFeel.xml");
                        }
                    });
                    
                    // Override HTML link color to baby blue, which is the same as the Synthetica black eye highlight color. 
                    // Don't ask me why the following works even after the HTMLEditorKit instance goes out of scope. Some stones are best left unturned.
                    HTMLEditorKit kit = new HTMLEditorKit();
                    StyleSheet styleSheet = kit.getStyleSheet();
                    styleSheet.addRule("a {color:#BADCFB;}");
                    
                }
                catch (IllegalComponentStateException ex) {
                    ConsoleApp.handleException(ex);
                }
            }
            else {
                UIManager.setLookAndFeel(lookAndFeelClassName);
            }

            ConsoleApp.getConsoleApp().setModelProperty(OptionConstants.DISPLAY_LOOK_AND_FEEL, lookAndFeelClassName);
            log.info("Configured Look and Feel: {}", lookAndFeelClassName);
        }
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }
    
    public boolean isDarkLook() {
        return isDarkLook;
    }
    
    @Subscribe
    public void cleanup(ApplicationClosing e) {
        log.info("Saving auto-share template");
        ConsoleApp.getConsoleApp().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }

    public NavigationHistory getNavigationHistory(DomainListViewTopComponent topComponent) {
        if (topComponent==null) return null;
        NavigationHistory navigationHistory = navigationHistoryMap.get(topComponent);
        if (navigationHistory==null) {
            navigationHistory = new NavigationHistory();
            navigationHistoryMap.put(topComponent, navigationHistory);
        }
        return navigationHistory;
    }
    
    public NavigationHistory getNavigationHistory() {
        return getNavigationHistory(DomainListViewManager.getInstance().getActiveViewer());
    }

    public void updateNavigationButtons(DomainListViewTopComponent topComponent) {
        NavigationHistory navigationHistory = getNavigationHistory(topComponent);
        if (navigationHistory!=null) {
            navigationHistory.updateButtons();
        }
    }
    
    public UserColorMapping getUserColorMapping() {
        return userColorMapping;
    }

    public Color getUserAnnotationColor(String username) {
        return userColorMapping.getColor(username);
    }
    
    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String) ConsoleApp.getConsoleApp().getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) {
            return null;
        }
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public void setCurrentOntologyId(Long ontologyId) {
        log.info("Setting current ontology to {}", ontologyId);
        String idStr = ontologyId==null?null:ontologyId.toString();
        ConsoleApp.getConsoleApp().setModelProperty("lastSelectedOntology", idStr);
        Events.getInstance().postOnEventBus(new OntologySelectionEvent(ontologyId));
    }

    public Ontology getCurrentOntology() throws Exception {
        Long currentOntologyId = getCurrentOntologyId();
        if (currentOntologyId==null) return null;
        return DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, currentOntologyId);
    }
    
    public Annotation getCurrentSelectedOntologyAnnotation() {
        return currentSelectedOntologyAnnotation;
    }

    public void setCurrentSelectedOntologyAnnotation(Annotation currentSelectedOntologyAnnotation) {
        this.currentSelectedOntologyAnnotation = currentSelectedOntologyAnnotation;
    }
    
    public OntologyTerm getErrorOntology() {
        // TODO: use DomainDAO.getErrorOntologyCategory
        if (errorOntology == null) {
            try {
                List<Ontology> ontologies = DomainMgr.getDomainMgr().getModel().getDomainObjects(Ontology.class, DomainConstants.ERROR_ONTOLOGY_NAME);

                for (Ontology ontology : ontologies) {
                    if (DomainConstants.GENERAL_USER_GROUP_KEY.equals(ontology.getOwnerKey())) {
                        OntologyTerm term = ontology.findTerm(DomainConstants.ERROR_ONTOLOGY_CATEGORY);
                        if (term instanceof Category) {
                            errorOntology = term;
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
            
        }
        return errorOntology;
    }

    public OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) throws Exception {
        String category = DomainConstants.PREFERENCE_CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
        List<Preference> prefs = DomainMgr.getDomainMgr().getPreferences(category);
        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(AccessManager.getSubjectKey(), ontologyId);
        for (Preference pref : prefs) {
            log.debug("Found preference: {}", pref);
            ontologyKeyBindings.addBinding(pref.getKey(), Long.parseLong((String)pref.getValue()));
        }
        log.debug("Loaded {} key bindings for ontology {}", ontologyKeyBindings.getKeybinds().size(), ontologyKeyBindings.getOntologyId());
        return ontologyKeyBindings;
    }

    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {
        String category = DomainConstants.PREFERENCE_CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
        boolean changed = false;
        Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        log.debug("Saving {} key bindings for ontology {}", keybinds.size(), ontologyKeyBindings.getOntologyId());
        for (OntologyKeyBind bind : keybinds) {
            Preference pref = DomainMgr.getDomainMgr().getPreference(category, bind.getKey());
            String value = bind.getOntologyTermId().toString();
            if (pref==null) {
                // Create
                pref = new Preference(AccessManager.getSubjectKey(), category, bind.getKey(), value);
                log.debug("Creating new preference: {}", pref);
                DomainMgr.getDomainMgr().savePreference(pref);
                changed = true;
            }
            else if (!StringUtils.areEqual(pref.getValue(), value)) {
                // Update
                log.debug("Updating value for preference {}: {}={}", pref.getId(), pref.getKey(), value);
                pref.setValue(value);
                DomainMgr.getDomainMgr().savePreference(pref);
                changed = true;
            }
            else {
                log.debug("Preference already exists: {}", pref);
            }
        }

        if (changed) {
            Ontology ontology = DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, ontologyKeyBindings.getOntologyId());
            if (ontology!=null) {
                Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(ontology));
            }
        }
    }

    public PermissionTemplate getAutoShareTemplate() {
        return autoShareTemplate;
    }

    public void setAutoShareTemplate(PermissionTemplate autoShareTemplate) {
        this.autoShareTemplate = autoShareTemplate;
        ConsoleApp.getConsoleApp().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }
    
    public List<String> getRecentlyOpenedHistory() {
        return getHistoryProperty(RECENTLY_OPENED_HISTORY);
    }

    public void updateRecentlyOpenedHistory(String ref) {
        updateHistoryProperty(RECENTLY_OPENED_HISTORY, MAX_RECENTLY_OPENED_HISTORY, ref);
    }

    public List<String> getAddToFolderHistory() {
        return getHistoryProperty(ADD_TO_FOLDER_HISTORY);
    }

    public void updateAddToFolderHistory(String ref) {
        updateHistoryProperty(ADD_TO_FOLDER_HISTORY, MAX_ADD_TO_ROOT_HISTORY, ref);
    }
    
    private List<String> getHistoryProperty(String prop) {
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>)ConsoleApp.getConsoleApp().getModelProperty(prop);
        if (history == null) return new ArrayList<>();
        // Must make a copy of the list so that we don't use the same reference that's in the cache.
        log.debug("History property {} contains {}",prop,history);
        return new ArrayList<>(history);
    }

    private void updateHistoryProperty(String prop, int maxItems, String value) {
        List<String> history = getHistoryProperty(prop);
        if (history.contains(value)) {
            log.debug("Recently opened history already contains {}. Bringing it forward.",value);
            history.remove(value);
        }
        if (history.size()>=maxItems) {
            history.remove(history.size()-1);
        }
        history.add(0, value);
        log.debug("Adding {} to recently opened history",value);
        // Must make a copy of the list so that our reference doesn't go into the cache.
        List<String> copy = new ArrayList<>(history);
        ConsoleApp.getConsoleApp().setModelProperty(prop, copy); 
    }
    
    public Task getTaskById(Long taskId) throws Exception {
        return DomainMgr.getDomainMgr().getLegacyFacade().getTaskById(taskId);
    }
    
    public Task submitJob(String processDefName, String displayName) throws Exception {
        HashSet<TaskParameter> taskParameters = new HashSet<>();
        return submitJob(processDefName, displayName, taskParameters);
    }

    public Task submitJob(String processDefName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        GenericTask task = new GenericTask(new HashSet<Node>(), AccessManager.getSubjectKey(), new ArrayList<Event>(),
                parameters, processDefName, displayName);
        return submitJob(task);
    }

    public Task saveOrUpdateTask(Task task) throws Exception {
        return DomainMgr.getDomainMgr().getLegacyFacade().saveOrUpdateTask(task);
    }
    
    private Task submitJob(GenericTask genericTask) throws Exception {
        Task task = saveOrUpdateTask(genericTask);
        submitJob(task.getTaskName(), task);
        return task;
    }

    public void submitJob(String processDefName, Task task) throws Exception {
        DomainMgr.getDomainMgr().getLegacyFacade().submitJob(processDefName, task.getObjectId());
    }

    /**
     * Like the submitJob method, but this one pushes the task to a dispatcher, for scheduled (balanced?)
     * retrieval by waiting servers.
     *
     * @param processDefName name for labeling.
     * @param task with all params
     * @return the request created.
     * @throws Exception
     */
    public void dispatchJob(String processDefName, Task task) throws Exception {
        DomainMgr.getDomainMgr().getLegacyFacade().dispatchJob(processDefName, task.getObjectId());
    }
}
