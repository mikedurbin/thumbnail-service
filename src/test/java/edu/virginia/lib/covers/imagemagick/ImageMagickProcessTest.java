package edu.virginia.lib.covers.imagemagick;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import edu.virginia.lib.covers.ImageMetadata;

public class ImageMagickProcessTest {

    private ImageMagickProcess p;

    @Before
    public void setup() throws IOException {
        p = new ImageMagickProcess();
    }

    @Test
    public void testImageMetadataJPEG() throws Exception {
        assertMetadata("test.jpg", 100, 66, "image/jpeg");
    }

    @Test
    public void testImageMetadataBMP() throws Exception {
        assertMetadata("test.bmp", 100, 66, "image/bmp");
    }

    @Test
    public void testImageMetadataGIF() throws Exception {
        assertMetadata("test.gif", 100, 66, "image/gif");
    }

    @Test
    public void testImageMetadataPNG() throws Exception {
        assertMetadata("test.png", 100, 66, "image/png");
    }

    @Test
    public void testImageMetadataXCF() throws Exception {
        assertMetadata("test.xcf", 100, 66, "image/x-xcf");
    }

    @Test
    public void testCreateThumbnail() throws Exception {
        File output = File.createTempFile("thumbnail", ".jpg");
        output.deleteOnExit();
        File input = getFileFromClassPath("test.bmp");
        ImageMetadata m = p.getMetadata(input);

        // to test that it retains aspect ratio, we set a bounding box that is
        // half the length of the longer dimension and full size for the shorter
        // dimension.  This should force the shorter dimension to be cut in
        // half if the aspect ration is preserved.
        boolean wide = m.width() > m.height();
        p.createThumbnail(input, output, wide ? m.width() / 2 : m.width(), wide ? m.height() : m.height() / 2);
        assertMetadata(output, m.width() / 2, m.height() / 2, "image/jpeg");
    }

    private File getFileFromClassPath(String filePath) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(filePath).toURI());
    }

    private void assertDimensions(String filePath, int width, int height) throws Exception {
        assertMetadata(getFileFromClassPath(filePath), width, height, null);
    }

    private void assertMetadata(String filePath, int width, int height, String mimeType) throws Exception {
        assertMetadata(getFileFromClassPath(filePath), width, height, mimeType);
    }

    private void assertMetadata(File f, int width, int height, String mimeType) throws Exception {
        ImageMetadata m = p.getMetadata(f);
        Assert.assertEquals("Failed to calculate correct height for \"" + f.getAbsolutePath() + "\"!", height, m.height());
        Assert.assertEquals("Failed to calculate correct width for \""  + f.getAbsolutePath() + "\"!", width, m.width());
        if (mimeType != null) {
            Assert.assertEquals("Failed to correctly identify mime type for \"" + f.getAbsolutePath() + "\"!", mimeType, m.mimeType());
        }
    }

}
