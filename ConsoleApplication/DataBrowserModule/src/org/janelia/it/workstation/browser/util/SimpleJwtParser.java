package org.janelia.it.workstation.browser.util;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extremely simple JWT payload parser, without all the complicated encryption code.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleJwtParser {

    private String token;
    private String typ;
    private String alg;
    private String exp;
    private String username;
    private String fullname;
    private String mail;
    
    public SimpleJwtParser(String token) throws JsonProcessingException, IOException {
        this.token = token;
    
        String[] s = token.split("\\.");
        
        if (s.length<3) {
            throw new IllegalStateException("Invalid JWT token");
        }
        
        String headerJson = new String(Base64.getDecoder().decode(s[0]));
        String payload = new String(Base64.getDecoder().decode(s[1]));

        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode headerNode = mapper.readTree(headerJson);
        this.typ = headerNode.get("typ").asText();
        this.alg = headerNode.get("alg").asText();
        
        JsonNode payloadNode = mapper.readTree(payload);
        this.exp = payloadNode.get("exp").asText();
        this.username = payloadNode.get("user_name").asText();
        this.fullname = payloadNode.get("full_name").asText();
        this.mail = payloadNode.get("mail").asText();
    }

    public String getToken() {
        return token;
    }

    public String getTyp() {
        return typ;
    }

    public String getAlg() {
        return alg;
    }

    public String getExp() {
        return exp;
    }

    public String getUsername() {
        return username;
    }

    public String getFullname() {
        return fullname;
    }

    public String getMail() {
        return mail;
    }
}
