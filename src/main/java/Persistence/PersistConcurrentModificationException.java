package Persistence;

/**
 * Thrown when a save is refused because persisted {@code persistVersion}
 * no longer matches the version the caller last loaded (lost update / stale write).
 */
public final class PersistConcurrentModificationException extends Exception {

    private final long diskPersistVersion;

    public PersistConcurrentModificationException(String message, long diskPersistVersion) {
        super(message);
        this.diskPersistVersion = diskPersistVersion;
    }

    public long getDiskPersistVersion() {
        return diskPersistVersion;
    }
}
