package Algorithm.sa;

/**
 * Observer callback for SA progress updates.
 */
@FunctionalInterface
public interface SaProgressListener {
    void onProgress(SaProgressEvent event);
}
