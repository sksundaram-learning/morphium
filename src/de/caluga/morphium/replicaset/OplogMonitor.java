package de.caluga.morphium.replicaset;

import de.caluga.morphium.Logger;
import de.caluga.morphium.Morphium;
import de.caluga.morphium.ShutdownListener;
import de.caluga.morphium.driver.DriverTailableIterationCallback;
import de.caluga.morphium.driver.MorphiumDriverException;
import org.bson.types.BSONTimestamp;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

/**
 * Created by stephan on 15.11.16.
 */
public class OplogMonitor implements Runnable, ShutdownListener {
    private Collection<OplogListener> listeners;
    private Morphium morphium;
    private boolean running = true;
    private Logger log = new Logger(OplogMonitor.class);
    private long timestamp;
    private Thread oplogMonitorThread;

    private String nameSpace;
    private boolean useRegex;


    public OplogMonitor(Morphium m) {
        this(m, null, false);
    }

    public OplogMonitor(Morphium m, Class<?> entity) {
        this(m, m.getConfig().getDatabase() + "." + m.getMapper().getCollectionName(entity), false);
    }

    public OplogMonitor(Morphium m, String nameSpace, boolean regex) {
        morphium = m;
        listeners = new ConcurrentLinkedDeque<OplogListener>();
        timestamp = System.currentTimeMillis() / 1000;
        morphium.addShutdownListener(this);
        this.nameSpace = nameSpace;
        this.useRegex = regex;
    }

    public void addListener(OplogListener lst) {
        listeners.add(lst);
    }

    public void removeListener(OplogListener lst) {
        listeners.remove(lst);
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public void start() {
        if (oplogMonitorThread != null) {
            throw new RuntimeException("Already running!");
        }
        oplogMonitorThread = new Thread(this);
        oplogMonitorThread.setDaemon(true);
        oplogMonitorThread.setName("oplogmonitor");
        oplogMonitorThread.start();
    }

    public void stop() {
        running = false;
        long start = System.currentTimeMillis();
        while (oplogMonitorThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //ignoring it
            }
            if (System.currentTimeMillis() - start > 1000) {
                break;
            }
        }
        if (oplogMonitorThread.isAlive()) {
            oplogMonitorThread.interrupt();
        }
        oplogMonitorThread = null;
        morphium.removeShutdownListener(this);
    }

    public String getNameSpace() {
        return nameSpace;
    }

    @Override
    public void run() {
        Map<String, Object> q = new LinkedHashMap<>();
        Map<String, Object> q2 = new HashMap<>();
        q2.put("$gt", new BSONTimestamp((int) timestamp, 0));
        String ns = null;


        if (nameSpace != null) {
            ns = morphium.getConfig().getDatabase() + "." + nameSpace;
            if (nameSpace.contains(".") && !useRegex) {
                ns = nameSpace; //assuming you specify DB
            }
            if (useRegex) {
                q.put("ns", Pattern.compile(ns));
            } else {
                q.put("ns", ns);
            }
        }
        q.put("ts", q2);
        while (running) {
            try {
                morphium.getDriver().tailableIteration("local", "oplog.rs", q, null, null, 0, 0, 1000, null, 1000, new DriverTailableIterationCallback() {
                    @Override
                    public boolean incomingData(Map<String, Object> data, long dur) {
                        timestamp = (Integer) data.get("ts");
                        for (OplogListener lst : listeners) {
                            try {
                                lst.incomingData(data);
                            } catch (Exception e) {
                                log.error("listener threw exception", e);
                            }
                        }
                        return true;
                    }
                });
            } catch (MorphiumDriverException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

    @Override
    public void onShutdown(Morphium m) {
        stop();
    }
}
