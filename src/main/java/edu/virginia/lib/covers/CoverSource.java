package edu.virginia.lib.covers;

import java.io.IOException;
import java.util.List;

/**
 * Represents a service that can get an image file representing a cover-image
 * for a resource.  This may be book covers, DVD covers, album covers, or any
 * other sort of preview image for a physical manifestation of an intellectual
 * work.
 */
public interface CoverSource {

    /**
     * Gets the image described by the identifiers from the underlying source
     * (if available) in the format returned by the underlying source.
     *
     * @param identifiers several different identifiers for the same item.  The
     *                    implementation will determine the ids (if any) that
     *                    it considers to to be acceptable to search.  It may
     *                    also make decisions about whether to try more than
     *                    one based on the nature of the search service.
     */
    public CoverImage getCoverImage(List<Identifier> identifiers)
            throws IOException, UnsupportedIDTypeException;
}
