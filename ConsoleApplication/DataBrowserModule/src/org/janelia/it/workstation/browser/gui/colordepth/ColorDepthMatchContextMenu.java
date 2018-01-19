package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.actions.OpenInFinderAction;
import org.janelia.it.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthMatchContextMenu.class);
    
    // Current selection
    protected ColorDepthResult contextObject;
    protected Map<Reference, Sample> sampleMap;
    protected List<ColorDepthMatch> matches;
    protected boolean multiple;
    protected ColorDepthMatch match;
    protected String matchName;

    public ColorDepthMatchContextMenu(ColorDepthResult result, List<ColorDepthMatch> matches, Map<Reference, Sample> sampleMap) {
        this.contextObject = result;
        this.matches = matches;
        this.sampleMap = sampleMap;
        this.multiple = matches.size() > 1;
        this.match = matches.size() == 1 ? matches.get(0) : null;
        Sample sample = match==null ? null : sampleMap.get(match.getSample());
        this.matchName = multiple ? null : (sample == null ? "Access denied" : sample.getName());
        ActivityLogHelper.logUserAction("ColorDepthMatchContextMenu.create", match);
    }

    public void runDefaultAction() {
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
        String name = multiple ? "(Multiple selected)" : matchName;
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
        
        Sample sample = sampleMap.get(match.getSample());
        if (sample==null) return null;
        
        return null;
        // TODO: figure out the result descriptor, or create a new action to make this work
        //return getNamedActionItem(new CreateMaskFromSampleAction(sample, resultDescriptor, typeName));
    }
   
    protected JMenuItem getAddToMaskResultsItem() {

        String name = matches.size() > 1 ? "  Add " + matches.size() + " Items to Mask Results" : "  Add Mask Results";
        
        JMenuItem addToMaskItem = new JMenuItem(name);
        addToMaskItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.getAddToMaskResultsItem", matches);
                
                SimpleWorker worker = new SimpleWorker() {
                    
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    
                    @Override
                    protected void doStuff() throws Exception {
                        ColorDepthMask mask = model.getDomainObject(matches.get(0).getMaskRef());
                        model.addChildren(mask, getSamples());
                    }

                    @Override
                    protected void hadSuccess() {
                        // TODO: reload data explorer?
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
                    }
                };
                worker.execute();
            }
        });
        return addToMaskItem;
    }
    
    protected JMenuItem getAddToFolderItem() {
        AddToFolderAction action = AddToFolderAction.get();
        action.setDomainObjects(getSamples());
        JMenuItem item = action.getPopupPresenter();
        if (item!=null) {
            item.setText("  " + item.getText());
        }
        return item;
    }

    protected JMenuItem getHudMenuItem() {
        if (multiple) return null;
        
        JMenuItem toggleHudMI = new JMenuItem("  Show in Lightbox");
        toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.showInLightbox", match);
//                Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName);
            }
        });

        return toggleHudMI;
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
    
    protected List<Sample> getSamples() {
        List<Sample> samples = new ArrayList<>();
        for(ColorDepthMatch match : matches) {
            samples.add(sampleMap.get(match.getSample()));
        }
        return samples;
    }
    
    
}
