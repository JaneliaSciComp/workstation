package org.janelia.it.workstation.gui.alignment_board.loader;

import org.janelia.it.workstation.gui.alignment_board_viewer.MaskChanStreamSourceI;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.FileStats;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.VolumeConsistencyChecker;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 * <p/>
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class MaskChanMultiFileLoader {

    public static final int NUM_SEGMENTS = 16;
    public static final int N_THREADS = 16;
    private boolean checkForConsistency = false;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;
    private FileStats fileStats;

    private VolumeConsistencyChecker checker = new VolumeConsistencyChecker();
    private boolean enforcePadding = true;
    private boolean dimWriteback;

    private final Logger logger = LoggerFactory.getLogger(MaskChanMultiFileLoader.class);

    /**
     * Anything on this list could receive data from the files under study.
     */
    public void setAcceptors(Collection<MaskChanDataAcceptorI> acceptors) {
        maskAcceptors = new ArrayList<>();
        channelAcceptors = new ArrayList<>();

        for (MaskChanDataAcceptorI acceptor : acceptors) {
            if (acceptor.getAcceptableInputs().equals(MaskChanDataAcceptorI.Acceptable.channel)) {
                channelAcceptors.add(acceptor);
            }
            if (acceptor.getAcceptableInputs().equals(MaskChanDataAcceptorI.Acceptable.mask)) {
                maskAcceptors.add(acceptor);
            }
            if (acceptor.getAcceptableInputs().equals(MaskChanDataAcceptorI.Acceptable.both)) {
                channelAcceptors.add(acceptor);
                maskAcceptors.add(acceptor);
            }
        }
    }

    /**
     * This is the master-method that tells all the single-file-loaders to read their data into the common volume.
     *
     * @param bean         info read is applicable to this
     * @param streamSource has the compression/ray data and the channel or intensity data.
     * @throws Exception thrown by called methods.
     */
    public void read(final RenderableBean bean, final MaskChanStreamSourceI streamSource)
            throws Exception {
        logger.debug("Read called.");

        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        List<Future<Void>> followUps = new ArrayList<>();

        ChannelSingleFileLoader channelLoader = new ChannelSingleFileLoader(bean);
        InputStream channelInputStream = streamSource.getChannelInputStream();
        ChannelSingleFileLoader.ChannelDataBean channelDataBean = null;
        if (channelInputStream != null) {
            channelDataBean = channelLoader.readChannelData(
                    channelInputStream,
                    channelAcceptors.size() > 0
            );
            channelInputStream.close();
        }
        final ChannelSingleFileLoader.ChannelDataBean finalChannelDataBean = channelDataBean;

        for (int slabNo = 0; slabNo < NUM_SEGMENTS; slabNo++) {
            final int finalSlabNo = slabNo;
            Callable<Void> segmentTask = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    MaskSingleFileLoader singleFileLoader = new MaskSingleFileLoader(maskAcceptors, channelAcceptors, bean, fileStats);
                    singleFileLoader.setApplicableSegment(finalSlabNo, NUM_SEGMENTS);

                    // Here, may override the pad-out to ensure resulting volume exactly matches the original space.
                    if (!enforcePadding) {
                        singleFileLoader.setAxialLengthDivisibility(1);
                    }
                    if (dimWriteback) {
                        singleFileLoader.setIntensityDivisor(5);
                    } else {
                        singleFileLoader.setIntensityDivisor(1);
                    }
                    try (InputStream maskInputStream = streamSource.getMaskInputStream()) {
                        singleFileLoader.read(maskInputStream, finalChannelDataBean);
                        
                        // Accumulate information for final sanity check.
                        if (isCheckForConsistency()) {
                            checker.accumulate(
                                    bean.getTranslatedNum(), singleFileLoader.getDimensions(), singleFileLoader.getChannelMetaData()
                            );
                        }
                    }
                    return null;
                }
            };
            followUps.add(executorService.submit(segmentTask));
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);

        Exception lastException = null;
        for (Future<Void> future : followUps) {
            try {
                future.get();
            }
            catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
                lastException = ex;
            }
        }
        if (lastException != null) {
            throw lastException;
        }

        logger.debug("Read complete.");
    }

    /**
     * Call this after all reading has been completed.
     */
    public void close() {
        if (isCheckForConsistency()) {
            checker.report(true, logger);
        }
        if (maskAcceptors != null) {
            for (MaskChanDataAcceptorI acceptor : maskAcceptors) {
                acceptor.endData(logger);
            }
        }
        if (channelAcceptors != null) {
            for (MaskChanDataAcceptorI acceptor : channelAcceptors) {
                acceptor.endData(logger);
            }
        }
    }

    public void setEnforcePadding(boolean enforcePadding) {
        this.enforcePadding = enforcePadding;
    }

    public boolean isCheckForConsistency() {
        return checkForConsistency;
    }

    public void setCheckForConsistency(boolean checkForConsistency) {
        this.checkForConsistency = checkForConsistency;
    }

    public void setDimWriteback(boolean dimWriteback) {
        this.dimWriteback = dimWriteback;
    }

    public void setFileStats(FileStats fileStats) {
        this.fileStats = fileStats;
    }
}
