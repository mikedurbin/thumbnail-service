package edu.virginia.lib.covers.sources.lastfm;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.InputStream;

/**
 * A quick and dirty class that that's annotated to parse out the useful bits
 * of the XML response from the LastFM album info service.
 */
@XmlRootElement(name="lfm")
public class LastFMAlbumInfoResponse {

    @XmlAttribute
    private String status;

    public boolean albumFound() {
        return status.equals("ok");
    }

    @XmlElement
    private Album album;

    public String getMegaImageUrl() {
        if (album != null && album.image != null) {
            for (Image i : album.image) {
                if (i.size.equals("mega")) {
                    return i.url.trim();
                }
            }
        }
        return null;
    }

    private static class Album {

        @XmlElement
        private Image[] image;

    }

    private static class Image {

        @XmlAttribute
        private String size;

        @XmlValue
        private String url;

    }

    public static LastFMAlbumInfoResponse parse(InputStream is) {
        return (LastFMAlbumInfoResponse) JAXB.unmarshal(is, LastFMAlbumInfoResponse.class);
    }

}
