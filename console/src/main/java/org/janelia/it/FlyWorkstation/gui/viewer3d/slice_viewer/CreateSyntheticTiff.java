package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

public class CreateSyntheticTiff {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
			System.exit(1);
		}
		File folder = new File(args[0]);
		// One tiff file for each channel
		BufferedImage slices[] = new BufferedImage[256];
		for (int channel : new int[] {0,1,2}) {
			String tiffName = "default."+channel+".tif";
			File tiffFile = new File(folder, tiffName);
			int pixel[] = {0};
			for (int z = 0; z < 256; ++z) {
				slices[z] = new BufferedImage(256,256,BufferedImage.TYPE_BYTE_GRAY);
				for (int y = 0; y < 256; ++y) {
					for (int x = 0; x < 256; ++x) {
						int xyz[] = {x,y,z};
						pixel[0] = xyz[channel];
						slices[z].getRaster().setPixel(x, y, pixel);
					}
				}
			}
			// Write output tiff
			TIFFEncodeParam params = new TIFFEncodeParam();
			Iterator<BufferedImage> it = Iterators.forArray(slices);
			if (it.hasNext()) it.next(); // Avoid duplicate first slice
			params.setExtraImages(it);
			OutputStream out;
			try {
				out = new FileOutputStream(tiffFile);
				ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
				encoder.encode(slices[0]);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void usage() {
		System.out.println("Usage: java -jar CreateSyntheticTiff.jar <folder_path>");
	}
	
}
