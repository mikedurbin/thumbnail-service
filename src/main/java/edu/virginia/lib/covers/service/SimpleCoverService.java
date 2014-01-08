package edu.virginia.lib.covers.service;

import edu.virginia.lib.covers.BinaryCache;
import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.CoverService;
import edu.virginia.lib.covers.CoverSource;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.UnsupportedIDTypeException;
import edu.virginia.lib.covers.imagemagick.ImageMagickProcess;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

// TODO: add custom mimetyping

public class SimpleCoverService implements CoverService {

    private static final Logger LOGGER = getLogger(SimpleCoverService.class);

    private List<CoverSource> sources;

    private BinaryCache cache;

    public SimpleCoverService() throws IOException {
        this.sources = new ArrayList<CoverSource>();
        this.lockedIds = new HashSet<Identifier>();
    }

    @Override
    public void addCoverSource(CoverSource source) {
        // TODO: add thread safety to sources
        sources.add(source);
    }

    @Override
    public void setCoverCache(BinaryCache cache) {
        this.cache = cache;
    }

    @Override
    public InputStream getCoverImage(List<Identifier> identifiers, int maxWidth, int maxHeight) throws IOException {
        identifiers = new ArrayList<Identifier>(identifiers); // some subsequent calls modify this list
        IdentifierLock l = acquireLock(identifiers);
        try {
            final InputStream cachedResult = consultCache(identifiers, maxWidth, maxHeight);
            if (cachedResult != null) {
                return cachedResult;
            }

            if (identifiers.isEmpty()) {
                return null;
            } else {
                return getAndCacheCoverImageFromSources(identifiers, maxWidth, maxHeight);
            }
        } finally {
            l.release();
        }
    }

    /**
     * Checks the cache for cover images and returns them if found, also updates
     * the passed list of identifiers to remove any that have been marked as
     * having no content with the cache.
     */
    private InputStream consultCache(List<Identifier> identifiers, int maxWidth, int maxHeight) throws IOException {
        if (cache == null) {
            return null;
        }
        List<Identifier> identifiersToRemove = new ArrayList<Identifier>();
        try {
            for (Identifier id :identifiers) {
                final String origCacheKey = getCacheKeyForCoverImage(id);
                if (cache.isNoContent(origCacheKey)) {
                    identifiersToRemove.add(id);
                } else {
                    final String scaledCacheKey = getCacheKeyForCoverImage(id, maxWidth, maxHeight);
                    final InputStream result = cache.getContent(scaledCacheKey);
                    if (result != null) {
                        return result;
                    } else {
                        final File origFile = cache.getContentAsFile(origCacheKey);
                        if (origFile != null) {
                            return createAndCacheScaledImage(id, origFile, maxWidth, maxHeight);
                        }
                    }
                }
            }
            return null;
        } finally {
            identifiers.removeAll(identifiersToRemove);
        }
    }

    private InputStream createAndCacheScaledImage(Identifier id, File orig, int maxWidth, int maxHeight) throws IOException {
        File temp = File.createTempFile("scaled", "thumbnail.jpg");
        ImageMagickProcess.getInstance().createThumbnail(orig, temp, maxWidth, maxHeight);
        FileInputStream fis = new FileInputStream(temp);
        if (cache != null) {
            final String cacheKey = getCacheKeyForCoverImage(id, maxWidth, maxHeight);
            try {
                cache.storeContent(cacheKey, fis);
            } finally {
                fis.close();
                if (!temp.delete()) {
                    LOGGER.warn("Temporary file \"{}\"could not be deleted!", temp.getName());
                }
            }
            return cache.getContent(cacheKey);
        } else {
            return fis;
        }
    }

    private InputStream getAndCacheCoverImageFromSources(List<Identifier> identifiers, int maxWidth, int maxHeight) throws IOException {
        for (CoverSource source : sources) {
            try {
                CoverImage coverImage = source.getCoverImage(identifiers);
                if (coverImage != null) {
                    return createAndCacheResult(coverImage, maxWidth, maxHeight);
                }
            } catch (UnsupportedIDTypeException ex) {
                // skip this source
            } catch (Throwable t) {
                LOGGER.warn("Error fetching thumnail from source: " + source.getClass().getName(), t);
            }
        }
        if (cache != null) {
            for (Identifier id : identifiers) {
                cache.markNoContent(getCacheKeyForCoverImage(id));
            }
        }
        return null;
    }

    /**
     * Takes a non-null result and returns an InputStream for a scaled version
     * of that result.  If a cache is configured, both the original and the
     * scaled version will be stored in it.
     */
    private InputStream createAndCacheResult(CoverImage image, int maxWidth, int maxHeight) throws IOException {
        if (cache != null) {
            final String origKey = getCacheKeyForCoverImage(image.getId());
            cache.storeContent(origKey, image.getInputStream());
            return createAndCacheScaledImage(image.getId(), cache.getContentAsFile(origKey), maxWidth, maxHeight);
        } else {
            return image.getInputStream();
        }
    }

    /**
     * Gets a cache key for the original cover image.
     */
    protected String getCacheKeyForCoverImage(Identifier id) {
        return id.getKey() + "/original";
    }

    /**
     * Gets a cache key for a scaled cover image.
     */
    protected String getCacheKeyForCoverImage(Identifier id, int width, int height) {
        return id.getKey() + File.separatorChar + width + "x" + height;
    }

    /**
     * Gets a lock on the given identifiers.  If another lock is held on any of
     * those identifiers this method blocks until that lock can be acquired.
     */
    protected IdentifierLock acquireLock(List<Identifier> identifiers) {
        return new IdentifierLock(identifiers);
    }

    /**
     * This member variable is used by the IdentifierLock to contain all of the
     * identifiers for the current operation.  The goal of this mechanism is to
     * avoid concurrency issues with the cache and to avoid duplicate effort by
     * having concurrent operations on the same IDs block.
     */
    private final Collection<Identifier> lockedIds;

    private final class IdentifierLock {

        private List<Identifier> identifiers;

        public IdentifierLock(List<Identifier> ids) {
            this.identifiers = new ArrayList<Identifier>(ids);
            boolean acquired = false;
            boolean logged = false;
            while (!acquired) {
                synchronized (lockedIds) {
                    if (Collections.disjoint(lockedIds, identifiers)) {
                        lockedIds.addAll(identifiers);
                        acquired = true;
                        if (LOGGER.isDebugEnabled()) {
                            for (Identifier i : identifiers) {
                                LOGGER.debug("Thread " + Thread.currentThread().getName() + " acquired the lock on ID " + i + ".");
                            }
                        }
                    }
                }
                if (!acquired) {
                    if (LOGGER.isDebugEnabled()) {
                        if (!logged) {
                            LOGGER.debug("Thread " + Thread.currentThread().getName() + " is yielding to another thread working on one or more of its ids.");
                            logged = true;
                        }
                    }
                    Thread.yield();
                }
            }
        }

        public void release() {
            synchronized (lockedIds) {
                lockedIds.removeAll(identifiers);
                if (LOGGER.isDebugEnabled()) {
                    for (Identifier i : identifiers) {
                        LOGGER.debug("Thread " + Thread.currentThread().getName() + " released the lock on ID " + i + ".");
                    }
                }
            }
        }

    }
}
