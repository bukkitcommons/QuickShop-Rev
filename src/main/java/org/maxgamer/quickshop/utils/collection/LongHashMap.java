package org.maxgamer.quickshop.utils.collection;

import javax.annotation.concurrent.NotThreadSafe;
import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.compile.ConcurrentModificationUnchecked;
import com.koloboke.compile.KolobokeMap;

@KolobokeMap
@NotThreadSafe
@ConcurrentModificationUnchecked
public abstract class LongHashMap<V> implements HashLongObjMap<V> {
    public static <V> LongHashMap<V> withExpectedSize(int expectedSize) {
        return new KolobokeLongHashMap<V>(expectedSize);
    }
}
