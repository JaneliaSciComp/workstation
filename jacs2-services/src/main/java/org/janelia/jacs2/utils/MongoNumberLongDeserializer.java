package org.janelia.jacs2.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigInteger;

public class MongoNumberLongDeserializer extends JsonDeserializer<Number> {

    @Override
    public Number deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.readValueAsTree();
        if (node.get("$numberLong") != null) {
            return Long.valueOf(node.get("$numberLong").asText());
        } else {
            return Long.valueOf(node.asText());
        }
    }
}
