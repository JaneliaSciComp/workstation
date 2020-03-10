package org.janelia.gltools;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MJ2Parser {
    enum BOXTYPE {
        JP2 ("6a502020"), FTYP ("66747970"),
        MDAT ("6d646174"), MVHD ("6d766864"),
        TRAK ("7472616b"), TKHD ("746b6864"),
        MOOV ("6d6f6f76"), MDIA ("6d646961"),
        MDHD ("6d646864"), HDLR  ("68646C72"),
        MINF ("6d696e66"), STBL ("7374626c"),
        VMHD ("766d6864"), DINF  ("64696e66"),
        STSD ("73747364"), MJ2 ("6d6a7032"),
        STTS ("73747473"), STSC ("73747363"),
        STSZ ("7374737a"), STCO ("7374636f");

        String hexValue;
        BOXTYPE (String value) {
            hexValue = value;
        }

        public static BOXTYPE fromString(String type) {
            for (BOXTYPE b : BOXTYPE.values()) {
                if (b.hexValue.equalsIgnoreCase(type)) {
                    return b;
                }
            }
            return null;
        }

    }
    int width;
    int height;
    int numFrames;
    int sampleSize[];
    int sampleOffset[];

    public MJ2Parser() {

    }

    private boolean parseHeader(InputStream dataStream) throws IOException {
        // JP2 header
        byte[] jp2box = new byte[12];
        dataStream.read(jp2box);
        StringBuilder sb = new StringBuilder();
        for (byte b : jp2box) {
            sb.append(String.format("%02x", b));
        }

        // FTYP header
        BoxInfo ftypHeader = new BoxInfo();
        ftypHeader.readBoxHeader(dataStream);
        if (ftypHeader.type == BOXTYPE.FTYP) {
            // skip to next header
            dataStream.skip(ftypHeader.length - 8);
        }

        // MDAT header
        BoxInfo header = new BoxInfo();
        header.readBoxHeader(dataStream);
        if (header.type == BOXTYPE.MDAT) {
            // skip to next header
            dataStream.skip(header.length - 16);
        }

        // MOOV header
        BoxInfo moovHeader = new BoxInfo();
        moovHeader.readBoxHeader(dataStream);

        // MVHD header
        BoxInfo mvhdHeader = new BoxInfo();
        mvhdHeader.readBoxHeader(dataStream);
        if (mvhdHeader.type == BOXTYPE.MVHD) {
            // ignore most of the information, since we don't need it
            dataStream.skip(16);
            byte[] jp2_info = new byte[4];
            dataStream.read(jp2_info);
            numFrames = ByteBuffer.wrap(jp2_info, 0, 4).getInt() / 10;
            dataStream.skip(mvhdHeader.length - 28);
        }

        // TRAK header
        BoxInfo trakHeader = new BoxInfo();
        trakHeader.readBoxHeader(dataStream);

        // TKHD header
        BoxInfo tkhdHeader = new BoxInfo();
        tkhdHeader.readBoxHeader(dataStream);
        if (tkhdHeader.type == BOXTYPE.TKHD) {
            dataStream.skip(tkhdHeader.length - 8);
        }

        // MDIA header
        BoxInfo mdiaHeader = new BoxInfo();
        mdiaHeader.readBoxHeader(dataStream);

        // MDHD header
        BoxInfo mdhdHeader = new BoxInfo();
        mdhdHeader.readBoxHeader(dataStream);
        if (mdhdHeader.type == BOXTYPE.MDHD) {
            dataStream.skip(mdhdHeader.length - 8);
        }

        // HDLR header
        BoxInfo hdlrHeader = new BoxInfo();
        hdlrHeader.readBoxHeader(dataStream);
        if (hdlrHeader.type == BOXTYPE.HDLR) {
            dataStream.skip(hdlrHeader.length - 8);
        }

        // MINF header
        BoxInfo minfHeader = new BoxInfo();
        minfHeader.readBoxHeader(dataStream);

        // VMHD header
        BoxInfo vmhdHeader = new BoxInfo();
        vmhdHeader.readBoxHeader(dataStream);
        if (vmhdHeader.type == BOXTYPE.VMHD) {
            dataStream.skip(vmhdHeader.length - 8);
        }

        // DINF header
        BoxInfo dinfHeader = new BoxInfo();
        dinfHeader.readBoxHeader(dataStream);
        if (dinfHeader.type == BOXTYPE.DINF) {
            dataStream.skip(dinfHeader.length - 8);
        }

        // STBL header
        BoxInfo stblHeader = new BoxInfo();
        stblHeader.readBoxHeader(dataStream);

        // STSD header
        BoxInfo stsdHeader = new BoxInfo();
        stsdHeader.readBoxHeader(dataStream);
        if (stsdHeader.type == BOXTYPE.STSD) {
            dataStream.skip(8);
        }

        // MJ2 header
        BoxInfo mj2Header = new BoxInfo();
        mj2Header.readBoxHeader(dataStream);
        if (mj2Header.type == BOXTYPE.MJ2) {
            dataStream.skip(24);
            byte[] dims = new byte[4];
            dataStream.read(dims);
            width = ByteBuffer.wrap(dims, 0, 2).getShort();
            height = ByteBuffer.wrap(dims, 2, 2).getShort();
            dataStream.skip(mj2Header.length - 36);
        }

        // STTS header
        BoxInfo sttsHeader = new BoxInfo();
        sttsHeader.readBoxHeader(dataStream);
        if (sttsHeader.type == BOXTYPE.STTS) {
            dataStream.skip(sttsHeader.length - 8);
        }

        // STSC header
        BoxInfo stscHeader = new BoxInfo();
        stscHeader.readBoxHeader(dataStream);
        if (stscHeader.type == BOXTYPE.STSC) {
            dataStream.skip(stscHeader.length - 8);
        }

        // STSZ header
        BoxInfo stszHeader = new BoxInfo();
        stszHeader.readBoxHeader(dataStream);
        sampleSize = new int[1];
        if (stszHeader.type == BOXTYPE.STSZ) {
            dataStream.skip(8);

            byte[] sampleCount = new byte[4];
            dataStream.read(sampleCount);
            int numSamples = ByteBuffer.wrap(sampleCount, 0, 4).getInt();
            sampleSize = new int[numSamples];

            for (int i = 0; i < numSamples; i++) {
                byte[] size = new byte[4];
                dataStream.read(size);
                sampleSize[i] = ByteBuffer.wrap(size, 0, 4).getInt();
            }
        }

        // STCO header
        BoxInfo stcoHeader = new BoxInfo();
        stcoHeader.readBoxHeader(dataStream);
        if (stcoHeader.type == BOXTYPE.STCO) {
            dataStream.skip(4);
            byte[] sampleCount = new byte[4];
            dataStream.read(sampleCount);
            int numSamples = ByteBuffer.wrap(sampleCount, 0, 4).getInt();
            sampleOffset = new int[numSamples];
            for (int i = 0; i < numSamples; i++) {
                byte[] offset = new byte[4];
                dataStream.read(offset);
                sampleOffset[i] = ByteBuffer.wrap(offset, 0, 4).getInt();
            }
        }
        return true;
    }

    public Pair<Raster[], ColorModel> extractSlices (InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(in, baos);
        byte[] dataStreamBytes = baos.toByteArray();

        // parse header
        ByteArrayInputStream dataStream = new ByteArrayInputStream(dataStreamBytes);
        parseHeader(dataStream);
        dataStream.close();

        // extract slices
        BufferedImage image0 = null;
        dataStream = new ByteArrayInputStream(dataStreamBytes);
        if (numFrames>0) {
            Raster[] slices = new Raster[numFrames];
            ColorModel cm = null;
            for (int i = 0; i < numFrames; i++) {
                long start = sampleOffset[i] + 8;
                long end = start + sampleSize[i] - 8;
                byte[] jp2Slice = Arrays.copyOfRange(dataStreamBytes,(int)start, (int)end);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(jp2Slice));
                slices[i] = image.getData();
                if (i==0)
                    image0 = image;
            }
            cm = image0.getColorModel();
            dataStream.close();
            return Pair.of(slices, cm);
        }

        return null;
    }

    public class BoxInfo {

        public int length;
        public BOXTYPE type;

        public void readBoxHeader(InputStream stream) throws IOException {
            byte[] boxHeader = new byte[8];
            stream.read(boxHeader);
            length = ByteBuffer.wrap(boxHeader,0,4).getInt();
            String hexString = Integer.toHexString(ByteBuffer.wrap(boxHeader,4,4).getInt());
            type = BOXTYPE.fromString(hexString);
            if (type==null)
                return;
            if (length==1) {
                stream.read(boxHeader);
                length = ByteBuffer.wrap(boxHeader,4,4).getInt();
            }
        }
    }
}
