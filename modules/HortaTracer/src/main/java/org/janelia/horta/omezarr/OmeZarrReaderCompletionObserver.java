package org.janelia.horta.omezarr;

import org.janelia.horta.blocks.OmeZarrBlockTileSource;

public interface OmeZarrReaderCompletionObserver {
    void complete(OmeZarrBlockTileSource source);
}
