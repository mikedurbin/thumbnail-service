package edu.virginia.lib.covers;

import java.io.IOException;
import java.io.InputStream;

public interface CoverImage {

    public InputStream getInputStream() throws IOException;

    public ImageMetadata getMetadata();

    /**
     * Returns the ID of the resource for which this is a cover image.
     */
    public Identifier getId();
}
