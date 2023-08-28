# FlyLight Sample Pipeline

Data on the [Split GAL4](https://splitgal4.janelia.org/) and [Gen1 MCFO](https://gen1mcfo.janelia.org) websites is produced from raw LSM images by the Workstation pipelines. 

At a high level:
1. Images are grouped by *slide code* to create **Samples**
2. Each Sample is processed by considering all the acquired imagery from each microscope objective and each anatomical area. For example: 20x Brain, 20x VNC, 63x Brain
3. **LSM Summary** - the LSM images are summarized with MIPs and movies
4. **Distortion Correction** - correction for optical distortions and chromatic aberrations
5. **Sample Processing** - the LSM image tiles are merged and stitched into a single volume
6. **Post Processing** - the tiles and final image are summarized with MIPs and movies
7. **Alignment** - the sample is registered to standard reference images
8. **Color Depth MIPs** - color depths MIPs are generated from the aligned volumes
9. **Compression** - the image files are compressed using the [H5J file format](H5JFileFormat.md)

Detailed information for reproducing each result is documented below.

## LSM Summary

MIPs and movies for LSMs are generated using [basicMIP.sh](https://github.com/JaneliaSciComp/jacs-tools-docker/blob/master/flylight_tools/scripts/cmd/basicMIP.sh) in the flylight_tools container. 

The specific options passed to this script are:
* LASER: laser power, extracted from LSM metadata
* GAIN: laser gain, extracted from LSM metadata
* CHAN_SPEC: channel specification string listing (s)ignal and (r)eference channels contained in the LSM file, e.g. `rss`
* COLOR_SPEC: colors to use for each channel, extracted from the LSM metadata, e.g. `1BG` for a grey reference with blue and green signals
* DIV_SPEC: division spec which is used to attenuate the brightness of the reference channel by dividing the intensity, e.g. `211`
* OPTIONS: options used by the Fiji macro, in the case of LSMs we use "mips:movies:legends:bcomp"

### File types

* Download LSM: Raw data

## Distortion Correction

Confocal scanned images have optical distortions. Without the distortion correction, the confocal images show two main problems:

1. The shifted position problem between different channels. Each channel has different distortion in multi-channel confocal data. If we simply merge multiple channels, the co-localized signal will not stay in the same place in around ~70-100% of the peripheral area (0% is the center, 100% is the edge) of the confocal view.

![Chromatic aberration correction](distcorrection_1.png)

Left panel: The neuron with chromatic aberration. The green and purple signals are shifted. Right panel: After the chromatic aberration correction. The green and purple are staying in same position.

2. Double blended signals between overlapping tiles. The optical distortion leads to image distortion. If the multiple distorted images are stitched together, the overlapped area will not match perfectly. This mismatch can be seen as “double blended signals”.   

![Canceling double blending](distcorrection_1.png)

Left panel: Before the distortion correction. Right panel: After the distortion correction.

### Implementation 

Stephan Saalfeld created the method for correcting the chromatic aberration and the distortion correction by using TrackEM2. Hideo Otsuna modified the Java code for implementation, performed image quality tests by using more than ~2000 confocal tiles 
and wrote the distortion fields auto-selector ([code](https://github.com/JaneliaSciComp/otsunah/tree/master/Distortion_correction)).

### The nature of the distortion/chromatic aberration

Hideo found that the distortion exhibits slight fluctuation in every confocal scan. After the distortion correction, ~1-2% of images still have double blend signals after the stitching. The double blended signals can sometimes be fixed by using the 
distortion field from other scopes.

The distortion was changing every several months. After large changes in the distortion, previous distortion fields do not fix the double blending problem. Once we detect the double blending problem in our pipeline results, we need to 
re-take the image (glass beads 4x4 tiles with 60% overlapping), then generate a new distortion field. Scope 1 63x changed the distortion over about a five month period, and Scope 2 63x changed over ~8 months, Scope 5 63x changed over ~6 months, and 
Scope 6 63x over ~9 months.

### List of the distortion fields

Files are named with the format scope#_objective+resolution_the first day for applying the field: year_MMDDHH.json file, ~10kb.

If the sample capture date is earlier than the field capture date, our pipeline will use the oldest field for the sample.  Hideo found the many of samples from 2013 could fix/improve the double blending by using the distortion field from 2016 (except scope 1).

The distortion fields for each microscope can be found in the [confocal-distortion-fields](https://github.com/JaneliaSciComp/confocal-distortion-fields) repository.


## Sample Processing

### Merging

When more than 3 signal channels are captured on the Zeiss microscopes, they are usually captured in 2 passes, and 2 LSM files are produced. Each file contains a reference channel and 1 or more signal channels. 
The pipeline always begins by merging any LSM pairs using either a rigid or non-rigid transformation using the [ANTS](http://stnava.github.io/ANTs/) toolkit. The merge algorithm arranges the channels in the output file according to one of two methods:
* **Multicolor** - this algorithm is usually specified when the content is "multicolor" (e.g. MCFO) and the channel colors are RGB. The merge program reads the channel colors encoded in the LSM file and rearranges them in order as: red, green, blue, reference. 
* **Ordered** - this algorithm is usually specified for non-multicolor images. The original channel order is preserved, apart from the reference channel which is always put last.

The merge step does NOT use reference channel information from the metadata. Instead, it does a pair-wise comparison of all channels, and picks the two most similar channels across both files to serve as the reference. 
We use mutual information (MI) to compare all pair-wise color channels to find the reference color channel based on the assumption of the reference color channels have more similarity. Then we use block matching approach to determine the 
transformation type. If the transformation type is pure translations, we use a fast template matching approach to estimate the transformation. If the transformation type is non-rigid, we use ANTS (MI and SyN) to estimate deformable transformation.

This step is implemented by the [merge.sh](https://github.com/JaneliaSciComp/jacs-tools-docker/blob/master/flylight_tools/scripts/cmd/merge.sh) script in the flylight_tools container.

### Conversion to Sample Image

In many cases, the LSMs that come off the scopes do not have their channels in any particular order, and the order can change with the imaging protocol. In some cases the channel order is rearranged by the LSM Merging step as described in the previous section. 
In order to normalize the order of the channels, each pipeline is configured to use one of two methods:
* Channel Specification - each LSM is annotated with a chan_spec annotation such as "rss" or "sr" which lists in order the content of each channel in the file ("s" for signal, and "r" for reference).
  The pipeline rearranges the channels so that the reference channel is last. If this was already completed by the merge step, then this step is skipped. Obviously, this method cannot distinguish between different signal types.
  No channels are ever discarded with this method.
* Dye Specification - each pipeline using this method is configured to associate certain dye names to content types. For example if the dye is "Alexa Fluor 633" or "Alexa Fluor 647" then the channel content is "presynaptic".
  The dye information that was read from the LSM files during the LSM Metadata Extraction step is used to rearrange the channels in some predetermined order (e.g. presynaptic, membrance, reference). Unused channels are discarded.

Aside from channel reordering, this step also ensures that the sample images are in v3draw format. This is important in cases where a single LSM is neither merged nor stitched and so is never implicitly converted out of TIFF format.

### Stitching 

In cases where there is only a single tile per objective and anatomical area, this step is skipped.

In cases where multiple image tiles have the same objective and anatomical area, they are clustered into non-overlapping groups by the "grouping" algorithm. There is currently no pipeline support for stitching multiple groups of images per Sample,
so only the largest group is stitched together and blended.

This step is implemented by the [stitchAndBlend.sh](https://github.com/JaneliaSciComp/jacs-tools-docker/blob/master/flylight_tools/scripts/cmd/stitchAndBlend.sh) script in the flylight_tools container.

The actual stitching is done using Yang Yu's iStitch and iFusion plugins for Vaa3d, available here: https://code.google.com/p/vaa3d/wiki/imageStitch

The underlying stitching algorithm is described here: http://home.penglab.com/papersall/docpdf/2011_ISBI_istitch.pdf


### File types

* MIP: Unaligned (all channels)
* MIP: Unaligned (signal channels)
* Movie: Unaligned (all channels)
* Movie: Unaligned (signal channels)
* Download H5J stack: Unaligned
  
## Post Processing



## Alignment

Aligners are implemented with Singularity containers in the [jacs-tools repository](https://github.com/JaneliaSciComp/jacs-tools). 
Each aligner is built into one or more Singularity apps defined in the Singularity file. These apps are invoked by the Workstation pipelines to generate the aligned volumes. 

### File types
* MIP: Aligned gendered (all channels)
* MIP: Aligned unisex (all channels)
* Download H5J stack: Aligned gendered
* Download H5J stack: Aligned unisex


## Color Depth MIPs

This step is implemented by the [[colorDepthMIP.sh](https://github.com/JaneliaSciComp/jacs-tools-docker/blob/master/flylight_tools/scripts/cmd/colorDepthMIP.sh) script in the flylight_tools container.

### File types
* Color depth MIP: Aligned (Channel N)
  
  
## Compression
