package org.janelia.jacs2.dao.mongo.utils;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.math.BigInteger;

/**
 * EnumCodec implements a Codec for an enum type.
 */
public class EnumCodec<E extends Enum<E>> implements Codec<E> {

    private final Class<E> encoderClass;

    public EnumCodec(Class<E> encoderClass) {
        this.encoderClass = encoderClass;
    }

    @Override
    public E decode(BsonReader reader, DecoderContext decoderContext) {
        return Enum.valueOf(getEncoderClass(), reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, E value, EncoderContext encoderContext) {
        writer.writeString(value.name());
    }

    @Override
    public Class<E> getEncoderClass() {
        return encoderClass;
    }
}
