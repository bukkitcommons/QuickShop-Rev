package org.maxgamer.quickshop.utils.collection;

import javax.annotation.concurrent.NotThreadSafe;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.compile.ConcurrentModificationUnchecked;
import com.koloboke.compile.KolobokeMap;

@KolobokeMap
@NotThreadSafe
@ConcurrentModificationUnchecked
public abstract class ObjectsHashMap<K, V> implements HashObjObjMap<K, V> {
    public static <K, V> ObjectsHashMap<K, V> withExpectedSize(int expectedSize) {
        return new KolobokeObjectsHashMap<K, V>(expectedSize);
    }
}
