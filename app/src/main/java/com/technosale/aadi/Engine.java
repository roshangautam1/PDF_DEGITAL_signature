package com.technosale.aadi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Locale;
public class Engine {
    public static  SecurityAccess door;
    private final String serviceName;
    private volatile ServiceCacheEntry serviceCache;
    private static final class ServiceCacheEntry {
        private final String algorithm;
        private final int refreshNumber;
        private final Provider.Service service;
        private ServiceCacheEntry(String algorithm,
                                  int refreshNumber,
                                  Provider.Service service) {
            this.algorithm = algorithm;
            this.refreshNumber = refreshNumber;
            this.service = service;
        }
    }
    public static final class SpiAndProvider {
        public final Object spi;
        public final Provider provider;
        private SpiAndProvider(Object spi, Provider provider) {
            this.spi = spi;
            this.provider = provider;
        }
    }

    public Engine(String service) {
        this.serviceName = service;
    }
    public SpiAndProvider getInstance(String algorithm, Object param)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Null algorithm name");
        }
 Services.refresh();
        Provider.Service service;
        ServiceCacheEntry cacheEntry = this.serviceCache;
        if (cacheEntry != null
                && cacheEntry.algorithm.equalsIgnoreCase(algorithm)
                && Services.refreshNumber == cacheEntry.refreshNumber) {
            service = cacheEntry.service;
        } else {
            if ( Services.isEmpty()) {
                throw notFound(serviceName, algorithm);
            }
            String name = this.serviceName + "." + algorithm.toUpperCase(Locale.US);
            service =  Services.getService(name);
            if (service == null) {
                throw notFound(serviceName, algorithm);
            }
            this.serviceCache = new ServiceCacheEntry(algorithm,  Services.refreshNumber, service);
        }
        return new SpiAndProvider(service.newInstance(param), service.getProvider());
    }

    public Object getInstance(String algorithm, Provider provider, Object param)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("algorithm == null");
        }
        Provider.Service service = provider.getService(serviceName, algorithm);
        if (service == null) {
            throw notFound(serviceName, algorithm);
        }
        return service.newInstance(param);
    }

    private NoSuchAlgorithmException notFound(String serviceName, String algorithm)
            throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException(serviceName + " " + algorithm
                                           + " implementation not found");
    }
}
