package org.janelia.horta.omezarr;

import org.janelia.horta.blocks.OmeZarrBlockTileSource;

public interface OmeZarrReaderProgressObserver {
    void update(OmeZarrBlockTileSource source, String progress);
}
