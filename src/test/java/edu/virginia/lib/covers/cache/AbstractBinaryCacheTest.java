package edu.virginia.lib.covers.cache;

import edu.virginia.lib.covers.BinaryCache;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public abstract class AbstractBinaryCacheTest {

    public final String exampleContent = "This is example content.";

    public abstract BinaryCache getNewCache() throws Exception;

    public abstract void cleanUpCache(BinaryCache cache) throws Exception;

    @Test
    public void testSimpleStoreAndRetrieve() throws Exception {
        BinaryCache c = getNewCache();

        String valueStr = "123456";
        String key = "1";

        Assert.assertNull("Initialized cache should not have entry for key "
                + key + "!", c.getContent(key));

        c.storeContent(key, stringToInputStream(valueStr));

        Assert.assertEquals("Value must be preserved exactly in cache!",
                valueStr, inputStreamToString(c.getContent(key)));
        Assert.assertEquals("Value must be preserved exactly in cache!",
                valueStr, inputStreamToString(new FileInputStream(c.getContentAsFile(key))));

        cleanUpCache(c);
    }

    public InputStream stringToInputStream(String str) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(str.getBytes("UTF-8"));
    }

    public String inputStreamToString(InputStream inputStream) throws IOException {
        return new String(IOUtils.toByteArray(inputStream), "UTF-8");
    }

}
