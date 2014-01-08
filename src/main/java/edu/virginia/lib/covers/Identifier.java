package edu.virginia.lib.covers;

import java.io.File;

/**
 * A value or values that uniquely identify a resource in at least one of the
 * services backing a CoverSource implementation.
 */
public class Identifier {

    public static Identifier ISBN(String isbn) {
        return new Identifier(IdentifierType.ISBN, new String[] { isbn });
    }

    public static Identifier OCLC(String oclc) {
        return new Identifier(IdentifierType.OCLC, new String[] { oclc });
    }

    public static Identifier LCCN(String lccn) {
        return new Identifier(IdentifierType.LCCN, new String[] { lccn });
    }

    public static Identifier GOOGLE_BOOKS_ID(String id) {
        return new Identifier(IdentifierType.GOOGLE_BOOKS_ID, new String[] { id });
    }

    public static Identifier UPC(String upc) {
        return new Identifier(IdentifierType.UPC, new String[] { upc });
    }

    public static Identifier MBID(String mbid) {
        return new Identifier(IdentifierType.MBID, new String[] { mbid });
    }

    public static Identifier ALBUM(String artist, String album) {
        return new Identifier(IdentifierType.ARTIST_ALBUM, new String[] { artist, album });
    }

    public static Identifier getFirstIdWithType(Iterable<Identifier> ids, IdentifierType type) {
        for (Identifier id : ids) {
            if (id.getType().equals(type)) {
                return id;
            }
        }
        return null;
    }


    public static enum IdentifierType {
        ISBN,
        OCLC,
        LCCN,
        GOOGLE_BOOKS_ID,
        UPC,
        MBID, /* music brainz id */
        ARTIST_ALBUM, /* artist name, likely useless without album_name */
    }

    private IdentifierType type;

    private String[] values;

    private Identifier(IdentifierType type, String[] values) {
        this.type = type;
        this.values = values;
    }

    public IdentifierType getType() {
        return type;
    }

    public String getValue() {
        if (type.equals(IdentifierType.ARTIST_ALBUM)) {
            throw new IllegalArgumentException();
        } else {
            return values[0];
        }
    }

    public String[] getValues() {
        return values;
    }


    /**
     * Gets a unique String version of this Identifier.
     * @return
     */
    public String getKey() {
        StringBuffer sb = new StringBuffer();
        sb.append(type.name());
        for (String value : values) {
            sb.append(File.separatorChar);
            sb.append(value);
        }
        return sb.toString();
    }

    /**
     * Returns the result of getKey().
     */
    public String toString() {
        return getKey();
    }

    public boolean equals(Object o) {
        return (o instanceof Identifier) && ((Identifier) o).getKey().equals(getKey());
    }

    public int hashCode() {
        return getKey().hashCode();
    }

}
