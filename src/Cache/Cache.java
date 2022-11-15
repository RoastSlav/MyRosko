package Cache;

import java.util.*;

public class Cache {
    private final int flushInterval;
    private final int size;
    private final boolean readOnly;
    private final HashMap<String, ElementWrapper> cache;
    private Queue<ElementWrapper> removeQueue;
    private Timer clearTimer;

    protected Cache(String eviction, int flushInterval, int size, boolean readOnly) {
        if (eviction.equals("FIFO"))
            removeQueue = new PriorityQueue<>(size);
        else if (eviction.equals("LRU"))
            removeQueue = new PriorityQueue<>(size, Comparator.comparing(ElementWrapper::getReadFrequency));
        cache = new HashMap<>(size);
        this.flushInterval = flushInterval;
        this.size = size;
        this.readOnly = readOnly;

        if (flushInterval > 0) {
            clearTimer = new Timer();
            setActivityTimer();
        }
    }

    public Object get(String key) {
        ElementWrapper element = cache.get(key);
        Object value = element.getValue();
        removeQueue.remove(element);
        removeQueue.add(element);
        return value;
    }

    public Object set(String key, Object value) {
        ElementWrapper element = new ElementWrapper(key, value);
        if (cache.containsKey(key)) {
            cache.put(key, element);
        } else if (cache.size() + 1 > size) {
            evict();
            element = cache.put(key, element);
            removeQueue.add(element);
        }
        return value;
    }

    private void setActivityTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                cache.clear();
                removeQueue.clear();
            }
        };
        clearTimer.schedule(task, flushInterval);
    }

    private void evict() {
        ElementWrapper remove = removeQueue.remove();
        cache.remove(remove.key);
    }

    public void clearCache() {
        cache.clear();
        removeQueue.clear();
        if (flushInterval > 0) {
            clearTimer.cancel();
            setActivityTimer();
        }
    }


    static class ElementWrapper implements Cloneable {
        private final String key;
        private final Object value;
        private int readCount;

        ElementWrapper(String key, Object value) {
            this.value = value;
            this.key = key;
        }

        int getReadFrequency() {
            return readCount;
        }

        Object getValue() {
            readCount++;
            return value;
        }
    }
}
