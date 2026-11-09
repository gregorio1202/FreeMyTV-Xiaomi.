package io.freemytv;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses VLESS URI lists or base64 subscription bodies; does not connect a VPN. */
final class VlessSubscription {
    private VlessSubscription() {}

    static final class Server {
        final String name;
        final String uri;
        Server(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }
    }

    static List<Server> parse(String response) {
        if (response == null || response.trim().isEmpty()) return Collections.emptyList();
        String body = response.trim();
        if (!body.contains("vless://")) {
            try {
                String decoded = new String(Base64.decode(body, Base64.DEFAULT), StandardCharsets.UTF_8);
                if (decoded.contains("vless://")) body = decoded;
            } catch (IllegalArgumentException ignored) {
                return Collections.emptyList();
            }
        }
        List<Server> result = new ArrayList<>();
        for (String line : body.split("\r?\n")) {
            String raw = line.trim();
            if (!raw.startsWith("vless://")) continue;
            try {
                Uri parsed = Uri.parse(raw);
                String host = parsed.getHost();
                if (host == null || host.isEmpty() || parsed.getPort() < 1) continue;
                String name = parsed.getFragment();
                if (name == null || name.trim().isEmpty()) name = host;
                result.add(new Server(name, raw));
            } catch (RuntimeException ignored) {
                // Invalid subscription entries are skipped without logging credentials.
            }
        }
        return result;
    }
}
