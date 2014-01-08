package edu.virginia.lib.covers.cache.filesystem;

import edu.virginia.lib.covers.BinaryCache;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class FileBinaryCache implements BinaryCache {

    private final File baseDir;

    public FileBinaryCache(File base) {
        this.baseDir = base;
    }

    @Override
    public synchronized void storeContent(String id, InputStream content) throws IOException {
        File path = getPathForId(id);
        path.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(path);
        try {
            IOUtils.copy(content, fos);
            clearNoConent(id);
        } finally {
            fos.close();
        }
    }

    @Override
    public synchronized void markNoContent(String id) throws IOException {
        FileUtils.writeStringToFile(getNoContentPathForId(id), "Marked as having no content on " + new Date() + ".");
    }

    private void clearNoConent(String id) {
        File noContentFile = getNoContentPathForId(id);
        if (noContentFile.exists()) {
            noContentFile.delete();
        }
    }

    @Override
    public synchronized InputStream getContent(String id) throws IOException {
        File path = getPathForId(id);
        if (path.exists()) {
            return new FileInputStream(path);
        } else {
            return null;
        }
    }

    @Override
    public synchronized boolean isNoContent(String id) {
        return getNoContentPathForId(id).exists();
    }

    @Override
    public synchronized File getContentAsFile(String id) throws IOException {
        File path = getPathForId(id);
        if (path.exists()) {
            return path;
        } else {
            return null;
        }
    }

    protected File getPathForId(String id) {
        return new File(baseDir, id);
    }

    protected File getNoContentPathForId(String id) {
        return new File(baseDir, id + "-nocontent");
    }

    protected File getBaseDir() {
        return baseDir;
    }


}
