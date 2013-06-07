package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		File tiff = new File(fileName);
		File outTiff = new File(args[1]);
		// Permute one or two levels
		int permuteSteps = new Integer(args[2]);
		permuteSteps = permuteSteps % 3;
		try {
			permuteTiff(tiff, outTiff, permuteSteps);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		params.setExtraImages(it); 
		OutputStream out = new FileOutputStream(outTiff); 
		ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
		encoder.encode(outSlices[0]); 
		out.close(); 
	}
	
	private static void permute(int[] in, int count) {
		for (int i = 0; i < count; ++i) {
			permute1(in);
		}
	}
	
	private static void permute1(int[] in) {
		int last = in[in.length-1];
		for (int i = in.length-1; i > 0; --i) {
			in[i] = in[i-1];
		}
		in[0] = last;
	}

}
