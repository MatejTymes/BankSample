package mtymes.test.db;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;

import java.io.IOException;

import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;

// doc: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo
public class EmbeddedDB {

    private static final IFeatureAwareVersion V3_4_7 = Versions.withFeatures(new GenericVersion("3.4.7"), Feature.SYNC_DELAY, Feature.STORAGE_ENGINE, Feature.ONLY_64BIT, Feature.NO_CHUNKSIZE_ARG, Feature.MONGOS_CONFIGDB_SET_STYLE);

    private final int port;

    private MongodExecutable executable;
    private MongodProcess process;
    private boolean started = false;

    public EmbeddedDB(int port) {
        this.port = port;
    }

    public static EmbeddedDB embeddedDB() {
        try {
            int port = getFreeServerPort();

            return new EmbeddedDB(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized EmbeddedDB start() {
        if (started) {
            throw new IllegalStateException("Embedded MongoDB already started");
        }
        try {
            IMongodConfig config = new MongodConfigBuilder()
                    .version(V3_4_7) // Version.V3_4_1
                    .net(new Net(port, localhostIsIPv6()))
                    .build();
            Command command = Command.MongoD;
            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaults(command)
                    .artifactStore(new ExtractedArtifactStoreBuilder()
                            .defaults(command)
                            .download(new DownloadConfigBuilder()
                                    .defaultsForCommand(command)
                                    .artifactStorePath(new FixedPath("build/mongo"))
                                    .build())
                            .executableNaming(new UserTempNaming()))
                    .build();

            // todo: clear previous exe file

            MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
            executable = starter.prepare(config);
            process = executable.start();
            started = true;

            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (!started) {
            throw new IllegalStateException("Embedded MongoDB not started yet started");
        }
        process.stop();
        executable.stop();
    }


    public static void main(String[] args) throws IOException {
        EmbeddedDB embeddedDB = embeddedDB().start();

        embeddedDB.stop();
    }
}
