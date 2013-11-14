package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.UnitVec3;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.CropCoordSet;
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
    public static final String CAMERA_DEPTH = "CameraFocus";
    public static final String FOCUS_SETTING = "InGroundFocus";
    public static final String MIN_VOXELS_SETTING = "MinVoxelCutoff";
    public static final String MAX_NEURON_SETTING = "MaxNeuronsCutoff";
    public static final String SAVE_BRIGHTNESS_SETTING = "SaveGammaInTiff";

    public static final int MAX_SERIALIZED_SETTINGS_STR = 65535;

    private SerializationAdapter serializationAdapter;
    private Entity alignmentBoard;

    private Logger logger = LoggerFactory.getLogger( UserSettingSerializer.class );

    /**
     * Convenience method for quickly checking if any alignment board has settings stored with it.  Could help
     * to establish that a board is brand new.
     *
     * @param alignmentBoard what to look at.
     * @return T: found string; F: no settings stored before this call.
     */
    public static boolean settingsExist( Entity alignmentBoard ) {
        return getSettingsString( alignmentBoard ) != null;
    }

    /**
     * Construct with sources/sinks for all data flowing through.
     *
     * @param alignmentBoard entity about which this data exists.
     * @param volumeModel for manipulating 3D settings.
     * @param alignmentBoardSettings for user-click settings.
     */
    public UserSettingSerializer(
            Entity alignmentBoard, VolumeModel volumeModel, AlignmentBoardSettings alignmentBoardSettings ) {
        serializationAdapter = new DirectStateSerializationAdapter( volumeModel, alignmentBoardSettings );
        this.alignmentBoard = alignmentBoard;
        logger.debug("Serializer: with VolumeModel {}, with cam3D {}.", volumeModel, volumeModel.getCamera3d() );
    }

    public UserSettingSerializer(
        SerializationAdapter serializationAdapter
    ) {
        this.serializationAdapter = serializationAdapter;
    }

    /**
     * Save back things that user will wish to see restored to former glory after cycling console.
     *
     * @see #deserializeSettings()
     */
    public synchronized void serializeSettings() {
        try {
            String settingsString = getSettingsString();
            // This excessive length
            if ( settingsString.length() > MAX_SERIALIZED_SETTINGS_STR ) {
                logger.warn( "Abandoning the serialized string {}.", settingsString );
                // Write back.
                ModelMgr.getModelMgr().setOrUpdateValue(alignmentBoard, EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS, "");
                settingsString = "";
            }
            logger.info( "Save-back Setting string: {}.", settingsString );

            // Write back.
            ModelMgr.getModelMgr().setOrUpdateValue(alignmentBoard, EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS, settingsString);
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    /**
     * Restore-to-former-glory, things that the user had in place during the previous cycle.
     * @see #serializeSettings()
     */
    public synchronized void deserializeSettings() {
        try {
            String settingString = getSettingsString( this.alignmentBoard );
            logger.info( "Read-Up Setting string: {} deserialized, from {}", settingString, alignmentBoard.getId() );

            if ( settingString != null ) {
                parseSettings(settingString);
            }

        } catch ( Exception ex ) {
            SessionMgr.getSessionMgr().handleException( ex );
        }

    }

    private static String getSettingsString( Entity alignmentBoard ) {
        // Read up.
        String settingString =
                alignmentBoard.getValueByAttributeName(
                        EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS
                );
        return settingString;
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
        boolean nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            float gamma = Float.parseFloat( str );
            serializationAdapter.setGammaAdjustment( gamma );
        }

        str = settingToValue.get( CROP_OUT_LEVEL_SETTING );
        nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            float cropOut = Float.parseFloat( str );
            serializationAdapter.setCropOutLevel(cropOut);
        }

        str = settingToValue.get( USE_SIGNAL_SETTING );
        nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            Boolean useSignal = Boolean.parseBoolean(str);
            serializationAdapter.setShowChannelData(useSignal);
        }

        str = settingToValue.get( SELECTION_BOUNDS_SETTING );
        nonEmpty = nonEmpty(str);
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
            serializationAdapter.setCropCoords( cropCoordSet );

        }

        str = settingToValue.get(FOCUS_SETTING);
        nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            Collection<double[]> coordinateSets = new ArrayList<double[]>();
            DoubleParseAcceptor doubleParseAcceptor = new DoubleParseAcceptor( coordinateSets );
            parseTuples(str, 3, doubleParseAcceptor);
            if ( coordinateSets.size() >= 1 ) {
                double[] cameraFocusArr = coordinateSets.iterator().next();
                serializationAdapter.setFocus(
                        cameraFocusArr
                );
            }
        }

        str = settingToValue.get(CAMERA_DEPTH);
        nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            Collection<double[]> coordinateSets = new ArrayList<double[]>();
            DoubleParseAcceptor doubleParseAcceptor = new DoubleParseAcceptor( coordinateSets );
            parseTuples(str, 3, doubleParseAcceptor);
            if ( coordinateSets.size() >= 1 ) {
                double[] cameraFocusArr = coordinateSets.iterator().next();
                serializationAdapter.setCameraDepth(cameraFocusArr);
            }
        }

        str = settingToValue.get( CAMERA_ROTATION_SETTING );
        nonEmpty = nonEmpty(str);
        if ( nonEmpty ) {
            Collection<double[]> coordinateSets = new ArrayList<double[]>();
            DoubleParseAcceptor doubleParseAcceptor = new DoubleParseAcceptor( coordinateSets );
            parseTuples(str, 3, doubleParseAcceptor);
            if ( coordinateSets.size() == 3 ) {
                serializationAdapter.setRotation( coordinateSets );
            }
            else {
                logger.warn(
                        "Invalid number of coordinates deserialized from camera rotation.  " +
                                "Not restoring camera position.  Full settings string {}.",
                        settingsStrings
                );
            }
        }

        extractNeuronConstraint(MIN_VOXELS_SETTING, settingToValue, new ConstraintSetter() {
            @Override
            public void setConstraint( long value ) {
                serializationAdapter.setMinimumVoxelCount(value);
            }
        });

        extractNeuronConstraint(MAX_NEURON_SETTING, settingToValue, new ConstraintSetter() {
            @Override
            public void setConstraint( long value ) {
                serializationAdapter.setMaximumNeuronCount(value);
            }
        });

        str = settingToValue.get( SAVE_BRIGHTNESS_SETTING );
        nonEmpty = nonEmpty( str );
        if ( nonEmpty ) {
            try {
                serializationAdapter.setSaveColorBrightness( Boolean.parseBoolean( str ) );
            } catch ( Exception ex ) {
                logger.warn(
                        "Invalid boolean setting for saving of brightness with color tiffs {}.",
                        str
                );
            }
        }

    }

    /** Sets a neuron-cutoff constraint.  Here for purpose of non-redundancy. */
    private void extractNeuronConstraint(
            String settingString, Map<String, String> settingToValue, ConstraintSetter setter
    ) {
        boolean nonEmpty;
        String str = settingToValue.get( settingString );
        nonEmpty = nonEmpty( str );
        if ( nonEmpty ) {
            try {
                Long constraint = Long.parseLong( str );
                setter.setConstraint(constraint);
            } catch ( Exception ex ) {
                logger.warn(
                        "Invalid min voxel count cuttoff of {} stored.  Ignoring value.",
                        str
                );
            }
        }
    }

    /** Quick method to test whether setting is empty. */
    private boolean nonEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    String getSettingsString() {
        // Locate all the settings to be saved.

        // Save back all of interest.
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(GAMMA_SETTING + "=%f", serializationAdapter.getGammaAdjustment()));
        builder.append("\n");
        builder.append(String.format(CROP_OUT_LEVEL_SETTING + "=%f", serializationAdapter.getCropOutLevel()));
        builder.append("\n");
        builder.append(USE_SIGNAL_SETTING).append("=");
        builder.append(serializationAdapter.isShowChannelData());
        builder.append("\n");

        CropCoordSet cropCoordSet = serializationAdapter.getCropCoords();
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

        Rotation3d rotation = serializationAdapter.getRotation();
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

        Vec3 focusInGround = serializationAdapter.getFocus();
        if ( focusInGround != null ) {
            builder.append(FOCUS_SETTING).append( "=" );
            appendVec3(builder, focusInGround );
            builder.append("\n");
        }

        Vec3 depth = serializationAdapter.getCameraDepth();
        if ( depth != null ) {
            builder.append(CAMERA_DEPTH).append( "=" );
            appendVec3(builder, depth);
            builder.append("\n");
        }

        builder.append( MIN_VOXELS_SETTING ).append("=").append(serializationAdapter.getMinimumVoxelCount()).append("\n");

        builder.append( MAX_NEURON_SETTING ).append("=").append(serializationAdapter.getMaximumNeuronCount()).append("\n");

        builder.append( SAVE_BRIGHTNESS_SETTING ).append("=").append(serializationAdapter.isSaveColorBrightness()).append("\n");

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

    /** Convenience interface to leverage a single method with lots of commonality for two settings. */
    static interface ConstraintSetter {
        public void setConstraint( long value );
    }

    public static interface SerializationAdapter {
        // Serialization
        float getGammaAdjustment();                 // VolumeModel, alignmentBoardSettings as getGammaFactor()
        float getCropOutLevel();                    // VolumeModel
        long getMinimumVoxelCount();                // AlignmentBoardSettings
        long getMaximumNeuronCount();               // AlignmentBoardSettings
        Rotation3d getRotation();                   // VolumeModel.getCamera3d()
        boolean isShowChannelData();                // AlignmentBoardSettings
        Vec3 getCameraDepth();                      // VolumeModel
        Vec3 getFocus();                            // VolumeModel.getCamera3d()
        CropCoordSet getCropCoords();               // VolumeModel
        boolean isSaveColorBrightness();            // VolumeModel

        // Deserialization
        void setGammaAdjustment( float gamma );     // VolumeModel, alignmentBoardSettings as setGammaFactor(float)
        void setCropOutLevel(float level);          // VolumeModel
        void setMinimumVoxelCount( long count );    // VolumeModel
        void setMaximumNeuronCount( long count );   // VolumeModel
        void setCropCoords( CropCoordSet coordSet );// VolumeModel.setCropCoordinates(cropCoordArray), VolumeModel.setAcceptedCoordinates().
        void setShowChannelData(boolean show);      // AlignmentBoardSettings
        void setFocus( double[] focus );            // VolumeModel.getCamera3d().setFocus()
        void setCameraDepth(double[] cameraFocusArr);  // Complicated...
        void setRotation( Collection<double[]> rotation );  // volumeModel.getCamera3d().getRotation().setWithCaution(
        void setSaveColorBrightness( boolean b );   // VolumeModel
   }

}