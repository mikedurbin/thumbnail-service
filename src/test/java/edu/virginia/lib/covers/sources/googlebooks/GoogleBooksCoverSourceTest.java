package edu.virginia.lib.covers.sources.googlebooks;

import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.UnsupportedIDTypeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleBooksCoverSourceTest {

    private GoogleBooksCoverSource source;

    @Before
    public void setUp() {
        source = new GoogleBooksCoverSource();
    }

    @Test
    public void testExtractCoverUrlFromResponse() throws IOException, UnsupportedIDTypeException {
        List<Identifier> ids = new ArrayList<Identifier>();
        ids.add(Identifier.OCLC("2680406"));
        Assert.assertEquals("http://bks9.books.google.com/books?id=Sq8NAQAAIAAJ&printsec=frontcover&img=1&zoom=5", source.extractCoverUrlFromResponse(getClass().getClassLoader().getResourceAsStream("google-example.json"), source.getQueryForID(source.getBestId(ids))));
    }

}
