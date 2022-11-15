package Cache;

public class CacheFactoryBuilder {
    private static final String DEFAULT_EVICTION = "LRU";
    private static final int DEFAULT_FLUSH_INTERVAL = 0;
    private static final int DEFAULT_SIZE = 1024;
    private static final boolean DEFAULT_READ_ONLY = false;
    private static final CacheFactoryBuilder builder = new CacheFactoryBuilder();
    private String eviction = DEFAULT_EVICTION;
    private int flushInterval = DEFAULT_FLUSH_INTERVAL;
    private int size = DEFAULT_SIZE;
    private boolean readOnly = DEFAULT_READ_ONLY;

    public static CacheFactoryBuilder getBuilder() {
        return builder;
    }

    public CacheFactoryBuilder setEviction(String eviction) {
        if (eviction != null)
            this.eviction = eviction;

        return this;
    }

    public CacheFactoryBuilder setFlushInterval(int flushInterval) {
        if (eviction != null)
            this.flushInterval = flushInterval;
        return this;
    }

    public CacheFactoryBuilder setSize(int size) {
        if (eviction != null)
            this.size = size;
        return this;
    }

    public CacheFactoryBuilder setReadOnly(boolean readOnly) {
        if (eviction != null)
            this.readOnly = readOnly;
        return this;
    }

    public CacheFactory build() {
        try {
            return new CacheFactory(eviction, flushInterval, size, readOnly);
        } finally {
            eviction = DEFAULT_EVICTION;
            flushInterval = DEFAULT_FLUSH_INTERVAL;
            size = DEFAULT_SIZE;
            readOnly = DEFAULT_READ_ONLY;
        }
    }
}
