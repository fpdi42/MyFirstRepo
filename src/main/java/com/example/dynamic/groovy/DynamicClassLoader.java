package com.example.dynamic.groovy;

import groovy.lang.GroovyClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hanterar dynamisk klassladning med caching, GC-support och isolering
 * Använder SoftReference för att tillåta GC att frigöra minne när det behövs
 */
public class DynamicClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(DynamicClassLoader.class);

    private final GroovyClassLoader groovyClassLoader;
    private final ConcurrentHashMap<String, SoftReference<Class<?>>> classCache;
    private final ConcurrentHashMap<String, String> sourceCodeCache;
    private static final int MAX_CACHE_SIZE = 1000;

    public DynamicClassLoader() {
        // Skapa isolerad GroovyClassLoader
        this.groovyClassLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
        this.classCache = new ConcurrentHashMap<>();
        this.sourceCodeCache = new ConcurrentHashMap<>();
        logger.info("DynamicClassLoader initialiserad");
    }

    /**
     * Kompilerar och laddar Groovy-klasskällkod
     * @param fullyQualifiedName Klassnamn med paket (t.ex. com.example.Person)
     * @param sourceCode Groovy-klasskällkod
     * @return Kompilerad klass
     * @throws GroovyCompilationException om kompilering misslyckas
     */
    public Class<?> compileAndLoad(String fullyQualifiedName, String sourceCode) throws GroovyCompilationException {
        try {
            // Beräkna cache-nyckel baserat på källkod-hash
            String cacheKey = generateCacheKey(fullyQualifiedName, sourceCode);

            // Kontrollera om klassen redan är cachad
            SoftReference<Class<?>> cachedReference = classCache.get(cacheKey);
            if (cachedReference != null) {
                Class<?> cachedClass = cachedReference.get();
                if (cachedClass != null) {
                    logger.debug("Använder cachad klass: {}", fullyQualifiedName);
                    return cachedClass;
                } else {
                    logger.debug("Cachad klass samtalades av GC: {}", fullyQualifiedName);
                    classCache.remove(cacheKey);
                }
            }

            // Begränsa cache-storlek
            if (classCache.size() >= MAX_CACHE_SIZE) {
                logger.warn("KlassCache nådde maximal storlek ({}). Rensar cache.", MAX_CACHE_SIZE);
                classCache.clear();
            }

            // Kompilera Groovy-kod
            logger.debug("Kompilerar Groovy-klass: {}", fullyQualifiedName);
            Class<?> compiledClass = groovyClassLoader.parseClass(sourceCode, fullyQualifiedName);

            // Cache kompilerad klass och källkod
            classCache.put(cacheKey, new SoftReference<>(compiledClass));
            sourceCodeCache.put(cacheKey, sourceCode);

            logger.info("Klass kompilerad och cachad framgångsrikt: {}", fullyQualifiedName);
            return compiledClass;

        } catch (Exception e) {
            logger.error("Fel vid kompilering av Groovy-klass: {}", fullyQualifiedName, e);
            throw new GroovyCompilationException("Misslyckades att kompilera Groovy-klass: " + fullyQualifiedName, e);
        }
    }

    /**
     * Instansierar en tidigare kompilerad klass
     */
    public Object instantiate(Class<?> clazz) throws GroovyCompilationException {
        try {
            logger.debug("Instansierar klass: {}", clazz.getName());
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Fel vid instansiering av klass: {}", clazz.getName(), e);
            throw new GroovyCompilationException("Misslyckades att instansiera: " + clazz.getName(), e);
        }
    }

    /**
     * Genererar cache-nyckel baserat på klassnamn och källkod-hash
     */
    private String generateCacheKey(String fullyQualifiedName, String sourceCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceCode.getBytes());
            String sourceHash = HexFormat.of().formatHex(hash);
            return fullyQualifiedName + "_" + sourceHash;
        } catch (NoSuchAlgorithmException e) {
            logger.warn("SHA-256 ej tillgängligt, använder fallback-nyckel");
            return fullyQualifiedName + "_" + sourceCode.hashCode();
        }
    }

    /**
     * Hämtar ursprunglig källkod för en cachad klass
     */
    public String getSourceCode(String cacheKey) {
        return sourceCodeCache.get(cacheKey);
    }

    /**
     * Returnerar cachestatus
     */
    public CacheStats getCacheStats() {
        return new CacheStats(classCache.size(), sourceCodeCache.size());
    }

    /**
     * Rensar cache och stänger GroovyClassLoader
     */
    public void cleanup() {
        logger.info("Rensar DynamicClassLoader-cache");
        classCache.clear();
        sourceCodeCache.clear();
        groovyClassLoader.clearCache();
    }

    /**
     * Custom exception för Groovy-kompileringsfel
     */
    public static class GroovyCompilationException extends Exception {
        public GroovyCompilationException(String message) {
            super(message);
        }

        public GroovyCompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Cache-statistik
     */
    public static class CacheStats {
        private final int classCount;
        private final int sourceCodeCount;

        public CacheStats(int classCount, int sourceCodeCount) {
            this.classCount = classCount;
            this.sourceCodeCount = sourceCodeCount;
        }

        public int getClassCount() {
            return classCount;
        }

        public int getSourceCodeCount() {
            return sourceCodeCount;
        }

        @Override
        public String toString() {
            return "CacheStats{" +
                    "classCount=" + classCount +
                    ", sourceCodeCount=" + sourceCodeCount +
                    '}';
        }
    }
}
