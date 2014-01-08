package edu.virginia.lib.covers.sources.filesystem;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.ImageMetadata;
import edu.virginia.lib.covers.imagemagick.ImageMagickProcess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileCoverImage implements CoverImage {

    private File f;

    private ImageMetadata m;

    private Identifier id;

    public FileCoverImage(File f, Identifier id) throws IOException, InterruptedException {
        this.f = f;
        m = new ImageMagickProcess().getMetadata(f);
        this.id = id;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(f);
    }

    @Override
    public ImageMetadata getMetadata() {
        return m;
    }

    @Override
    public Identifier getId() {
        return id;
    }

}
