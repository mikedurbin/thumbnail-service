package edu.virginia.lib.covers.service;

import edu.virginia.lib.covers.BinaryCache;
import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.CoverService;
import edu.virginia.lib.covers.CoverSource;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.ImageMetadata;
import edu.virginia.lib.covers.cache.filesystem.FileBinaryCacheTest;
import edu.virginia.lib.covers.imagemagick.ImageMagickProcess;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

public class SimpleCoverServiceTest {

    private static final Logger LOGGER = getLogger(SimpleCoverServiceTest.class);

    private static final int MAX_WIDTH = 100;
    private static final int MAX_HEIGHT = 100;
    private static final List<Identifier> SAMPLE_ISBN = Arrays.asList(new Identifier[]{ Identifier.ISBN("1234")} );
    private static final List<Identifier> SAMPLE_OCLC = Arrays.asList(new Identifier[]{ Identifier.ISBN("2345")} );

    private static final int DEADLOCK_TIME = 2000;

    /**
     * This test involves two requests to the same resource at the same time in
     * which we need to verify that neither holds up the other.
     */
    @Test
    public void testConcurrentNonConflictingSearches() throws InterruptedException, IOException {
        final Cop cop = new Cop();
        final CoverSource source = new MockCoverSource(cop, null);
        final SimpleCoverService service = new SimpleCoverService();
        service.addCoverSource(source);

        // make request 1 with a thread
        final Thread t1 = new Thread(new RequestRunnable(service, SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT));
        cop.issueWarrant(t1);
        t1.start();

        // make request 2 with a thread
        final Thread t2 = new Thread(new RequestRunnable(service, SAMPLE_OCLC, MAX_WIDTH, MAX_HEIGHT));
        cop.issueWarrant(t2);
        t2.start();

        // verify that they're both detained while processing (ie, process in parallel)
        cop.waitForCapture(DEADLOCK_TIME, t1, t2);

        Assert.assertTrue("Request 1 was blocked waiting for unrelated request 2!", cop.isDetained(t1));
        Assert.assertTrue("Request 2 was blocked waiting for unrelated request 1!", cop.isDetained(t2));
        cop.pardon(t1);
        cop.pardon(t2);
        t1.join();
        t2.join();
    }

    /**
     * This test involves two identical requests, which should NOT result in
     * concurrent calls to the underlying CoverSource implementations.
     */
    @Test
    public void testConcurrentDeferredSearch() throws InterruptedException, IOException {
        final Cop cop = new Cop();
        final CoverSource source = new MockCoverSource(cop, null);
        final SimpleCoverService service = new SimpleCoverService();
        service.addCoverSource(source);

        // make request 1 with a thread
        final Thread t1 = new Thread(new RequestRunnable(service, SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT));
        cop.issueWarrant(t1);
        t1.start();

        // wait until that thread is detained, simulating a long-running process
        cop.waitForCapture(DEADLOCK_TIME, t1);

        // make an identical request with another thread
        final Thread t2 = new Thread(new RequestRunnable(service, SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT));
        t2.start();

        // this thread should be blocked before processing...
        t2.join(DEADLOCK_TIME);
        Assert.assertTrue("Second request should still be blocked waiting for the first!", t2.isAlive());

        // release the first one
        cop.pardon(t1);
        t1.join(DEADLOCK_TIME);
        Assert.assertFalse("First request should have completed!", t1.isAlive());

        t2.join(DEADLOCK_TIME);
        Assert.assertFalse("Second request should be completed!", t1.isAlive());
    }

    /**
     * Tests that when a cache is available:
     * 1.  Subsequent requests for the same content do not hit the CoverSource
     * 2.  Subsequent requests for new sizes of previously viewed content do not hit the CoverSource
     */
    @Test
    public void testCaching() throws Exception {
        ImageMagickProcess p = new ImageMagickProcess();

        final CoverImage response = mock(CoverImage.class);
        when(response.getInputStream()).thenReturn(getClass().getClassLoader().getResourceAsStream("test.bmp"));
        when(response.getId()).thenReturn(SAMPLE_ISBN.get(0));

        final Cop cop = new Cop();
        final MockCoverSource source = new MockCoverSource(cop, response);
        FileBinaryCacheTest cacheTest = new FileBinaryCacheTest();
        final BinaryCache cache = cacheTest.getNewCache();
        final SimpleCoverService service = new SimpleCoverService();
        service.addCoverSource(source);
        service.setCoverCache(cache);

        try {
            // request the cover image (should come from the source)
            final InputStream thumbnail = service.getCoverImage(SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT);
            final ImageMetadata m1 = p.getMetadata(thumbnail);
            Assert.assertTrue("Width must less than or equal to the specified maximum.", m1.width() <= MAX_WIDTH);
            Assert.assertTrue("Height must less than or equal to the specified maximum.", m1.height() <= MAX_HEIGHT);
            Assert.assertNotNull("Original image should be cached!", cache.getContentAsFile(service.getCacheKeyForCoverImage(SAMPLE_ISBN.get(0))));
            Assert.assertNotNull("Scaled image should be cached!", cache.getContentAsFile(service.getCacheKeyForCoverImage(SAMPLE_ISBN.get(0), MAX_WIDTH, MAX_HEIGHT)));

            // make an additional request (should come from cache)
            final RequestRunnable request2 = new RequestRunnable(service, SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT);
            final Thread t1 = new Thread(request2);
            t1.start();
            t1.join(DEADLOCK_TIME);
            Assert.assertFalse("DEADLOCK DETECTED!", t1.isAlive());

            Assert.assertEquals("The CoverSource should only have been accessed once!", 1, source.getRequestCount());
            final InputStream thumbnail2 = request2.getResult();
            final ImageMetadata m2 = p.getMetadata(thumbnail2);
            Assert.assertTrue("Width must less than or equal to the specified maximum.", m2.width() <= MAX_WIDTH);
            Assert.assertTrue("Height must less than or equal to the specified maximum.", m2.height() <= MAX_HEIGHT);


            // make an additional request (should be resized from cache)
            final RequestRunnable request3 = new RequestRunnable(service, SAMPLE_ISBN, MAX_WIDTH / 2, MAX_HEIGHT / 2);
            final Thread t2 = new Thread(request3);
            t2.start();
            t2.join(DEADLOCK_TIME);
            Assert.assertFalse("DEADLOCK DETECTED!", t1.isAlive());
            final InputStream thumbnail3 = request3.getResult();
            final ImageMetadata m3 = p.getMetadata(thumbnail3);
            Assert.assertTrue("Width must less than or equal to the specified maximum.", m3.width() <= MAX_WIDTH / 2);
            Assert.assertTrue("Height must less than or equal to the specified maximum.", m3.height() <= MAX_HEIGHT / 2);
            Assert.assertNotNull("Scaled image should be cached!", cache.getContentAsFile(service.getCacheKeyForCoverImage(SAMPLE_ISBN.get(0), MAX_WIDTH / 2, MAX_HEIGHT /2)));

        } finally {
            cacheTest.cleanUpCache(cache);
        }

    }

    /**
     * This test involves caching and two searches for the same item that fail.
     * If correctly implemented the CoverService will only query the source
     * the first time, having cached the failed search for the second request.
     */
    @Test
    public void testSequentialFailedSearches() throws Exception {
        final Cop cop = new Cop();
        final MockCoverSource source = new MockCoverSource(cop, null);
        FileBinaryCacheTest cacheTest = new FileBinaryCacheTest();
        final BinaryCache cache = cacheTest.getNewCache();
        final SimpleCoverService service = new SimpleCoverService();
        service.addCoverSource(source);
        service.setCoverCache(cache);

        // make request 1
        InputStream result1 = service.getCoverImage(SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT);
        Assert.assertNull("No item should have been found!", result1);
        Assert.assertEquals("Exactly one request should have been made against the source!", 1, source.getRequestCount());
        Assert.assertTrue("The cache should know about the item that wasn't found!", cache.isNoContent(service.getCacheKeyForCoverImage(SAMPLE_ISBN.get(0))));

        // make request 2
        InputStream result2 = service.getCoverImage(SAMPLE_ISBN, MAX_WIDTH, MAX_HEIGHT);
        Assert.assertNull("No item should have been found!", result1);
        Assert.assertEquals("No subsequent request should have been made against the source!", 1, source.getRequestCount());
    }

    private static class RequestRunnable implements Runnable {

        private CoverService service;

        private List<Identifier> identifiers;
        private int maxWidth;
        private int maxHeight;

        private InputStream result;

        public RequestRunnable(CoverService service, List<Identifier> identifiers, int maxWidth, int maxHeight) {
            this.service = service;
            this.identifiers = identifiers;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }

        public InputStream getResult() {
            return result;
        }

        @Override
        public void run() {
            try {
                result = service.getCoverImage(identifiers, maxWidth, maxHeight);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Cop {

        private Set<Thread> warrants;

        private Set<Thread> detainees;

        public Cop() {
            warrants = new HashSet<Thread>();
            detainees = new HashSet<Thread>();
        }

        public synchronized void issueWarrant(Thread t) {
            warrants.add(t);
        }

        public void amIFreeToGo() {
            boolean detained = false;
            do {
                synchronized (this) {
                    if (warrants.contains(Thread.currentThread())) {
                        detainees.add(Thread.currentThread());
                        detained = true;
                    } else {
                        detained = false;
                    }
                }
                if (detained) {
                    Thread.yield();
                }
            } while (detained);
        }

        public void waitForCapture(int timeoutInMs, Thread ... threads) {
            long timeout = System.currentTimeMillis() + timeoutInMs;
            while (System.currentTimeMillis() < timeout) {
                synchronized (this) {
                    boolean allDetained = true;
                    for (Thread t : threads) {
                        if (!isDetained(t)) {
                            allDetained = false;
                        }
                    }
                    if (allDetained) {
                        return;
                    }
                }
                Thread.yield();
            }
            throw new RuntimeException("Deadlock detected!");
        }

        public boolean isDetained(Thread t) {
            synchronized (this) {
                return detainees.contains(t);
            }
        }

        public void release(Thread t) {
              process(t, false, true, false);
        }

        public void pardon(Thread t) {
            process(t, false, true, true);
        }

        private void process(Thread t, boolean waitUntilCapture, boolean release, boolean pardon) {
            boolean done;
            do {
                synchronized (this) {
                    done = !waitUntilCapture || isDetained(t);
                    if (release) {
                        detainees.remove(t);
                    }
                    if (pardon) {
                        warrants.remove(t);
                    }
                }
                Thread.yield();
            } while (!done);
        }
    }

    /**
     * A cover source that "processes" until it is removed from the set of
     * "detainees", at which point it returns
     */
    public static final class MockCoverSource implements CoverSource {

        private int requestCount;

        private Cop cop;

        private CoverImage response;

        public MockCoverSource(Cop cop, CoverImage response) {
            this.cop = cop;
            this.response = response;
        }

        @Override
        public CoverImage getCoverImage(List<Identifier> identifiers) throws IOException {
            requestCount ++;
            LOGGER.debug("getCoverImage invoked by thread " + Thread.currentThread().getName());
            cop.amIFreeToGo();
            return response;
        }

        public int getRequestCount() {
            return requestCount;
        }
    }
}
