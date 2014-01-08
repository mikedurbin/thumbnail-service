package edu.virginia.lib.covers.sources.filesystem;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.Identifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FilesystemCoverSourceTest {

    private FilesystemCoverSource source;

    @Before
    public void setUp() throws URISyntaxException {
        source = new FilesystemCoverSource();
        source.setDatabaseRoot(new File(getClass().getClassLoader().getResource("database").toURI()));
    }

    @Test
    public void testGetExistingCoverImage() throws IOException {
        List<Identifier> ids = new ArrayList<Identifier>();
        ids.add(Identifier.UPC("0000000000"));
        CoverImage i = source.getCoverImage(ids);
        Assert.assertNotNull("The cover image was not found!", i);
        Assert.assertNotNull("An input stream must be available fo rthe cover image!", i.getInputStream());
        Assert.assertEquals(100, i.getMetadata().width());
        Assert.assertEquals(66, i.getMetadata().height());
    }

    @Test
    public void testGetMissingCoverImage() throws IOException {
        List<Identifier> ids = new ArrayList<Identifier>();
        ids.add(Identifier.ISBN("0000000000"));
        CoverImage i = source.getCoverImage(ids);
        Assert.assertNull("A cover image was found, when none should have been!", i);
    }
}
