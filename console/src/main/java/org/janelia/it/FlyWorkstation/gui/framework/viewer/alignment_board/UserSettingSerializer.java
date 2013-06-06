package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserSettingSerializer implements Serializable {
    private static final String SELECTION_BOUNDS_SETTING = "SelectionBounds";
    private static final String USE_SIGNAL_SETTING = "UseSignal";
    private static final String CROP_OUT_LEVEL_SETTING = "CropOutLevel";
    private static final String GAMMA_SETTING = "Gamma";

    private VolumeModel volumeModel;
    private AlignmentBoardSettings alignmentBoardSettings;
    private Entity alignmentBoard;

    private Logger logger = LoggerFactory.getLogger( UserSettingSerializer.class );

    /**
     * Construct with sources/sinks for all data flowing through.
     *
     * @param alignmentBoard entity about which this data exists.
     * @param volumeModel for manipulating 3D settings.
     * @param alignmentBoardSettings for user-click settings.
     */
    public UserSettingSerializer( Entity alignmentBoard, VolumeModel volumeModel, AlignmentBoardSettings alignmentBoardSettings ) {
        this.alignmentBoard = alignmentBoard;
        this.alignmentBoardSettings = alignmentBoardSettings;
        this.volumeModel = volumeModel;
    }

    /**
     * Save back things that user will wish to see restored to former glory after cycling console.
     *
     * @see #deserializeSettings()
     */
    synchronized void serializeSettings() {
        try {
            String settingsString = getSettingsString();

            // Write back.
            alignmentBoard.setValueByAttributeName(
                    EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS, settingsString
            );

            ModelMgr.getModelMgr().saveOrUpdateEntity( alignmentBoard );
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    /**
     * Restore-to-former-glory, things that the user had in place during the previous cycle.
     * @see #serializeSettings()
     */
    synchronized void deserializeSettings() {
        try {
            // Read up.
            String settingString =
                    this.alignmentBoard.getValueByAttributeName(
                            EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS
                    );
            logger.info("SETTINGS: {} deserialized", settingString);

            parseSettings(settingString);
        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }

    }

    void parseSettings(String settingString) {
        if ( settingString == null ) {
            logger.info(
                    "No serialized values for board {}/{}.",
                    this.alignmentBoard.getName(), this.alignmentBoard.getId()
            );
            return;
        }

        // Add settings to current runtime.
        String[] settingsStrings = settingString.split( "\n" );
        Map<String,String> settingToValue = new HashMap<String,String>();
        for ( String setting: settingsStrings ) {
            // Ignore comments.
            int equalPos = setting.indexOf( '=' );
            if ( Character.isWhitespace( setting.charAt( 0 ) )  ||
                    equalPos == -1 ) {
                continue;
            }
            settingToValue.put( setting.substring( 0, equalPos ), setting.substring( equalPos+1 ) );
        }

        String str = null;
        str = settingToValue.get( GAMMA_SETTING );
        if ( str != null ) {
            float gamma = Float.parseFloat( str );
            alignmentBoardSettings.setGammaFactor( gamma );
            volumeModel.setGammaAdjustment( gamma );
        }

        str = settingToValue.get( CROP_OUT_LEVEL_SETTING );
        if ( str != null ) {
            float cropOut = Float.parseFloat( str );
            volumeModel.setCropOutLevel( cropOut );
        }

        str = settingToValue.get( USE_SIGNAL_SETTING );
        if ( str != null ) {
            Boolean useSignal = Boolean.parseBoolean( str );
            alignmentBoardSettings.setShowChannelData( useSignal );
        }

        str = settingToValue.get( SELECTION_BOUNDS_SETTING );
        if ( str != null ) {
            float[] cropCoordArray = null;
            Collection<float[]> coordinateSets = new ArrayList<float[]>();
            String[] coordSetStrs = str.split( "]" );
            for ( String coordSetStr: coordSetStrs ) {
                if ( coordSetStr.length() == 0 )
                    continue;

                String[] coordStrs = coordSetStr.substring( 1 ).split( "," );
                if ( coordStrs.length != 6 ) {
                    logger.warn(
                            "Did not find sufficient coordinate set serialized.  Expected {}, found {} coords.",
                            6, coordStrs.length
                    );
                }
                else {
                    cropCoordArray = new float[ 6 ];
                    int offs = 0;
                    for ( String coordStr: coordStrs ) {
                        cropCoordArray[ offs++ ] = Float.parseFloat( coordStr );
                    }
                    coordinateSets.add( cropCoordArray );
                }

            }

            CropCoordSet cropCoordSet = new CropCoordSet();
            if ( cropCoordArray != null ) {
                cropCoordSet.setCurrentCoordinates( cropCoordArray );
            }
            cropCoordSet.setAcceptedCoordinates( coordinateSets );
            volumeModel.setCropCoords( cropCoordSet );

        }
    }

    String getSettingsString() {
        // Locate all the settings to be saved.
        AlignmentBoardSettings userClickSettings = alignmentBoardSettings;

        // Save back all of interest.
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(GAMMA_SETTING + "=%f", volumeModel.getGammaAdjustment()));
        builder.append("\n");
        builder.append(String.format(CROP_OUT_LEVEL_SETTING + "=%f", volumeModel.getCropOutLevel()));
        builder.append("\n");
        builder.append(USE_SIGNAL_SETTING).append("=");
        builder.append(userClickSettings.isShowChannelData());
        builder.append("\n");

        CropCoordSet cropCoordSet = volumeModel.getCropCoords();
        builder.append(SELECTION_BOUNDS_SETTING + "=");
        for (float[] nextCoordSet : cropCoordSet.getAcceptedCoordinates()) {
            appendCoordinateArray(builder, nextCoordSet);
        }
        // May add on any current (on-ORed) coordinates.
        if ( cropCoordSet.getCurrentCoordinates() != null ) {
            StringBuilder currBuf = new StringBuilder();
            String currCoordStr = currBuf.toString();
            if ( ! currCoordStr.contains( currCoordStr ) ) {
                appendCoordinateArray( currBuf, cropCoordSet.getCurrentCoordinates() );
                builder.append( currCoordStr );
            }
        }
        builder.append("\n");
        logger.info("SETTINGS: {} serialized", builder);
        return builder.toString();
    }

    private void appendCoordinateArray(StringBuilder builder, float[] coordArray) {
        builder.append("[");
        for (int i = 0; i < coordArray.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(String.format("%f", coordArray[i]));
        }
        builder.append("]");
    }

}