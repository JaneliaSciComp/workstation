package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.actions.OpenInFinderAction;
import org.janelia.it.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainViewerManager;
import org.janelia.it.workstation.browser.components.DomainViewerTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;

/**
 * Context pop up menu for color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchContextMenu extends PopupContextMenu {
    
    // Current selection
    protected ColorDepthResult contextObject;
    protected ImageModel<ColorDepthMatch, String> imageModel;
    protected Map<Reference, Sample> sampleMap;
    protected List<ColorDepthMatch> matches;
    protected boolean multiple;
    
    // If single result selected, these will be not null
    protected ColorDepthMatch match;
    protected Sample sample;
    protected String matchName;
    
    public ColorDepthMatchContextMenu(ColorDepthResult result, List<ColorDepthMatch> matches, Map<Reference, Sample> sampleMap, ImageModel<ColorDepthMatch, String> imageModel) {
        this.contextObject = result;
        this.matches = matches;
        this.sampleMap = sampleMap;
        this.imageModel = imageModel;
        this.multiple = matches.size() > 1;
        this.match = matches.size() == 1 ? matches.get(0) : null;
        if (match != null) {
            if (match.getSample()==null) {
                this.matchName = match.getFile().getName();
            }
            else {
                this.sample = match==null ? null : sampleMap.get(match.getSample());
                this.matchName = multiple ? null : (sample == null ? "Access denied" : sample.getName());
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
        add(getAddToMaskResultsItem());
        add(getAddToFolderItem());
        add(getColorDepthSearchItem());
        
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


    protected JMenuItem getColorDepthSearchItem() {

        if (multiple) return null;
        
        return null;
        // TODO: figure out the result descriptor, or create a new action to make this work
        //return getNamedActionItem(new CreateMaskFromSampleAction(sample, resultDescriptor, typeName));
    }
   
    protected JMenuItem getAddToMaskResultsItem() {
        
        List<Sample> samples = getSamples();
        if (samples.isEmpty()) return null;
        if (match==null) return null;
        
        AddToResultsAction action = AddToResultsAction.get();
        action.setDomainObjects(samples);
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        try {
            // This should properly be done in a background thread, but as a shortcut we'll rely on the fact it's cached 
            ColorDepthMask mask = model.getDomainObject(match.getMaskRef());
            action.setMask(mask);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            return null;
        }
        
        JMenuItem item = action.getPopupPresenter();
        if (item!=null) {
            // Override title to include the word "Sample" instead of generic "Item"
            String title = samples.size() > 1 ? "Add " + samples.size() + " Samples To Result Set" : "Add Sample To Result Set";
            item.setText("  " + title);
        }
        return item;
    }
    
    protected JMenuItem getAddToFolderItem() {
        
        List<Sample> samples = getSamples();
        if (samples.isEmpty()) return null;
        
        AddToFolderAction action = AddToFolderAction.get();
        action.setDomainObjects(samples);
        
        JMenuItem item = action.getPopupPresenter();
        if (item!=null) {
            // Override title to include the word "Sample" instead of generic "Item"
            String title = samples.size() > 1 ? "Add " + samples.size() + " Samples To Folder" : "Add Sample To Folder";
            item.setText("  " + title);
        }
        return item;
    }

    protected JMenuItem getOpenInFinderItem() {
    	if (multiple) return null;
        String path = match.getFilepath();
        if (path==null) return null;
        if (!OpenInFinderAction.isSupported()) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
    	if (multiple) return null;
        String path = match.getFilepath();
        if (path==null) return null;
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }

    protected JMenuItem getHudMenuItem() {
        if (multiple) return null;
        
        JMenuItem toggleHudMI = new JMenuItem("  Show in Lightbox");
        toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.showInLightbox", match);
                String title = imageModel.getImageTitle(match);
                Hud.getSingletonInstance().setFilepathAndToggleDialog(match.getFilepath(), title, true, false);
            }
        });

        return toggleHudMI;
    }
    
    protected List<Sample> getSamples() {
        List<Sample> samples = new ArrayList<>();
        for(ColorDepthMatch match : matches) {
            if (match.getSample()!=null) {
                samples.add(sampleMap.get(match.getSample()));
            }
        }
        return samples;
    }
}
