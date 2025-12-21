package com.example.multi_tanent.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class StringObjectDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isObject()) {
            if (node.has("value")) {
                return node.get("value").asText();
            } else if (node.has("label")) {
                return node.get("label").asText();
            } else if (node.has("name")) {
                return node.get("name").asText();
            } else if (node.has("id")) {
                return node.get("id").asText();
            }
            return node.toString(); // Fallback
        }
        return node.asText();
    }
}
