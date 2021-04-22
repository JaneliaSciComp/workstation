package org.janelia.workstation.core.model.local;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.janelia.workstation.integration.util.FrameworkAccess;

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
        mongo = new MongoClient("localhost", port);
    }

    public static void cleanupConnection() {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }
}