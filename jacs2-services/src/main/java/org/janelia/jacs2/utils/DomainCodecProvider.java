package org.janelia.jacs2.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.jacs2.model.domain.DomainObject;

import java.io.IOException;
import java.io.UncheckedIOException;

public class DomainCodecProvider implements CodecProvider {

    private final ObjectMapper objectMapper;

    public DomainCodecProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (DomainObject.class.isAssignableFrom(clazz)) {
            final Codec<RawBsonDocument> rawBsonDocumentCodec = registry.get(RawBsonDocument.class);
            return new Codec<T>() {
                @Override
                public T decode(BsonReader reader, DecoderContext decoderContext) {
                    try {
                        RawBsonDocument document = rawBsonDocumentCodec.decode(reader, decoderContext);
                        return objectMapper.readValue(document.toJson(), clazz);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
                    try {
                        String json = objectMapper.writeValueAsString(value);
                        rawBsonDocumentCodec.encode(writer, RawBsonDocument.parse(json), encoderContext);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public Class<T> getEncoderClass() {
                    return clazz;
                }
            };
        }
        return null;
    }

}
