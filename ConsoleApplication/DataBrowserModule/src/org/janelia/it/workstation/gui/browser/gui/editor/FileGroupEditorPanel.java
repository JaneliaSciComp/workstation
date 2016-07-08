package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.FileGroupSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.IconGridViewerPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An editor which can display the file groups for a given sample result.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileGroupEditorPanel extends JPanel implements SampleResultEditor {

    // Constants
    private static final Logger log = LoggerFactory.getLogger(FileGroupEditorPanel.class);
    private static final BufferedImage UNKNOWN_ICON = Utils.toBufferedImage(Icons.getIcon("question_block_large.png").getImage());

    // Utilities
    private final Debouncer debouncer = new Debouncer();

    // UI Elements
    private final ConfigPanel configPanel;
    private final FileGroupIconGridViewer resultsPanel;
    private final ImageTypeSelectionButton typeButton;

    // State
    private final FileGroupSelectionModel selectionModel = new FileGroupSelectionModel();
    private PipelineResult result;

    public FileGroupEditorPanel() {
        
    	setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        configPanel = new ConfigPanel(false);

        typeButton = new ImageTypeSelectionButton() {
            @Override
            protected void imageTypeChanged(String typeName) {
                log.info("Setting image type preference: "+typeName);
                setPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, typeName);
            }
        };

        resultsPanel = new FileGroupIconGridViewer();
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->FileGroupEditorPanel"));
    }

    @Override
    public void loadSampleResult(final PipelineResult result, final boolean isUserDriven, final Callable<Void> success) {

        if (result==null) return;
        
        if (!debouncer.queue(null)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadSampleResult(PipelineResult:{})",result.getName());

        this.result = result;
        Sample sample = result.getParentRun().getParent().getParent();
        selectionModel.setParentObject(sample);

        if (this.result == null) {
            showNothing();
            debouncer.success();
        }
        else {
            showResult(result, isUserDriven, success);
        }

        debouncer.success();
        updateUI();
    }

    private void showResult(PipelineResult result, final boolean isUserDriven, final Callable<Void> success) {
        try {
            ObjectiveSample objectiveSample = result.getParentRun().getParent();
            configPanel.setTitle(objectiveSample.getObjective()+" "+result.getName());

            HasFileGroups hasFileGroups = (HasFileGroups)result;

            DomainObject parentObject = (DomainObject)selectionModel.getParentObject();

            Preference preference2 = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_IMAGE_TYPE, parentObject.getId().toString());
            log.info("Got image type preference: "+preference2);
            if (preference2!=null) {
                typeButton.setImageType((String)preference2.getValue());
            }
            typeButton.populate(hasFileGroups.getGroups());

            List<FileGroup> sortedGroups = new ArrayList<>(hasFileGroups.getGroups());
            Collections.sort(sortedGroups, new Comparator<FileGroup>() {
                @Override
                public int compare(FileGroup o1, FileGroup o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            log.info("Showing "+hasFileGroups.getGroupKeys().size()+" file groups");
            resultsPanel.showObjects(sortedGroups, success);
            showResults(isUserDriven);
        }  catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    public void showResults(boolean isUserDriven) {
        add(configPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        updateUI();
    }

    public void showNothing() {
        removeAll();
        updateUI();
    }

    public void search() {
        showResult(result, true, null);
    }

    @Override
    public String getName() {
        return "File Group Editor";
    }
    
    @Override
    public Object getEventBusListener() {
        return resultsPanel;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        if (result==null) return;
        if (event.isTotalInvalidation()) {
            log.info("Total invalidation, reloading...");
            refreshResult();
        }
        else {
            Sample sample = result.getParentRun().getParent().getParent();
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(sample.getId())) {
                    log.info("Sample invalidated, reloading...");
                    refreshResult();
                    break;
                }
            }
        }
    }

    private void refreshResult() {
        try {
            Sample sample = result.getParentRun().getParent().getParent();
            Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, sample.getId());
            if (updatedSample!=null) {
                List<PipelineResult> results = updatedSample.getResultsById(PipelineResult.class, result.getId());
                if (results.isEmpty()) {
                    log.info("Sample no longer has result with id: "+ result.getId());
                    showNothing();
                    return;
                }
                showResult(results.get(results.size()-1), false, null);
            }
            else {
                showNothing();
            }
        }  
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private FileGroup getFileGroup(String key) {
        return ((HasFileGroups)result).getGroup(key);
    }

    private final ImageModel<FileGroup, String> imageModel = new ImageModel<FileGroup, String>() {

        @Override
        public FileGroup getImageByUniqueId(String id) {
            return getFileGroup(id);
        }

        @Override
        public String getImageUniqueId(FileGroup imageObject) {
            return imageObject.getKey();
        }

        @Override
        public String getImageFilepath(FileGroup imageObject) {
            return DomainUtils.getFilepath(imageObject, typeButton.getImageType());
        }

        @Override
        public String getImageTitle(FileGroup imageObject) {
            return imageObject.getKey();
        }

        @Override
        public String getImageSubtitle(FileGroup imageObject) {
            return null;
        }

        @Override
        public BufferedImage getStaticIcon(FileGroup imageObject) {
            return UNKNOWN_ICON;
        }

        @Override
        public List<Annotation> getAnnotations(FileGroup imageObject) {
            return Collections.emptyList();
        }
    };

    private class FileGroupIconGridViewer extends IconGridViewerPanel<FileGroup,String> {

        public FileGroupIconGridViewer() {
            setImageModel(imageModel);
            setSelectionModel(selectionModel);
            getToolbar().getShowTagsButton().setVisible(false); // No annotations
            getToolbar().getConfigButton().setVisible(false); // No configuration
            getToolbar().addCustomComponent(typeButton);
        }

        @Override
        protected void objectDoubleClick(FileGroup object) {
        }

        @Override
        protected JPopupMenu getContextualPopupMenu() {
            return null;
        }

        @Override
        protected void moreAnnotationsButtonDoubleClicked(FileGroup userObject) {
        }

        @Override
        protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
            return null;
        }

        @Override
        protected void configButtonPressed() {

        }
    }

    private void setPreference(final String name, final String value) {

        Utils.setMainFrameCursorWaitStatus(true);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
                if (parentObject.getId()!=null) {
                    DomainMgr.getDomainMgr().setPreference(name, parentObject.getId().toString(), value);
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setMainFrameCursorWaitStatus(false);
                search();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setMainFrameCursorWaitStatus(false);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }
}
