package org.janelia.it.workstation.browser.api.web;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.auth.RFC2617Scheme;
import org.janelia.it.workstation.browser.api.FileMgr;

public class JwtAuthScheme implements AuthScheme {

    public static final String JWT_AUTH_SCHEME_NAME = "jwt";

    @Override
    public void processChallenge(String challenge) throws MalformedChallengeException {
        // do nothing here
    }

    @Override
    public String getParameter(String s) {
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public String getID() {
        return JWT_AUTH_SCHEME_NAME;
    }

    @Override
    public String getSchemeName() {
        return JWT_AUTH_SCHEME_NAME;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException {
        JWTCredentials jwtCredentials = null;
        try {
            jwtCredentials = (JWTCredentials) credentials;
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException("Credentials cannot be used for JWT authentication", e);
        }
        return authenticate(jwtCredentials);
    }

    @Override
    public String authenticate(Credentials credentials, HttpMethod httpMethod) throws AuthenticationException {
        JWTCredentials jwtCredentials = null;
        try {
            jwtCredentials = (JWTCredentials) credentials;
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException("Credentials cannot be used for JWT authentication", e);
        }
        return authenticate(jwtCredentials);
    }

    private String authenticate(JWTCredentials jwtCredentials) {
        return "Bearer " + jwtCredentials.getJwtToken();
    }
}
