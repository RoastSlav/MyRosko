package Cache;

public class CacheFactory {
    String eviction;
    int flushInterval;
    int size;
    boolean readOnly;

    protected CacheFactory(String eviction, int flushInterval, int size, boolean readOnly) {
        this.eviction = eviction;
        this.flushInterval = flushInterval;
        this.size = size;
        this.readOnly = readOnly;
    }

    public Cache getCache() {
        return new Cache(eviction, flushInterval, size, readOnly);
    }
}
