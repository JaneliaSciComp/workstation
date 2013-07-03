package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.UnitVec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
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
    public static final String CAMERA_ROTATION_SETTING = "CameraRotation";
    public static final String CAMERA_FOCUS_SETTING = "CameraFocus";

    private static final int X_OFFS = 0;
    private static final int Y_OFFS = 1;
    private static final int Z_OFFS = 2;
    public static final int MAX_SERIALIZED_SETTINGS_STR = 65535;

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
    public UserSettingSerializer(
            Entity alignmentBoard, VolumeModel volumeModel, AlignmentBoardSettings alignmentBoardSettings ) {
        this.alignmentBoard = alignmentBoard;
        this.alignmentBoardSettings = alignmentBoardSettings;
        this.volumeModel = volumeModel;
        logger.debug("Serializer: with VolumeModel {}, with cam3D {}.", volumeModel, volumeModel.getCamera3d() );
    }

    /**
     * Save back things that user will wish to see restored to former glory after cycling console.
     *
     * @see #deserializeSettings()
     */
    synchronized void serializeSettings() {
        try {
            String settingsString = getSettingsString();
            // This excessive length
            if ( settingsString.length() > MAX_SERIALIZED_SETTINGS_STR ) {
                logger.warn( "Abandoning the serialized string {}.", settingsString );
                // Write back.
                alignmentBoard.setValueByAttributeName(
                        EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS, ""
                );
                settingsString = "";
            }
            logger.info( "Save-back Setting string: {}.", settingsString );

            // Write back.
            alignmentBoard.setValueByAttributeName(
                    EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS, settingsString
            );

            ModelMgr.getModelMgr().saveOrUpdateEntity(alignmentBoard);
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
            logger.info( "Read-Up Setting string: {}deserialized, from {}", settingString, alignmentBoard.getId() );

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
            if ( setting.trim().length() == 0 ||
                    Character.isWhitespace( setting.charAt( 0 ) )  ||
                    equalPos == -1 ) {
                continue;
            }
            settingToValue.put( setting.substring( 0, equalPos ), setting.substring( equalPos+1 ) );
        }

        String str = null;
        str = settingToValue.get( GAMMA_SETTING );
        boolean nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            float gamma = Float.parseFloat( str );
            alignmentBoardSettings.setGammaFactor( gamma );
            volumeModel.setGammaAdjustment( gamma );
        }

        str = settingToValue.get( CROP_OUT_LEVEL_SETTING );
        nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            float cropOut = Float.parseFloat( str );
            volumeModel.setCropOutLevel( cropOut );
        }

        str = settingToValue.get( USE_SIGNAL_SETTING );
        nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            Boolean useSignal = Boolean.parseBoolean( str );
            alignmentBoardSettings.setShowChannelData( useSignal );
        }

        str = settingToValue.get( SELECTION_BOUNDS_SETTING );
        nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            Collection<float[]> coordinateSets = new ArrayList<float[]>();
            FloatParseAcceptor floatParseAcceptor = new FloatParseAcceptor( coordinateSets );
            parseTuples(str, 6, floatParseAcceptor);
            CropCoordSet cropCoordSet = new CropCoordSet();

            float[] cropCoordArray = coordinateSets.iterator().next();
            if ( cropCoordArray != null ) {
                cropCoordSet.setCurrentCoordinates( cropCoordArray );
            }
            cropCoordSet.setAcceptedCoordinates( coordinateSets );
            volumeModel.setCropCoords( cropCoordSet );

        }

        str = settingToValue.get( CAMERA_FOCUS_SETTING );
        nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            Collection<double[]> coordinateSets = new ArrayList<double[]>();
            DoubleParseAcceptor doubleParseAcceptor = new DoubleParseAcceptor( coordinateSets );
            parseTuples(str, 3, doubleParseAcceptor);
            if ( coordinateSets.size() >= 1 ) {
                double[] cameraFocusArr = coordinateSets.iterator().next();
                volumeModel.getCamera3d().setFocus(
                        cameraFocusArr[ X_OFFS ], cameraFocusArr[ Y_OFFS ], cameraFocusArr[ Z_OFFS ]
                );
            }
        }

        str = settingToValue.get( CAMERA_ROTATION_SETTING );
        nonEmpty = str != null && str.trim().length() > 0;
        if ( nonEmpty ) {
            Collection<double[]> coordinateSets = new ArrayList<double[]>();
            DoubleParseAcceptor doubleParseAcceptor = new DoubleParseAcceptor( coordinateSets );
            parseTuples(str, 3, doubleParseAcceptor);
            if ( coordinateSets.size() == 3 ) {
                int i = 0;
                for ( double[] coordinateSet: coordinateSets ) {
                    volumeModel.getCamera3d().getRotation().setWithCaution(
                            i++,
                            new UnitVec3(coordinateSet[X_OFFS], coordinateSet[Y_OFFS], coordinateSet[Z_OFFS])
                    );
                }
            }
            else {
                logger.warn(
                        "Invalid number of coordinates deserialized from camera rotation.  " +
                         "Not restoring camera position.  Full settings string {}.",
                        settingsStrings
                );
            }
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
        if (! cropCoordSet.isEmpty() ) {
            builder.append( SELECTION_BOUNDS_SETTING ).append( "=" );
            for (float[] nextCoordSet : cropCoordSet.getAcceptedCoordinates()) {
                appendFloatArray(builder, nextCoordSet);
            }
            // May add on any current (un-ORed) coordinates.
            if ( cropCoordSet.getCurrentCoordinates() != null ) {
                StringBuilder currBuf = new StringBuilder();
                String currCoordStr = currBuf.toString();
                if ( ! builder.toString().contains(currCoordStr) ) {
                    appendFloatArray( currBuf, cropCoordSet.getCurrentCoordinates() );
                    builder.append( currCoordStr );
                }
            }
            builder.append("\n");
        }

        Rotation3d rotation = volumeModel.getCamera3d().getRotation();
        if ( rotation != null ) {
            builder.append( CAMERA_ROTATION_SETTING ).append( "=" );
            for ( int i = 0; i < 3; i++ ) {
                UnitVec3 nextVec = rotation.get(i);
                appendVec3(builder, nextVec);
            }
            builder.append( "\n" );
        }
        else {
            logger.info("Null rotation in Volume Model.");
        }

        Vec3 focus = volumeModel.getCamera3d().getFocus();
        if ( focus != null ) {
            builder.append( CAMERA_FOCUS_SETTING ).append( "=" );
            appendVec3(builder, focus);
            builder.append("\n");
        }

        logger.debug("SETTINGS: {} serialized", builder);
        return builder.toString();
    }

    private void appendVec3(StringBuilder builder, Vec3 nextVec) {
        appendFloatArray(
                builder,
                new float[]{
                        (float) nextVec.getX(), (float) nextVec.getY(), (float) nextVec.getZ()
                }
        );
    }

    private void appendFloatArray(StringBuilder builder, float[] array) {
        builder.append("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(String.format("%f", array[i]));
        }
        builder.append("]");
    }

    /**
     * Parses the input string by breaking it into bracket-delimited sub-strings, then splitting each of those into
     * individual array-of-strings.  These are each handed off to the acceptor to do with as it pleases.
     *
     * @param str input to parse.
     * @param expectedCount expected number of values in each inner string array.
     * @param coordinateSets takes each string-array and interprets as needed.
     */
    private void parseTuples(String str, int expectedCount, CoordTupleAcceptor acceptor) {
        String[] coordSetStrs = str.split( "]" );
        for ( String coordSetStr: coordSetStrs ) {
            if ( coordSetStr.length() == 0 )
                continue;

            String[] coordStrs = coordSetStr.substring( 1 ).split( "," );

            if ( coordStrs.length != expectedCount ) {
                logger.warn(
                        "Did not find sufficient coordinate set serialized.  Expected {}, found {} coords.",
                        expectedCount, coordStrs.length
                );
            }
            else {
                acceptor.accept( coordStrs );
            }

        }
    }

    /**
     * Implement this to help dispose of multi-comma-sep'd arrays of values.
     *
     * @see #parseTuples(String, int, org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.UserSettingSerializer.CoordTupleAcceptor)
     */
    private interface CoordTupleAcceptor {
        void accept( String[] stringArr );
    }

    private class FloatParseAcceptor  implements CoordTupleAcceptor {
        private Collection<float[]> targetCollection;
        public FloatParseAcceptor( Collection<float[]> targetCollection ) {
            this.targetCollection = targetCollection;
        }
        public void accept( String[] coords ) {
            float[] cropCoordArray = new float[ coords.length ];
            int offs = 0;
            for ( String coordStr: coords ) {
                cropCoordArray[ offs++ ] = Float.parseFloat( coordStr );
            }
            targetCollection.add( cropCoordArray );
        }
    }

    private class DoubleParseAcceptor  implements CoordTupleAcceptor {
        private Collection<double[]> targetCollection;
        public DoubleParseAcceptor( Collection<double[]> targetCollection ) {
            this.targetCollection = targetCollection;
        }
        public void accept( String[] coords ) {
            double[] cropCoordArray = new double[ coords.length ];
            int offs = 0;
            for ( String coordStr: coords ) {
                cropCoordArray[ offs++ ] = Double.parseDouble(coordStr);
            }
            targetCollection.add( cropCoordArray );
        }
    }

}