package org.janelia.jacs2.utils;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.math.BigInteger;

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
