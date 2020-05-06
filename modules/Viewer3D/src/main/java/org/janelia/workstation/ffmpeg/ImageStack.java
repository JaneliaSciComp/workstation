/**
 * Created by bolstadm on 10/29/14.
 */
package org.janelia.workstation.ffmpeg;

import java.util.ArrayList;

/**
 * A stack of video frames read from FFmpeg
 */
public class ImageStack
{
    /**
     * The width of the images in the stack
     *
     * @return the width of an individual image
     */
    public int width()
    {
        return _width;
    }

    /**
     * Setter method for width
     *
     * @param width - the width of an individual image
     */
    public void setWidth(int width)
    {
        this._width = width;
    }

    /**
     * The height of the image in the stack
     *
     * @return the height of an individual image (in pixels)
     */
    public int height()
    {
        return _height;
    }

    /**
     * Setter method for height
     * @param height - the height of an individual image
     */
    public void setHeight(int height)
    {
        this._height = height;
    }

    /**
     * @return the _padding_right
     */
    public int getPaddingRight() {
        return _padding_right;
    }

    /**
     * @param _padding_right the _padding_right to set
     */
    public void setPaddingRight(int _padding_right) {
        this._padding_right = _padding_right;
    }

    /**
     * @return the _padding_bottom
     */
    public int getPaddingBottom() {
        return _padding_bottom;
    }

    /**
     * @param _padding_bottom the _padding_bottom to set
     */
    public void setPaddingBottom(int _padding_bottom) {
        this._padding_bottom = _padding_bottom;
    }

    /**
     * The number of frames in the stack
     *
     * @return the number of individual frames
     */
    public int getNumFrames()
    {
        return _image.size();
    }

    public int getNumComponents()
    {
        if ( _image.size() > 0 )
            return _image.get(0).imageBytes.size();
        else
            return 0;
    }

    public int getBytesPerPixel()
    {
        return _bytes_per_pixel;
    }

    public void setBytesPerPixel(int bytes_per_pixel)
    {
        this._bytes_per_pixel = bytes_per_pixel;
    }

    /**
     * Return a byte arryay of the pixels of the ith frame/image in the stack
     * @param i - image index
     * @param component - component index
     * @return the bytes representing the image
     */
    public byte[] image(int i, int component) { return _image.get(i).imageBytes.get(component); }

    /**
     * Return a byte array of the pixels of the ith frame/image in the stack
     * @param idx - image index
     * @param component - component index
     * @param count - number of components to use
     * @return the bytes representing the image
     */
    public byte[] interleave(int idx, int component, int count) {
        byte[] result = new byte[_width * _height * count];
        Frame f = _image.get(idx);
        for ( int j = 0; j < count; j++ ) {
            if ( component+j >= f.imageBytes.size() ) {
                for ( int i = 0; i < _width * _height; i++ )
                    result[count*i + j] = 0;
            } else {
                byte[] bytes = f.imageBytes.get(component + j);
                for ( int i = 0; i < _width * _height; i++ )
                {
                    result[count * i + j] = bytes[i];
                }
            }
        }
        return result;
    }

    /**
     * Returns a Frame of the ith image in the stack
     * @param i - image index
     * @return the individual frame
     */
    public Frame frame(int i) { return _image.get(i); }

    /**
     * A convenience routine for the size of a line of data (FFmpeg specific)
     * @param i - image index
     * @return the size of a line of data
     */
    public int linesize(int i) { return _image.get(i).image.linesize(0); }

    /**
     * Add a Frame to the end of the stack
     * @param f - The Frame to add
     */
    public void add(Frame f) { _image.add(f); }

    /**
     * Merge the channels from another ImageStack to the end of this one
     * @param other - The other ImageStack to add
     */
    public void merge(ImageStack other) {
        if ( _image.size() == 0 ) {
            _width = other.width();
            _height = other.height();
            _bytes_per_pixel = other.getBytesPerPixel();

            for (int i = 0; i < other.getNumFrames(); i++)
            {
                add(other.frame(i));
            }
        } else {
            for (int i = 0; i < other.getNumFrames(); i++)
            {
                frame(i).imageBytes.add(other.frame(i).imageBytes.get(0));
            }
        }
    }

    /**
     * Release the resources used by this class
     * @throws Exception
     */
    public void release() throws Exception
    {
        for (int i = 0; i < _image.size(); i++)
        {
            _image.get(i).release();
        }

        _image.clear();

        _height = 0;
        _width = 0;
    }

    private int _height;
    private int _width;
    
    private int _padding_right;
    private int _padding_bottom;

    private int _bytes_per_pixel;

    private ArrayList<Frame> _image = new ArrayList<>();

}

