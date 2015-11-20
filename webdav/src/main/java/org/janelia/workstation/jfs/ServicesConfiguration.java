package org.janelia.workstation.jfs;

import java.io.IOException;
import java.util.*;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.mongodb.*;
import org.janelia.workstation.jfs.mongo.MongoConfiguration;
import org.janelia.workstation.jfs.security.Authorizer;
import org.janelia.workstation.jfs.security.LDAPGroupAuthorizer;
import org.janelia.workstation.jfs.security.Permission;
import org.janelia.workstation.jfs.security.UsersAuthorizer;
import org.jongo.Jongo;
import org.jongo.marshall.jackson.JacksonMapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.janelia.workstation.jfs.fileshare.*;

/**
 * Created by schauderd on 6/25/15.
 */
@WebListener
public class ServicesConfiguration implements ServletContextListener  {
    private static Map<String, Provider> providers = new HashMap<>();
    private static Map<String, Authorizer> authorizers = new HashMap<>();
    private static Map<String, FileShare> resourcesByMapping = new HashMap<>();
    private static Map<String, FileShare> resourcesByLogical = new HashMap<>();
    private static Map<String, MongoConfiguration> metaStores = new HashMap<>();
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ObjectMapper mapper = new ObjectMapper();
            File configFile = new File("/opt/jfs/config.json");
            if (!configFile.exists()) {
                throw new ServiceConfigurationError("Configuration file not found for Janelia File Services");
            }
            Map<String, Object> config = (Map<String,Object>)mapper.readValue(configFile, Map.class);

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
                    ScalityProvider scality = new ScalityProvider();
                    Map<String, Object> scalityConfig = (Map<String,Object>)providersConfig.get("scality");
                    Map<String, Object> rings = (Map<String, Object>)scalityConfig.get("rings");
                    Iterator<String> ringIter = rings.keySet().iterator();
                    Map<String, String> ringDefs = new HashMap<String,String>();
                    while (ringIter.hasNext()) {
                        String ringKey = ringIter.next();
                        ringDefs.put(ringKey, (String)rings.get(ringKey));
                    }
                    scality.setRings(ringDefs);
                    scality.init();
                    providers.put("scality", scality);
                }
            }

            // load metadata persistence stores
            Map<String, Object> metadataConfig = (Map<String,Object>)config.get("metadata");
            if (metadataConfig != null) {
                Iterator<String> metaIter = metadataConfig.keySet().iterator();
                while (metaIter.hasNext()) {
                    String metaKey = metaIter.next();
                    Map<String, Object> metadataPersistent = (Map<String,Object>)metadataConfig.get(metaKey);
                    String[] members = ((String)metadataPersistent.get("host")).split(",");
                    ServerAddress[] replicaUrls = new ServerAddress[members.length];
                    String port = (metadataPersistent.get("port")==null)?"27017": (String)metadataPersistent.get("port");
                    for (int i=0; i<members.length; i++) {
                        replicaUrls[i] = new ServerAddress(members[i] + ":"+port);
                    }
                    String username = (String)metadataPersistent.get("username");
                    String password = (String)metadataPersistent.get("password");
                    String databaseName = (String)metadataPersistent.get("database");
                    MongoClientOptions options = new MongoClientOptions.Builder()
                            .connectionsPerHost(100).minConnectionsPerHost(100)
                            .build();
                    MongoClient m;
                    if (username != null && password != null) {
                        MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
                        m = new MongoClient(Arrays.asList(replicaUrls), Arrays.asList(credential));
                    }
                    else {
                        m = new MongoClient(Arrays.asList(replicaUrls),options);
                    }
                    DB db = m.getDB(databaseName);
                    Jongo jongo = new Jongo(db,
                            new JacksonMapper.Builder()
                                    .enable(MapperFeature.AUTO_DETECT_GETTERS)
                                    .enable(MapperFeature.AUTO_DETECT_SETTERS)
                                    .build());
                    MongoConfiguration mc = new MongoConfiguration();
                    mc.setClient(m);
                    mc.setJongo(jongo);
                    mc.setCollection((String)metadataPersistent.get("collection"));
                    metaStores.put(metaKey, mc);
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
                    if (((String)resourceInfo.get("type")).equals("block")) {
                        newResource = new BlockFileShare();
                    } else {
                        newResource = new ObjectFileShare();
                        ((ObjectFileShare)newResource).setRing((String) resourceInfo.get("ring"));
                        MongoConfiguration metadata = metaStores.get(resourceInfo.get("metadata"));
                        ((ObjectFileShare) newResource).setMongo(metadata);
                        scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.scheduleAtFixedRate((ObjectFileShare)newResource, 0, 6, TimeUnit.HOURS);
                    }
                    // default admin user
                    if ((String)resourceInfo.get("adminUser") != null) {
                        newResource.setAdminUser((String)resourceInfo.get("adminUser"));
                    }
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

                    // initialize filestore
                    newResource.init();
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
        ServicesConfiguration.providers = providers;
    }

    public static Map<String, Authorizer> getAuthorizers() {
        return authorizers;
    }

    public static void setAuthorizers(Map<String, Authorizer> authorizers) {
        ServicesConfiguration.authorizers = authorizers;
    }

    public static Map<String, FileShare> getResourcesByMapping() {
        return resourcesByMapping;
    }

    public static void setResourcesByMapping(Map<String, FileShare> resourcesByMapping) {
        ServicesConfiguration.resourcesByMapping = resourcesByMapping;
    }

    public static Map<String, FileShare> getResourcesByLogical() {
        return resourcesByLogical;
    }

    public static void setResourcesByLogical(Map<String, FileShare> resourcesByLogical) {
        ServicesConfiguration.resourcesByLogical = resourcesByLogical;
    }

    public static Map<String, MongoConfiguration> getMetaStores() {
        return metaStores;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        Iterator<MongoConfiguration> mongos = metaStores.values().iterator();
        while (mongos.hasNext()) {
            mongos.next().getClient().close();
        }
        scheduler.shutdownNow();
    }
}
