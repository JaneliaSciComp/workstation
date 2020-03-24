/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.img_3d_loader;

import org.janelia.workstation.ffmpeg.*;
import org.janelia.workstation.img_3d_loader.AbstractVolumeFileLoader;
import org.janelia.workstation.img_3d_loader.H26nFileLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file loader to handle H.265 files for the workstation.
 * 
 * @author fosterl
 */
public class H265FileLoader extends AbstractVolumeFileLoader {

    private final Logger logger = LoggerFactory.getLogger(H265FileLoader.class);
    
    private final H26nFileLoadHelper helper = new H26nFileLoadHelper();
    
    @Override
    public void loadVolumeFile(String filename) throws Exception {
        setUnCachedFileName(filename);
        H5JLoader reader = new H5JLoader(filename);
        setChannelCount( 3 );
        try {
            ByteGatherAcceptor acceptor = gatherBytes(reader);
            helper.captureData(acceptor, this);
            //dumpMeta(acceptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        reader.close();
    }
    
    public void saveFramesAsPPM(String filename) {
        H5JLoader reader = new H5JLoader(filename);
        FFMPGByteAcceptor acceptor = new PPMFileAcceptor();
        accept(reader, acceptor);
    }

    /**
     * Save the frames into an acceptor, and hand back the populated acceptor.
     * 
     * @param reader generated around an input file.
     * @return the acceptor.
     * @throws Exception 
     */
    private ByteGatherAcceptor gatherBytes(H5JLoader reader) throws Exception {
        ByteGatherAcceptor acceptor = new ByteGatherAcceptor();
        accept(reader, acceptor);
        //helper.dumpMeta(acceptor);
        return acceptor;
    }
    
    private void accept(H5JLoader reader, FFMPGByteAcceptor acceptor) {
        try {
            ImageStack image = reader.extractAllChannels();
            int maxFrames = image.getNumFrames();
            int startingFrame = 0;
            AcceptorAdapter acceptorAdapter = new AcceptorAdapter(acceptor);
            int endingFrame = startingFrame + maxFrames;            
            for (int i = startingFrame; i < endingFrame; i++) {
                acceptor.setPixelBytes(image.getBytesPerPixel());
                acceptor.setFrameNum(i);
                reader.saveFrame(i, acceptorAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class AcceptorAdapter implements H5JLoader.DataAcceptor {
        private FFMPGByteAcceptor wrappedAcceptor;
        public AcceptorAdapter( FFMPGByteAcceptor wrappedAcceptor) {
            this.wrappedAcceptor = wrappedAcceptor;
        }

        @Override
        public void accept(byte[] data, int linesize, int width, int height) {
            wrappedAcceptor.accept(data,linesize,width,height);
        }
        
        
    }
    
}
