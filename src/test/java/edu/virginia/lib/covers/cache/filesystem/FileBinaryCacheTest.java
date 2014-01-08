package edu.virginia.lib.covers.cache.filesystem;

import edu.virginia.lib.covers.BinaryCache;
import edu.virginia.lib.covers.cache.AbstractBinaryCacheTest;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class FileBinaryCacheTest extends AbstractBinaryCacheTest {

    @Override
    public BinaryCache getNewCache() throws Exception {
        File baseDir = File.createTempFile("testcache", "basedir");
        baseDir.delete();
        baseDir.mkdir();
        return new FileBinaryCache(baseDir);
    }

    @Override
    public void cleanUpCache(BinaryCache cache) throws IOException {
        FileBinaryCache c = (FileBinaryCache) cache;
        FileUtils.deleteDirectory(c.getBaseDir());
    }
}
