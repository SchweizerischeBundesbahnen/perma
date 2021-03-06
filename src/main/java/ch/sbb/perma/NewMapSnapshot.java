/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.perma;

import ch.sbb.perma.datastore.MapFileData;
import ch.sbb.perma.file.FileGroup;
import ch.sbb.perma.serializers.KeyOrValueSerializer;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * A snapshot of a map that was never persisted.
 * <p>
 *      Create a new persisted snapshot of the map.
 * </p>
 *
 * @author u206123 (Florian Seidl)
 * @since 1.0, 2017.
 */
class NewMapSnapshot<K,V> implements MapSnapshot<K,V> {
    private static final Logger LOG = LoggerFactory.getLogger(MapSnapshot.class);

    private final String name;
    private final FileGroup files;
    private final Options options;
    private final KeyOrValueSerializer<K> keySerializer;
    private final KeyOrValueSerializer<V> valueSerializer;

    NewMapSnapshot(String name, FileGroup directory, Options options, KeyOrValueSerializer<K> keySerializer, KeyOrValueSerializer<V> valueSerializer) {
        LOG.debug("Creating new Snapshot");
        this.name = name;
        this.files = directory;
        this.options = options;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public MapSnapshot<K,V> writeNext(Map<K,V> current)  throws IOException {
        return writeNext(ImmutableMap.copyOf(current));
    }

    private MapSnapshot<K,V> writeNext(ImmutableMap<K,V> currentImmutable)  throws IOException{
        if(currentImmutable.isEmpty() && !files.exists()) {
            LOG.debug("Noting to write (map is not yet peristed and still empty), ignoring");
            return this;
        }
        FileGroup newFullFileGroup = files.withNextFull(options.compression());
        LOG.debug("Writing full file with mapSize={} to file {} after deleting stale temp files",
                currentImmutable.size(),
                newFullFileGroup.fullFile());
        MapFileData<K,V> fullData = MapFileData
                                .createNewFull(name, currentImmutable)
                                .writeTo(newFullFileGroup.fullFile(),
                                        keySerializer,
                                        valueSerializer);
        return new PersistedMapSnapshot<>(
                name,
                newFullFileGroup,
                options,
                currentImmutable,
                fullData,
                keySerializer,
                valueSerializer);
    }

    @Override
    public MapSnapshot<K, V> refresh() throws IOException {
        FileGroup refreshedFiles = files.refresh();
        if(!refreshedFiles.exists()) {
            LOG.debug("No file found, cancelling refresh");
            return this;
        }
        return PersistedMapSnapshot.load(name, refreshedFiles, options, keySerializer, valueSerializer);
    }

    @Override
    public MapSnapshot<K, V> compact() {
        return this;
    }

    @Override
    public ImmutableMap<K,V> asImmutableMap() {
        return ImmutableMap.of();
    }
}
