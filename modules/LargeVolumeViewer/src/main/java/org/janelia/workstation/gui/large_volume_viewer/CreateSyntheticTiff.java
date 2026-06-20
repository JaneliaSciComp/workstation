package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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
			// Write multi-page output tiff using ImageIO + TwelveMonkeys
			try (FileOutputStream fos = new FileOutputStream(tiffFile);
			     ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
				ImageWriter writer = TiffImageIOHelper.getTiffWriter();
				writer.setOutput(ios);
				writer.prepareWriteSequence(null);
				for (BufferedImage slice : slices) {
					writer.writeToSequence(new IIOImage(slice, null, null), null);
				}
				writer.endWriteSequence();
				writer.dispose();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void usage() {
		System.out.println("Usage: java -jar CreateSyntheticTiff.jar <folder_path>");
	}

}
