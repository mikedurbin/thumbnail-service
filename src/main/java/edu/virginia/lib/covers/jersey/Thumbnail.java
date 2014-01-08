package edu.virginia.lib.covers.jersey;

import edu.virginia.lib.covers.CoverService;
import edu.virginia.lib.covers.CoverSource;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.cache.filesystem.FileBinaryCache;
import edu.virginia.lib.covers.service.SimpleCoverService;
import edu.virginia.lib.covers.sources.googlebooks.GoogleBooksCoverSource;
import edu.virginia.lib.covers.sources.lastfm.LastFMCoverSource;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

// TODO: add support for a timeout (continue fetching content but return early)
@Path("thumbnail")
public class Thumbnail {

    private static final Logger LOGGER = getLogger(Thumbnail.class);

    public CoverService service;

    private String noCoverPath = "default_bookcover.gif";

    public Thumbnail() throws IOException {
        service = new SimpleCoverService();
        Properties p = new Properties();
        p.load(getClass().getClassLoader().getResourceAsStream("last-fm.properties"));
        service.addCoverSource(new LastFMCoverSource(p.getProperty("api-key")));
        service.addCoverSource(new GoogleBooksCoverSource());
        service.setCoverCache(new FileBinaryCache(new File("cache")));
    }


    @GET
    @Produces({"image/jpeg", "image/gif"})
    public Response getContent(@QueryParam("ISBN") final String isbn,
                             @QueryParam("OCLC") final String oclc,
                             @QueryParam("UPC") final String upc,
                             @QueryParam("MBID") final String mbid,
                             @QueryParam("artist") final String artist,
                             @QueryParam("album") final String album,
                             @QueryParam("maxWidth") @DefaultValue("120") final int maxWidth,
                             @QueryParam("maxHeight") @DefaultValue("120") final int maxHeight) throws IOException {
        List<Identifier> ids = new ArrayList<Identifier>();
        if (isbn != null) {
            ids.add(Identifier.ISBN(isbn));
        }
        if (oclc != null) {
            ids.add(Identifier.OCLC(oclc));
        }
        if (upc != null) {
            ids.add(Identifier.UPC(upc));
        }
        if (mbid != null) {
            ids.add(Identifier.MBID(mbid));
        }
        if (artist != null && album != null) {
            ids.add(Identifier.ALBUM(artist, album));
        }
        if (ids.isEmpty()) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).build();
        }
        if (service == null) {
            throw new RuntimeException("Service not wired!");
        }
        InputStream content = service.getCoverImage(ids, maxWidth, maxHeight);
        if (content == null) {
            return Response.ok(getClass().getClassLoader().getResourceAsStream(noCoverPath), "image/gif").build();
        } else {
            return Response.ok(content, "image/jpeg").build();
        }
    }
}
