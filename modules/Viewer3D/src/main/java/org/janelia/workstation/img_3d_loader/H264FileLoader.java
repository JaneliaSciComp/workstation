package org.janelia.workstation.img_3d_loader;

import org.janelia.workstation.ffmpeg.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file loader to handle H.264 files for the workstation.
 * 
 * @author fosterl
 */
public class H264FileLoader extends AbstractVolumeFileLoader {

    private final Logger logger = LoggerFactory.getLogger(H264FileLoader.class);
    
    private final H26nFileLoadHelper helper = new H26nFileLoadHelper();
    
    @Override
    public void loadVolumeFile(String filename) throws Exception {
        setUnCachedFileName(filename);        
        FFMpegLoader movie = new FFMpegLoader(filename);
        try {
            ByteGatherAcceptor acceptor = populateAcceptor(movie);
            helper.captureData(acceptor, this);
            //dumpMeta(acceptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFramesAsPPM(String filename) {
        FFMpegLoader reader = new FFMpegLoader(filename);
        FFMPGByteAcceptor acceptor = new PPMFileAcceptor();
        accept(reader, acceptor);
//        FFMpegLoader movie = new FFMpegLoader(filename);
//        try {
//            movie.start();
//            movie.grab();
//            ImageStack image = movie.getImage();
//            int frames = image.getNumFrames();
//            PPMFileAcceptor acceptor = new PPMFileAcceptor();
//            for (int i = 0; i < frames; i++ ) {
//                acceptor.setFrameNum(i);
//                movie.saveFrame(i, acceptor);
//            }
//            movie.release();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
    
    /**
     * Save the frames into an acceptor, and hand back the populated acceptor.
     * 
     * @param movie generated around an input file.
     * @return the acceptor.
     * @throws Exception 
     */
    private ByteGatherAcceptor populateAcceptor(FFMpegLoader movie) throws Exception {
        movie.start();
        movie.grab();
        ImageStack image = movie.getImage();
        int frames = image.getNumFrames();
        
        ByteGatherAcceptor acceptor = new ByteGatherAcceptor();
        for (int i = 0; i < frames; i++ ) {
            movie.saveFrame(i, acceptor);
        }
        movie.release();
        
        //helper.dumpMeta(acceptor);
        return acceptor;
    }
    
    private void accept(FFMpegLoader reader, FFMPGByteAcceptor acceptor) {
        try {
            reader.start();
            reader.grab();

            ImageStack image = reader.getImage();
            int maxFrames = image.getNumFrames();
            int startingFrame = 0;
            int endingFrame = startingFrame + maxFrames;
            for (int i = startingFrame; i < endingFrame; i++) {
                acceptor.setPixelBytes(image.getBytesPerPixel());
                acceptor.setFrameNum(i);
                reader.saveFrame(i, acceptor);
            }
            reader.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
