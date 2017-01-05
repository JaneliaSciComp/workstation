package org.janelia.jacs2.utils;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

import java.util.Map;

/**
 * Map of Enum to Object Codec.
 */
public class MapOfEnumCodec<E extends Enum<E>, M extends Map<E, Object>> implements Codec<M> {
    private static final DocumentCodec DOCUMENT_CODEC = new DocumentCodec();

    private final Class<E> enumTypeClass;
    private final Class<M> mapTypeClass;

    public MapOfEnumCodec(Class<E> enumTypeClass, Class<M> mapTypeClass) {
        this.enumTypeClass = enumTypeClass;
        this.mapTypeClass = mapTypeClass;
    }

    @Override
    public M decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = DOCUMENT_CODEC.decode(reader, decoderContext);
        try {
            M map = mapTypeClass.newInstance();
            document.entrySet().forEach(e -> map.put(Enum.valueOf(enumTypeClass, e.getKey()), e.getValue()));
            return map;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void encode(BsonWriter writer, M mapValue, EncoderContext encoderContext) {
        Document doc = new Document();
        mapValue.forEach((k, v) -> doc.put(k.name(), v));
        DOCUMENT_CODEC.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<M> getEncoderClass() {
        return mapTypeClass;
    }
}
