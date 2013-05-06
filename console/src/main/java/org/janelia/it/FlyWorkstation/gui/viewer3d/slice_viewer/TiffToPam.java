package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.io.IOException;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

/**
 * Convert block tiff octree render artifacts to slice pam/lz4 artifacts.
 * @author brunsc
 *
 */
public class TiffToPam {
	
	private BlockTiffOctreeLoadAdapter loadAdapter;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		File inputTiffFolder = new File(args[0]);
		File outputPamFolder = new File(args[1]);
		TiffToPam tiffToPam = new TiffToPam();
		tiffToPam.loadAdapter = new BlockTiffOctreeLoadAdapter();
		try {
			tiffToPam.loadAdapter.setTopFolder(inputTiffFolder);
			tiffToPam.convertFolder(inputTiffFolder, outputPamFolder);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Recursively convert a folder and all of it's descendants.
	 * @param inputFolder
	 * @param outputFolder
	 * @return
	 */
	private boolean convertFolder(File inputFolder, File outputFolder) {
		if (! inputFolder.exists())
			return false;
		if (! outputFolder.exists()) {
			if (! outputFolder.mkdirs()) // create output folder
				return false;
		}
		System.out.println("Converting folder "+inputFolder+" to "+outputFolder);
		// Open tiff files, one per channel
		int sc = loadAdapter.getTileFormat().getChannelCount();
		ImageDecoder decoder[] = new ImageDecoder[sc];
		for (int c = 0; c < sc; ++c) {
			File tiff = new File(inputFolder, "default."+c+".tif");
			if (! tiff.exists())
				return false;
			try {
				SeekableStream s = new FileSeekableStream(tiff);
				decoder[c] = ImageCodec.createImageDecoder("tiff", s, null);
			} catch (IOException e) {
				return false;
			}
		}
		// TODO - convert files
		// TODO - convert subfolders
		return true;
	}
	
	private static void usage() {
		System.out.println("Usage: java "+TiffToPam.class.getName()+" <input_tiff_folder> <output_pam_folder>");
	}

}
