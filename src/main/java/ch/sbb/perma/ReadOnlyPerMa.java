/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.perma;

import ch.sbb.perma.datastore.KeyOrValueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Read only persistent map.
 * <p>
 *     Is the public PerMa API for immutable maps. Loads a persisted map and updates it on request.
 * </p>
 *
 * @author u206123 (Florian Seidl)
 * @since 1.0, 2017.
 */
public class ReadOnlyPerMa<K,V> implements PerMa<K,V> {
    private final static Logger LOG = LoggerFactory.getLogger(ReadOnlyPerMa.class);

    private MapSnapshot<K,V> lastLoaded;
    private final ReentrantLock loadLock = new ReentrantLock();

    private ReadOnlyPerMa(MapSnapshot<K, V> loaded) {
        this.lastLoaded = loaded;
    }

    public static ReadOnlyPerMa loadStringMap(File dir, String name) throws IOException {
        return load(dir,
                    name,
                    KeyOrValueSerializer.STRING,
                    KeyOrValueSerializer.STRING);
    }

    public static <K,V> ReadOnlyPerMa load(File dir,
                                              String name,
                                              KeyOrValueSerializer<K> keySerializer,
                                              KeyOrValueSerializer<V> valueSerializer) throws IOException {
        LOG.info("Loading readonly PerMa {} from directory {}", name, dir);
        return new ReadOnlyPerMa<>(MapSnapshot.loadOrCreate(dir, name, keySerializer, valueSerializer));
    }

    public void udpate() throws IOException {
        try {
            loadLock.lock();
            LOG.debug("Updating map");
            lastLoaded = lastLoaded.refresh();
            LOG.debug("Loaded map snapshot with {} entries", lastLoaded.asImmutableMap().size());
        }
        finally {
            loadLock.unlock();
        }
    }


    @Override
    public Map<K, V> map() {
        return lastLoaded.asImmutableMap();
    }
}