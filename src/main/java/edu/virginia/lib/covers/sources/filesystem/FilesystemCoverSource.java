package edu.virginia.lib.covers.sources.filesystem;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.CoverSource;
import edu.virginia.lib.covers.Identifier;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A CoverSource implementation that exposes cover images found on a
 * filesystem.  The filesystem is expected to be dynamic and updated/maintained
 * by one or more external processes.
 */
public class FilesystemCoverSource implements CoverSource {

    private File databaseRoot;

    public void setDatabaseRoot(File f) {
        databaseRoot = f;
    }

    @Override
    public CoverImage getCoverImage(List<Identifier> identifiers) throws IOException {
        try {
            for (Identifier id : identifiers) {
                if (id.getType().equals(Identifier.IdentifierType.UPC)) {
                    return new FileCoverImage(
                            new File(databaseRoot, id.getKey() + ".jpg"),
                            id);
                }
            }
            return null;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
