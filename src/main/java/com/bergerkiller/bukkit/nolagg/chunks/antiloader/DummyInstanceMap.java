package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.HashMap;

public class DummyInstanceMap extends HashMap<Long, Object> {
    public static boolean ENABLED = false;
    private final DummyPlayerManager manager;

    public DummyInstanceMap(Object oldMap, DummyPlayerManager playerManager) {
        this.manager = playerManager;
    }
}

/*
public class DummyInstanceMap extends LongHashMapBase {
    public static boolean ENABLED = false;
    private final DummyPlayerManager manager;

    public DummyInstanceMap(LongHashMap<Object> oldMap, DummyPlayerManager playerManager) {
        this.manager = playerManager;
        for (Object value : oldMap.getValues()) {
            DummyInstancePlayerList.replace(this.manager, value);
        }
        LongHashMapRef.entriesField.transfer(oldMap.getHandle(), this);
    }

    @Override
    public void put(long key, Object value) {
        if (ENABLED) {
            DummyInstancePlayerList.replace(this.manager, value);
        }
        super.put(key, value);
    }
}
*/