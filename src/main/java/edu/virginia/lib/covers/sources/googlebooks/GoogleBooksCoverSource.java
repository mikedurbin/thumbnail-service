package edu.virginia.lib.covers.sources.googlebooks;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.UnsupportedIDTypeException;
import edu.virginia.lib.covers.sources.WebServiceCoverSource;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class GoogleBooksCoverSource extends WebServiceCoverSource {

    private static final Logger LOGGER = getLogger(GoogleBooksCoverSource.class);

    private String bookQueryPattern = "http://books.google.com/books?jscmd=viewapi&bibkeys={query}&callback=CALLBACK&zoom=0";

    @Override
    public CoverImage getCoverImage(List<Identifier> identifiers) throws IOException, UnsupportedIDTypeException {
        final Identifier id = getBestId(identifiers);
        final String query = getQueryForID(id);
        final String queryUrl = bookQueryPattern.replace("{query}", URLEncoder.encode(query, "UTF-8"));
        try {
            final String coverUrl = extractCoverUrlFromResponse(getResponseBody(queryUrl), query);
            if (coverUrl == null) {
                return null;
            }
            return new WebServiceCoverImage(new HttpGet(coverUrl), id);
        } catch (JSONException ex) {
            LOGGER.debug("No thumnbail_url found in google response!", ex);
            return null;
        }
    }

    private static final Identifier.IdentifierType[] TYPE_PREFERENCE = new Identifier.IdentifierType[] {Identifier.IdentifierType.GOOGLE_BOOKS_ID, Identifier.IdentifierType.ISBN, Identifier.IdentifierType.OCLC, Identifier.IdentifierType.LCCN };

    protected Identifier getBestId(List<Identifier> identifiers) throws UnsupportedIDTypeException {
        for (Identifier.IdentifierType type : TYPE_PREFERENCE) {
            final Identifier id = Identifier.getFirstIdWithType(identifiers, type);
            if (id != null) {
                return id;
            }
        }
        throw new UnsupportedIDTypeException();
    }

    protected String getQueryForID(Identifier id) {
        if (id.getType().equals(Identifier.IdentifierType.GOOGLE_BOOKS_ID)) {
            return id.getValue();
        } else if (id.getType().equals(Identifier.IdentifierType.ISBN)) {
            return "ISBN:" + id.getValue();
        } else if (id.getType().equals(Identifier.IdentifierType.OCLC)) {
            return "OCLC:" + id.getValue();
        } else if (id.getType().equals(Identifier.IdentifierType.LCCN)) {
            return "LCCN:" + id.getValue();
        }
        return null;
    }

    protected String extractCoverUrlFromResponse(InputStream response, String query) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(response, baos);
        response.close();
        String jsonStr = new String(baos.toByteArray(), "UTF-8");
        if (jsonStr.equals("CALLBACK({});")) {
            return null;
        }
        jsonStr = jsonStr.substring("CALLBACK(".length(), jsonStr.length() - 2);
        JSONObject o = new JSONObject(new JSONTokener(jsonStr));
        return o.getJSONObject(query).getString("thumbnail_url");
    }

}
