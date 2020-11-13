package org.janelia.workstation.controller.color_slider;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.controller.widgets.SimpleIcons;
import org.janelia.workstation.controller.listener.ColorModelListener;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.tiledMicroscope.TmColorModel;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.access.ModelTranslation;
import org.janelia.workstation.controller.action.ImportExportColorModelAction;
import org.janelia.workstation.controller.eventbus.ColorModelUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates all multi-slider functionality for reuse.
 * @author fosterl
 */
public class SliderPanel extends JPanel {

    private final static Logger log = LoggerFactory.getLogger(SliderPanel.class);

    public enum VIEW {Horta, LVV};
    private static final String IMAGES_LOCK = "lock.png";
    private static final String IMAGES_LOCK_UNLOCK = "lock_unlock.png";

    private ColorChannelWidget colorChannelWidget_0;
	private ColorChannelWidget colorChannelWidget_1;
	private ColorChannelWidget colorChannelWidget_2;
	private ColorChannelWidget colorChannelWidget_3;
	private ColorChannelWidget[] colorWidgets;
    private JToggleButton lockBlackButton;
    private JToggleButton lockGrayButton;
    private JToggleButton lockWhiteButton;
    private JButton gearButton;
	private JPanel colorLockPanel = new JPanel();
    private ImageColorModel imageColorModel;
    private ColorModelListener visibilityListener;
    private VIEW top;

    public enum ModelType {
        COLORMODEL_2D,
        COLORMODEL_3D
    }
    private ModelType modelType;

    public SliderPanel(ModelType modelType) {
        // currently used by Horta 3D viewer
        this.modelType = modelType;
        ViewerEventBus.registerForEvents(this);
    }
    
    public SliderPanel(ImageColorModel imageColorModel, ModelType modelType) {
        // currently used by Horta 2D viewer
        this.modelType = modelType;
        setImageColorModel(imageColorModel);
    }
    
    public void setTop(VIEW top) {
        this.top = top;
    }
    
    public VIEW getTop() {
        return top;
    }

    @Subscribe
    public void updateColorModel(ColorModelUpdateEvent event) {
        ImageColorModel colorModel = event.getImageColorModel();
        if (colorModel!=null) {
            setImageColorModel(colorModel);
        }
    }
    
    public final synchronized void setImageColorModel( ImageColorModel imageColorModel ) {
        if ( colorWidgets != null ) {
            for ( ColorChannelWidget ccw: colorWidgets ) {
                this.remove( ccw );
            }
        }
        colorChannelWidget_0 = new ColorChannelWidget(0, imageColorModel);
        colorChannelWidget_1 = new ColorChannelWidget(1, imageColorModel);
        colorChannelWidget_2 = new ColorChannelWidget(2, imageColorModel);
        colorChannelWidget_3 = new ColorChannelWidget(3, imageColorModel);
        colorWidgets = new ColorChannelWidget[] {
            colorChannelWidget_0, 
            colorChannelWidget_1, 
            colorChannelWidget_2, 
            colorChannelWidget_3
        };
        if ( visibilityListener != null && imageColorModel != null ) {
            imageColorModel.removeColorModelListener(visibilityListener);
        }
        this.imageColorModel = imageColorModel;
        guiInit();
        updateLockButtons();
        setVisible(true);
    }

    @Override
    public void setVisible(boolean isVisible) {
        int sc = imageColorModel.getChannelCount();
        for ( int i = 0; i < sc; i++ ) {
            colorWidgets[ i ].setVisible( isVisible );
        }
        super.setVisible(isVisible);

        colorLockPanel.setVisible(sc > 1);
        // TODO Trying without success to get sliders to initially paint correctly
        SliderPanel.this.validate();
        SliderPanel.this.repaint();
    }
    
    public void updateLockButtons() {
        if ( lockBlackButton != null ) {
            lockBlackButton.setSelected(imageColorModel.isBlackSynchronized());
            lockGrayButton.setSelected(imageColorModel.isGammaSynchronized());
            lockWhiteButton.setSelected(imageColorModel.isWhiteSynchronized());
        }
    }
    
    public void guiInit() {
        if ( colorLockPanel == null ) {
            return;
        }
        
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Must add all widgets up front, because number of channels is a
        // dynamic value.
        for ( ColorChannelWidget widget: colorWidgets ) {
            widget.setVisible( false );
            this.add( widget );
        }
		
		// JPanel colorLockPanel = new JPanel();
		colorLockPanel.setVisible(false);
        colorLockPanel.removeAll();
        this.remove(colorLockPanel);
		this.add(colorLockPanel);
		colorLockPanel.setLayout(new BoxLayout(colorLockPanel, BoxLayout.X_AXIS));
		
		colorLockPanel.add(Box.createHorizontalStrut(30));
		
		lockBlackButton = new JToggleButton("");
		lockBlackButton.setToolTipText("Synchronize channel black levels");
		lockBlackButton.setMargin(new Insets(0, 0, 0, 0));
		lockBlackButton.setRolloverIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockBlackButton.setRolloverSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockBlackButton.setIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockBlackButton.setSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockBlackButton.setSelected(true);
		colorLockPanel.add(lockBlackButton);
		lockBlackButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setBlackSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		lockGrayButton = new JToggleButton("");
		lockGrayButton.setToolTipText("Synchronize channel gray levels");
		lockGrayButton.setMargin(new Insets(0, 0, 0, 0));
		lockGrayButton.setRolloverIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockGrayButton.setRolloverSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockGrayButton.setIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockGrayButton.setSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockGrayButton.setSelected(true);
		colorLockPanel.add(lockGrayButton);
		lockGrayButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setGammaSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		lockWhiteButton = new JToggleButton("");
		lockWhiteButton.setToolTipText("Synchronize channel white levels");
		lockWhiteButton.setMargin(new Insets(0, 0, 0, 0));
		lockWhiteButton.setRolloverIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockWhiteButton.setRolloverSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockWhiteButton.setIcon(SimpleIcons.getIcon(IMAGES_LOCK_UNLOCK));
		lockWhiteButton.setSelectedIcon(SimpleIcons.getIcon(IMAGES_LOCK));
		lockWhiteButton.setSelected(true);
		colorLockPanel.add(lockWhiteButton);
		lockWhiteButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setWhiteSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalStrut(30));

		gearButton = new JButton("");
		gearButton.setToolTipText("Color model options");
        ImageIcon gearIcon = Icons.getIcon("cog.png");
		gearButton.setIcon(gearIcon);
		gearButton.setHideActionText(true);
		gearButton.setMinimumSize(gearButton.getPreferredSize());
		colorLockPanel.add(gearButton);

        if ( visibilityListener == null ) {
            visibilityListener = new ColorModelListener() {
                @Override
                public void colorModelChanged() {
                    setVisible(imageColorModel.getChannelCount() > 0);
                }
            };
        }
        imageColorModel.addColorModelListener(visibilityListener);

        // add popup-menu for saving/loading color model information from file
        JPopupMenu sliderPanelMenu = new JPopupMenu();

        ImportExportColorModelAction importColorModelAction = new ImportExportColorModelAction(ImportExportColorModelAction.MODE.IMPORT, this, colorLockPanel);
        importColorModelAction.putValue(Action.NAME, "Import Color Model");
        importColorModelAction.putValue(Action.SHORT_DESCRIPTION,
                "Import a color model from external file");
        sliderPanelMenu.add(new JMenuItem(importColorModelAction));

        ImportExportColorModelAction exportColorModelAction = new ImportExportColorModelAction(ImportExportColorModelAction.MODE.EXPORT, this, colorLockPanel);
        exportColorModelAction.putValue(Action.NAME, "Export Color Model");
        exportColorModelAction.putValue(Action.SHORT_DESCRIPTION,
                "Export Workspace color model to external file");
        exportColorModelAction.putValue("top", this.top);
        sliderPanelMenu.add(new JMenuItem(exportColorModelAction));

        ImportExportColorModelAction saveWorkspaceColorModelAction = new ImportExportColorModelAction(ImportExportColorModelAction.MODE.SAVE_WORKSPACE, this, colorLockPanel);
        saveWorkspaceColorModelAction.putValue(Action.NAME, "Save Color Model To Workspace");
        saveWorkspaceColorModelAction.putValue(Action.SHORT_DESCRIPTION,
                "Save color model to be default for this sample/workspace");
        saveWorkspaceColorModelAction.putValue("top", this.top);
        sliderPanelMenu.add(new JMenuItem(saveWorkspaceColorModelAction));

        ImportExportColorModelAction saveUserColorModelAction = new ImportExportColorModelAction(ImportExportColorModelAction.MODE.SAVE_USER, this, colorLockPanel);
        saveUserColorModelAction.putValue(Action.NAME, "Save Color Model As User Preference");
        saveUserColorModelAction.putValue(Action.SHORT_DESCRIPTION,
                "Save color model as a user preference for this sample/workspace");
        saveUserColorModelAction.putValue("top", this.top);
        sliderPanelMenu.add(new JMenuItem(saveUserColorModelAction));

        gearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sliderPanelMenu.show(gearButton,0, gearButton.getHeight());
            }
        });

    }

    public void importCompleteColorModel(File modelFile) {
        try (FileInputStream fileReader = new FileInputStream(modelFile)) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> fullColorModel = mapper.readValue(fileReader, new TypeReference<Map<String,Object>>(){});
            List<String> channelList = (List<String>)fullColorModel.get("channels");

            ImageColorModel importModel = mapper.convertValue(fullColorModel.get("topLevelModel"), ImageColorModel.class);
            for (int i=0; i<channelList.size(); i++) {
                ChannelColorModel ccm = imageColorModel.getChannel(i);
                ccm.fromString(channelList.get(i));
            }
            
            // update sync values on image model; not sure if this makes sense
            imageColorModel.setBlackSynchronized(importModel.isBlackSynchronized());
            imageColorModel.setGammaSynchronized(importModel.isGammaSynchronized());
            imageColorModel.setWhiteSynchronized(importModel.isWhiteSynchronized());
            updateLockButtons();
            
            // update unmixing parameters
            imageColorModel.setUnmixParameters(importModel.getUnmixParameters());
            imageColorModel.fireUnmixingParametersChanged();
        }
        catch (Exception e) {
            log.error("Error importing color model", e);
        }
    }

    public void saveColorModel(ImportExportColorModelAction.MODE saveMode) {
        Long projectId = null;
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        TmSample sample = TmModelManager.getInstance().getCurrentSample();
        projectId=(workspace==null)?sample.getId():workspace.getId();
        ImageColorModel imageColorModel = colorWidgets[0].getImageColorModel();
        TmColorModel tmColorModel = ModelTranslation.translateColorModel(imageColorModel);

        try {
            switch (saveMode) {
                case SAVE_USER:
                    FrameworkAccess.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MOUSELIGHT_COLORMODEL,
                            projectId.toString(), tmColorModel);
                    break;
                case SAVE_WORKSPACE:
                    // these sliders are used for both 2d and 3d viewers; save the correct color model
                    if (modelType == ModelType.COLORMODEL_3D) {
                        workspace.setColorModel3d(tmColorModel);
                    } else if (modelType == ModelType.COLORMODEL_2D) {
                        workspace.setColorModel(tmColorModel);
                    }
                    TmModelManager.getInstance().saveWorkspace(workspace);
                    break;
            }
        } catch (Exception e) {
            FrameworkAccess.handleException("Problem saving color model", e);
        }
    }


    public void exportCompleteColorModel(File modelFile) {
        // For now, only need to sync the unmixing params when we export since they aren't used by LVV
        try (FileOutputStream fileWriter = new FileOutputStream(modelFile)) {
            // assume at least one color channel
            ImageColorModel imageColorModel = colorWidgets[0].getImageColorModel();
            List<String> channelList = new ArrayList<>();
            for (int i=0; i<imageColorModel.getChannelCount(); i++) {
                channelList.add(imageColorModel.getChannel(i).asString());
            }

            // using map to capture all pertinent information
            Map<String, Object> fullColorModel = new HashMap<>();
            fullColorModel.put("channels", channelList);
            fullColorModel.put("topLevelModel", imageColorModel);

            UnmixingParameters unmixingResult = Utilities.actionsGlobalContext().lookup(UnmixingParameters.class);
            if (unmixingResult!=null) {
                fullColorModel.put("unmixing", unmixingResult);
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fileWriter, fullColorModel);
        } catch (Exception e) {
            log.error("Error exporting color model", e);
        }
    }

    public File getDefaultColorModelDirectory() {
        // at Janelia, users store their saved color models in a shared, standard
        //  location; if it's reachable, start the file dialogs in that dialog
        // do a brute force search, similar to AnnotationManager.getDefaultSwcDirectory()
        String osName = System.getProperty("os.name").toLowerCase();
        List<Path> prefixesToTry = new Vector<>();
        if (osName.contains("win")) {
            for (File fileRoot : File.listRoots()) {
                prefixesToTry.add(fileRoot.toPath());
            }
        } else if (osName.contains("os x")) {
            // for Mac, it's simpler:
            prefixesToTry.add(new File("/Volumes").toPath());
        } else if (osName.contains("lin")) {
            // Linux
            prefixesToTry.add(new File("/groups/mousebrainmicro").toPath());
        }
        boolean found = false;
        // java and its "may not have been initialized" errors...
        File testFile = new File(System.getProperty("user.home"));
        for (Path prefix: prefixesToTry) {
            // test with and without the first part
            testFile = prefix.resolve("shared_tracing/Color_Models").toFile();
            if (testFile.exists()) {
                found = true;
                break;
            }
            testFile = prefix.resolve("mousebrainmicro/shared_tracing/Color_Models").toFile();
            if (testFile.exists()) {
                found = true;
                break;
            }
        }
        if (!found) {
            testFile = new File(System.getProperty("user.home"));
        }
        return testFile;
    }

}
