package com.ai.application.model;

import com.ai.application.model.Entity.userData;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class UserDataListDeserializer extends JsonDeserializer<List<userData>> {
    private static final Logger logger = LoggerFactory.getLogger(UserDataListDeserializer.class);

    @Override
    public List<userData> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NULL || token == null) {
            return Collections.emptyList();
        } else if (token == JsonToken.START_OBJECT) {
            try {
                // Parse as single userData and wrap in list
                userData single = p.getCodec().readValue(p, userData.class);
                return List.of(single);
            } catch (Exception e) {
                logger.warn("Failed to parse userData object as single item; treating as empty list: {}", e.getMessage());
                return Collections.emptyList();
            }
        } else if (token == JsonToken.START_ARRAY) {
            // Delegate to default list deserialization
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(p, mapper.getTypeFactory().constructCollectionType(List.class, userData.class));
        } else {
            logger.warn("Unexpected token for userData: {}; treating as empty list", token);
            return Collections.emptyList();
        }
    }
}