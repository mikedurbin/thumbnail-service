package edu.virginia.lib.covers;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface BinaryCache {

    /**
     * Stores content to the cache for later retrieval.
     */
    public void storeContent(String id, InputStream content) throws IOException;

    /**
     * Asks the cache to record that no content is available for the given
     * id for later querying.
     */
    public void markNoContent(String id) throws IOException;

    /**
     * Gets content previously stored in the cache.
     * @return InputStream access to the content, or null if no content is
     *         available.
     */
    public InputStream getContent(String id) throws IOException;

    /**
     * Determine if the ID has been marked as having no content.
     */
    public boolean isNoContent(String id);

    /**
     * When file-based access is needed to the content this method *may* be
     * implemented more efficiently than getContent() when the cache uses
     * Files under the hood.
     * @return a File with the content or null.  When a File is returned it
     *         must be read-only.
     */
    public File getContentAsFile(String id) throws IOException;

}
