package edu.virginia.lib.covers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * An interface that when implemented allows cover images to be retrieved from
 * an array of CoverSource instances which are scaled to meet the caller's
 * requirements.  Optionally a BinaryCache implementation may be provided which
 * will be used by this class to cache all master and derivative images.
 */
public interface CoverService {

    /**
     * Adds a cover source to this provider.  The order
     * @param source
     */
    public void addCoverSource(CoverSource source);

    public void setCoverCache(BinaryCache cache);

    /**
     * Gets the image described by the identifiers from the cache or any of the
     * underlying sources scaled to the specified size.
     *
     * @param identifiers a map from identifier types to their values for the
     *                    known identifiers for the resource whose cover image
     *                    is being requested.
     */
    public InputStream getCoverImage(List<Identifier> identifiers,
                                     int maxWidth, int maxHeight) throws IOException;

}
