package io.freemytv;
final class NetworkTrafficMeterTest {
    private static void check(boolean okay) { if (!okay) throw new AssertionError(); }
    public static void main(String[] args) {
        NetworkTrafficMeter m = new NetworkTrafficMeter();
        check(m.label().contains("—"));
        check(!m.update(1_000, 2_000, 1000));
        check(m.update(2_001_000, 1_002_000, 3000));
        check(m.label().contains("8.0") && m.label().contains("4.0"));
        check(m.update(10, 10, 5000));
        check(m.label().contains("—"));
        m.reset();
        check(m.label().contains("—"));
        check(!m.update(-1, -1, 6000));
        System.out.println("NetworkTrafficMeter checks passed");
    }
}
