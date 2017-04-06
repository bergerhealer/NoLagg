package com.bergerkiller.bukkit.nolagg.examine;

import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.reflection.org.bukkit.craftbukkit.CBCraftScheduler;
import com.bergerkiller.reflection.org.bukkit.craftbukkit.CBCraftTask;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import java.util.PriorityQueue;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SchedulerWatcher extends PriorityQueue {
    private static final long serialVersionUID = -3457587669129548810L;
    private final PluginLogger logger;

    private SchedulerWatcher(PriorityQueue queue, PluginLogger logger) {
        super(queue);
        this.logger = logger;
    }

    public static void init(PluginLogger logger) {
        CBCraftScheduler.pending.set(Bukkit.getScheduler(), new SchedulerWatcher(CBCraftScheduler.pending.get(Bukkit.getScheduler()), logger));
    }

    public static void deinit() {
        // Obtain a blank new Queue with the elements of the original, and filter out timed wrappers
        PriorityQueue<Object> queue = new PriorityQueue<Object>(CBCraftScheduler.pending.get(Bukkit.getScheduler()));
        for (Object element : queue) {
            // Remove the timed wrapper if needed
            if (CBCraftTask.T.isType(element)) {
                Runnable run = CBCraftTask.task.get(element);
                if (run instanceof TimedWrapper) {
                    CBCraftTask.task.set(element, ((TimedWrapper) run).getProxyBase());
                }
            }
        }
        // Set in the server
        CBCraftScheduler.pending.set(Bukkit.getScheduler(), queue);
    }

    @Override
    public Object remove() {
        if (!logger.isRunning()) {
            return super.remove();
        }
        Object o = super.remove();
        if (o == null) {
            return null;
        }
        try {
            if (CBCraftTask.T.isType(o)) {
                Runnable run = CBCraftTask.task.get(o);
                if (run != null && !logger.isIgnoredTask(run)) {
                    Plugin plugin = CBCraftTask.plugin.get(o);
                    if (plugin != null && plugin.isEnabled()) {
                        CBCraftTask.task.set(o, logger.getWrapper(run, plugin));
                    }
                }
            }
        } catch (Throwable t) {
            NoLagg.plugin.handle(t);
        }
        return o;
    }
}
