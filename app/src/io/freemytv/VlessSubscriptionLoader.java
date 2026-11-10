package io.freemytv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

/**
 * Reads a subscription over HTTPS and returns parsed VLESS servers.
 * Must run on a worker thread; never call from an Activity's UI thread.
 *
 * The URL may contain an access token: never log it, include it in an error
 * message, send it to another host via redirects, or commit it to source.
 * Does not install a VPN tunnel or save the subscription URL.
 */
final class VlessSubscriptionLoader {
    private static final int MAX_BYTES = 2_000_000;
    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private VlessSubscriptionLoader() {}

    static List<VlessSubscription.Server> fetch(String subscriptionUrl) throws IOException {
        if (subscriptionUrl == null || subscriptionUrl.length() > 8192) {
            throw new IOException("Invalid subscription URL");
        }
        URL url;
        try {
            url = new URL(subscriptionUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid subscription URL");
        }
        if (!"https".equalsIgnoreCase(url.getProtocol()) ||
                url.getHost() == null || url.getHost().isEmpty() ||
                url.getUserInfo() != null) {
            throw new IOException("Only HTTPS subscriptions without URL credentials are supported");
        }
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false); // Do not leak token to redirect host.
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/plain, */*");
            connection.setUseCaches(false);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                // Status only; response Location/body can contain confidential tokens.
                throw new IOException("Subscription request returned HTTP " + status);
            }
            int contentLength = connection.getContentLength();
            if (contentLength > MAX_BYTES) throw new IOException("Subscription is too large");
            byte[] content;
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = in.read(chunk)) != -1) {
                    if (out.size() + count > MAX_BYTES) throw new IOException("Subscription is too large");
                    out.write(chunk, 0, count);
                }
                content = out.toByteArray();
            }
            return VlessSubscription.parse(new String(content, StandardCharsets.UTF_8));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
