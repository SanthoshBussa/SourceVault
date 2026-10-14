package com.sourcevault.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates URLs and IP addresses to prevent Server-Side Request Forgery (SSRF).
 * Rejects loopback, private RFC1918, link-local, cloud metadata, and unsupported schemes.
 */
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);
    private static final Pattern IPV4_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final boolean allowLocalForTesting;

    public SsrfValidator() {
        this(Boolean.getBoolean("sourcevault.security.allow-local") || 
             "true".equalsIgnoreCase(System.getenv("SOURCEVAULT_ALLOW_LOCAL")));
    }

    public SsrfValidator(boolean allowLocalForTesting) {
        this.allowLocalForTesting = allowLocalForTesting;
    }

    /**
     * Normalizes and validates a given website URL.
     * Returns the normalized URI.
     */
    public URI validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }

        String trimmed = rawUrl.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("http://") && 
            !trimmed.toLowerCase(Locale.ROOT).startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }

        URI uri;
        try {
            uri = URI.create(trimmed).normalize();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + rawUrl, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are permitted. Found: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("URL must specify a valid host.");
        }

        host = host.toLowerCase(Locale.ROOT);

        // Disallow username / password in URL
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URLs with user credentials are not allowed.");
        }

        // Validate Host and resolved IPs
        validateHost(host);

        return uri;
    }

    public void validateHost(String host) {
        if (allowLocalForTesting) {
            return;
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);

        // Check dangerous literal hostnames
        if (lowerHost.equals("localhost") || 
            lowerHost.endsWith(".localhost") || 
            lowerHost.equals("127.0.0.1") ||
            lowerHost.equals("0.0.0.0") ||
            lowerHost.equals("::1") ||
            lowerHost.equals("metadata.google.internal") ||
            lowerHost.equals("instance-data")) {
            throw new SecurityException("Access to local/metadata host is blocked: " + host);
        }

        // DNS resolution check
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException("Host could not be resolved: " + host);
            }

            for (InetAddress address : addresses) {
                validateAddress(address);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + host, e);
        }
    }

    public void validateAddress(InetAddress address) {
        if (allowLocalForTesting) {
            return;
        }

        if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
            throw new SecurityException("Loopback address access blocked: " + address.getHostAddress());
        }

        if (address.isSiteLocalAddress()) {
            throw new SecurityException("Private RFC1918 address access blocked: " + address.getHostAddress());
        }

        if (address.isLinkLocalAddress()) {
            throw new SecurityException("Link-local address access blocked: " + address.getHostAddress());
        }

        if (address.isMulticastAddress()) {
            throw new SecurityException("Multicast address access blocked: " + address.getHostAddress());
        }

        byte[] rawBytes = address.getAddress();

        // Check IPv4 specific ranges
        if (rawBytes.length == 4) {
            int b0 = rawBytes[0] & 0xFF;
            int b1 = rawBytes[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) {
                throw new SecurityException("Access to 10.0.0.0/8 is blocked.");
            }
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) {
                throw new SecurityException("Access to 172.16.0.0/12 is blocked.");
            }
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) {
                throw new SecurityException("Access to 192.168.0.0/16 is blocked.");
            }
            // 169.254.0.0/16 (Cloud Metadata / Link-local)
            if (b0 == 169 && b1 == 254) {
                throw new SecurityException("Access to link-local/cloud metadata (169.254.x.x) is blocked.");
            }
            // 100.64.0.0/10 (Carrier grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                throw new SecurityException("Access to carrier-grade NAT range is blocked.");
            }
            // 127.0.0.0/8
            if (b0 == 127) {
                throw new SecurityException("Access to loopback range is blocked.");
            }
            // 0.0.0.0/8
            if (b0 == 0) {
                throw new SecurityException("Access to 0.0.0.0/8 is blocked.");
            }
        }
    }

    public boolean isAllowLocalForTesting() {
        return allowLocalForTesting;
    }
}
