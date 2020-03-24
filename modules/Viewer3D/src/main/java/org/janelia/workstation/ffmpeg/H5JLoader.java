package org.janelia.workstation.ffmpeg;
// Used for testing outside of the workstation
//package ffmpeg;

import ch.systemsx.cisd.hdf5.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class H5JLoader
{
    private static final String PAD_RIGHT_ATTRIB = "pad_right";
    private static final String PAD_BOTTOM_ATTRIB = "pad_bottom";
    private static final String CHANNELS_QUERY_PATH = "/Channels";

    private String _filename;
    private IHDF5Reader _reader;
    private ImageStack _image;
    
    public H5JLoader(String filename) {
        this._filename = filename;
        IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
        conf.performNumericConversions();
        _reader = conf.reader();
    }

    public void close() throws Exception {
        _reader.close();
    }

    public int numberOfChannels() {
        return _reader.object().getAllGroupMembers(CHANNELS_QUERY_PATH).size();
    }

    public List<String> channelNames() { return _reader.object().getAllGroupMembers(CHANNELS_QUERY_PATH); }

    public ImageStack extractAllChannels() {
        _image = new ImageStack();

        List<String> channels = channelNames();
        for (ListIterator<String> iter = channels.listIterator(); iter.hasNext(); )
        {
            String channel_id = iter.next();
            try
            {
                ImageStack frames = extract(channel_id);
                _image.merge( frames );
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return _image;
    }

    public List<ImageStack> extractAllChannelsAsList() {
        _image = new ImageStack();
        List<String> channels = channelNames();
        List<ImageStack> channelImageStackList=new ArrayList<>();
        for (ListIterator<String> iter = channels.listIterator(); iter.hasNext(); )
        {
            String channel_id = iter.next();
            try
            {
                ImageStack frames = extract(channel_id);
                channelImageStackList.add(frames);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return channelImageStackList;
    }

    public ImageStack extract(String channelID) throws Exception
    {
        IHDF5OpaqueReader channel = _reader.opaque();
        byte[] data = channel.readArray(CHANNELS_QUERY_PATH + "/" + channelID);

        FFMpegLoader movie = new FFMpegLoader(data);
        movie.start();
        movie.grab();
        ImageStack stack = movie.getImage();

        extractAttributes();
        
        return stack;
    }

    private void extractAttributes() {
        IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(_filename);
        conf.performNumericConversions();
        IHDF5Reader ihdf5reader = conf.reader();
        if (ihdf5reader.object().hasAttribute(CHANNELS_QUERY_PATH, PAD_BOTTOM_ATTRIB)) {
            IHDF5LongReader ihdf5LongReader = ihdf5reader.int64();
            final int paddingBottom = (int) ihdf5LongReader.getAttr(CHANNELS_QUERY_PATH, PAD_BOTTOM_ATTRIB);
            _image.setPaddingBottom(paddingBottom);
        } else {
            _image.setPaddingBottom(-1);
        }
        if (ihdf5reader.object().hasAttribute(CHANNELS_QUERY_PATH, PAD_RIGHT_ATTRIB)) {
            IHDF5LongReader ihdf5LongReader = ihdf5reader.int64();
            final int paddingRight = (int) ihdf5LongReader.getAttr(CHANNELS_QUERY_PATH, PAD_RIGHT_ATTRIB);
            _image.setPaddingRight(paddingRight);
        } else {
            _image.setPaddingRight(-1);
        }
    }


    public void saveFrame(int iFrame, DataAcceptor acceptor)
            throws Exception {
        int width = _image.width();
        int height = _image.height();
        byte[] data = _image.interleave(iFrame, 0, 3);
        int linesize = _image.linesize(iFrame);
        acceptor.accept(data, linesize, width, height);
    }
    
    public static interface DataAcceptor {
        void accept(byte[] data, int linesize, int width, int height);
    }

}