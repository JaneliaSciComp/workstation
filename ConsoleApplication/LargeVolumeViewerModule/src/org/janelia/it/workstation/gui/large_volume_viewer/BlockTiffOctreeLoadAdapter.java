package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

import com.sun.media.jai.codec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import static org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID;

/*
 * Loader for large volume viewer format negotiated with Nathan Clack
 * March 21, 2013.
 * 512x512 tiles
 * Z-order octree folder layout
 * uncompressed tiff stack for each set of slices
 * named like "default.0.tif" for channel zero
 * 16-bit unsigned int
 * intensity range 0-65535
 */
public class BlockTiffOctreeLoadAdapter 
extends AbstractTextureLoadAdapter 
{
	private static final Logger log = LoggerFactory.getLogger(BlockTiffOctreeLoadAdapter.class);
    private static final CategoryString LIX_CATEGORY_STRING = new CategoryString("loadTileIndexToRam");
    private static final CategoryString LVV_SESSION_CATEGORY_STRING = new CategoryString("openFolder");

	// Metadata: file location required for local system as mount point.
	private File topFolder;
    // Metadata: file location required for remote system.
    private String remoteBasePath;
    // Metadata: different folders could be opened during a user's session.
    private Long folderOpenTimestamp = null;
    
	public LoadTimer loadTimer = new LoadTimer();
    
	public BlockTiffOctreeLoadAdapter()
	{
		tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
		// Report performance statistics when program closes
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				loadTimer.report();
			}
		});
	}
	
    public void setRemoteBasePath(String basePath) {
        remoteBasePath = basePath;
    }
    
	public File getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(File topFolder) 
	throws DataSourceInitializeException
	{
		this.topFolder = topFolder;        
        OctreeMetadataSniffer sniffer = new OctreeMetadataSniffer(topFolder, tileFormat);
        sniffer.setRemoteBasePath(remoteBasePath);
        folderOpenTimestamp = new Date().getTime();
		sniffer.sniffMetadata(topFolder);
        
        SessionMgr.getSessionMgr().logGenericToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_SESSION_CATEGORY_STRING, 
                new ActionString(remoteBasePath + ":" + folderOpenTimestamp)
        );
		// Don't launch pre-fetch yet.
		// That must occur AFTER volume initialized signal is sent.
	}

	@Override
	public TextureData2dGL loadToRam(TileIndex tileIndex)
			throws TileLoadError, MissingTileException 
	{
        return loadToRam(tileIndex, true);
	}

	public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift)
			throws TileLoadError, MissingTileException 
	{
		// Create a local load timer to measure timings just in this thread
		LoadTimer localLoadTimer = new LoadTimer();
        long startTime = System.nanoTime();
		localLoadTimer.mark("starting slice load");
        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, zOriginNegativeShift);
        if (octreeFilePath == null) {
            return null;
        }
		// TODO - generalize to URL, if possible
		// (though TIFF requires seek, right?)
		// Compute octree path from Raveler-style tile indices
		File folder = new File(topFolder, octreeFilePath.toString());
        
		// TODO for debugging, show file name for X tiles
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int axisIx = tileIndex.getSliceAxis().index();
		int tileDepth = tileFormat.getTileSize()[axisIx];
		int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
		int relativeSlice = absoluteSlice % tileDepth;
		// Raveller y is flipped so flip when slicing in Y (right?)
		if (axisIx == 1)
			relativeSlice = tileDepth - relativeSlice - 1;
		
        // Calling this with "true" means I, the caller, accept that the array
        // returned may have one or more nulls in it.
		ImageDecoder[] decoders = createImageDecoders(folder, tileIndex.getSliceAxis(), true, tileFormat.getChannelCount());
		
		// log.info(tileIndex + "" + folder + " : " + relativeSlice);
		TextureData2dGL result = loadSlice(relativeSlice, decoders, tileFormat.getChannelCount());
		localLoadTimer.mark("finished slice load");
        
        final double elapsedMs = (double) (System.nanoTime() - startTime) / 1000000.0;
        if (result != null) {
            final ActionString actionString = new ActionString(
                     folderOpenTimestamp + ":" + relativeSlice + ":" + tileIndex.toString() + ":elapsed_ms=" + elapsedMs
            );
            SessionMgr.getSessionMgr().logToolEvent(
                    LVV_LOGSTAMP_ID,
                    LIX_CATEGORY_STRING,
                    actionString,
                    elapsedMs,
                    999.0,
                    false
            );
        }


		loadTimer.putAll(localLoadTimer);
		return result;
	}

	public static TextureData2dGL loadSlice(int relativeZ, ImageDecoder[] decoders, int channelCount)
	throws TileLoadError
    {
		// 2 - decode image
		RenderedImage channels[] = new RenderedImage[channelCount];
        boolean emptyChannel = false;
        for (int c = 0; c < channelCount; ++c) {
            if (decoders[c] == null)
                emptyChannel = true;
        }
        if (emptyChannel) {
            return null;
        }
        else {
            for (int c = 0; c < channelCount; ++c) {
                try {
                    ImageDecoder decoder = decoders[c];
                    assert (relativeZ < decoder.getNumPages());
                    channels[c] = decoder.decodeAsRenderedImage(relativeZ);
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
                // localLoadTimer.mark("loaded slice, channel "+c);
            }
            // Combine channels into one image
            RenderedImage composite = channels[0];
            if (channelCount > 1) {
                try {
                ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
                for (int c = 0; c < channelCount; ++c) {
                    pb.addSource(channels[c]);
                }
                composite = JAI.create("bandmerge", pb);
                } catch (NoClassDefFoundError exc) {
                    exc.printStackTrace();
                    return null;
                }
                // localLoadTimer.mark("merged channels");
            }

            TextureData2dGL result = null;
            // My texture wrapper implementation
            TextureData2dGL tex = new TextureData2dGL();
            tex.loadRenderedImage(composite);
            result = tex;
            return result;
        }
	}

	// TODO - cache decoders if folder has not changed
	public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis)
			throws MissingTileException, TileLoadError
	{
		return createImageDecoders(folder, axis, false, tileFormat.getChannelCount());
	}

    public static ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders, int channelCount)
            throws MissingTileException, TileLoadError
    {
        return createImageDecoders(folder, axis, acceptNullDecoders, channelCount, false);
    }

    public static ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders, int channelCount, boolean fileToMemory)
			throws MissingTileException, TileLoadError 
	{
        String tiffBase = OctreeMetadataSniffer.getTiffBase(axis);
		ImageDecoder decoders[] = new ImageDecoder[channelCount];
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();

		for (int c = 0; c < channelCount; ++c) {
			File tiff = new File(folder, OctreeMetadataSniffer.getFilenameForChannel(tiffBase, c));
            if ( requestedTiffs.length() > 0 ) {
                requestedTiffs.append("; ");
            }
            requestedTiffs.append(tiff);
			if (! tiff.exists()) {
                if ( acceptNullDecoders ) {
                    if ( missingTiffs.length() > 0 ) {
                        missingTiffs.append(", ");
                    }
                    missingTiffs.append( tiff );
                }
                else {
    				throw new MissingTileException("Putative tiff file: " + tiff);
                }
            }
            else {
                try {
                    boolean useUrl = false;
                    if (useUrl) { // So SLOW
                        // test URL stream vs (seekable) file stream
                        URL url = tiff.toURI().toURL();
                        InputStream inputStream = url.openStream();
                        decoders[c] = ImageCodec.createImageDecoder("tiff", inputStream, null);
                    } else {
                        SeekableStream s=null;
                        if (fileToMemory) {
                            Path path = Paths.get(tiff.getAbsolutePath());
                            byte[] data = Files.readAllBytes(path);
                            s = new ByteArraySeekableStream(data);
                        } else {
                            s = new FileSeekableStream(tiff);
                        }
                        decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
                    }
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
            }
		}
        if ( missingTiffs.length() > 0 ) {
            log.debug("All requested tiffs: " + requestedTiffs);
            log.debug( "Putative tiff file(s): " + missingTiffs + " not found.  Padding with zeros." );
        }
		return decoders;
	}

}
