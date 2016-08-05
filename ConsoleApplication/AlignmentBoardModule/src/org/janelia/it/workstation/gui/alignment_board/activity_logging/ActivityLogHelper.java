/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board.activity_logging;

import com.google.common.eventbus.Subscribe;
import java.util.Collection;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardReference;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.ControlsListener;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.SavebackEvent;
import org.janelia.it.workstation.gui.browser.events.Events;

/**
 * Helps capture events generated at the Alignment Board.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    private static final ToolString TOOL_STRING = new ToolString("AlignmentBoard");

    private static final String LOG_PART_SEP = ":";

    private static final CategoryString OPEN_CATEGORY = new CategoryString("openAB");
    private static final CategoryString BRIGHTNESS_CATEGORY = new CategoryString("gamma");
    private static final CategoryString SETTINGS_CATEGORY = new CategoryString("settings");
    private static final CategoryString EXPORT_CATEGORY = new CategoryString("exportSelection");
    private static final CategoryString CROP_BLACKOUT_CATEGORY = new CategoryString("blackoutCropped");    
    private static final CategoryString SUBVOL_SELECT_CATEGORY = new CategoryString("subvolumeSelect");
    //private static final CategoryString SUBVOL_SELECT_SETTINGS_CATEGORY = new CategoryString("subvolumeSelectSettings");
    private static final CategoryString TOGGLE_SHOW_AXES_CATEGORY = new CategoryString("toggleAxes");
    private static final CategoryString TOGGLE_INTENSITY_CATEGORY = new CategoryString("toggleSignal");
    private static final CategoryString TOGGLE_SAVE_BRIGHTNESS_CATEGORY = new CategoryString("toggleSaveBrightnessToExport");
    private static final CategoryString FORCE_REFRESH_CATEGORY = new CategoryString("forceRenderRefresh");
    private static final CategoryString FORCE_REBUILD_CATEGORY = new CategoryString("forceBoardRebuild");
    private static final CategoryString CONNECT_EDIT_CATEGORY = new CategoryString("connectEditEvents");
    private static final CategoryString ABOARD_CHANGE_CATEGORY = new CategoryString("changeEvent");

    private static final ActivityLogging ACTIVITY_LOGGING = FrameworkImplProvider.getSessionSupport();

    public void logOpen(String boardName) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, OPEN_CATEGORY, new ActionString(boardName));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public void logSubVolSelect(String boardName, Collection<float[]> acceptedCords) {
        try {
            StringBuilder acceptedCoordsValue = new StringBuilder();
            for (float[] f: acceptedCords) {
                if (acceptedCoordsValue.length() > 0) {
                    acceptedCoordsValue.append(";");
                }
                for (float c: f) {
                    acceptedCoordsValue.append(c);
                    acceptedCoordsValue.append(" ");
                }
            }
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SUBVOL_SELECT_CATEGORY, new ActionString((boardName == null ? "" : boardName)  + ":" + acceptedCoordsValue.toString()));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public void logToggleIntensity(String boardName, boolean value) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, TOGGLE_INTENSITY_CATEGORY, new ActionString((boardName == null ? "" : boardName) + ":" + value));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public void logToggleAxes(boolean showAxes) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, TOGGLE_SHOW_AXES_CATEGORY, new ActionString(new Boolean(showAxes).toString()));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
        
    public void logToggleSaveBrightness(boolean saveBrightnessToExports) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, TOGGLE_SAVE_BRIGHTNESS_CATEGORY, new ActionString(new Boolean(saveBrightnessToExports).toString()));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public static class ControlListener implements ControlsListener {
        private AlignmentBoardSettings settings;
        private Long id;
        public ControlListener(AlignmentBoardSettings settings, Long alignmentBoardId) {
            this.settings = settings;
            this.id = alignmentBoardId;
            Events.getInstance().registerOnEventBus(this);
        }
        
        public void close() {
            Events.getInstance().unregisterOnEventBus(this);
        }

        @Override
        public void setBrightness(double brightness) {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, BRIGHTNESS_CATEGORY, new ActionString(abIdPrefix() + String.format("%5.4f", brightness)));
        }

        @Override
        public void updateSettings() {
            try {
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SETTINGS_CATEGORY, new ActionString(abIdPrefix() + settings.printSettings()));
            } catch (Throwable th) {
                // Eat all exceptions.  Avoid harming caller.
                th.printStackTrace();
            }
        }

        @Override
        public void updateCropCoords() {
            // This makes for a lot of misleading chatter.  Leaving this out
            // avoids spurious log output.
            //ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SUBVOL_SELECT_SETTINGS_CATEGORY, new ActionString(abIdPrefix() + settings.printSettings()));            
        }

        @Override
        public void exportSelection(SavebackEvent savebackEvent) {
            try {
                StringBuilder bldr = new StringBuilder();
                bldr.append(id);
                bldr.append(LOG_PART_SEP);
                bldr.append(savebackEvent.getMethod());
                bldr.append(LOG_PART_SEP);
                bldr.append(String.format("4.3f", savebackEvent.getGammaFactor()));
                bldr.append(LOG_PART_SEP);
                // Coords are optional.  Their absense will be marked by the dangling part-separator above.
                for (float[] coord : savebackEvent.getAbsoluteCoords()) {
                    bldr.append('(');
                    for (float coordMem : coord) {
                        bldr.append(coordMem);
                        bldr.append(',');
                    }
                    bldr.setCharAt(bldr.length() - 1, ')');
                }
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, EXPORT_CATEGORY, new ActionString(bldr.toString()));
            } catch (Exception th) {
                th.printStackTrace();
            }
        }

        @Override
        public void setCropBlackout(boolean blackout) {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, CROP_BLACKOUT_CATEGORY, new ActionString(abIdPrefix() + blackout));            
        }

        @Override
        public void forceRenderRefresh() {
            try {
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, FORCE_REFRESH_CATEGORY, new ActionString(abIdPrefix() + settings.printSettings()));
            } catch (Throwable th) {
                // Eat all exceptions.  Avoid harming caller.
                th.printStackTrace();
            }
        }

        @Override
        public void setConnectEditEvents(boolean connectEditEvents) {
            try {
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, CONNECT_EDIT_CATEGORY, new ActionString(abIdPrefix() + connectEditEvents));
            } catch (Throwable th) {
                // Eat all exceptions.  Avoid harming caller.
                th.printStackTrace();
            }
        }

        @Override
        public void forceRebuild() {
            try {
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, FORCE_REBUILD_CATEGORY, new ActionString(abIdPrefix() + settings.printSettings()));
            } catch (Throwable th) {
                // Eat all exceptions.  Avoid harming caller.
                th.printStackTrace();
            }
        }
        
        private String abIdPrefix() {
            return ""+ id + LOG_PART_SEP;
        }

        @Subscribe
        public void itemChanged(AlignmentBoardItemChangeEvent event) {
            try {
                StringBuilder bldr = new StringBuilder();
                bldr.append(event.getChangeType());
                bldr.append(LOG_PART_SEP);
                final AlignmentBoardItem item = event.getItem();
                if (item != null) {
                    bldr.append(event.getItem().getName());                    
                }
                bldr.append(LOG_PART_SEP);
                if (item != null) {
                    bldr.append(event.getItem().getColor());
                }
                bldr.append(LOG_PART_SEP);
                if (item != null) {
                    bldr.append(event.getItem().getInclusionStatus());
                }
                bldr.append(LOG_PART_SEP);
                if (item != null) {
                    bldr.append(event.getItem().getRenderMethod());
                }
                bldr.append(LOG_PART_SEP);
                // In event of no item/target (perhaps for ref channel, dangling separator tells the tale.
                if (item != null) {
                    final AlignmentBoardReference target = item.getTarget();
                    if (target != null) {
                        bldr.append(target.getItemId());
                    }
                }
                bldr.append(LOG_PART_SEP);
                ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, ABOARD_CHANGE_CATEGORY, new ActionString(bldr.toString()));
            } catch (Throwable th) {
                // Eat all exceptions.  Avoid harming caller.
                th.printStackTrace();
            }
        }
    }
}
