package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainViewerManager;
import org.janelia.it.workstation.gui.browser.components.DomainViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerManager;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.WrapperCreatorItemFactory;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.browser.nb_action.DomainObjectAcceptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectContextMenu.class);

    // Current selection
    protected DomainObject contextObject;
    protected List<DomainObject> domainObjectList;
    protected DomainObject domainObject;
    protected boolean multiple;
    protected ResultDescriptor resultDescriptor;
    protected String typeName;

    public DomainObjectContextMenu(DomainObject contextObject, List<DomainObject> domainObjectList, ResultDescriptor resultDescriptor, String typeName) {
        this.contextObject = contextObject;
        this.domainObjectList = domainObjectList;
        this.domainObject = domainObjectList.size() == 1 ? domainObjectList.get(0) : null;
        this.multiple = domainObjectList.size() > 1;
        this.resultDescriptor = resultDescriptor;
        this.typeName = typeName;
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.create", domainObject);
    }

    public void runDefaultAction() {
        if (DomainViewerTopComponent.isSupported(domainObject)) {
            DomainViewerTopComponent viewer = ViewerUtils.getViewer(DomainViewerManager.getInstance(), "editor2");
            if (viewer == null || !viewer.isCurrent(domainObject)) {
                viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(domainObject, true);
            }
        }
        else if (DomainExplorerTopComponent.isSupported(domainObject)) {
            // TODO: here we should select by path to ensure we get the right one, but for that to happen the domain object needs to know its path
            DomainExplorerTopComponent.getInstance().expandNodeById(contextObject.getId());
            DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(domainObject.getId());
        }
    }

    public void addMenuItems() {

        if (domainObjectList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());

        setNextAddRequiresSeparator(true);
        add(getOpenInNewEditorItem());
        add(getOpenSeparationInNewEditorItem());

        setNextAddRequiresSeparator(true);
        add(getPasteAnnotationItem());
        add(getDetailsItem());
        add(getPermissionItem());

        add(getAddToFolderItem());
        add(getRemoveFromFolderItem());

        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getFijiViewerItem());
        add(getDownloadItem());

        setNextAddRequiresSeparator(true);
        add(getReportProblemItem());
//        add(getMarkForReprocessingItem());
        addRerunSamplesAction();
        add(getSampleCompressionTypeItem());
        add(getProcessingBlockItem());
        add(getMergeItem());
//        add(getImportItem());
//
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
        //for ( JComponent item: getOpenForContextItems() ) {
        //    add(item);
        //}
        for (JMenuItem item: this.getWrapObjectItems()) {
			add(item);
		}

		for (JMenuItem item: this.getAppendObjectItems()) {
			add(item);
		}

        add(getSpecialAnnotationSession());
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : domainObject.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("Name",domainObject.getName()));
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("GUID",domainObject.getId().toString()));
    }

    protected JMenuItem getOpenInNewEditorItem() {
        if (multiple) return null;
        if (!DomainViewerTopComponent.isSupported(domainObject)) return null;

        try {
            final DomainObject objectToLoad = DomainViewerManager.getInstance().getObjectToLoad(domainObject);
            JMenuItem openItem = new JMenuItem("  Open " + objectToLoad.getType() + " In New Viewer");
            openItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.openInNewEditorItem", domainObject);
                    DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                    viewer.requestActive();
                    viewer.loadDomainObject(objectToLoad, true);
                }
            });
            return openItem;
        }
        catch (Exception e) {
            log.error("Error creating 'Open In New Viewer' menu item",e);
            return null;
        }
    }

    protected JMenuItem getOpenSeparationInNewEditorItem() {
        if (multiple) return null;
        if (!(domainObject instanceof NeuronFragment)) return null;

        JMenuItem copyMenuItem = new JMenuItem("  Open Neuron Separation In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContentMenu.openSeparationInNewEditorItem", domainObject);
                final SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                final NeuronFragment neuronFragment = (NeuronFragment)domainObject;

                SimpleWorker worker = new SimpleWorker() {
                    Sample sample;
                    PipelineResult result;

                    @Override
                    protected void doStuff() throws Exception {
                        sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                        if (sample!=null) {
                            result = SampleUtils.getResultContainingNeuronSeparation(sample, neuronFragment);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        if (sample==null) {
                            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                        }
                        else if (result==null) {
                            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
                        }
                        else {
                            viewer.requestActive();
                            viewer.loadSampleResult(result, true, new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    // TODO: It would be nice to select the NeuronFragment that the user clicked on to get here, but the required APIs are not curently easily accessible from the outside
                                    return null;
                                }
                            });
                        }
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getDetailsItem() {
        if (multiple) return null;
        JMenuItem detailsMenuItem = new JMenuItem("  View Details");
        detailsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_MASK));
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContentMenu.viewDetails", domainObject);
                new DomainDetailsDialog().showForDomainObject(domainObject);
            }
        });
        return detailsMenuItem;
    }

    protected JMenuItem getPermissionItem() {
        if (multiple) return null;

        JMenuItem detailsMenuItem = new JMenuItem("  Change Permissions");
        detailsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContentMenu.changePermissions", domainObject);
                new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
            }
        });

        if (!ClientDomainUtils.isOwner(domainObject)) {
            detailsMenuItem.setEnabled(false);
        }

        return detailsMenuItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = new JMenuItem("  Show in Lightbox");
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContentMenu.showInLightbox", domainObject);
                Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName);
            }
        });

        return toggleHudMI;
    }

    public JMenuItem getPasteAnnotationItem() {
        if (null == StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation()) {
            return null;
        }
        NamedAction action = new PasteAnnotationTermAction(domainObjectList);
        JMenuItem pasteItem = getNamedActionItem(action);
        return pasteItem;
    }

    protected JMenu getReportProblemItem() {
        if (multiple) return null;

        JMenu errorMenu = new JMenu("  Report A Problem With This Data");

        OntologyTerm errorOntology = StateMgr.getStateMgr().getErrorOntology();
        if (errorOntology==null) return null;

        for (final OntologyTerm term : errorOntology.getTerms()) {
            errorMenu.add(new JMenuItem(term.getName())).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.reportAProblemWithThisData", domainObject);

                    final ApplyAnnotationAction action = ApplyAnnotationAction.get();
                    final String value = (String)JOptionPane.showInputDialog(mainFrame,
                            "Please provide details:\n", term.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if (value==null || value.equals("")) return;

                    SimpleWorker simpleWorker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            action.doAnnotation(domainObject, term, value);
                            String annotationValue = "";
                            List<Annotation> annotations = DomainMgr.getDomainMgr().getModel().getAnnotations(domainObject);
                            for (Annotation annotation : annotations) {
                                if (annotation.getKeyTerm().getOntologyTermId().equals(term.getId())) {
                                    annotationValue = annotation.getName();
                                }
                            }
                            DataReporter reporter = new DataReporter((String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL), ConsoleProperties.getString("console.HelpEmail"));
                            reporter.reportData(domainObject, annotationValue);
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };

                    simpleWorker.execute();
                }
            });

        }

        return errorMenu;
    }
    protected JMenuItem getSampleCompressionTypeItem() {

        final List<Sample> samples = new ArrayList<>();
        for (DomainObject re : domainObjectList) {
            if (re instanceof Sample) {
                samples.add((Sample)re);
            }
        }

        if (samples.isEmpty()) return null;

        final String samplesText = multiple?samples.size()+" Samples":"Sample";
        JMenu submenu = new JMenu("  Change Sample Compression Strategy");

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        JMenuItem vllMenuItem = new JMenuItem("Visually Lossless (h5j)");
        vllMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("DomainObjectContentMenu.changeSampleCompressionStrategyToVisuallyLossless", domainObject);

                final String targetCompression = EntityConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
                String message = "Are you sure you want to convert "+samplesText+" to Visually Lossless (h5j) format?\n"
                        + "This will immediately delete all Lossless v3dpbd files for this Sample and result in a large decrease in disk space usage.\n"
                        + "Lossless files can be regenerated by reprocessing the Sample later.";
                int result = JOptionPane.showConfirmDialog(mainFrame, message,  "Change Sample Compression", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {

                    StringBuilder sampleIdBuf = new StringBuilder();

                    @Override
                    protected void doStuff() throws Exception {
                        for(final Sample sample : samples) {
                            model.updateProperty(sample, "compressionType", targetCompression);
                            // Target is Visually Lossless, just run the compression service
                            if (sampleIdBuf.length()>0) sampleIdBuf.append(",");
                            sampleIdBuf.append(sample.getId());
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        if (sampleIdBuf.length()==0) return;

                        Task task;
                        try {
                            HashSet<TaskParameter> taskParameters = new HashSet<>();
                            taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
                            task = ModelMgr.getModelMgr().submitJob("ConsoleSampleCompression", "Console Sample Compression", taskParameters);
                        }
                        catch (Exception e) {
                            SessionMgr.getSessionMgr().handleException(e);
                            return;
                        }

                        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                            @Override
                            public String getName() {
                                return "Compressing "+samples.size()+" samples";
                            }

                            @Override
                            protected void doStuff() throws Exception {
                                setStatus("Executing");
                                super.doStuff();
                                model.invalidate(samples);
                            }

                            @Override
                            public Callable<Void> getSuccessCallback() {
                                return new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        SessionMgr.getBrowser().getEntityOutline().refresh();
                                        return null;
                                    }
                                };
                            }
                        };

                        taskWorker.executeWithEvents();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });

        submenu.add(vllMenuItem);

        JMenuItem llMenuItem = new JMenuItem("Lossless (v3dpbd)");
        llMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("DomainObjectContentMenu.changeSampleCompressionStrategyToLossless", domainObject);

                final String targetCompression = EntityConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J;
                String message = "Are you sure you want to mark "+samplesText+" for reprocessing into Lossless (v3dpbd) format?";
                int result = JOptionPane.showConfirmDialog(mainFrame, message,  "Change Sample Compression", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for(final Sample sample : samples) {
                            model.updateProperty(sample, "compressionType", targetCompression);
                            model.updateProperty(sample, "status", EntityConstants.VALUE_MARKED);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                         JOptionPane.showMessageDialog(mainFrame, samplesText+" are marked for reprocessing to Lossless (v3dpbd) format, and will be available once the pipeline is run.",
                                 "Marked Samples", JOptionPane.INFORMATION_MESSAGE);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });

        submenu.add(llMenuItem);

        for(Sample sample : samples) {
            if (!ClientDomainUtils.hasWriteAccess(sample)) {
                submenu.setEnabled(false);
                break;
            }
        }

        return submenu;
    }

    protected JMenuItem getProcessingBlockItem() {

        final List<Sample> samples = new ArrayList<>();
        for (DomainObject re : domainObjectList) {
            if (re instanceof Sample) {
                samples.add((Sample)re);
            }
        }

        if (samples.isEmpty()) return null;

        final String samplesText = multiple?samples.size()+" Samples":"Sample";

        JMenuItem blockItem = new JMenuItem("  Purge And Block "+samplesText+" (Background Task)");
        blockItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("DomainObjectContentMenu.purgeAndBlock", domainObject);

                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(),
                        "Are you sure you want to purge " + samples.size() + " sample(s) " +
                                "by deleting all large files associated with them, and block all future processing?",
                        "Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                Task task;
                try {
                    StringBuilder sampleIdBuf = new StringBuilder();
                    for (Sample sample : samples) {
                        if (sampleIdBuf.length() > 0) sampleIdBuf.append(",");
                        sampleIdBuf.append(sample.getId());
                    }

                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
                    task = ModelMgr.getModelMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                    return;
                }

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Purging and blocking " + samples.size() + " samples";
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        setStatus("Executing");
                        super.doStuff();
                        for (Sample sample : samples) {
                            DomainMgr.getDomainMgr().getModel().invalidate(sample);
                        }
                        DomainMgr.getDomainMgr().getModel().invalidate(samples);
                    }
                };

                taskWorker.executeWithEvents();
            }
        });

        for(Sample sample : samples) {
            if (!ClientDomainUtils.hasWriteAccess(sample)) {
                blockItem.setEnabled(false);
                break;
            }
        }

        return blockItem;
    }

    /** Allows users to rerun their own samples. */
    protected JMenuItem addRerunSamplesAction() {

        JMenuItem rtnVal = null;
        NamedAction rerunAction = RerunSamplesAction.createAction(domainObjectList);
        if (rerunAction != null) {
            rtnVal = getNamedActionItem(rerunAction);
            add(rtnVal);
        }
        return rtnVal;
    }

    protected JMenuItem getMarkForReprocessingItem() {

        final List<Sample> samples = new ArrayList<>();
        for (DomainObject re : domainObjectList) {
            if (re instanceof Sample) {
                samples.add((Sample)re);
            }
        }

        if (samples.isEmpty()) return null;

        final String samplesText = multiple?samples.size()+" Samples":"Sample";

        JMenuItem markItem = new JMenuItem("  Mark "+samplesText+" for Reprocessing");
        markItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("DomainObjectContentMenu.markForReprocessing", domainObject);

                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want these "+samples.size()+" sample(s) to be reprocessed "
                        + "during the next scheduled refresh?",  "Mark for Reprocessing", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        for(final Sample sample : samples) {
                            DomainMgr.getDomainMgr().getModel().updateProperty(sample, "status", DomainConstants.VALUE_MARKED);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
            }
        });

        for(Sample sample : samples) {
            if (!ClientDomainUtils.hasWriteAccess(sample)) {
                markItem.setEnabled(false);
                break;
            }
        }

        return markItem;
    }

    protected JMenuItem getAddToFolderItem() {
        AddToFolderAction action = AddToFolderAction.get();
        action.setDomainObjects(domainObjectList);
        JMenuItem item = action.getPopupPresenter();
        item.setText("  " + item.getText());
        return item;
    }

    protected JMenuItem getRemoveFromFolderItem() {

        NamedAction action;
        if (contextObject instanceof TreeNode) {
            action = new RemoveItemsFromFolderAction((TreeNode)contextObject, domainObjectList);
        }
        else {
            return null;
        }

        JMenuItem deleteItem = getNamedActionItem(action);

        if (!ClientDomainUtils.hasWriteAccess(contextObject)) {
            deleteItem.setEnabled(false);
        }

        return deleteItem;
    }

    protected JMenuItem getOpenInFinderItem() {
    	if (multiple) return null;
        HasFiles fileProvider = getSingleResult();
        if (fileProvider==null) return null;
        String path = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (path==null) return null;
        if (!OpenInFinderAction.isSupported()) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
    	if (multiple) return null;
        HasFiles fileProvider = getSingleResult();
        if (fileProvider==null) return null;
        String path = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (path==null) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

    protected JMenuItem getNeuronAnnotatorItem() {
        if (multiple) return null;
        if (domainObject instanceof NeuronFragment) {
            return getNamedActionItem(new OpenInNeuronAnnotatorAction((NeuronFragment)domainObject));
        }
        return null;
    }

    protected JMenuItem getVaa3dTriViewItem() {
    	if (multiple) return null;
    	HasFiles fileProvider = getSingleResult();
        if (fileProvider==null) return null;
        String path = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, null));
    }

    protected JMenuItem getVaa3d3dViewItem() {
    	if (multiple) return null;
        HasFiles fileProvider = getSingleResult();
        if (fileProvider==null) return null;
        String path = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_3D));
    }

    protected JMenuItem getFijiViewerItem() {
    	if (multiple) return null;
        HasFiles fileProvider = getSingleResult();
        if (fileProvider==null) return null;
        String path = DomainUtils.getDefault3dImageFilePath(fileProvider);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_FIJI, path, null));
    }

    protected JMenuItem getDownloadItem() {

        String label = domainObjectList.size() > 1 ? "Download " + domainObjectList.size() + " Items..." : "Download...";

        JMenuItem toggleHudMI = new JMenuItem("  "+label);
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContextMenu.download", domainObject);
                DownloadDialog dialog = new DownloadDialog();
                dialog.showDialog(domainObjectList, resultDescriptor);
            }
        });

        return toggleHudMI;
    }

    private long startMergeTask() throws Exception {
        Long parentId = null;
        List<NeuronFragment> fragments = new ArrayList<>();
        for (DomainObject domainObj : domainObjectList) {
            NeuronFragment fragment = (NeuronFragment)domainObj;
            Long resultId = fragment.getSeparationId();
            if (parentId == null) {
                parentId = resultId;
            } else if (resultId == null || !parentId.equals(resultId)) {
                throw new IllegalStateException(
                        "The selected neuron fragments are not part of the same neuron separation result: parentId="
                                + parentId + " resultId=" + resultId);
            }
            fragments.add(fragment);
        }

        Collections.sort(fragments, new Comparator<NeuronFragment>() {
            @Override
            public int compare(NeuronFragment o1, NeuronFragment o2) {
                Integer o1n = o1.getNumber();
                Integer o2n = o2.getNumber();
                return o1n.compareTo(o2n);
            }
        });

        HashSet<String> fragmentIds = new LinkedHashSet<>();
        for (NeuronFragment fragment : fragments) {
            fragmentIds.add(fragment.getId().toString());
        }

        HashSet<TaskParameter> taskParameters = new HashSet<>();
        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_separationEntityId, parentId.toString(), null));
        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, Task.csvStringFromCollection(fragmentIds), null));
        Task mergeTask = ModelMgr.getModelMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
        return mergeTask.getObjectId();
    }

    protected JMenuItem getMergeItem() {
        try {
            // If multiple items are not selected then leave
            if (!multiple) {
                return null;
            }

            HashSet<Long> fragmentIds = new HashSet<>();
            for (DomainObject domainObject : domainObjectList) {
                if (!(domainObject instanceof NeuronFragment)) {
                    continue;
                }
                fragmentIds.add(domainObject.getId());
            }
            if (fragmentIds.size()<2) {
                return null;
            }

            JMenuItem mergeItem = new JMenuItem("  Merge " + fragmentIds.size() + " Selected Neurons");
            NeuronFragment fragment = (NeuronFragment) domainObjectList.get(0);
            Reference sampleRef = fragment.getSample();
            final Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);

            mergeItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    ActivityLogHelper.logUserAction("DomainObjectContextMenu.mergeSelectedNeurons");
                    try {
                        BackgroundWorker executeWorker = new TaskMonitoringWorker() {

                            @Override
                            public String getName() {
                                return "Merge Neuron Fragments ";
                            }

                            @Override
                            protected void doStuff() throws Exception {

                                setStatus("Submitting task");
                                long taskId = startMergeTask();
                                setTaskId(taskId);
                                setStatus("Grid execution");

                                // Wait until task is finished
                                super.doStuff();

                                if (isCancelled()) throw new CancellationException();
                                setStatus("Done merging");
                            }

                            @Override
                            public Callable<Void> getSuccessCallback() {
                                return new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        SimpleWorker worker = new SimpleWorker() {

                                            @Override
                                            protected void doStuff() throws Exception {
                                                try {
                                                    final DomainModel model = DomainMgr.getDomainMgr().getModel();
                                                    model.invalidate(sample);
                                                    SessionMgr.getBrowser().getEntityOutline().refresh();
                                                }  catch (Exception e) {
                                                    SessionMgr.getSessionMgr().handleException(e);
                                                }
                                            }

                                            @Override
                                            protected void hadSuccess() {
                                            }

                                            @Override
                                            protected void hadError(Throwable error) {
                                                SessionMgr.getSessionMgr().handleException(error);
                                            }
                                        };

                                        worker.execute();
                                        return null;
                                    }
                                };
                            }
                        };

                        executeWorker.executeWithEvents();
                    }
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });
            mergeItem.setEnabled(multiple);
            return mergeItem;
        } catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return null;
        }
    }

//    protected JMenuItem getImportItem() {
//        if (multiple) return null;
//        
//        String entityTypeName = rootedEntity.getEntity().getEntityTypeName();
//        if (EntityConstants.TYPE_FOLDER.equals(entityTypeName) || EntityConstants.TYPE_SAMPLE.equals(entityTypeName)) {
//            JMenuItem newAttachmentItem = new JMenuItem("  Import File(s) Here");
//            newAttachmentItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    try {
//                        browser.getImportDialog().showDialog(rootedEntity);
//                    } catch (Exception ex) {
//                        SessionMgr.getSessionMgr().handleException(ex);
//                    }
//                }
//            });
//
//            return newAttachmentItem;
//        }
//        return null;
//    }

    private JMenuItem getSpecialAnnotationSession() {
        if (this.multiple) return null;
        if (!SessionMgr.getSubjectKey().equals("user:simpsonj") && !SessionMgr.getSubjectKey().equals("group:simpsonlab")) {
                return null;
        }
        JMenuItem specialAnnotationSession = new JMenuItem("  Special Annotation");
        specialAnnotationSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("DomainObjectContextMenu.specialAnnotation");
                if (null == StateMgr.getStateMgr().getCurrentOntologyId()) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Please select an ontology in the ontology window.", "Null Ontology Warning",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    SpecialAnnotationChooserDialog dialog = SpecialAnnotationChooserDialog.getDialog();
                    if (!dialog.isVisible()) {
                        dialog.setVisible(true);
                    } else {
                        dialog.transferFocus();
                    }
                }
            }
        });

        return specialAnnotationSession;
    }

	private List<JMenuItem> getWrapObjectItems() {
		if (multiple) {
			return Collections.EMPTY_LIST;
		}
		return new WrapperCreatorItemFactory().makeWrapperCreatorItems(domainObject);
	}

	private List<JMenuItem> getAppendObjectItems() {
		return new WrapperCreatorItemFactory().makeObjectAppenderItems(domainObjectList);
	}

	public class DomainObjectAcceptorActionListener implements ActionListener {

		private DomainObjectAcceptor domainObjectAcceptor;

		public DomainObjectAcceptorActionListener(DomainObjectAcceptor domainObjectAcceptor) {
			this.domainObjectAcceptor = domainObjectAcceptor;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Pickup the sought value.
				domainObjectAcceptor.acceptDomainObject(domainObject);
			} catch (Exception ex) {
				ModelMgr.getModelMgr().handleException(ex);
			}

		}
	}

    private HasFiles getSingleResult() {
        HasFiles result = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            result = SampleUtils.getResult(sample, resultDescriptor);
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result;
    }
}
