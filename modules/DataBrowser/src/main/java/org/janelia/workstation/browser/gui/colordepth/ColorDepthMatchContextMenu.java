package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.workstation.core.actions.NodeContext;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.browser.actions.OpenInFinderAction;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.actions.context.AddToFolderAction;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;

/**
 * Context pop up menu for color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchContextMenu extends PopupContextMenu {
    
    // Current selection
    protected ColorDepthResult contextObject;
    protected ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel;
    protected ColorDepthResultImageModel imageModel;
    protected List<ColorDepthMatch> matches;
    protected boolean multiple;
    
    // If single result selected, these will be not null
    protected ColorDepthMatch match;
    protected ColorDepthImage image;
    protected Sample sample;
    protected String matchName;
    
    public ColorDepthMatchContextMenu(ColorDepthResult result, List<ColorDepthMatch> matches, ColorDepthResultImageModel imageModel, 
            ChildSelectionModel<ColorDepthMatch,Reference> editSelectionModel) {
        this.contextObject = result;
        this.matches = matches;
        this.imageModel = imageModel;
        this.editSelectionModel = editSelectionModel;
        this.multiple = matches.size() > 1;
        this.match = matches.size() == 1 ? matches.get(0) : null;
        if (match != null) {
            this.image = imageModel.getImage(match);
            this.sample = imageModel.getSample(match);
            if (sample==null) {
                this.matchName = image.getName();
            }
            else {
                this.matchName = sample.getName();
            }
        }
        ActivityLogHelper.logUserAction("ColorDepthMatchContextMenu.create", match);
    }

    public void runDefaultAction() {
        if (multiple) return;
        if (DomainViewerTopComponent.isSupported(match)) {
            List<Sample> samples = getSamples();
            if (!samples.isEmpty()) {
                Sample sample = getSamples().get(0);
                DomainViewerTopComponent viewer = ViewerUtils.getViewer(DomainViewerManager.getInstance(), "editor2");
                if (viewer == null || !viewer.isCurrent(sample)) {
                    viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                    viewer.requestActive();
                    viewer.loadDomainObject(sample, true);
                }
            }
        }
    }

    public void addMenuItems() {

        if (matches.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        setNextAddRequiresSeparator(true);

        if (editSelectionModel != null) {
            add(getCheckItem(true));
            add(getCheckItem(false));
        }
        
        add(getAddToMaskResultsItem());
        add(getAddToFolderItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());

        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : StringUtils.abbreviate(matchName, 50);
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("Name",matchName));
    }

    protected JMenuItem getCheckItem(boolean check) {
        String title = check ? "Check" : "Uncheck";
        JMenuItem menuItem = new JMenuItem(title+" Selected");
        menuItem.addActionListener((e) -> {
            if (check) {
                editSelectionModel.select(matches, false, true);
            }
            else {
                editSelectionModel.deselect(matches, true);
            }
        });
        return menuItem;
    }

    protected JMenuItem getAddToMaskResultsItem() {
        
        List<Sample> samples = getSamples();
        if (samples.isEmpty()) return null;
        if (match==null) return null;

        AddToResultsAction action = AddToResultsAction.get();
        action.setDomainObjects(samples);
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        action.setMask(imageModel.getMask());
        
        JMenuItem item = action.getPopupPresenter();
        if (item!=null) {
            // Override title to include the word "Sample" instead of generic "Item"
            String title = samples.size() > 1 ? "Add " + samples.size() + " Samples To Result Set" : "Add Sample To Result Set";
            item.setText(title);
        }
        return item;
    }
    
    protected JMenuItem getAddToFolderItem() {
        
        List<Sample> samples = getSamples();
        if (samples.isEmpty()) return null;
        
        AddToFolderAction action = AddToFolderAction.get();
        action.enable(new NodeContext(new ChildObjectsNode(samples)), null);

        JMenuItem item = action.getPopupPresenter();
        if (item!=null) {
            // Override title to include the word "Sample" instead of generic "Item"
            String title = samples.size() > 1 ? "Add " + samples.size() + " Samples To Folder" : "Add Sample To Folder";
            item.setText(title);
        }
        return item;
    }

    protected JMenuItem getOpenInFinderItem() {
    	if (multiple) return null;
        String path = image.getFilepath();
        if (path==null) return null;
        if (!OpenInFinderAction.isSupported()) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
    	if (multiple) return null;
        String path = image.getFilepath();
        if (path==null) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

    protected JMenuItem getHudMenuItem() {
        if (multiple) return null;
        
        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
        toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        toggleHudMI.addActionListener(e -> {
            ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.showInLightbox", match);
            String title = imageModel.getImageTitle(match);
            Hud.getSingletonInstance().setFilepathAndToggleDialog(image.getFilepath(), title, true, false);
        });

        return toggleHudMI;
    }
    
    protected List<Sample> getSamples() {
        List<Sample> samples = new ArrayList<>();
        for(ColorDepthMatch match : matches) {
            Sample sample = imageModel.getSample(match);
            if (sample!=null) {
                samples.add(sample);
            }
        }
        return samples;
    }
}
