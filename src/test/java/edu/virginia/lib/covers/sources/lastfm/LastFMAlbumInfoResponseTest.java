package edu.virginia.lib.covers.sources.lastfm;

import org.junit.Assert;
import org.junit.Test;

public class LastFMAlbumInfoResponseTest {

    @Test
    public void simpleParseTest() {
        LastFMAlbumInfoResponse r = LastFMAlbumInfoResponse.parse(this.getClass().getClassLoader().getResourceAsStream("example-album-info.xml"));
        Assert.assertTrue(r.albumFound());
        Assert.assertEquals("mega-url", r.getMegaImageUrl());
    }

    @Test
    public void missingParseTest() {
        LastFMAlbumInfoResponse r = LastFMAlbumInfoResponse.parse(this.getClass().getClassLoader().getResourceAsStream("example-missing-album-info.xml"));
        Assert.assertFalse(r.albumFound());
        Assert.assertNull(r.getMegaImageUrl());
    }
}
