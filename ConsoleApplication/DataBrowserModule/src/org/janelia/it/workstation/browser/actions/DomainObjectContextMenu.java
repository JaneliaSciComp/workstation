package org.janelia.it.workstation.browser.actions;

import static org.janelia.it.workstation.browser.util.Utils.SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.OrderStatus;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.Constants;
import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.components.DomainViewerManager;
import org.janelia.it.workstation.browser.components.DomainViewerTopComponent;
import org.janelia.it.workstation.browser.components.SampleResultViewerManager;
import org.janelia.it.workstation.browser.components.SampleResultViewerTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.browser.gui.dialogs.SecondaryDataRemovalDialog;
import org.janelia.it.workstation.browser.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.gui.listview.WrapperCreatorItemFactory;
import org.janelia.it.workstation.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.browser.nb_action.SetPublishingNameAction;
import org.janelia.it.workstation.browser.tools.ToolMgr;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectContextMenu.class);
    public static final String WHOLE_AA_REMOVAL_MSG = "Remove/preclude anatomical area of sample";
    public static final String STITCHED_IMG_REMOVAL_MSG = "Remove/preclude Stitched Image";
    public static final String NEURON_SEP_REMOVAL_MSG = "Remove/preclude Neuron Separation(s)";

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

        // TODO: move these options to a separate "Confocal" module 
        // along with all other Sample-specific functionality 
        setNextAddRequiresSeparator(true);
        add(getSampleCompressionTypeItem());
        add(getReportProblemItem());
        add(getRerunSamplesAction());
        add(getPartialSecondaryDataDeletiontItem());
        add(getProcessingBlockItem());
        add(getApplyPublishingNameItem());
        add(getMergeItem());
        
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());

        if (domainObject!=null) {
            for (JComponent item : this.getOpenObjectItems()) {
                add(item);
            }
            for (JMenuItem item : this.getWrapObjectItems()) {
                add(item);
            }
            for (JMenuItem item : this.getAppendObjectItems()) {
                add(item);
            }
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
            final DomainObject objectToLoad = DomainViewerManager.getObjectToLoad(domainObject);
            if (objectToLoad==null) return null;
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
                            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                        }
                        else if (result==null) {
                            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
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
                        ConsoleApp.handleException(error);
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
        Action action = new PasteAnnotationTermAction(domainObjectList);
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
                    if (StringUtils.isEmpty(value)) return;

                    SimpleWorker simpleWorker = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            action.addAnnotation(domainObject, term, value);
                            String annotationValue = "";
                            List<Annotation> annotations = DomainMgr.getDomainMgr().getModel().getAnnotations(domainObject);
                            for (Annotation annotation : annotations) {
                                if (annotation.getKeyTerm().getOntologyTermId().equals(term.getId())) {
                                    annotationValue = annotation.getName();
                                }
                            }
                            DataReporter reporter = new DataReporter((String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_EMAIL), ConsoleProperties.getString("console.HelpEmail"));
                            reporter.reportData(domainObject, annotationValue);
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            ConsoleApp.handleException(error);
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

                final String targetCompression = DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS;
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
                            task = StateMgr.getStateMgr().submitJob("ConsoleSampleCompression", "Console Sample Compression", taskParameters);
                        }
                        catch (Exception e) {
                            ConsoleApp.handleException(e);
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
                                    	DomainExplorerTopComponent.getInstance().refresh();
                                        return null;
                                    }
                                };
                            }
                        };

                        taskWorker.executeWithEvents();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
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

                final String targetCompression = DomainConstants.VALUE_COMPRESSION_LOSSLESS_AND_H5J;
                String message = "Are you sure you want to reprocess "+samplesText+" into Lossless (v3dpbd) format?";
                int result = JOptionPane.showConfirmDialog(mainFrame, message,  "Change Sample Compression", JOptionPane.OK_CANCEL_OPTION);

                if (result != 0) return;

                SimpleWorker worker = new SimpleWorker() {

                    private static final String TASK_LABEL = "GSPS_CompleteSamplePipeline";
                    
                    @Override
                    protected void doStuff() throws Exception {

                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat format = new SimpleDateFormat("yyyyddMMhh");
                        Sample sampleInfo = samples.get(0);
                        String orderNo = "Workstation_" + sampleInfo.getOwnerName() + "_" +
                                sampleInfo.getDataSet() + "_" +format.format(Calendar.getInstance().getTime());

                        List<Long> sampleIds = new ArrayList<>();
                        for(Sample sample : samples) {

                            String status = sample.getStatus();
                            if (PipelineStatus.Scheduled.toString().equals(status)  ||
                                        PipelineStatus.Processing.toString().equals(status)) {
                                log.info("Bypassing sample " + sample.getName() + " because it is already marked {}.", status);
                                continue;
                            }
                            
                            Set<TaskParameter> taskParameters = new HashSet<>();
                            taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
                            taskParameters.add(new TaskParameter("order no", orderNo, null));
                            taskParameters.add(new TaskParameter("reuse summary", "false", null));
                            taskParameters.add(new TaskParameter("reuse processing", "false", null));
                            taskParameters.add(new TaskParameter("reuse post", "false", null));
                            taskParameters.add(new TaskParameter("reuse alignment", "false", null));
                            Task task = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), 
                                    new ArrayList<Event>(), taskParameters, TASK_LABEL, TASK_LABEL);
                            task = StateMgr.getStateMgr().saveOrUpdateTask(task);
                            
                            StatusTransition transition = new StatusTransition();
                            transition.setOrderNo(orderNo);
                            transition.setSource(PipelineStatus.valueOf(status));
                            transition.setProcess("Front End Processing");
                            transition.setSampleId(sample.getId());
                            transition.setTarget(PipelineStatus.Scheduled);
                            model.addPipelineStatusTransition(transition);
                            model.updateProperty(sample, "compressionType", targetCompression);
                            model.updateProperty(sample, "status", PipelineStatus.Scheduled.toString());
                            StateMgr.getStateMgr().dispatchJob(TASK_LABEL, task);
                            sampleIds.add(sample.getId());
                        }

                        // add an intake order to track all these Samples
                        if (samples.size()>0) {
                            // check if there is an existing order no
                            IntakeOrder order = DomainMgr.getDomainMgr().getModel().getIntakeOrder(orderNo);
                            if (order==null) {
                                order = new IntakeOrder();
                                order.setOrderNo(orderNo);
                                order.setOwner(sampleInfo.getOwnerKey());
                                order.setStartDate(c.getTime());
                                order.setStatus(OrderStatus.Intake);
                                order.setSampleIds(sampleIds);
                            } 
                            else {
                                List<Long> currIds = order.getSampleIds();
                                currIds.addAll(sampleIds);
                                order.setSampleIds(currIds);
                            }
                            model.putOrUpdateIntakeOrder(order);
                        }

                    }

                    @Override
                    protected void hadSuccess() {
                         JOptionPane.showMessageDialog(mainFrame, samplesText+" are marked for reprocessing to Lossless (v3dpbd) format, and will be available once the pipeline is run.",
                                 "Marked Samples", JOptionPane.INFORMATION_MESSAGE);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
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

                int result = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(),
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
                    task = StateMgr.getStateMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
                }
                catch (Exception e) {
                    ConsoleApp.handleException(e);
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

    protected JMenuItem getApplyPublishingNameItem() {
        
        List<Sample> samples = new ArrayList<>();
        for(DomainObject domainObject : domainObjectList) {
            if (domainObject instanceof Sample) {
                samples.add((Sample)domainObject);
            }
        }
        
        if (samples.size()!=domainObjectList.size()) return null;
        
        JMenuItem menuItem = getNamedActionItem(new SetPublishingNameAction(samples));
        
        for(Sample sample : samples) {
            if (!ClientDomainUtils.hasWriteAccess(sample)) {
                menuItem.setEnabled(false);
                break;
            }
        }
        
        return menuItem;
    }

    /** Allows users to rerun their own samples. */
    protected JMenuItem getRerunSamplesAction() {
        JMenuItem rtnVal = null;
        Action rerunAction = RerunSamplesAction.createAction(domainObjectList);
        if (rerunAction != null) {
            rtnVal = getNamedActionItem(rerunAction);
        }
        return rtnVal;
    }
    
    protected JMenuItem getPartialSecondaryDataDeletiontItem() {

        List<Sample> samples = new ArrayList<>();
        for(DomainObject domainObject : domainObjectList) {
            if (domainObject instanceof Sample) {
                samples.add((Sample)domainObject);
            }
        }
        
        if (samples.size()!=domainObjectList.size()) return null;
        if (samples.size()!=1) return null;
        
        JMenu secondaryDeletionMenu = new JMenu("  Remove Secondary Data");
        
        JMenuItem itm = getPartialSecondaryDataDeletionItem(samples);
        if (itm != null) {
            secondaryDeletionMenu.add(itm);
        }
        
        itm = getStitchedImageDeletionItem(samples);
        if (itm != null) {
            secondaryDeletionMenu.add(itm);
        }
        
        /* Removing this feature until such time as this level of flexibility has user demand. */
        if (SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI) {
            itm = getNeuronSeparationDeletionItem();
            if (itm != null) {
                secondaryDeletionMenu.add(itm);
            }
        }
        
        if (secondaryDeletionMenu.getItemCount() > 0) {
            for(Sample sample : samples) {
                if (!ClientDomainUtils.hasWriteAccess(sample)) {
                    secondaryDeletionMenu.setEnabled(false);
                    break;
                }
            }
            return secondaryDeletionMenu;
        }
        return null;
    }
    
    protected JMenuItem getPartialSecondaryDataDeletionItem(List<Sample> samples) {
        JMenuItem rtnVal = null;
        if (samples.size() == 1) {
            final Sample sample = samples.get(0);
            rtnVal = new JMenuItem("  " + WHOLE_AA_REMOVAL_MSG);
            rtnVal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                            FrameworkImplProvider.getMainFrame(),
                            sample,
                            WHOLE_AA_REMOVAL_MSG,
                            Constants.TRIM_DEPTH_WHOLE_AREA_VALUE

                    );
                    dialog.setVisible(true);
                }
            });
        }
        return rtnVal;
    }

    protected JMenuItem getStitchedImageDeletionItem(List<Sample> samples) {
        JMenuItem rtnVal = null;
        if (samples.size() == 1) {
            final Sample sample = samples.get(0);
            rtnVal = new JMenuItem("  " + STITCHED_IMG_REMOVAL_MSG);
            rtnVal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                            FrameworkImplProvider.getMainFrame(),
                            sample,
                            STITCHED_IMG_REMOVAL_MSG,
                            Constants.TRIM_DEPTH_AREA_IMAGE_VALUE
                    );
                    dialog.setVisible(true);
                }
            });
        }
        return rtnVal;
    }

    protected JMenuItem getNeuronSeparationDeletionItem() {
        JMenuItem rtnVal = null;
        if (domainObjectList.size() == 1  &&  domainObjectList.get(0) instanceof Sample) {
            final Sample sample = (Sample)domainObjectList.get(0);
            rtnVal = new JMenuItem("  " + NEURON_SEP_REMOVAL_MSG);
            rtnVal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                            FrameworkImplProvider.getMainFrame(),
                            sample,
                            NEURON_SEP_REMOVAL_MSG,
                            Constants.TRIM_DEPTH_NEURON_SEPARATION_VALUE
                    );
                    dialog.setVisible(true);
                }
            });
        }
        return rtnVal;
    }

    protected JMenuItem getAddToFolderItem() {
        AddToFolderAction action = AddToFolderAction.get();
        action.setDomainObjects(domainObjectList);
        JMenuItem item = action.getPopupPresenter();
        item.setText("  " + item.getText());
        return item;
    }

    protected JMenuItem getRemoveFromFolderItem() {

        Action action;
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
        Task mergeTask = StateMgr.getStateMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
        return mergeTask.getObjectId();
    }

    protected JMenuItem getMergeItem() {
        try {
            // If multiple items are not selected then leave
            if (!multiple) {
                return null;
            }

            HashSet<NeuronFragment> fragments = new LinkedHashSet<>();
            for (DomainObject domainObject : domainObjectList) {
                if (!(domainObject instanceof NeuronFragment)) {
                    continue;
                }
                fragments.add((NeuronFragment)domainObject);
            }
            if (fragments.size()<2) {
                return null;
            }

            JMenuItem mergeItem = new JMenuItem("  Merge " + fragments.size() + " Selected Neurons");
            NeuronFragment fragment = (NeuronFragment) fragments.iterator().next();
            Reference sampleRef = fragment.getSample();
            final Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);

            mergeItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    ActivityLogHelper.logUserAction("DomainObjectContextMenu.mergeSelectedNeurons");
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
                                	DomainMgr.getDomainMgr().getModel().invalidate(sample);
                                    return null;
                                }
                            };
                        }
                    };

                    executeWorker.executeWithEvents();
                }
            });
            mergeItem.setEnabled(multiple);
            return mergeItem;
        } catch (Exception e) {
            ConsoleApp.handleException(e);
            return null;
        }
    }

    private JMenuItem getSpecialAnnotationSession() {
        if (this.multiple) return null;
        if (!AccessManager.getSubjectKey().equals("user:simpsonj") && !AccessManager.getSubjectKey().equals("group:simpsonlab")) {
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
        
    private Collection<JComponent> getOpenObjectItems() {
        if (multiple) {
            return Collections.emptyList();
        }
        return ServiceAcceptorActionHelper.getOpenForContextItems(domainObject);
    }
    
    private List<JMenuItem> getWrapObjectItems() {
        if (multiple) {
            return Collections.emptyList();
        }
        return new WrapperCreatorItemFactory().makeWrapperCreatorItems(domainObject);
    }

    private List<JMenuItem> getAppendObjectItems() {
        return new WrapperCreatorItemFactory().makeObjectAppenderItems(domainObjectList);
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
