/* $Id:$ */

package ibis.util;

/**
 * Utility for measuring time, using the Java 1.5 nanotimer, but in such
 * a way that this compiles on Java 1.4.
 */
public class NanoTimer extends Timer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns implementation name of this timer ("e.g., "javaTimer").
     * 
     * @return the implementation name.
     */
    public String implementationName() {
        return "ibis.util.NanoTimer";
    }

    public synchronized void add(long t, int cnt) {
        time += 1000.0 * t;
        count += cnt;
    }

    public double accuracy() {
        return 1e-9;
    }

    public long currentTimeNanos() {
        return invokeNanoTimer();
    }

    public double totalTimeVal() {
        long cur_time = 0;
        if (started) {
            cur_time = invokeNanoTimer() - t_start;
        }
        return (time+cur_time) / 1000.0;
    }

    public double averageTimeVal() {
        if (count > 0) {
            return ((double) time) / (1000 * count);
        }
        return 0.0;
    }

    public double lastTimeVal() {
        return lastTime / 1000.0;
    }

    public double maxTimeVal() {
        return maxTime / 1000.0;
    }

    public double minTimeVal() {
        return minTime / 1000.0;
    }

    public void start() {
        if (started) {
            throw new Error("Timer started twice");
        }
        started = true;
        t_start = invokeNanoTimer();
    }

    public void stop() {
        if (!started) {
            throw new Error("Time stopped, but not started");
        }

        lastTime = invokeNanoTimer() - t_start;
        time += lastTime;
        if (lastTime > maxTime) {
            maxTime = lastTime;
        }
        if (lastTime < minTime) {
            minTime = lastTime;
        }
        ++count;
        started = false;
    }
}
