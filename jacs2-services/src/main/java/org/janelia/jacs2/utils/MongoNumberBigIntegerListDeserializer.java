package org.janelia.jacs2.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MongoNumberBigIntegerListDeserializer extends JsonDeserializer<List<Number>> {

    @Override
    public List<Number> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode listNode = jsonParser.readValueAsTree();
        List<Number> res = new ArrayList<>();
        listNode.iterator().forEachRemaining(node -> {
            if (node.get("$numberLong") != null) {
                res.add(new BigInteger(node.get("$numberLong").asText()));
            } else {
                res.add(new BigInteger(node.asText()));
            }
        });
        return res;
    }
}
