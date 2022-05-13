# H5J File Format

H5J is a “visually lossless” file format, developed at Janelia Research Campus, for storing multichannel 3d image stacks e.g. from confocal microscopy. It supports both 8-bit and 12-bit depth, and uses the [H.265 codec](https://en.wikipedia.org/wiki/High_Efficiency_Video_Coding) (a.k.a. HEVC or High Efficiency Video Coding) and differential compression ratios on a per-channel basis to obtain maximum compression while minimizing visually-relevant artifacts.

H5J is currently compatible with the following tools:
* [Fiji](https://fiji.sc/) (read/write)
* [VVD Viewer](https://github.com/takashi310/VVD_Viewer) (read)
* [Vaa3D](https://github.com/Vaa3D/release) (read/write)
* [Janelia Workstation](https://github.com/JaneliaSciComp/workstation) (read)
* [web-vol-viewer](https://github.com/JaneliaSciComp/web-vol-viewer) (read)
* [web-h5j-loader](https://github.com/JaneliaSciComp/web-h5j-loader) (read)


## Specification

An H5J file is simply a standard [HDF5 (Hierarchical Data Format)](https://en.wikipedia.org/wiki/Hierarchical_Data_Format) file with a specific structure designed for wrapping compressed image stacks. Within the HDF5 structure, the data is divided into multiple data sets, one per channel. Here is an example of the structure for a 2-channel image stack:

```
/ (Group)
    Attribute: area scalar
        Type:  5-byte null-padded ASCII string
        Data:  "Brain"
    Attribute: channel_spec scalar
        Type:  2-byte null-padded ASCII string
        Data:  "sr"
    Attribute: image_size {3}
        Type:  native double
        Data:  1024, 1024, 153
    Attribute: objective scalar
        Type:  3-byte null-padded ASCII string
        Data:  "20x"
    Attribute: owner scalar
        Type:  14-byte null-padded ASCII string
        Data:  "group:flylight"
    Attribute: sample_id scalar
        Type:  native long
        Data:  2313842424838881317
    Attribute: unit scalar
        Type:  6-byte null-padded ASCII string
        Data:  "micron"
    Attribute: voxel_size {3}
        Type:  native double
        Data:  0.62, 0.62, 1
    Location:  1:96
    Links:     1
/Channels (Group)
    Attribute: frames {1}
        Type:  native long
        Data:  153
    Attribute: height {1}
        Type:  native long
        Data:  1024
    Attribute: pad_bottom {1}
        Type:  native long
        Data:  0
    Attribute: pad_right {1}
        Type:  native long
        Data:  0
    Attribute: width {1}
        Type:  native long
        Data:  1024
    Location:  1:800
    Links:     1
/Channels/Channel_0 (Dataset) {23499450/23499450}
    Attribute: content_type scalar
        Type:  6-byte null-padded ASCII string
        Data:  "signal"
    Location:  1:2232
    Links:     1
    Storage:   23499450 logical bytes, 23499450 allocated bytes, 100.00% utilization
    Type:      native unsigned char
/Channels/Channel_1 (Dataset) {2848446/2848446}
    Attribute: content_type scalar
        Type:   9-byte null-padded ASCII string
        Data:  "reference"
    Location:  1:23502282
    Links:     1
    Storage:   2848446 logical bytes, 2848446 allocated bytes, 100.00% utilization
    Type:      native unsigned char

```

Each channel is encoded separately using FFMPEG and stored in a byte array within a dataset. Each channel is a byte-exact representation of the FFmpeg data as it would reside on the file system. H5J uses the following x256 params:
* crf=7 - Constant Rate Factor. Controls the tradeoff between compression and image quality. With a setting of 15, the compressed images are "visually lossless" when compared to the original. For images produced by the Janelia Workstation pipeline, we use crf=7 for neuronal signal channels and crf=21 for the reference (e.g. NC82) channel.
* psy-rd=1.0 - Psyco-Visual Options - This particular value is designed to reduce blurring when the codec detects motion. Since features move from layer to layer, the codec detects that change of position as motion. The value of 1 is a happy medium between to much blur, and introducing visual artifacts.
* -pix_fmt +yuv444p - The color space for encoding. HEVC only supports a limited number of the variants offered by FFmpeg, and almost all are some variant of the yuv color space. The gray12 color space is used for 12-bit H5J, and gives us more bits per pixel than the standard mapping, and therefore, reduces quantization errors.

Metadata is encoded as attributes including the image size and voxel size. Each channel data set has a "content_type" attribute which describes its content, usually "signal" or "reference". . 

You can read the metadata using any HDF5-compliant library, but we have also made a [Docker container](https://github.com/JaneliaSciComp/jacs-tools-docker/tree/master/h5j_metadata) available to dump the metadata to JSON. Alternatively, you can install the [HDF5 Command Line Tools](https://support.hdfgroup.org/products/hdf5_tools/#cmd) and run the h5ls command. To get the output shown in the example above, try this:

```
$ h5ls -v -r tile-2627326651758805030.h5j
```

## Converting to H5J Using Vaa3D

The Vaa3D implementation of the H5J writer can convert any format supported by Vaa3D into an H5J file. It supports many options including per-channel compression. It does not support arbitrary metadata, only some basic metadata inferred from the imagery, such as the image size. To write additional metadata to the H5J, use the Docker container described above.

The syntax for running the command line per channel encoder is similar to the other converter modes. The basic syntax looks like:
```
vaa3d -cmd image-loader -codecs <input-file> <output-file.h5j> [options]
```

For the codecs mode, the only output file type allowed is h5j, so make sure the filename has that extension. By default, with no options specified the individual channels are compressed as before using the HEVC codec with magic parameters picked by me to maximize compression and minimize visual artifacts.

For the compression of individual channels, the format is the following:

```
<channels>:<codec>[:<codec options> [<codec options>]]
```

where:
* channels:  a 1-based comma separated list
* codec: only two are currently supported, HEVC (lossy), and FFV1(lossless)
* options: vary based on the codec, but they are a string (including quotation marks) of colon separated token=value pairs.

Example:

```
1,2:FFV1 4:HEVC:"crf=2:psy-rd=1.0"
```

in this example:
channels 1 and 2 will be encoded lossless using the default parameters  
channel 3 will use the default lossy codec  
channel 4 will be encoded lossy overriding the default value  

The Vaa3D H5J implementation supports two compression codecs, described below.

### FFV1 Codec

FFV1 (FF Video Codec 1) is a lossless video/audio codec that is known to have good performance with regards to speed and size compared to other lossless codecs. One of its main features is that it is one of the recommended codecs for archiving and preservation of moving images, and is the only codec recommended by The Society of American Archivists. There are a number of options for controlling the encoding, but through various tests, the default options seem to provide the best compression ratio for the widest set of test cases.

The defaults are: "level=3:coder=1:context=1:g=1:slices=4:slicecrc=1"

For more information on the various options (version 3) see: https://trac.ffmpeg.org/wiki/Encode/FFV1.

### HEVC Codec

High Efficiency Video Codec (HEVC) is a video compression standard implemented in FFmpeg through the libx265 codec. HEVC is one of several potential successors to H.264/MPEG-4. HEVC offers double the compression of H.264 at the same visually comparable video quality, or better quality at the same bit rate.

The defaults for HEVC are: "crf=15:psy-rd=1.0"

While their are a large amount of variables that can be passed to the HEVC encoder, the crf (Constant Rate Factor) seems to have the largest impact on compression rate. The lower the crf value the closer the output is to a lossless encoding. A value of 0 invokes the lossless HEVC encoder, but in practice we still see a small amount of difference between the compressed version and the original.

The FFmpeg documentation has a [minimal overview of the HEVC options](https://trac.ffmpeg.org/wiki/Encode/H.265). For a complete list of all the possible options for the encoder see http://x265.readthedocs.org/en/default/cli.html. Please note that beyond the two options listed above, no other options have been tested. By default, we use the 'medium' preset which gives a good balance between compression efficiency, and compression time. The slower settings do result in a couple of percent increase in compression, but at the expense of extremely long compression times. Currently, the preset is not changeable from the command-line.

## Converting to H5J Using Fiji

The [H5J loader plugin](https://github.com/fiji/H5J_Loader_Plugin) is included with the Fiji distribution, but if you want the H5J writer, you'll need to [download it separately](https://github.com/JaneliaSciComp/H5J_Writer_For_Fiji) and install it into your Fiji.


