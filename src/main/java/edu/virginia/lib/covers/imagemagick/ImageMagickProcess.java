package edu.virginia.lib.covers.imagemagick;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A thin wrapper around the ImageMagick's "convert" and "identify" utilities.
 * For this class to work, the "convert" utility must be in the path (ie,
 * executable with the simple command "convert").
 */
public class ImageMagickProcess {

    private static ImageMagickProcess SINGLETON;

    public static ImageMagickProcess getInstance() throws IOException {
        if (SINGLETON == null) {
            SINGLETON = new ImageMagickProcess();
        }
        return SINGLETON;
    }

    private static final Logger LOGGER = getLogger(ImageMagickProcess.class);

    private String convertCommandPath; 

    private String identifyCommandPath;

    public ImageMagickProcess() throws IOException {
        convertCommandPath = "convert";
        identifyCommandPath = "identify";
        try {
            LOGGER.info("ImageMagick convert version:  " + getConvertVersion());
            LOGGER.info("ImageMagick identify version: " + getIdentifyVersion());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize " + getClass().getName() + "!", ex);
        }
    }

    public String getIdentifyVersion() throws IOException {
        Process p = new ProcessBuilder(convertCommandPath, "-version").start();
        return parseOutVersionNumber(runProcessGetOutput(p));
    }

    public String getConvertVersion() throws IOException {
        Process p = new ProcessBuilder(identifyCommandPath, "-version").start();
        return parseOutVersionNumber(runProcessGetOutput(p));
    }

    private String parseOutVersionNumber(String response) {
        Pattern pattern = Pattern.compile("(?s).*\\QVersion: \\E(.*) http.*");
        Matcher m = pattern.matcher(response);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unsupported Version: \"" + response + "\"");
        }
        return (m.group(1));
    }

    private String runProcessGetOutput(Process p) throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thread t1 = new Thread(new OutputDrainerThread(p.getInputStream(), baos));
        t1.start();
        Thread t2 = new Thread(new OutputDrainerThread(p.getErrorStream()));
        t2.start();
        try {
            int returnCode = p.waitFor();
            t1.join();
            t2.join();
            if (returnCode != 0) {
                throw new RuntimeException("Invalid return code for process!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }

    public void createThumbnail(File inputFile, File outputFile, int maxWidth, int maxHeight) throws IOException {
        Process p = new ProcessBuilder(convertCommandPath, "-thumbnail", maxWidth + "x" + maxHeight, inputFile.getPath() + "[0]", outputFile.getPath()).start();
        runProcessGetOutput(p);
    }

    public ImageMetadata getMetadata(InputStream is) throws IOException {
        File tmp = File.createTempFile("image", "content");
        FileOutputStream fos = new FileOutputStream(tmp);
        try {
            IOUtils.copy(is, fos);
        } finally {
            fos.close();
        }
        try {
            return getMetadata(tmp);
        } finally {
            tmp.delete();
        }
    }

    public ImageMetadata getMetadata(File inputFile) throws IOException {
        Process p = new ProcessBuilder(identifyCommandPath, inputFile.getAbsolutePath()).start();
        return new ImageMetadata(runProcessGetOutput(p));
    }

    public static class ImageMetadata implements edu.virginia.lib.covers.ImageMetadata {

        private String type;
        private int width;
        private int height;

        private ImageMetadata(String output) {
            Pattern pattern = Pattern.compile("(.* ([A-Z]+) (\\d+)x(\\d+) .*\\n)+");
            Matcher m = pattern.matcher(output);
            if (!m.matches()) {
                throw new IllegalArgumentException("Unrecognized output: \"" + output + "\"");
            }

            type = m.group(2);
            width = Integer.parseInt(m.group(3));
            height = Integer.parseInt(m.group(4));
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public String mimeType() {
            if (type.equals("JPEG")) {
                return "image/jpeg";
            } else if (type.equals("BMP")) {
                return "image/bmp";
            } else if (type.equals("GIF")) {
                return "image/gif";
            } else if (type.equals("PNG")) {
                return "image/png";
            } else if (type.equals("XCF")) {
                return "image/x-xcf";
            } else {
                LOGGER.warn("Unrecognized Image Magick type code: \"" + type + "\"");
                return "application/octet-stream";
            }
        }
    }
}
