package org.janelia.workstation.browser.gui.colordepth;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.OpenInFinderAction;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.browser.actions.context.AddToFolderAction;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.NodeContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.nodes.ChildObjectsNode;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchContextMenu extends PopupContextMenu {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthMatchContextMenu.class);

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
    protected boolean flyem;
    protected String bodyId;

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
        // Are any FlyEM skeletons selected?
        this.flyem = false;
        OUTER: for (ColorDepthMatch match : matches) {
            ColorDepthImage matchImage = imageModel.getImage(match);
            if (matchImage != null && matchImage.getLibraries() != null) {
                for (String library : matchImage.getLibraries()) {
                    if (library.startsWith("flyem_")) {
                        this.flyem = true;
                        break OUTER;
                    }
                }
            }
        }

        if (flyem) {
            this.bodyId = getBodyId(image);
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

    private String getBodyId(ColorDepthImage image) {
        if (image==null || image.getName()==null) return null;
        Pattern p = Pattern.compile(".*?(\\d{9,}).*?");
        Matcher m = p.matcher(image.getName());
        return m.matches() ? m.group(1) : null;
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
        add(getCopyBodyIdsToClipboardItem());
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
        add(getDownloadItem());
        add(getOpenWithNeuprintItem());
        add(getCreateMaskAction());

        setNextAddRequiresSeparator(true);
        add(getCompareHudMenuItem());
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

    protected JMenuItem getCopyBodyIdsToClipboardItem() {
        if (!flyem) return null;
        if (multiple) {
            // Collect all the body ids delimited by whitespace
            String bodyIds = matches.stream()
                    .map(m -> getBodyId(imageModel.getImage(m)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
            return getNamedActionItem(new CopyToClipboardAction("FlyEM Body Ids", bodyIds));
        }
        else {
            return getNamedActionItem(new CopyToClipboardAction("FlyEM Body Id", bodyId));
        }
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

    protected JMenuItem getOpenWithNeuprintItem() {
        if (multiple) return null;
        if (!flyem) return null;

        final String neuprintUrl = ConsoleProperties.getInstance().getProperty("neuprint.url");
        final String fullUrl = neuprintUrl+"?bodyid="+bodyId;

        JMenuItem item = new JMenuItem("Open Neuron Skeleton in neuPrint");
        item.addActionListener(e -> {
            log.info("Opening in browser: {}", fullUrl);
            Utils.openUrlInBrowser(fullUrl);
        });

        if (bodyId==null) {
            item.setEnabled(false);
        }

        return item;
    }

    protected JMenuItem getCreateMaskAction() {
        if (multiple) return null;
        return getNamedActionItem(new CreateMaskFromImageAction(image));
    }

    protected JMenuItem getDownloadItem() {

        // Get all images selected
        List<ColorDepthImage> images = matches.stream()
                .map(m -> imageModel.getImage(m))
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (!images.isEmpty()) {
            JMenuItem downloadItem = new JMenuItem("Download...");
            downloadItem.addActionListener(new DownloadWizardAction(images, null));
            return downloadItem;
        }
        
        return null;
    }

    protected JMenuItem getCompareHudMenuItem() {
        if (multiple) return null;

        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
        toggleHudMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        toggleHudMI.addActionListener(e -> {
            ActivityLogHelper.logUserAction("ColorDepthMatchContentMenu.showInLightWithMask", match);
            ColorDepthHud.getSingletonInstance().setObjectAndToggleDialog(match, imageModel,true);
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
