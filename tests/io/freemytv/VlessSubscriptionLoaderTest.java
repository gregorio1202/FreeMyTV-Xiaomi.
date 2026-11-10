package io.freemytv;

import java.io.IOException;

/** JVM smoke tests for rejecting insecure subscription URLs; no network required. */
public class VlessSubscriptionLoaderTest {
    private static void rejects(String url) {
        try {
            VlessSubscriptionLoader.fetch(url);
            throw new AssertionError("Accepted insecure or invalid URL");
        } catch (IOException expected) {
            // Only the error class is asserted, never print URL tokens.
        }
    }

    public static void main(String[] args) {
        rejects(null);
        rejects("http://example.test/subscription?token=dummy");
        rejects("file:///etc/passwd");
        rejects("https://user:pass@example.test/subscription");
        rejects("bad url");
        System.out.println("VlessSubscriptionLoaderTest: 5 invalid-URL checks passed");
    }
}
