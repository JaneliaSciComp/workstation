package org.janelia.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from Apache HttpClient. A streamed, non-repeatable entity that obtains its 
 * content from an {@link InputStream}. Modifications from InputStreamEntity:
 * <ol>
 * <li>If the content length is unspecified then this entity will records how many bytes it has streamed and 
 *     make the apparent content length available.</li>
 * <li>If compress is true, then the stream will be compressed as it is being sent.</li> 
 * </ol>
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 * @see org.apache.http.entity.InputStreamEntity
 */
@NotThreadSafe
public class MeasuringInputStreamEntity extends AbstractHttpEntity {

    private static final Logger log = LoggerFactory.getLogger(MeasuringInputStreamEntity.class);
    
    private final InputStream content;
    private long inputLength = -1;
    private long outputLength = -1;
    private boolean compress = false;

    /**
     * Creates an entity with an unknown length.
     * Equivalent to {@code new InputStreamEntity(instream, -1)}.
     *
     * @param instream input stream
     * @throws IllegalArgumentException if {@code instream} is {@code null}
     * @since 4.3
     */
    public MeasuringInputStreamEntity(final InputStream instream, boolean compress) {
        this(instream, -1, compress);
    }

    /**
     * Creates an entity with a specified content length.
     *
     * @param instream input stream
     * @param length of the input stream, {@code -1} if unknown
     * @throws IllegalArgumentException if {@code instream} is {@code null}
     */
    public MeasuringInputStreamEntity(final InputStream instream, final long length, boolean compress) {
        this(instream, length, null, compress);
    }

    /**
     * Creates an entity with a content type and unknown length.
     * Equivalent to {@code new InputStreamEntity(instream, -1, contentType)}.
     *
     * @param instream input stream
     * @param contentType content type
     * @throws IllegalArgumentException if {@code instream} is {@code null}
     * @since 4.3
     */
    public MeasuringInputStreamEntity(final InputStream instream, final ContentType contentType, boolean compress) {
        this(instream, -1, contentType, compress);
    }

    /**
     * @param instream input stream
     * @param length of the input stream, {@code -1} if unknown
     * @param contentType for specifying the {@code Content-Type} header, may be {@code null}
     * @throws IllegalArgumentException if {@code instream} is {@code null}
     * @since 4.2
     */
    public MeasuringInputStreamEntity(final InputStream instream, final long length, final ContentType contentType, boolean compress) {
        super();
        this.content = Args.notNull(instream, "Source input stream");
        this.inputLength = length;
        this.compress = compress;
        if (contentType != null) {
            setContentType(contentType.toString());
        }
    }

    public boolean isRepeatable() {
        return false;
    }

    /**
     * @return the content length or {@code -1} if unknown
     */
    public long getContentLength() {
        return this.outputLength;
    }

    public InputStream getContent() throws IOException {
        return this.content;
    }

    /**
     * Writes bytes from the {@code InputStream} this entity was constructed
     * with to an {@code OutputStream}.  The content length
     * determines how many bytes are written.  If the length is unknown ({@code -1}), the
     * stream will be completely consumed (to the end of the stream).
     *
     */
    public void writeTo(OutputStream outstream) throws IOException {

        CountingOutputStream counted = new CountingOutputStream(outstream);
        OutputStream out = counted;
        
        if (compress) {
            log.debug("Compressing data with bzip2");
            out = new BZip2CompressorOutputStream(counted);
        }
        
        final InputStream instream = this.content;
        try {
            final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
            int l;
            if (this.inputLength < 0) {
                long len = 0;
                // consume until EOF
                while ((l = instream.read(buffer)) != -1) {
                    out.write(buffer, 0, l);
                    len += l;
                }
                this.inputLength = len;
            } 
            else {
                // consume no more than length
                long remaining = this.inputLength;
                while (remaining > 0) {
                    l = instream.read(buffer, 0, (int)Math.min(OUTPUT_BUFFER_SIZE, remaining));
                    if (l == -1) {
                        break;
                    }
                    out.write(buffer, 0, l);
                    remaining -= l;
                }
            }
        } 
        finally {
            instream.close();
            out.close();
        }
        
        this.outputLength = counted.getByteCount();
        log.debug("in={}, out={}",this.inputLength,this.outputLength);
    }

    public boolean isStreaming() {
        return true;
    }
}
