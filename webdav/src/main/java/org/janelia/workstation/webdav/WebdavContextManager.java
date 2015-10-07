package org.janelia.workstation.webdav;

import java.io.IOException;
import java.util.*;
import java.io.File;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.jaxrs.config.BeanConfig;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;

/**
 * Created by schauderd on 6/25/15.
 */
@WebListener
public class WebdavContextManager implements ServletContextListener  {
    private static Map<String, Provider> providers = new HashMap<>();
    private static Map<String, Authorizer> authorizers = new HashMap<>();
    private static Map<String, FileShare> resourcesByMapping = new HashMap<>();
    private static Map<String, FileShare> resourcesByLogical = new HashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = (Map<String,Object>)mapper.readValue(new File(classLoader.getResource("resources.json").getFile()), Map.class);

            // load providers
            Map<String, Object> providersConfig = (Map<String,Object>)config.get("providers");
            if (providersConfig != null) {
                if (providersConfig.containsKey("ldap")) {
                    LDAPProvider ldap = new LDAPProvider();
                    Map<String, Object> ldapConfig = (Map<String,Object>)providersConfig.get("ldap");
                    ldap.setUrl((String)ldapConfig.get("url"));
                    ldap.setGroupAttribute((String) ldapConfig.get("groupattribute"));
                    ldap.init();
                    providers.put("ldap",ldap);
                }
                if (providersConfig.containsKey("scality")) {
                    JOSSProvider scality = new JOSSProvider();
                    Map<String, Object> scalityConfig = (Map<String,Object>)providersConfig.get("scality");
                    scality.setObjectUrl((String)scalityConfig.get("objectUrl"));
                    scality.setMetaUrl((String) scalityConfig.get("metaUrl"));
                    scality.setSearchUrl((String) scalityConfig.get("searchUrl"));
                    scality.setUser((String)scalityConfig.get("user"));
                    scality.setPassword((String)scalityConfig.get("password"));
                    scality.init();
                    providers.put("scality",scality);
                }
            }

            // load authorizers
            Map<String, Object> authorizeConfig = (Map<String,Object>)config.get("authorizers");
            if (authorizeConfig != null) {
                Iterator<String> authIter = authorizeConfig.keySet().iterator();
                while (authIter.hasNext()) {
                    String authKey = authIter.next();
                    Map<String,Object> newAuth = (Map<String,Object>)authorizeConfig.get(authKey);
                    if (newAuth.get("type").equals("ldapgroup")) {
                        String group = (String)newAuth.get("group");
                        try {
                            LDAPGroupAuthorizer ldapAuth = new LDAPGroupAuthorizer();
                            ldapAuth.setGroupDN(new Dn(group));
                            authorizers.put(authKey, ldapAuth);
                        } catch (LdapInvalidDnException e) {
                            throw new RuntimeException("DN used for group attribute " + group + " is not a valid DN");
                        }
                    } else if (newAuth.get("type").equals("user")) {
                        Map<String, String> users = (Map<String, String>)newAuth.get("users");
                        UsersAuthorizer userAuth = new UsersAuthorizer();
                        userAuth.setUserPasswords(users);
                        authorizers.put(authKey, userAuth);
                    }
                }
            }

            // load resources
            Map<String, Object> resourceConfig = (Map<String,Object>)config.get("resources");
            if (resourceConfig != null) {
                Iterator<String> resourceIter = resourceConfig.keySet().iterator();
                while (resourceIter.hasNext()) {
                    String resourceKey = resourceIter.next();
                    Map<String,Object> resourceInfo = (Map<String,Object>)resourceConfig.get(resourceKey);

                    // throw exception if required attributes not set
                    Set<String> reqAttributes = new HashSet<String>(Arrays.asList("type", "path", "mapping", "permissions"));
                    if (!resourceInfo.keySet().containsAll(reqAttributes)) {
                        throw new RuntimeException(resourceKey + " resource is missing required attributes");
                    }

                    FileShare newResource;
                    System.out.println (resourceInfo);
                    if (((String)resourceInfo.get("type")).equals("block")) {
                        newResource = new BlockFileShare();
                    } else newResource = new ObjectFileShare();
                    newResource.setMapping((String) resourceInfo.get("mapping"));
                    String newPath = ((String)resourceInfo.get("path")).toUpperCase();
                    newResource.setPath(Path.valueOf(newPath));
                    String authKey = (String)resourceInfo.get("authorizer");
                    if (authKey!=null) {
                        newResource.setAuthorizer(authorizers.get(authKey));
                    }
                    ArrayList<String> permissionsList = (ArrayList<String>) resourceInfo.get("permissions");
                    for (String permission : permissionsList) {
                        newResource.addPermission(Permission.valueOf(permission.toUpperCase()));
                    }
                    resourcesByMapping.put(newResource.getMapping(), newResource);
                    resourcesByLogical.put(resourceKey, newResource);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Provider> getProviders() {
        return providers;
    }

    public static void setProviders(Map<String, Provider> providers) {
        WebdavContextManager.providers = providers;
    }

    public static Map<String, Authorizer> getAuthorizers() {
        return authorizers;
    }

    public static void setAuthorizers(Map<String, Authorizer> authorizers) {
        WebdavContextManager.authorizers = authorizers;
    }

    public static Map<String, FileShare> getResourcesByMapping() {
        return resourcesByMapping;
    }

    public static void setResourcesByMapping(Map<String, FileShare> resourcesByMapping) {
        WebdavContextManager.resourcesByMapping = resourcesByMapping;
    }

    public static Map<String, FileShare> getResourcesByLogical() {
        return resourcesByLogical;
    }

    public static void setResourcesByLogical(Map<String, FileShare> resourcesByLogical) {
        WebdavContextManager.resourcesByLogical = resourcesByLogical;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
