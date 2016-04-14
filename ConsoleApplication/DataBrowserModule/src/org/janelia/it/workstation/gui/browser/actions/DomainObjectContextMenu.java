package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.components.*;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.SpecialAnnotationChooserDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;

/**
 * Context pop up menu for entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

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
    }
    
    public void runDefaultAction() {
        if (DomainViewerTopComponent.isSupported(domainObject)) {
            DomainViewerTopComponent viewer = ViewerUtils.getViewer(DomainViewerManager.getInstance(), "editor2");
            if (viewer == null || !DomainUtils.equals(viewer.getCurrent(), domainObject)) {
                viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(domainObject, true);
            }
        }
        else if (DomainExplorerTopComponent.isSupported(domainObject)) {
            // TODO: should select by path to ensure we get the right one, but for that to happen the domain object needs to know its path
            DomainExplorerTopComponent.getInstance().expandNodeById(contextObject.getId());
            DomainExplorerTopComponent.getInstance().selectNodeById(domainObject.getId());
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
        add(getOpenSampleInNewEditorItem());
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
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getFijiViewerItem());
        add(getDownloadItem());
        
        setNextAddRequiresSeparator(true);
        add(getReportProblemItem());
        add(getMarkForReprocessingItem());
        add(getSampleCompressionTypeItem());
        add(getProcessingBlockItem());
//        
//        setNextAddRequiresSeparator(true);
//        add(getMergeItem());
//        add(getImportItem());
//
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
//        for ( JComponent item: getOpenForContextItems() ) {
//            add(item);
//        }
//        add(getWrapEntityItem());
//
        
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
        
        JMenuItem openItem = new JMenuItem("  Open "+domainObject.getType()+" In New Viewer");
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(domainObject, true);
            }
        });
        return openItem;
    }

    protected JMenuItem getOpenSampleInNewEditorItem() {
        if (multiple) return null;
        if (!(domainObject instanceof NeuronFragment)) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Open Sample In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                final NeuronFragment neuronFragment = (NeuronFragment)domainObject;
                
                SimpleWorker worker = new SimpleWorker() {
                    Sample sample;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                    }

                    @Override
                    protected void hadSuccess() {
                        viewer.requestActive();
                        viewer.loadDomainObject(sample, true);
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
    
    protected JMenuItem getOpenSeparationInNewEditorItem() {
        if (multiple) return null;
        if (!(domainObject instanceof NeuronFragment)) return null;
        
        JMenuItem copyMenuItem = new JMenuItem("  Open Neuron Separation In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                final NeuronFragment neuronFragment = (NeuronFragment)domainObject;
                

                SimpleWorker worker = new SimpleWorker() {
                    NeuronSeparation separation;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                        separation = DomainModelViewUtils.getNeuronSeparation(sample, neuronFragment);
                    }

                    @Override
                    protected void hadSuccess() {
                        viewer.requestActive();
                        viewer.loadSampleResult(separation, true, null);
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
                vllMenuItem.setEnabled(false);
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

                int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), 
                        "Are you sure you want to purge "+samples.size()+" sample(s) "+
                        "by deleting all large files associated with them, and block all future processing?",  
                		"Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);
                
                if (result != 0) return;

                Task task;
                try {
                    StringBuilder sampleIdBuf = new StringBuilder();
                    for(Sample sample : samples) {
                        if (sampleIdBuf.length()>0) sampleIdBuf.append(",");
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
                        return "Purging and blocking "+samples.size()+" samples";
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        setStatus("Executing");
                        super.doStuff();
                        for(Sample sample : samples) {
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
        AddItemsToFolderAction action = new AddItemsToFolderAction(domainObjectList);
        JMenuItem item = action.getPopupPresenter();
        item.setText("  "+item.getText());
        return item;
    }
    
    protected JMenuItem getRemoveFromFolderItem() {
        
        NamedAction action = null;
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

        String label = domainObjectList.size() > 1 ? "Download \"" + domainObjectList.size() + "\" Items..." : "Download...";
        
        JMenuItem toggleHudMI = new JMenuItem("  "+label);
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                DownloadDialog dialog = new DownloadDialog();
                dialog.showDialog(domainObjectList, resultDescriptor);
            }
        });

        return toggleHudMI;
    }
  
//    protected JMenuItem getMergeItem() {
//
//        // If multiple items are not selected then leave
//        if (!multiple) {
//            return null;
//        }
//
//        HashSet<Long> parentIds = new HashSet<>();
//        for (RootedEntity rootedEntity : rootedEntityList) {
//            // Add all parent ids to a collection
//            if (null != rootedEntity.getEntityData().getParentEntity()
//                    && EntityConstants.TYPE_NEURON_FRAGMENT.equals(rootedEntity.getEntity().getEntityTypeName())) {
//                parentIds.add(rootedEntity.getEntityData().getParentEntity().getId());
//            }
//            // if one of the selected entities has no parent or isn't owner by
//            // the user then leave; cannot merge
//            else {
//                return null;
//            }
//        }
//        // Anything but one parent id for selected entities should not allow
//        // merge
//        if (parentIds.size() != 1) {
//            return null;
//        }
//
//        JMenuItem mergeItem = new JMenuItem("  Merge " + rootedEntityList.size() + " Selected Neurons");
//
//        mergeItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                SimpleWorker mergeTask = new SimpleWorker() {
//                    @Override
//                    protected void doStuff() throws Exception {
//                        setProgress(1);
//                        Long parentId = null;
//                        List<Entity> fragments = new ArrayList<>();
//                        for (RootedEntity entity : rootedEntityList) {
//                            Long resultId = ModelMgr
//                                    .getModelMgr()
//                                    .getAncestorWithType(entity.getEntity(),
//                                            EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).getId();
//                            if (parentId == null) {
//                                parentId = resultId;
//                            } else if (resultId == null || !parentId.equals(resultId)) {
//                                throw new IllegalStateException(
//                                        "The selected neuron fragments are not part of the same neuron separation result: parentId="
//                                                + parentId + " resultId=" + resultId);
//                            }
//                            fragments.add(entity.getEntityData().getChildEntity());
//                        }
//
//                        Collections.sort(fragments, new Comparator<Entity>() {
//                            @Override
//                            public int compare(Entity o1, Entity o2) {
//                                Integer o1n = Integer.parseInt(o1
//                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
//                                Integer o2n = Integer.parseInt(o2
//                                        .getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER));
//                                return o1n.compareTo(o2n);
//                            }
//                        });
//
//                        HashSet<String> fragmentIds = new LinkedHashSet<>();
//                        for (Entity fragment : fragments) {
//                            fragmentIds.add(fragment.getId().toString());
//                        }
//
//                        // This should never happen
//                        if (null == parentId) {
//                            return;
//                        }
//                        
//                        HashSet<TaskParameter> taskParameters = new HashSet<>();
//                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_separationEntityId, parentId.toString(), null));
//                        taskParameters.add(new TaskParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, Task.csvStringFromCollection(fragmentIds), null));
//                        ModelMgr.getModelMgr().submitJob("NeuronMerge", "Neuron Merge Task", taskParameters);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//
//                };
//
//                mergeTask.execute();
//            }
//        });
//
//        mergeItem.setEnabled(multiple);
//        return mergeItem;
//    }
//
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
//
    private JMenuItem getSpecialAnnotationSession() {
        if (this.multiple) return null;
        if (!SessionMgr.getSubjectKey().equals("user:simpsonj") && !SessionMgr.getSubjectKey().equals("group:simpsonlab")) {
                return null;
        }
        JMenuItem specialAnnotationSession = new JMenuItem("  Special Annotation");
        specialAnnotationSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

//  /** Makes the item for showing the entity in its own viewer iff the entity type is correct. */
//  public Collection<JComponent> getOpenForContextItems() {
//      TreeMap<Integer,JComponent> orderedMap = new TreeMap<>();
//      if (rootedEntity != null && rootedEntity.getEntityData() != null) {
//          final Entity entity = rootedEntity.getEntity();
//          if (entity!=null) {
//
//              final ServiceAcceptorHelper helper = new ServiceAcceptorHelper();
//              Collection<EntityAcceptor> entityAcceptors
//                      = helper.findHandler(
//                              entity, 
//                              EntityAcceptor.class,
//                              EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH
//                      );
//              boolean lastItemWasSeparator = false;
//              int expectedCount = 0;
//              List<JComponent> actionItemList = new ArrayList<>();
//              for ( EntityAcceptor entityAcceptor: entityAcceptors ) {                    
//                  final Integer order = entityAcceptor.getOrder();
//                  if (entityAcceptor.isPrecededBySeparator() && (! lastItemWasSeparator)) {
//                      orderedMap.put(order - 1, new JSeparator());
//                      expectedCount ++;
//                  }
//                  JMenuItem item = new JMenuItem(entityAcceptor.getActionLabel());
//                  item.addActionListener( new EntityAcceptorActionListener( entityAcceptor ) );
//                  orderedMap.put(order, item);
//                  actionItemList.add( item ); // Bail alternative if ordering fails.
//                  expectedCount ++;
//                  if (entityAcceptor.isSucceededBySeparator()) {
//                      orderedMap.put(order + 1, new JSeparator());
//                      expectedCount ++;
//                      lastItemWasSeparator = true;
//                  }
//                  else {
//                      lastItemWasSeparator = false;
//                  }
//              }
//              
//              // This is the bail strategy for order key clashes.
//              if ( orderedMap.size() < expectedCount) {
//                  log.warn("With menu items and separators, expected {} but added {} open-for-context items." +
//                          "  This indicates an order key clash.  Please check the getOrder methods of all impls." +
//                          "  Returning an unordered version of item list.",
//                          expectedCount, orderedMap.size());
//                  return actionItemList;
//              }
//          }
//      }
//      return orderedMap.values();
//  }
//    public class EntityAcceptorActionListener implements ActionListener {
//
//        private EntityAcceptor entityAcceptor;
//
//        public EntityAcceptorActionListener(EntityAcceptor entityAcceptor) {
//            this.entityAcceptor = entityAcceptor;
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            try {
//                Entity entity = rootedEntity.getEntity();
//                // Pickup the sought value.
//                entityAcceptor.acceptEntity(entity);
//            } catch (Exception ex) {
//                ModelMgr.getModelMgr().handleException(ex);
//            }
//
//        }
//    }

    private HasFiles getSingleResult() {
        HasFiles result = null;
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            result = DomainModelViewUtils.getResult(sample, resultDescriptor);
        }
        else if (domainObject instanceof HasFiles) {
            result = (HasFiles)domainObject;
        }
        return result;
    }
}
