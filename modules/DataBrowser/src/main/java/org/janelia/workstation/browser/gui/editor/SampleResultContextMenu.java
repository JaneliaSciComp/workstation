package org.janelia.workstation.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFileGroups;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.OpenInFinderAction;
import org.janelia.workstation.browser.actions.OpenInNeuronAnnotatorActionListener;
import org.janelia.workstation.browser.actions.OpenInToolAction;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.browser.gui.components.SampleResultViewerManager;
import org.janelia.workstation.browser.gui.components.SampleResultViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.listview.WrapperCreatorItemFactory;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.actions.PopupLabelAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;

/**
 * Right-click context menu for sample results presented in the Sample Editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResultContextMenu extends PopupContextMenu {

    private ViewerContext<DomainObject, Reference> viewerContext;
    private final PipelineResult result;

    public SampleResultContextMenu(PipelineResult result) {
        this.result = result;
    }
    
    public void addMenuItems() {
        
        if (result==null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        add(new PopupLabelAction(Collections.singletonList(result)));
        add((new CopyToClipboardAction("Name", result.getName())));
        add((new CopyToClipboardAction("GUID", result.getId().toString())));

        setNextAddRequiresSeparator(true);
        add(getOpenResultsInNewViewerItem());
        add(getOpenSeparationInNewViewerItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getVvdViewerItem());
        add(getFijiViewerItem());
        add(getDownloadItem());
        
        setNextAddRequiresSeparator(true);
        add(getVerificationMovieItem());
        
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());

//        addSeparator();
//        addSeparator();
//
//        for (Component currentContextMenuItem : DomainObjectAcceptorHelper.getCurrentContextMenuItems()) {
//            add(currentContextMenuItem);
//        }
    }

    public void runDefaultAction() {
        if (result.getLatestSeparationResult()!=null || result instanceof HasFileGroups) {
            SampleResultViewerTopComponent viewer = ViewerUtils.getViewer(SampleResultViewerManager.getInstance(), "editor3");
            if (viewer == null || !SampleUtils.equals(viewer.getCurrent(), result)) {
                viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                viewer.requestActive();
                viewer.loadSampleResult(result, true, null);
            }
        }
    }

    protected JMenuItem getOpenResultsInNewViewerItem() {
        if (!(result instanceof HasFileGroups)) return null;
        JMenuItem copyMenuItem = new JMenuItem("Open Results In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("SampleResultContextMenu.openResultsInNewViewer");
                SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                viewer.requestActive();
                viewer.loadSampleResult(result, true, null);
            }
        });
        return copyMenuItem;
    }

    protected JMenuItem getOpenSeparationInNewViewerItem() {
        if (result.getLatestSeparationResult()==null) return null;
        JMenuItem copyMenuItem = new JMenuItem("Open Neuron Separation In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("SampleResultContextMenu.openSeparationInNewViewerItem");
                SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
                viewer.requestActive(); 
                viewer.loadSampleResult(result, true, null);
            }
        });
        return copyMenuItem;
    }
    
    protected JMenuItem getOpenInFinderItem() {
        if (!OpenInFinderAction.isSupported()) return null;
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

//    private Collection<JComponent> getOpenObjectItems() {
//        // TODO: pass the actual file type the user clicked on
//        SampleImage sampleImage = new SampleImage(result, FileType.ReferenceMip);
//        return DomainObjectAcceptorHelper.getOpenForContextItems(sampleImage);
//    }
    
    protected List<JMenuItem> getWrapObjectItems() {
        return new WrapperCreatorItemFactory().makeWrapperCreatorItems(result);
    }
    
    protected List<JMenuItem> getAppendObjectItems() {
        return new WrapperCreatorItemFactory().makePipelineResultAppenderItems(result);
    }

    protected JMenuItem getNeuronAnnotatorItem() {
        final NeuronSeparation separation = result.getLatestSeparationResult();
        if (separation==null) {
            return getNamedActionItem("Open in Neuron Annotator", new OpenInNeuronAnnotatorActionListener(result));
        }
        return getNamedActionItem("Open in Neuron Annotator", new OpenInNeuronAnnotatorActionListener(separation));
    }

    protected JMenuItem getVaa3dTriViewItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, null));
    }

    protected JMenuItem getVaa3d3dViewItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_VAA3D_3D));
    }

    protected JMenuItem getFijiViewerItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_FIJI, path, null));
    }

    protected JMenuItem getVvdViewerItem() {
        String path = DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VVD, path, null));
    }
    protected JMenuItem getDownloadItem() {

        String path = DomainUtils.getDefault3dImageFilePath(result);
        ObjectiveSample objectiveSample = result.getParentRun().getParent();
        final Sample sample = objectiveSample.getParent();
        final ArtifactDescriptor descriptor = new ResultArtifactDescriptor(result);
        
        JMenuItem downloadItem = new JMenuItem("Download...");
        downloadItem.addActionListener(new DownloadWizardAction(Collections.singletonList(sample), descriptor));
        
        if (path==null) {
        	downloadItem.setEnabled(false);
        }

        return downloadItem;
    }
    
    private JMenuItem getVerificationMovieItem() {
        
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        final String path = DomainUtils.getFilepath(result,FileType.AlignmentVerificationMovie);
        if (path==null) return null;
        
        JMenuItem movieItem = new JMenuItem("View Alignment Verification Movie");
        movieItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(path);
                action.actionPerformed(event);
            }
        });

        return movieItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("SampleResultContextMenu.showInLightbox", result);
                ObjectiveSample objectiveSample = result.getParentRun().getParent();
                Sample sample = objectiveSample.getParent();
                ArtifactDescriptor descriptor = new ResultArtifactDescriptor(result);
                Hud.getSingletonInstance().setObjectAndToggleDialog(sample, descriptor, FileType.SignalMip.toString(), true, true);
            }
        });

        return toggleHudMI;
    }
}
