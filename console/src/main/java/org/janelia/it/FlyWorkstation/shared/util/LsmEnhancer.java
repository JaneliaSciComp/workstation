package org.janelia.it.FlyWorkstation.shared.util;

/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

import loci.common.RandomAccessInputStream;
import loci.common.RandomAccessOutputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.in.ZeissLSMReader;
import loci.formats.tiff.IFD;
import loci.formats.tiff.TiffConstants;
import loci.formats.tiff.TiffParser;
import loci.formats.tiff.TiffSaver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: add javadoc
 *
 * @author Eric Trautman
 */
public class LsmEnhancer {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        LsmEnhancer enhancer = new LsmEnhancer(args[0]);
        enhancer.run();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("\n\nProcessing completed in " + elapsed + " milliseconds.");
    }

    private String fileName;

    public LsmEnhancer(String fileName) {
        this.fileName = fileName;
    }

    public void run() {

        try {
            StringBuilder xml = new StringBuilder(32000);
            xml.append("<data>");
            String janeliaMetadata = "<janeliaMetadata>" + "<line>GMR_52E06_AE_01</line>" + "<age>A01</age>" + "<area>b</area>" + "<slideCode>02</slideCode>" + "</janeliaMetadata>";
            xml.append(janeliaMetadata);
            xml = appendZeissMetadata(xml);
            xml.append("</data>");
            addMetadata(xml.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private StringBuilder appendZeissMetadata(StringBuilder xml) throws FormatException, IOException {


        List<String> keyList;
        ZeissLSMReader reader = new ZeissLSMReader();
        reader.setId(fileName);

        xml.append("<zeissMetadata>");
        for (CoreMetadata coreMetadata : reader.getCoreMetadata()) {
            keyList = new ArrayList<String>(coreMetadata.seriesMetadata.keySet());
            Collections.sort(keyList);
            for (String key : keyList) {
                xml.append("<item><key>");
                xml.append(key);
                xml.append("</key>");
                xml.append("<value>");
                xml.append(coreMetadata.seriesMetadata.get(key));
                xml.append("</value></item>");
            }
            break;
        }
        xml.append("</zeissMetadata>");

        return xml;
    }

    private void addMetadata(String metadata) throws IOException, FormatException {

        RandomAccessInputStream in = null;
        RandomAccessOutputStream out = null;
        try {

            TiffParser parser = new TiffParser(fileName);
            Boolean isLittleEndian = parser.checkHeader();
            long[] ifdOffsets = parser.getIFDOffsets();
            long firstIFDOffset = ifdOffsets[0];
            IFD firstIFD = parser.getIFD(firstIFDOffset);

            in = parser.getStream();
            long next = getNextOffsetLocation(in, firstIFDOffset);
            long nextValue = (next & ~0xffffffffL) | (in.readInt() & 0xffffffffL);
            in.close();
            in = null;

            TiffSaver tiffSaver = new TiffSaver(fileName);
            tiffSaver.setLittleEndian(isLittleEndian);
            out = tiffSaver.getStream();
            long endOfFile = out.length();

            firstIFD.put(TIFF_JF_TAGGER_TAG, metadata);
//            firstIFD.put(IFD.IMAGE_DESCRIPTION, zeissMetadata);
            out.seek(endOfFile);

            tiffSaver.writeIFD(firstIFD, nextValue);

            out.seek(4);
            out.writeInt((int) endOfFile);

        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private long getNextOffsetLocation(RandomAccessInputStream in, long offset) throws IOException, FormatException {

        in.seek(offset);
        int nEntries = in.readUnsignedShort();
        in.skipBytes(nEntries * TiffConstants.BYTES_PER_ENTRY);
        return in.getFilePointer();
    }

    /**
     * Tag number reserved by Gene Myers for his tiff formatted files.
     */
    protected static final int TIFF_JF_TAGGER_TAG = 36036;

}
