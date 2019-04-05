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
        this.typ = getText(headerNode, "typ");
        this.alg = getText(headerNode, "alg");
        
        JsonNode payloadNode = mapper.readTree(payload);
        this.exp = getText(payloadNode, "exp");

        if (exp==null) {
            throw new IllegalStateException("JWT does not contain expiration date");
        }
        
        this.username = getText(payloadNode, "user_name");
        this.fullname = getText(payloadNode, "full_name");
        this.mail = getText(payloadNode, "mail");
        
    }
    
    private final String getText(JsonNode node, String name) {
        JsonNode mailNode = node.get(name);
        if (mailNode != null) {
            return mailNode.asText();
        }
        return null;
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
