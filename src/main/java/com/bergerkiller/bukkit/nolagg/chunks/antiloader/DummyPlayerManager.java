package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.reflection.net.minecraft.server.NMSPlayerChunk;
import com.bergerkiller.reflection.net.minecraft.server.NMSPlayerChunkMap;
import com.bergerkiller.reflection.net.minecraft.server.NMSWorldServer;
import com.bergerkiller.reflection.org.bukkit.craftbukkit.CBAsynchronousExecutor;
import com.bergerkiller.reflection.org.bukkit.craftbukkit.CBChunkIOExecutor;

import net.minecraft.server.v1_11_R1.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public class DummyPlayerManager extends PlayerChunkMapBase {
    // private static final DummyWorldServer DUMMYWORLD = DummyWorldServer.newInstance();
    private static final String playerInstanceClassName = CommonUtil.getNMSClass("PlayerChunk").getName();
    public final Object base;
    public final World world;
    private final DummyInstanceMap instances;
    private final Set<?> dirtyChunkQueue;
    private Object recentChunk;
    private boolean wasLoaded;
    private boolean isRecent;

    public DummyPlayerManager(World world) {
        this(NMSWorldServer.playerChunkMap.get(Conversion.toWorldHandle.convert(world)), world);
    }

    public DummyPlayerManager(final Object base, World world) {
        super(world, 10);
        this.world = world;
        this.instances = null;
        //this.instances = new DummyInstanceMap(NMSPlayerChunkMap.playerInstances.get(base), this);
        //NMSPlayerChunkMap.playerInstances.setInternal(base, this.instances);
        //NMSPlayerChunkMap.T.transfer(base, this);
        this.base = base;
        this.dirtyChunkQueue = NMSPlayerChunkMap.dirtyBlockChunks.get(base);
    }

    public static void convertAll() {
        // Alter player manager to prevent chunk loading outside range
        for (World world : Bukkit.getWorlds()) {
            DummyPlayerManager.convert(world);
        }
    }

    public static void convert(World world) {
        NMSWorldServer.playerChunkMap.set(Conversion.toWorldHandle.convert(world), new DummyPlayerManager(world));
    }

    public static void revert() {
        for (World world : WorldUtil.getWorlds()) {
            final Object worldHandle = Conversion.toWorldHandle.convert(world);
            Object playerChunkMap = NMSWorldServer.playerChunkMap.get(worldHandle);
            if (playerChunkMap instanceof DummyPlayerManager) {
                NMSWorldServer.playerChunkMap.set(worldHandle, ((DummyPlayerManager) playerChunkMap).base);
            }
        }
    }

    @Override
    public Object getPlayerChunk(Object playerchunk) {
        //Hacky hacky prevent error code from gettin called
        //Properly check if chunk is loaded or not
        this.recentChunk = playerchunk;
        this.wasLoaded = NMSPlayerChunk.loaded.get(playerchunk);
        this.isRecent = true;

        if (!wasLoaded) {
            Object asynchronousExecutor = CBChunkIOExecutor.asynchronousExecutor.get(null);
            Map<?, ?> tasks = CBAsynchronousExecutor.tasks.get(asynchronousExecutor);
            if (!tasks.containsKey(playerchunk)) {
                NMSPlayerChunk.loaded.set(playerchunk, true);
            }
        }

        return playerchunk;
    }

    @Override
    public void movePlayer(Player player) {
        DummyInstancePlayerList.FILTER = true;
        super.movePlayer(player);
        DummyInstancePlayerList.FILTER = false;

        //Reset chunk loaded
        if (isRecent && !wasLoaded) {
            NMSPlayerChunk.loaded.set(recentChunk, false);
        }

        this.recentChunk = null;
        this.isRecent = false;
        this.wasLoaded = false;
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);

        //Reset chunk loaded
        if (isRecent && !wasLoaded) {
            NMSPlayerChunk.loaded.set(recentChunk, false);
        }

        this.recentChunk = null;
        this.isRecent = false;
        this.wasLoaded = false;
    }

    @Override
    public void addChunksToSend(Player player) {
        int newCX = (int) EntityUtil.getLocX(player) >> 4;
        int newCZ = (int) EntityUtil.getLocX(player) >> 4;
        int dx, dz;
        for (dx = -2; dx <= 2; dx++) {
            for (dz = -2; dz <= 2; dz++) {
                player.getWorld().getChunkAt(newCX + dx, newCZ + dz);
            }
        }
        super.addChunksToSend(player);
    }

    public void removeInstance(IntVector2 location) {
        long key = (long) location.x + 0x7fffffffL | (long) location.z + 0x7fffffffL << 32;
        Object instance = instances.remove(key);
        if (instance != null) {
            this.dirtyChunkQueue.remove(instance);
        }
    }

    @Override
    public WorldServer getWorld() {
        for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
            if (elem.getMethodName().equals("<init>")) {
                if (elem.getClassName().equals(playerInstanceClassName)) {
                    throw new RuntimeException("SHITS BROKE YO");
                    //DUMMYWORLD.DUMMYCPS.setBase(super.getWorld());
                    //return Conversion.toWorld.convert(DUMMYWORLD);
                }
            }
        }
        return super.getWorld();
    }
}
