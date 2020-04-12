package de.tum.in.www1.artemis.config.kafka;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaHashMap<K, V> implements Map<K, V> {

    private static final Logger log = LoggerFactory.getLogger(KafkaHashMap.class);

    private Map<K, V> writeHashMap = new ConcurrentHashMap<>();

    private Map<K, V> readHashMap = new ConcurrentHashMap<>();

    private final String hashMapId;

    private KafkaHashMapService kafkaHashMapService;

    public KafkaHashMap(String hashMapId, KafkaHashMapService kafkaHashMapService) {
        this.hashMapId = hashMapId;
        this.kafkaHashMapService = kafkaHashMapService;
    }

    @Override
    public int size() {
        return writeHashMap.size() + readHashMap.size();
    }

    @Override
    public boolean isEmpty() {
        return writeHashMap.isEmpty() && readHashMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return writeHashMap.containsKey(o) || readHashMap.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return writeHashMap.containsValue(o) || readHashMap.containsValue(o);
    }

    @Override
    public V get(Object o) {
        V v = writeHashMap.get(o);
        if (v != null) {
            return v;
        }
        return readHashMap.get(v);
    }

    @Nullable
    @Override
    public V put(K k, V v) {
        readHashMap.remove(k); // Remove from readHashMap as the new value is newer
        kafkaHashMapService.putUpdate(hashMapId, k, v);
        // TODO distribute to other hashmaps
        return writeHashMap.put(k, v);
    }

    @Override
    public V remove(Object o) {
        V vRead = readHashMap.remove(o);
        V vWrite = writeHashMap.remove(o);
        // TODO distribute to other hashmaps

        return vWrite != null ? vWrite : vRead;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        readHashMap.clear();
        writeHashMap.clear();
        // TODO distribute to other hashmaps
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>(writeHashMap.keySet());
        keys.addAll(readHashMap.keySet());

        return keys;
    }

    @NotNull
    @Override
    public Collection<V> values() {
        Collection<V> values = new HashSet<>(writeHashMap.values());
        values.addAll(readHashMap.values());
        return values;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = writeHashMap.entrySet();
        entrySet.addAll(readHashMap.entrySet());
        return entrySet;
    }
}
