package org.janelia.workstation.core.model.local;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.janelia.workstation.integration.util.FrameworkAccess;

import java.util.Arrays;

public class LocalMongoService {
    private static MongoClient mongo;
    private static MongodExecutable mongodExecutable = null;

    public static MongoClient getClient() {
        if (mongo == null)
            setupConnection();
        return mongo;
    }

    public synchronized static void setupConnection () {
        Storage replication = new Storage("C:/JaneliaWorkstation/database",null,0);
        MongodStarter starter = MongodStarter.getDefaultInstance();
        int port = 15672;
        try {
            port = Network.getFreeServerPort();
            MongodConfig mongodConfig = MongodConfig.builder()
                    .version(Version.Main.PRODUCTION)
                    .net(new Net(port, Network.localhostIsIPv6()))
                    .replication(replication)
                    .build();
            mongodExecutable = starter.prepare(mongodConfig);
            MongodProcess mongod = mongodExecutable.start();

        } catch (Exception e) {
            FrameworkAccess.handleException(new RuntimeException("Problems initiating local database connection"));
        }

        MongoClientSettings.Builder mongoClientSettingsBuilder = MongoClientSettings.builder()
                .codecRegistry(createCodecRegistryWithJacksonEncoder());
        ClusterSettings clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(
                new ServerAddress[]{new ServerAddress("localhost", port)})).build();
        mongoClientSettingsBuilder.applyToClusterSettings(builder1 -> builder1.applySettings(clusterSettings));
        mongo = MongoClients.create(mongoClientSettingsBuilder.build());
    }

    public static void cleanupConnection() {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }

    public static CodecRegistry createCodecRegistry() {
        return CodecRegistries.fromRegistries(
                com.mongodb.MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(
                        new ReferenceCodec(),
                        new BigIntegerCodec(),
                        new ClassCodec()
                ),
                CodecRegistries.fromProviders(new EnumCodecProvider())
        );
    }

    public static CodecRegistry createCodecRegistryWithJacksonEncoder() {
        return CodecRegistries
                .fromRegistries(
                        createCodecRegistry(),
                        CodecRegistries.fromProviders(new JacksonCodecProvider(
                                new ObjectMapper().registerModule(new MongoModule())))
                );
    }
}