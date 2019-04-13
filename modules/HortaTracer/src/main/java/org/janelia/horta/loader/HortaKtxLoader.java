package org.janelia.horta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;
import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.blocks.KtxBlockLoadRunner;
import org.janelia.horta.render.NeuronMPRenderer;

/**
 *
 * @author Christopher Bruns
 */
public class HortaKtxLoader implements FileTypeLoader {

    private final NeuronMPRenderer renderer;

    public HortaKtxLoader(NeuronMPRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public boolean supports(DataSource source) {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("KTX")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException {
        final KtxBlockLoadRunner loader = new KtxBlockLoadRunner(source);
        loader.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (loader.state != KtxBlockLoadRunner.State.LOADED) {
                    return;
                }
                TetVolumeActor parentActor = TetVolumeActor.getInstance();
                parentActor.addChild(loader.blockActor);
                if (!renderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                    parentActor.setBrightnessModel(renderer.getBrightnessModel());
                    renderer.addVolumeActor(parentActor);
                }
            }
        });
        loader.run();
        return loader.state == KtxBlockLoadRunner.State.LOADED;
    }

}
