package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class FileBasedBlockTiffOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedBlockTiffOctreeLoadAdapter.class);

    // Metadata: file location required for local system as mount point.
    private final File baseFolder;

    public FileBasedBlockTiffOctreeLoadAdapter(TileFormat tileFormat, URI volumeBaseURI) {
        super(tileFormat, volumeBaseURI);
        this.baseFolder = new File(volumeBaseURI);
    }

    @Override
    public void loadMetadata() {
        FileBasedOctreeMetadataSniffer metadataSniffer = new FileBasedOctreeMetadataSniffer(baseFolder, getTileFormat());
        metadataSniffer.retrieveMetadata();
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws MissingTileException, TileLoadError {
        final Path octreeFilePath = FileBasedOctreeMetadataSniffer.getOctreeFilePath(tileIndex, getTileFormat());
        if (octreeFilePath == null) {
            return null;
        }
        // (though TIFF requires seek, right?)
        // Compute octree path from Raveler-style tile indices
        File folder = new File(baseFolder, octreeFilePath.toString());

        // Compute local z slice
        int zoomScale = (int) Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = getTileFormat().getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice;

        if (axisIx == 1) {
            // Raveller y is flipped so flip when slicing in Y (right?)
            relativeSlice = tileDepth - (absoluteSlice % tileDepth) - 1;
        } else {
            relativeSlice = absoluteSlice % tileDepth;
        }

        // Some of the decoders may be null
        LOG.info("Load slice {} from {} for tile {}", relativeSlice, folder, tileIndex);
        ImageDecoder[] decoders = createImageDecoders(folder, tileIndex.getSliceAxis(), true, getTileFormat().getChannelCount());

        return loadSlice(relativeSlice, decoders, getTileFormat().getChannelCount());
    }

    public TextureData2d loadSlice(int relativeZ, ImageDecoder[] decoders, int channelCount) throws TileLoadError {
        // 2 - decode image
        RenderedImage channels[] = new RenderedImage[channelCount];
        boolean emptyChannel = false;
        for (int c = 0; c < channelCount; ++c) {
            if (decoders[c] == null)
                emptyChannel = true;
        }
        if (emptyChannel) {
            return null;
        } else {
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
            }

            TextureData2d result = null;
            // My texture wrapper implementation
            TextureData2d tex = new TextureData2d();
            tex.loadRenderedImage(composite);
            result = tex;
            return result;
        }
    }

    public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis)
            throws MissingTileException, TileLoadError {
        return createImageDecoders(folder, axis, false, getTileFormat().getChannelCount());
    }

    private static ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders, int channelCount)
            throws MissingTileException, TileLoadError {
        return createImageDecoders(folder, axis, acceptNullDecoders, channelCount, false);
    }

    private static ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders, int channelCount, boolean fileToMemory)
            throws MissingTileException, TileLoadError {
        String tiffBase = FileBasedOctreeMetadataSniffer.getTiffBase(axis);
        ImageDecoder decoders[] = new ImageDecoder[channelCount];
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();

        for (int c = 0; c < channelCount; ++c) {
            File tiff = new File(folder, FileBasedOctreeMetadataSniffer.getFilenameForChannel(tiffBase, c));
            LOG.debug("Load channel TIFF from {}", tiff);
            if (requestedTiffs.length() > 0) {
                requestedTiffs.append("; ");
            }
            requestedTiffs.append(tiff);
            if (!tiff.exists()) {
                if (acceptNullDecoders) {
                    if (missingTiffs.length() > 0) {
                        missingTiffs.append(", ");
                    }
                    missingTiffs.append(tiff);
                } else {
                    throw new MissingTileException("Putative tiff file: " + tiff);
                }
            } else {
                try {
                    boolean useUrl = false;
                    if (useUrl) { // So SLOW
                        // test URL stream vs (seekable) file stream
                        URL url = tiff.toURI().toURL();
                        InputStream inputStream = url.openStream();
                        decoders[c] = ImageCodec.createImageDecoder("tiff", inputStream, null);
                    } else {
                        SeekableStream s = null;
                        if (fileToMemory) {
                            Path path = Paths.get(tiff.getAbsolutePath());
                            byte[] data = Files.readAllBytes(path);
                            s = new ByteArraySeekableStream(data);
                        } else {
                            LOG.info("Opening {}", tiff);
                            s = new FileSeekableStream(tiff);
                        }
                        decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
                    }
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
            }
        }
        if (missingTiffs.length() > 0) {
            LOG.info("All requested tiffs: " + requestedTiffs);
            LOG.info("Putative tiff file(s): " + missingTiffs + " not found.  Padding with zeros.");
        }
        return decoders;
    }

}
