package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.gui.browser.actions.OpenInFinderAction;
import org.janelia.it.workstation.gui.browser.actions.OpenInNeuronAnnotatorAction;
import org.janelia.it.workstation.gui.browser.actions.OpenInToolAction;
import org.janelia.it.workstation.gui.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerManager;
import org.janelia.it.workstation.gui.browser.components.SampleResultViewerTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;

/**
 * Right-click context menu for sample results presented in the Sample Editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResultContextMenu extends PopupContextMenu {

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
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenSeparationInNewViewer());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dTriViewItem());
        add(getVaa3d3dViewItem());
        add(getFijiViewerItem());
        add(getDownloadItem());
        
        setNextAddRequiresSeparator(true);
        add(getVerificationMovieItem());
        
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
        
    }
    
    public void runDefaultAction() {
        if (result.getLatestSeparationResult()==null) return;
        SampleResultViewerTopComponent viewer = ViewerUtils.getViewer(SampleResultViewerManager.getInstance(), "editor3");
        if (viewer==null || !DomainUtils.equals(viewer.getCurrent(), result)) {
            viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
            viewer.requestActive();
            viewer.loadSampleResult(result, true, null);
        }
    }
    
    protected JMenuItem getTitleItem() {
        String name = result.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("Name",result.getName()));
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("GUID",result.getId().toString()));
    }
    
    protected JMenuItem getOpenSeparationInNewViewer() {
        if (result.getLatestSeparationResult()==null) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open Neuron Separation In New Viewer");
        copyMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

    protected JMenuItem getNeuronAnnotatorItem() {
        final NeuronSeparation separation = result.getLatestSeparationResult();
        if (separation==null) return null;
        return getNamedActionItem(new OpenInNeuronAnnotatorAction(separation));
    }
        
    protected JMenuItem getVaa3dTriViewItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, null));
    }

    protected JMenuItem getVaa3d3dViewItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_VAA3D, path, ToolMgr.MODE_3D));
    }

    protected JMenuItem getFijiViewerItem() {
        String path = DomainUtils.getDefault3dImageFilePath(result);
        if (path==null) return null;
        return getNamedActionItem(new OpenInToolAction(ToolMgr.TOOL_FIJI, path, null));
    }

    protected JMenuItem getDownloadItem() {

        String path = DomainUtils.getDefault3dImageFilePath(result);
        ObjectiveSample objectiveSample = result.getParentRun().getParent();
        final Sample sample = objectiveSample.getParent();
        final ResultDescriptor descriptor = new ResultDescriptor(result);
        
        JMenuItem downloadItem = new JMenuItem("  Download...");
        downloadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                DownloadDialog dialog = new DownloadDialog();
                dialog.showDialog(Arrays.asList(sample), descriptor);
            }
        });
        
        if (path==null) {
        	downloadItem.setEnabled(false);
        }

        return downloadItem;
    }
    
    private JMenuItem getVerificationMovieItem() {
        
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        final String path = DomainUtils.getFilepath(result,FileType.AlignmentVerificationMovie);
        if (path==null) return null;
        
        JMenuItem movieItem = new JMenuItem("  View Alignment Verification Movie");
        movieItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(path);
                action.doAction();
            }
        });

        return movieItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = new JMenuItem("  Show in Lightbox");
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                ObjectiveSample objectiveSample = result.getParentRun().getParent();
                Sample sample = objectiveSample.getParent();
                ResultDescriptor descriptor = new ResultDescriptor(result);
                Hud.getSingletonInstance().setObjectAndToggleDialog(sample, descriptor, FileType.SignalMip.toString());
            }
        });

        return toggleHudMI;
    }
}
