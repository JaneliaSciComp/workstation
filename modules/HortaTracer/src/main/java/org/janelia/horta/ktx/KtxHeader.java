package org.janelia.horta.ktx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author brunsc
 */
public class KtxHeader {
    
    private static final byte[] IDENTIFIER = new byte[] {
        (byte)0xAB, 
        (byte)0x4B,  
        (byte)0x54,  
        (byte)0x58,  
        (byte)0x20,  
        (byte)0x31,  
        (byte)0x31,  
        (byte)0xBB,  
        (byte)0x0D,  
        (byte)0x0A,  
        (byte)0x1A,  
        (byte)0x0A
    };
    private static final byte[] LITTLE_ENDIAN = new byte[] {1,2,3,4};
    private static final byte[] BIG_ENDIAN = new byte[] {4,3,2,1};

    public ByteOrder byteOrder;
    public int glType;
    int glTypeSize;
    public int glFormat;
    public int glInternalFormat;
    public int glBaseInternalFormat;
    public int pixelWidth, pixelHeight, pixelDepth;
    int numberOfArrayElements;
    int numberOfFaces;
    public int numberOfMipmapLevels;
    public Map<String, String> keyValueMetadata = new LinkedHashMap<>(); // must preserve key order!
    
    public KtxHeader loadStream(InputStream stream) throws IOException
    {
        // https://www.khronos.org/opengles/sdk/tools/KTX/file_format_spec/
        byte[] identifier = new byte[12];
        int readCount = stream.read(identifier, 0, 12);
        if (readCount != 12)
            throw new IOException("Could not read KTX header identifier");
        if (! Arrays.equals(identifier, IDENTIFIER))
            throw new IOException("KTX header identifier mismatch");
        
        byte[] endian = new byte[4];
        stream.read(endian, 0, 4);
        if (Arrays.equals(endian, LITTLE_ENDIAN))
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        else if (Arrays.equals(endian, BIG_ENDIAN))
            byteOrder = ByteOrder.BIG_ENDIAN;
        else {
            throw new IOException("Error parsing KTX byte order");
        }
        
        // Read a sequence of unsigned 32 bit ints
        int expected_read_size = 4 * 12; // twelve ints, to be exact
        ByteBuffer b = ByteBuffer.allocate(expected_read_size);
        int read_size = stream.read(b.array());
        if (read_size != expected_read_size) {
            throw new IOException("Error reading KTX integer parameters from stream");
        }
        b.order(byteOrder);
        b.rewind();
        glType = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        glTypeSize = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        glFormat = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        glInternalFormat = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        glBaseInternalFormat = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        pixelWidth = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        pixelHeight = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        pixelDepth = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        numberOfArrayElements = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        numberOfFaces = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        numberOfMipmapLevels = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        
        int bytes_of_key_value_data = (int)((long)b.getInt() & 0xffffffffL); // unsigned so &0xffffffffL
        ByteBuffer kv = ByteBuffer.allocate(bytes_of_key_value_data);
        read_size = stream.read(kv.array());
        if (read_size != bytes_of_key_value_data) {
            throw new IOException("Error reading KTX key-value metadata from stream");
        }
        kv.order(byteOrder);
        kv.rewind();
        byte[] unused = new byte[4]; // 
        while (kv.remaining() > 4) {
            int keyAndValueByteSize = (int)((long)kv.getInt() & 0xffffffffL);
            byte[] keyAndValue = new byte[keyAndValueByteSize];
            kv.get(keyAndValue);
            int padding = 3 - ((keyAndValueByteSize + 3) % 4);
            kv.get(unused, 0, padding);
            String s = new String(keyAndValue);
            // Separate into distinct key and value string
            int nullPos = s.indexOf(0);
            String key = s.substring(0, nullPos);
            String value = s.substring(nullPos + 1);
            keyValueMetadata.put(key, value);
        }
        return this;
    }
    
}
