package org.janelia.jacs2.utils;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.math.BigInteger;

/**
 * BigIntegerCodec implements a Codec for a big integer type. The problem with the big ints is
 * that mongo as of 3.4 does not support big ints so I had to serialize this as a long. If I serialize it
 * as a string or binary the queries don't work so for now this is the only working option.
 */
public class BigIntegerCodec implements Codec<BigInteger> {

    @Override
    public BigInteger decode(BsonReader reader, DecoderContext decoderContext) {
        return BigInteger.valueOf(reader.readInt64());
    }

    @Override
    public void encode(BsonWriter writer, BigInteger value, EncoderContext encoderContext) {
        writer.writeInt64(value.longValue());
    }

    @Override
    public Class<BigInteger> getEncoderClass() {
        return BigInteger.class;
    }
}
