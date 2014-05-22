package org.janelia.it.workstation.gui.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import com.google.common.collect.Iterators;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFEncodeParam;

public class PermuteTiff {

	private static boolean oneFolderAtATime = true;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Sanity checks
		if (args.length < 1) {
			usage();
			System.exit(1);
		}
		File folder = new File(args[0]);
		if (! folder.exists()) {
			System.err.println("No such folder "+folder.getAbsolutePath());
			usage();
			System.exit(1);
		}
		// Grid mode vs. serial mode.
		if (oneFolderAtATime)
			permuteFolder(folder); // Permute contents of just one folder
		else
			permuteOctree(folder); // Serially permute entire tree
	}
	
	public static void usage() {
		System.out.println("Usage: java -jar PermuteTiff.jar <folder_path>");
	}
	
    public static void permuteOctree( File folder ) 
    {
        // Top folders first
        permuteFolder(folder);

        File[] list = folder.listFiles();
        for ( File f : list ) {
            if ( f.isDirectory() ) {
            	// only want subdirectories "1", "2", ..., "8"
            	// (we get top directory files automatically)
            	if (! (f.getName().length() == 1))
            		continue;
            	int ix = Integer.parseInt(f.getName());
            	if (ix < 1)
            		continue;
            	if (ix > 8)
            		continue;
                permuteOctree( f );
                // System.out.println( "Dir:" + f.getAbsoluteFile() );
            }
        }
    }
	
    public static void permuteFolder (File folder) {
    	if (folder == null)
    		return;
    	if (! folder.exists())
    		return;
    	File[] list = folder.listFiles();
    	if (list == null)
    		return;
    	for (File f : list) {
    		if (f.isDirectory())
    			continue;
    		if (! f.exists())
    			continue;
    		permuteTiff(f);
    	}
    }
    
	public static void permuteTiff(File inTiff, File outTiff, int permuteSteps) 
			throws IOException
	{
		permuteSteps = permuteSteps % 3;
		// Load first slice from input file to determine image format
		SeekableStream s = new FileSeekableStream(inTiff);
		ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
		NullOpImage slice1 = new NullOpImage(
				decoder.decodeAsRenderedImage(0),
				null,
				null,
				OpImage.OP_NETWORK_BOUND);
		// Note volume size
		int sx = slice1.getWidth();
		int sy = slice1.getHeight();
		int sz = decoder.getNumPages();
		BufferedImage bufferedSlice1 = slice1.getAsBufferedImage();
		// Initialize output slices
		int sizeOut[] = {sx, sy, sz};
		permute(sizeOut, permuteSteps);
		BufferedImage outSlices[] = new BufferedImage[sizeOut[2]];
		for (int z = 0; z < sizeOut[2]; ++z)
			outSlices[z] = new BufferedImage(
					sizeOut[0], sizeOut[1],
					bufferedSlice1.getType());
		// Copy permuted pixel by pixel
		int bandCount = slice1.getColorModel().getNumColorComponents();
		int pixel[] = new int[bandCount];
		for (int z = 0; z < sz; ++z) {
			Raster inSlice = decoder.decodeAsRaster(z);
			for (int y = 0; y < sy; ++y) {
				for (int x = 0; x < sx; ++x) {
					int ixOut[] = {x, y, z};
					permute(ixOut, permuteSteps);
					BufferedImage outSlice = outSlices[ixOut[2]];
					pixel = inSlice.getPixel(x, y, pixel);
					outSlice.getRaster().setPixel(ixOut[0], ixOut[1], pixel);
				}
			}
		}
		// Write output tiff
		TIFFEncodeParam params = new TIFFEncodeParam();
		Iterator<BufferedImage> it = Iterators.forArray(outSlices);
		if (it.hasNext()) it.next(); // Avoid duplicate first slice
		params.setExtraImages(it); 
		OutputStream out = new FileOutputStream(outTiff); 
		ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
		encoder.encode(outSlices[0]); 
		out.close(); 
	}
	
	public static void permuteTiff(File tiffFile) {
    	Pattern filePattern = Pattern.compile("^default\\.(\\d+)\\.tif$");
    	// Only want to convert default.?.tif
    	Matcher matcher = filePattern.matcher(tiffFile.getName());
    	if (! matcher.matches())
    		return;
    	int channel = Integer.parseInt(matcher.group(1));
        File yzFile = new File(tiffFile.getParentFile(), "YZ."+channel+".tif");
        File zxFile = new File(tiffFile.getParentFile(), "ZX."+channel+".tif");
        // System.out.println( "File:" + f.getAbsoluteFile());
        // System.out.println( "  YZ:" + yzFile.getAbsoluteFile());
        // System.out.println( "  ZX:" + zxFile.getAbsoluteFile());
        if (! yzFile.exists()) {
			try {
                System.out.println( "Creating:" + yzFile.getAbsoluteFile());
				permuteTiff(tiffFile, yzFile, 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if (! zxFile.exists()) {
			try {
                System.out.println( "Creating:" + zxFile.getAbsoluteFile());
				permuteTiff(tiffFile, zxFile, 2);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }		
	}
	
	private static void permute(int[] in, int count) {
		for (int i = 0; i < count; ++i) {
			permute1(in);
		}
	}
	
	private static void permute1(int[] in) {
		int first = in[0];
		for (int i = 0; i < (in.length-1); ++i) {
			in[i] = in[i+1];
		}
		in[in.length-1] = first;
	}

}
