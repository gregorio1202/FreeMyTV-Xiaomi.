package io.freemytv;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Imports VLESS URI subscriptions. This class only parses server metadata;
 * it does NOT create a VPN tunnel, probe latency, or store credentials.
 * Call it with the body of an HTTPS subscription response, not its URL.
 */
final class VlessSubscription {
    private static final int MAX_BODY_CHARS = 2_000_000;
    private static final int MAX_SERVERS = 500;
    private VlessSubscription() {}

    static final class Server {
        final String name;
        final String uri; // Sensitive: includes server UUID; never log or publish.
        final String host;
        final int port;

        Server(String name, String uri, String host, int port) {
            this.name = name;
            this.uri = uri;
            this.host = host;
            this.port = port;
        }
    }

    static List<Server> parse(String response) {
        if (response == null) return Collections.emptyList();
        String body = response.trim();
        if (body.isEmpty() || body.length() > MAX_BODY_CHARS) return Collections.emptyList();
        // Subscription providers typically return a plain URI list or base64 of it.
        if (!body.contains("vless://")) {
            String compact = body.replaceAll("\\s+", "");
            try {
                byte[] decoded = Base64.decode(compact, Base64.DEFAULT);
                String candidate = new String(decoded, StandardCharsets.UTF_8);
                if (candidate.contains("vless://") && candidate.length() <= MAX_BODY_CHARS) body = candidate;
                else return Collections.emptyList();
            } catch (IllegalArgumentException badBase64) {
                return Collections.emptyList();
            }
        }
        List<Server> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : body.split("\\r?\\n")) {
            if (result.size() >= MAX_SERVERS) break;
            String raw = line.trim();
            if (!raw.startsWith("vless://") || raw.length() > 8192 || !seen.add(raw)) continue;
            try {
                Uri parsed = Uri.parse(raw);
                String host = parsed.getHost();
                int port = parsed.getPort();
                String uuid = parsed.getUserInfo();
                if (host == null || host.isEmpty() || port < 1 || port > 65535 ||
                    uuid == null || uuid.trim().isEmpty()) continue;
                String name = parsed.getFragment();
                if (name == null || name.trim().isEmpty()) name = host;
                name = name.replaceAll("[\\p{Cntrl}]", " ").trim();
                if (name.length() > 120) name = name.substring(0, 120);
                result.add(new Server(name, raw, host, port));
            } catch (RuntimeException invalid) {
                // Never log raw: it contains a user-specific server credential.
            }
        }
        return result;
    }
}
