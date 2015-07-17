package org.janelia.jos.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import com.google.common.base.Optional;

/**
 * Very simple insecure authentication.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleAuthenticator implements Authenticator<BasicCredentials, SimplePrincipal> {

    @Override
    public Optional<SimplePrincipal> authenticate(BasicCredentials credentials) throws AuthenticationException {
        if ("jos".equals(credentials.getPassword())) {
            return Optional.of(new SimplePrincipal(credentials.getUsername()));
        }
        return Optional.absent();
    }
    
}