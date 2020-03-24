/**
 * Created by bolstadm on 10/23/14.
 *
 * Modeled on the javacpp tutorial with a fair amount of lifting from the javacv
 * FFmpegFrameGrabber class
 */

package org.janelia.workstation.ffmpeg;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avdevice.avdevice_register_all;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;

class ReadInput extends Read_packet_Pointer_BytePointer_int {
    private byte[] _buffer;
    private boolean _read_bytes;

    public ReadInput(byte[] bb) {
        super();
        this._buffer = bb;
        _read_bytes = true;
    }

    @Override
    public int call(Pointer opaque, BytePointer buffer, int buffer_size) {
        int buf_size = buffer_size;
        if ( _read_bytes )
        {
            buffer.put(_buffer, 0, buffer_size);
            _read_bytes = false;
        }
        else
            buf_size = 0;

        return buf_size;
    }
};

public class FFMpegLoader
{
    static
    {
        // Register all formats and codecs
        avcodec_register_all();
        avdevice_register_all();
        av_register_all();
        avformat_network_init();
    }

    public static enum ImageMode {
        COLOR, GRAY, RAW
    }

    private String _filename;
    private AVFormatContext _format_context;
    private AVStream _video_stream;
    private AVCodecContext _video_codec;
    private AVFrame picture, picture_rgb;
    private BytePointer _buffer_rgb;
    private AVPacket pkt, pkt2;
    private int[] got_frame;
    private SwsContext img_convert_ctx;
    private ImageStack _image;
    private boolean _frame_grabbed;
    private long _time_stamp;
    private int frameNumber;
    private boolean deinterlace = false;
    private BytePointer _ibuffer;
    private int _components_per_frame;

    public FFMpegLoader(String filename)
    {
        this._filename = filename;
        _format_context = new AVFormatContext(null);
    }

    public FFMpegLoader(byte[] ibytes)
    {
        this._filename = "";
        _ibuffer = new BytePointer(ibytes);
        int BUFFER_SIZE=ibytes.length;
        // allocate buffer
        BytePointer buffer = new BytePointer(av_malloc(BUFFER_SIZE));
        // create format context
        _format_context = avformat_alloc_context();
        _format_context.pb(avio_alloc_context(buffer, BUFFER_SIZE, 0, _ibuffer, new ReadInput(ibytes), null, null));
        _format_context.pb().seekable(0);
    }

    public ImageStack getImage()
    {
        return _image;
    }

    public void release() throws Exception {
        if (pkt != null && pkt2 != null) {
            if (pkt2.size() > 0) {
                av_free_packet(pkt);
            }
            pkt = pkt2 = null;
        }

        // Free the Image
        _image.release();

        // Close the video codec
        if (_video_codec != null) {
            avcodec_close(_video_codec);
            _video_codec = null;
        }

        // Close the video file
        if (_format_context != null && !_format_context.isNull()) {
            avformat_close_input(_format_context);
            _format_context = null;
        }

        if (img_convert_ctx != null) {
            sws_freeContext(img_convert_ctx);
            img_convert_ctx = null;
        }

        got_frame = null;
        _image = null;
        _frame_grabbed = false;
        _time_stamp = 0;
        frameNumber = 0;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
//        release();
    }

    public int getImageWidth() {
        return _image == null ? -1 : _image.width();
    }

    public int getImageHeight() {
        return _image == null ? -1 : _image.height();
    }

    public boolean isDeinterlace() {
        return deinterlace;
    }

    public void setDeinterlace(boolean deinterlace) {
        this.deinterlace = deinterlace;
    }

    public int getPixelFormat()
    {
        int result = AV_PIX_FMT_NONE;
        if (_components_per_frame == 1)
        {
            if (_image.getBytesPerPixel() == 1)
                result = AV_PIX_FMT_GRAY8;
            else if (_image.getBytesPerPixel() == 2)
                result = AV_PIX_FMT_GRAY16;
            else
                result = AV_PIX_FMT_NONE;
            }
        else if (_components_per_frame == 3)
        {
            result = AV_PIX_FMT_BGR24;
        }
        else if (_components_per_frame == 4)
        {
            result = AV_PIX_FMT_BGRA;
        }
        else if (_video_codec != null)
        { // RAW
            result = _video_codec.pix_fmt();
        }

        return result;
    }

    public double getFrameRate() {
        if (_video_stream == null) {
            return 0;
        } else {
            AVRational r = _video_stream.r_frame_rate();
            return (double) r.num() / r.den();
        }
    }

    public void setFrameNumber(int frameNumber) throws Exception {
        // best guess, AVSEEK_FLAG_FRAME has not been implemented in FFmpeg...
        setTimestamp(Math.round(1000000L * frameNumber / getFrameRate()));
    }

    public void setTimestamp(long time_stamp) throws Exception {
        int ret;
        if (_format_context != null) {
            time_stamp = time_stamp * AV_TIME_BASE / 1000000L;
            /* add the stream start time */
            if (_format_context.start_time() != AV_NOPTS_VALUE) {
                time_stamp += _format_context.start_time();
            }
            if ((ret = avformat_seek_file(_format_context, -1, Long.MIN_VALUE, time_stamp, Long.MAX_VALUE, AVSEEK_FLAG_BACKWARD)) < 0) {
                throw new Exception("avformat_seek_file() error " + ret + ": Could not seek file to time_stamp " + time_stamp + ".");
            }
            if (_video_codec != null) {
                avcodec_flush_buffers(_video_codec);
            }
            if (pkt2.size() > 0) {
                pkt2.size(0);
                av_free_packet(pkt);
            }
            /* comparing to time_stamp +/- 1 avoids rouding issues for framerates
             which are no proper divisors of 1000000, e.g. where
             av_frame_get_best_effort_timestamp in grabFrame sets this.time_stamp
             to ...666 and the given time_stamp has been rounded to ...667
             (or vice versa)
             */
            while (this._time_stamp > time_stamp + 1 && grabFrame() != null) {
                // flush frames if seeking backwards
            }
            while (this._time_stamp < time_stamp - 1 && grabFrame() != null) {
                // decode up to the desired frame
            }
            if (_video_codec != null) {
                _frame_grabbed = true;
            }
        }
    }

    public int getLengthInFrames() {
        // best guess...
        return (int) (getLengthInTime() * getFrameRate() / 1000000L);
    }

    public long getLengthInTime() {
        return _format_context.duration() * 1000000L / AV_TIME_BASE;
    }

    public void start() throws Exception {
        synchronized (org.bytedeco.javacpp.avcodec.class) {
            startUnsafe();
        }
    }

    public void startUnsafe() throws Exception {
        int ret;
        img_convert_ctx = null;
        _video_codec = null;
        pkt = new AVPacket();
        pkt2 = new AVPacket();
        got_frame = new int[1];
        _image = new ImageStack();
        _frame_grabbed = false;
        _time_stamp = 0;
        frameNumber = 0;

        pkt2.size(0);

        // Open video file
        AVDictionary options = new AVDictionary(null);

        if ((ret = avformat_open_input(_format_context, _filename, null, options)) < 0) {
            av_dict_set(options, "pixel_format", null, 0);
            if ((ret = avformat_open_input(_format_context, _filename, null, options)) < 0) {
                throw new Exception("avformat_open_input() error " + ret + ": Could not open input \"" + _filename + "\". (Has setFormat() been called?)");
            }
        }
        av_dict_free(options);

        // Retrieve stream information
        if ((ret = avformat_find_stream_info(_format_context, (PointerPointer) null)) < 0) {
            throw new Exception("avformat_find_stream_info() error " + ret + ": Could not find stream information.");
        }

        // Dump information about file onto standard error
        av_dump_format(_format_context, 0, _filename, 0);

        // Find the first video and audio stream
        _video_stream = null;
        int nb_streams = _format_context.nb_streams();
        for (int i = 0; i < nb_streams; i++) {
            AVStream st = _format_context.streams(i);
            // Get a pointer to the codec context for the video or audio stream
            AVCodecContext c = st.codec();
            if (_video_stream == null && c.codec_type() == AVMEDIA_TYPE_VIDEO) {
                _video_stream = st;
                _video_codec = c;
            }
        }
        if (_video_stream == null) {
            throw new Exception("Did not find a video stream inside \"" + _filename + "\".");
        }

        if (_video_stream != null) {
            // Find the decoder for the video stream
            AVCodec codec = avcodec_find_decoder(_video_codec.codec_id());
            if (codec == null) {
                throw new Exception("avcodec_find_decoder() error: Unsupported video format or codec not found: " + _video_codec.codec_id() + ".");
            }

            // Open video codec
            if ((ret = avcodec_open2(_video_codec, codec, (PointerPointer) null)) < 0) {
                throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
            }

            // Hack to correct wrong frame rates that seem to be generated by some codecs
            if (_video_codec.time_base().num() > 1000 && _video_codec.time_base().den() == 1) {
                _video_codec.time_base().den(1000);
            }
        }

        int pix_fmt = _video_codec.pix_fmt();
        AVPixFmtDescriptor fmt = av_pix_fmt_desc_get(pix_fmt);
        int comps = fmt.nb_components();
        _components_per_frame = comps;
        AVComponentDescriptor desc = fmt.comp();
        int bpp = comps / (desc.step_minus1() + 1);
        _image.setBytesPerPixel(bpp);
    }

    public void stop() throws Exception {
        release();
    }

    private void allocateFrame(Frame f) throws Exception {
        // Allocate video frame and an AVFrame structure for the RGB image
        if ((picture = av_frame_alloc()) == null) {
            throw new Exception("avcodec_alloc_frame() error: Could not allocate raw picture frame.");
        }
        if ((picture_rgb = av_frame_alloc()) == null) {
            throw new Exception("avcodec_alloc_frame() error: Could not allocate RGB picture frame.");
        }

        int width = getImageWidth() > 0 ? getImageWidth() : _video_codec.width();
        int height = getImageHeight() > 0 ? getImageHeight() : _video_codec.height();

        _image.setHeight(height);
        _image.setWidth(width);

        int fmt = getPixelFormat();

        // Determine required buffer size and allocate buffer
        int size = avpicture_get_size(fmt, width, height);
        _buffer_rgb = new BytePointer(av_malloc(size));

        // Assign appropriate parts of buffer to image planes in picture_rgb
        // Note that picture_rgb is an AVFrame, but AVFrame is a superset of AVPicture
        avpicture_fill(new AVPicture(picture_rgb), _buffer_rgb, fmt, width, height);

        // Assign to the frame so the memory can be deleted later
        f.buffer_rgb = _buffer_rgb;
        f.picture = picture;
        f.picture_rgb = picture_rgb;
        f.imageBytes.add( new byte[width * height] );
    }

    private void extractBytes(Frame frameOutput, BytePointer imageBytesInput) {
        int width = _video_codec.width();
        int height = _video_codec.height();
        int padding = _image.getPaddingRight();
        if (padding == -1) {
            padding = 0;
        }

        byte[] outputBytes = frameOutput.imageBytes.get(0);
        byte[] inputBytes = new byte[width * height * 3];
        imageBytesInput.get(inputBytes);

        int inputOffset = 0;
        int outputOffset = 0;
        for (int rows = 0; rows < height; rows++) {
            for (int cols = 0; cols < width; cols++) {
                outputBytes[ outputOffset ] = inputBytes[3 * inputOffset];
                inputOffset ++;
                outputOffset ++;
            }
            inputOffset += padding;
        }
    }
    
    private void processImage(Frame frame) throws Exception
    {
        // Deinterlace Picture
        if (deinterlace) {
            AVPicture p = new AVPicture(picture);
            avpicture_deinterlace(p, p, _video_codec.pix_fmt(), _video_codec.width(), _video_codec.height());
        }

        // Convert the image into BGR or GRAY format that OpenCV uses
        img_convert_ctx = sws_getCachedContext(img_convert_ctx,
                _video_codec.width(), _video_codec.height(), _video_codec.pix_fmt(),
                getImageWidth(), getImageHeight(), getPixelFormat(), SWS_BILINEAR,
                null, null, (DoublePointer) null);
        if (img_convert_ctx == null) {
            throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
        }

        // Convert the image from its native format to RGB or GRAY
        sws_scale(img_convert_ctx, new PointerPointer(picture), picture.linesize(), 0,
                _video_codec.height(), new PointerPointer(picture_rgb), picture_rgb.linesize());

        extractBytes(frame, picture_rgb.data(0));
    }

    public void grab() throws Exception {
        Frame f;
        boolean done = false;

        int i = 0;
        while (!done) {
            f = grabFrame();
            if (f != null) {
                // Uncomment to debug each frame as it is grabbed
                // SaveFrame(f, i++);
                _image.add(f);
            } else {
                done = true;
            }
        }
    }

    public Frame grabFrame() throws Exception {
        if (_format_context == null || _format_context.isNull()) {
            throw new Exception("Could not grab: No AVFormatContext. (Has start() been called?)");
        }
        Frame frame = new Frame();

        if (_frame_grabbed) {
            _frame_grabbed = false;
            processImage(frame);
            frame.keyFrame = picture.key_frame() != 0;
            frame.image = picture_rgb;
            return frame;
        }
        boolean done = false;
        while (!done)
        {
                if (av_read_frame(_format_context, pkt) < 0)
                {
                    if (_video_stream != null)
                    {
                        // The video codec may have buffered some frames
                        pkt.stream_index(_video_stream.index());
                        pkt.flags(AV_PKT_FLAG_KEY);
                        pkt.data(null);
                        pkt.size(0);
                    } else {
                        return null;
                    }
                }

            // Is this a packet from the video stream?
            if (_video_stream != null && pkt.stream_index() == _video_stream.index()) {
                // Allocate memory per frame as we want to retain these for
                // later processing
                allocateFrame(frame);

                // Decode video frame
                int len = avcodec_decode_video2(_video_codec, picture, got_frame, pkt);

                // Did we get a video frame?
                if (len >= 0 && got_frame[0] != 0) {
                    long pts = av_frame_get_best_effort_timestamp(picture);
                    AVRational time_base = _video_stream.time_base();
                    _time_stamp = 1000000L * pts * time_base.num() / time_base.den();
                    // best guess, AVCodecContext.frame_number = number of decoded frames...
                    frameNumber = (int) (_time_stamp * getFrameRate() / 1000000L);
                    processImage(frame);
                    done = true;
                    frame.keyFrame = picture.key_frame() != 0;
                    frame.image = picture_rgb;
                    frame.opaque = picture;
                } else if (pkt.data() == null && pkt.size() == 0) {
                    return null;
                } else
                    frame.release();
            }

            if (pkt.size() > 0)
            {
                // Free the packet that was allocated by av_read_frame
                av_free_packet(pkt);
            }
        }
        return frame;
    }

    public void saveFrame(int iFrame, FFMPGByteAcceptor acceptor)
            throws Exception {
        int width = _image.width();
        int height = _image.height();
        byte[] data = _image.image(iFrame, 0);
//                .interleave(iFrame, 0, 1);
        int linesize = _image.linesize(iFrame);
        acceptor.accept(data, linesize, width, height);
    }
}
