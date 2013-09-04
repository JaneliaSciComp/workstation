package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture.TextureChunk;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/21/13
 * Time: 12:52 PM
 *
 * Breaks an array of texture bytes, given its other characteristics, into the right set of chunks for its use.
 * Said chunks will have forward links between them (no back links).
 *
 * @deprecated Now downsampling textures to avoid their being large enough to require chunk-wise handling.
 */
public class TextureChunkDigester {

    /** This trivial builder method will make a single chunk out of all data. */
    public TextureChunk getTextureList( byte[] rawBytes, int sX, int sY, int sZ, int bytesPerEntry ) {
        TextureChunk rtnVal = new TextureChunk();
        rtnVal.setBytesPerCell(bytesPerEntry);
        rtnVal.setStartX(0);
        rtnVal.setStartY(0);
        rtnVal.setStartZ(0);
        rtnVal.setEndX(sX);
        rtnVal.setEndY(sY);
        rtnVal.setEndZ(sZ);
        rtnVal.setTextureData( rawBytes );
        return rtnVal;
    }

    /** This similarly-trivial method makes a chunk with all params given, and links accordingly. */
    public TextureChunk getNextTextureChunk(
            byte[] rawBytes,
            int startX, int startY, int startZ,
            int endX, int endY, int endZ,
            int bytesPerEntry,
            TextureChunk prevChunk
    ) {
        TextureChunk rtnVal = new TextureChunk();
        rtnVal.setStartX( startX );
        rtnVal.setStartY( startY );
        rtnVal.setStartZ( startZ );

        rtnVal.setEndX( endX );
        rtnVal.setEndY( endY );
        rtnVal.setEndZ( endZ );

        rtnVal.setTextureData( rawBytes );

        rtnVal.setBytesPerCell( bytesPerEntry );

        if ( prevChunk != null ) {
            prevChunk.setNextChunk( rtnVal );
        }

        return rtnVal;
    }
}
