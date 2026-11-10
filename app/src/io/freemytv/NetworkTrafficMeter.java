package io.freemytv;

import android.net.TrafficStats;
import android.os.SystemClock;
import java.util.Locale;

/**
 * Device-wide traffic rate, not a broadband speed test. VPN and local traffic may
 * be counted by Android, so the numbers are approximate transfer rates only.
 * Runs on the home view's main-thread timer, at most once every two seconds.
 */
final class NetworkTrafficMeter {
    private long previousRx = -1;
    private long previousTx = -1;
    private long previousTime = -1;
    private String label = "↓ —    ↑ —";

    void reset() {
        previousRx = -1;
        previousTx = -1;
        previousTime = -1;
        label = "↓ —    ↑ —";
    }

    String label() { return label; }

    boolean sample() {
        return update(TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes(),
                SystemClock.elapsedRealtime());
    }

    /** Package-private for deterministic tests without reading Android counters. */
    boolean update(long rx, long tx, long nowMs) {
        String next = label;
        if (rx < 0 || tx < 0) {
            reset();
            return !next.equals(label);
        }
        if (previousTime >= 0 && nowMs > previousTime && rx >= previousRx && tx >= previousTx) {
            double seconds = (nowMs - previousTime) / 1000.0;
            double down = (rx - previousRx) * 8.0 / seconds / 1_000_000.0;
            double up = (tx - previousTx) * 8.0 / seconds / 1_000_000.0;
            label = "↓ " + rate(down) + "    ↑ " + rate(up);
        } else {
            label = "↓ —    ↑ —";
        }
        previousRx = rx;
        previousTx = tx;
        previousTime = nowMs;
        return !next.equals(label);
    }

    private static String rate(double rate) {
        if (Double.isNaN(rate) || Double.isInfinite(rate) || rate < 0) return "—";
        if (rate > 9999) return ">9999 Мбит/с";
        return String.format(Locale.getDefault(), "%.1f Мбит/с", rate);
    }
}
