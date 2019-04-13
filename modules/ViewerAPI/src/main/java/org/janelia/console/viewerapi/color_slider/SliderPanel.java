/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.console.viewerapi.color_slider;

import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.console.viewerapi.SimpleIcons;
import org.janelia.console.viewerapi.actions.ImportExportColorModelAction;
import org.janelia.console.viewerapi.controller.ColorModelListener;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.model.UnmixingParameters;
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
	private JPanel colorLockPanel = new JPanel();
    private ImageColorModel imageColorModel;
    private ColorModelListener visibilityListener;
    private VIEW top;

    public SliderPanel() { // empty constructor so I can drag this widget in netbeans GUI builder
    }
    
    public SliderPanel( ImageColorModel imageColorModel ) {
        setImageColorModel(imageColorModel);
    }
    
    public void setTop(VIEW top) {
        this.top = top;
    }
    
    public VIEW getTop() {
        return top;
    }
    
    public final void setImageColorModel( ImageColorModel imageColorModel ) {
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

        colorLockPanel.setComponentPopupMenu(sliderPanelMenu);
        for (int i=0; i<colorWidgets.length; i++) {
             ColorChannelWidget cw = colorWidgets[i];
             cw.setComponentPopupMenu(sliderPanelMenu);
        }
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

}
