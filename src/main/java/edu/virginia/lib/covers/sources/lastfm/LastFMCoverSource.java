package edu.virginia.lib.covers.sources.lastfm;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.UnsupportedIDTypeException;
import edu.virginia.lib.covers.sources.WebServiceCoverSource;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class LastFMCoverSource extends WebServiceCoverSource {

    private String artistAlbumURLPattern = "http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key={apiKey}&artist={artist}&album={album}";

    private String mbidURLPattern = "http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key={apiKey}&mbid={mbid}";

    private String apiKey;

    public LastFMCoverSource(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    @Override
    public CoverImage getCoverImage(List<Identifier> identifiers) throws IOException, UnsupportedIDTypeException {
        final Identifier id = getBestId(identifiers);
        final String queryUrl = getAlbumInfoQueryURL(identifiers);
        InputStream response = getResponseBody(queryUrl);
        if (response == null) {
            return null;
        }
        final String coverUrl = extractCoverArtUrlFromResponse(response);
        if (coverUrl == null || coverUrl.trim().equals("")) {
            return null;
        }
        return new WebServiceCoverImage(new HttpGet(coverUrl), id);
    }

    protected Identifier getBestId(List<Identifier> identifiers) throws UnsupportedIDTypeException {
        final Identifier mbid = Identifier.getFirstIdWithType(identifiers, Identifier.IdentifierType.MBID);
        if (mbid != null) {
            return mbid;
        }
        final Identifier artistAlbum = Identifier.getFirstIdWithType(identifiers, Identifier.IdentifierType.ARTIST_ALBUM);
        if (artistAlbum != null) {
            return artistAlbum;
        }
        throw new UnsupportedIDTypeException();
    }

    protected String getAlbumInfoQueryURL(List<Identifier> identifiers) throws UnsupportedEncodingException {
        for (Identifier id : identifiers) {
            if (id.getType().equals(Identifier.IdentifierType.MBID)) {
                return mbidURLPattern.replace("{apiKey}", apiKey).replace("{mbid}", id.getValue());
            } else if (id.getType().equals(Identifier.IdentifierType.ARTIST_ALBUM)) {
                return artistAlbumURLPattern.replace("{apiKey}", apiKey).replace("{artist}", URLEncoder.encode(id.getValues()[0], "UTF-8")).replace("{album}", URLEncoder.encode(id.getValues()[1], "UTF-8"));
            }
        }
        throw new IllegalArgumentException("Required identifier types not present!");
    }

    protected String extractCoverArtUrlFromResponse(InputStream response) {
        LastFMAlbumInfoResponse r = LastFMAlbumInfoResponse.parse(response);
        if (r.albumFound()) {
            return r.getMegaImageUrl();
        } else {
            return null;
        }
    }
}
