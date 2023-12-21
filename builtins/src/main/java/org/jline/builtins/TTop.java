/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.builtins.Options.HelpException;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.*;

import static org.jline.builtins.TTop.Align.Left;
import static org.jline.builtins.TTop.Align.Right;

/**
 * Thread Top implementation.
 *
 * TODO: option modification at runtime (such as implemented in less) is not currently supported
 * TODO: one possible addition would be to detect deadlock threads and display them in a specific way
 */
public class TTop {

    public static final String STAT_UPTIME = "uptime";

    public static final String STAT_TID = "tid";
    public static final String STAT_NAME = "name";
    public static final String STAT_STATE = "state";
    public static final String STAT_BLOCKED_TIME = "blocked_time";
    public static final String STAT_BLOCKED_COUNT = "blocked_count";
    public static final String STAT_WAITED_TIME = "waited_time";
    public static final String STAT_WAITED_COUNT = "waited_count";
    public static final String STAT_LOCK_NAME = "lock_name";
    public static final String STAT_LOCK_OWNER_ID = "lock_owner_id";
    public static final String STAT_LOCK_OWNER_NAME = "lock_owner_name";
    public static final String STAT_USER_TIME = "user_time";
    public static final String STAT_USER_TIME_PERC = "user_time_perc";
    public static final String STAT_CPU_TIME = "cpu_time";
    public static final String STAT_CPU_TIME_PERC = "cpu_time_perc";

    public List<String> sort;
    public long delay;
    public List<String> stats;
    public int nthreads;

    public enum Align {
        Left,
        Right
    };

    public enum Operation {
        INCREASE_DELAY,
        DECREASE_DELAY,
        HELP,
        EXIT,
        CLEAR,
        REVERSE
    }

    public static void ttop(Terminal terminal, PrintStream out, PrintStream err, String[] argv) throws Exception {
        final String[] usage = {
            "ttop -  display and update sorted information about threads",
            "Usage: ttop [OPTIONS]",
            "  -? --help                    Show help",
            "  -o --order=ORDER             Comma separated list of sorting keys",
            "  -t --stats=STATS             Comma separated list of stats to display",
            "  -s --seconds=SECONDS         Delay between updates in seconds",
            "  -m --millis=MILLIS           Delay between updates in milliseconds",
            "  -n --nthreads=NTHREADS       Only display up to NTHREADS threads",
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        TTop ttop = new TTop(terminal);
        ttop.sort = opt.isSet("order") ? Arrays.asList(opt.get("order").split(",")) : null;
        ttop.delay = opt.isSet("seconds") ? opt.getNumber("seconds") * 1000 : ttop.delay;
        ttop.delay = opt.isSet("millis") ? opt.getNumber("millis") : ttop.delay;
        ttop.stats = opt.isSet("stats") ? Arrays.asList(opt.get("stats").split(",")) : null;
        ttop.nthreads = opt.isSet("nthreads") ? opt.getNumber("nthreads") : ttop.nthreads;
        ttop.run();
    }

    private final Map<String, Column> columns = new LinkedHashMap<>();
    private final Terminal terminal;
    private final Display display;
    private final BindingReader bindingReader;
    private final KeyMap<Operation> keys;
    private final Size size = new Size();

    private Comparator<Map<String, Comparable<?>>> comparator;

    // Internal cache data
    private Map<Long, Map<String, Object>> previous = new HashMap<>();
    private Map<Long, Map<String, Long>> changes = new HashMap<>();
    private Map<String, Integer> widths = new HashMap<>();

    public TTop(Terminal terminal) {
        this.terminal = terminal;
        this.display = new Display(terminal, true);
        this.bindingReader = new BindingReader(terminal.reader());

        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        DecimalFormat perc = new DecimalFormat("0.00%", dfs);

        register(STAT_TID, Right, "TID", o -> String.format("%3d", (Long) o));
        register(STAT_NAME, Left, "NAME", padcut(40));
        register(STAT_STATE, Left, "STATE", o -> o.toString().toLowerCase());
        register(STAT_BLOCKED_TIME, Right, "T-BLOCKED", o -> millis((Long) o));
        register(STAT_BLOCKED_COUNT, Right, "#-BLOCKED", Object::toString);
        register(STAT_WAITED_TIME, Right, "T-WAITED", o -> millis((Long) o));
        register(STAT_WAITED_COUNT, Right, "#-WAITED", Object::toString);
        register(STAT_LOCK_NAME, Left, "LOCK-NAME", Object::toString);
        register(STAT_LOCK_OWNER_ID, Right, "LOCK-OWNER-ID", id -> ((Long) id) >= 0 ? id.toString() : "");
        register(STAT_LOCK_OWNER_NAME, Left, "LOCK-OWNER-NAME", name -> name != null ? name.toString() : "");
        register(STAT_USER_TIME, Right, "T-USR", o -> nanos((Long) o));
        register(STAT_CPU_TIME, Right, "T-CPU", o -> nanos((Long) o));
        register(STAT_USER_TIME_PERC, Right, "%-USR", perc::format);
        register(STAT_CPU_TIME_PERC, Right, "%-CPU", perc::format);

        keys = new KeyMap<>();
        bindKeys(keys);
    }

    public KeyMap<Operation> getKeys() {
        return keys;
    }

    public void run() throws IOException, InterruptedException {
        comparator = buildComparator(sort);
        delay = delay > 0 ? Math.max(delay, 100) : 1000;
        if (stats == null || stats.isEmpty()) {
            stats = new ArrayList<>(Arrays.asList(STAT_TID, STAT_NAME, STAT_STATE, STAT_CPU_TIME, STAT_LOCK_OWNER_ID));
        }

        Boolean isThreadContentionMonitoringEnabled = null;
        ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
        if (stats.contains(STAT_BLOCKED_TIME)
                || stats.contains(STAT_BLOCKED_COUNT)
                || stats.contains(STAT_WAITED_TIME)
                || stats.contains(STAT_WAITED_COUNT)) {
            if (threadsBean.isThreadContentionMonitoringSupported()) {
                isThreadContentionMonitoringEnabled = threadsBean.isThreadContentionMonitoringEnabled();
                if (!isThreadContentionMonitoringEnabled) {
                    threadsBean.setThreadContentionMonitoringEnabled(true);
                }
            } else {
                stats.removeAll(
                        Arrays.asList(STAT_BLOCKED_TIME, STAT_BLOCKED_COUNT, STAT_WAITED_TIME, STAT_WAITED_COUNT));
            }
        }
        Boolean isThreadCpuTimeEnabled = null;
        if (stats.contains(STAT_USER_TIME) || stats.contains(STAT_CPU_TIME)) {
            if (threadsBean.isThreadCpuTimeSupported()) {
                isThreadCpuTimeEnabled = threadsBean.isThreadCpuTimeEnabled();
                if (!isThreadCpuTimeEnabled) {
                    threadsBean.setThreadCpuTimeEnabled(true);
                }
            } else {
                stats.removeAll(Arrays.asList(STAT_USER_TIME, STAT_CPU_TIME));
            }
        }

        size.copy(terminal.getSize());
        Terminal.SignalHandler prevHandler = terminal.handle(Terminal.Signal.WINCH, this::handle);
        Attributes attr = terminal.enterRawMode();
        try {

            // Use alternate buffer
            if (!terminal.puts(InfoCmp.Capability.enter_ca_mode)) {
                terminal.puts(InfoCmp.Capability.clear_screen);
            }
            terminal.puts(InfoCmp.Capability.keypad_xmit);
            terminal.puts(InfoCmp.Capability.cursor_invisible);
            terminal.writer().flush();

            long t0 = System.currentTimeMillis();

            Operation op;
            do {
                display();
                checkInterrupted();

                op = null;

                long delta = ((System.currentTimeMillis() - t0) / delay + 1) * delay + t0 - System.currentTimeMillis();
                int ch = bindingReader.peekCharacter(delta);
                if (ch == -1) {
                    op = Operation.EXIT;
                } else if (ch != NonBlockingReader.READ_EXPIRED) {
                    op = bindingReader.readBinding(keys, null, false);
                }
                if (op == null) {
                    continue;
                }

                switch (op) {
                    case INCREASE_DELAY:
                        delay = delay * 2;
                        t0 = System.currentTimeMillis();
                        break;
                    case DECREASE_DELAY:
                        delay = Math.max(delay / 2, 16);
                        t0 = System.currentTimeMillis();
                        break;
                    case CLEAR:
                        display.clear();
                        break;
                    case REVERSE:
                        comparator = comparator.reversed();
                        break;
                }
            } while (op != Operation.EXIT);
        } catch (InterruptedException ie) {
            // Do nothing
        } catch (Error err) {
            Log.info("Error: ", err);
            return;
        } finally {
            terminal.setAttributes(attr);
            if (prevHandler != null) {
                terminal.handle(Terminal.Signal.WINCH, prevHandler);
            }
            // Use main buffer
            if (!terminal.puts(InfoCmp.Capability.exit_ca_mode)) {
                terminal.puts(InfoCmp.Capability.clear_screen);
            }
            terminal.puts(InfoCmp.Capability.keypad_local);
            terminal.puts(InfoCmp.Capability.cursor_visible);
            terminal.writer().flush();

            if (isThreadContentionMonitoringEnabled != null) {
                threadsBean.setThreadContentionMonitoringEnabled(isThreadContentionMonitoringEnabled);
            }
            if (isThreadCpuTimeEnabled != null) {
                threadsBean.setThreadCpuTimeEnabled(isThreadCpuTimeEnabled);
            }
        }
    }

    private void handle(Terminal.Signal signal) {
        int prevw = size.getColumns();
        size.copy(terminal.getSize());
        try {
            if (size.getColumns() < prevw) {
                display.clear();
            }
            display();
        } catch (IOException e) {
            // ignore
        }
    }

    private List<Map<String, Comparable<?>>> infos() {
        long ctime = ManagementFactory.getRuntimeMXBean().getUptime();
        Long ptime = (Long) previous.computeIfAbsent(-1L, id -> new HashMap<>()).put(STAT_UPTIME, ctime);
        long delta = ptime != null ? ctime - ptime : 0L;

        ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = threadsBean.dumpAllThreads(false, false);
        List<Map<String, Comparable<?>>> threads = new ArrayList<>();
        for (ThreadInfo ti : infos) {
            Map<String, Comparable<?>> t = new HashMap<>();
            t.put(STAT_TID, ti.getThreadId());
            t.put(STAT_NAME, ti.getThreadName());
            t.put(STAT_STATE, ti.getThreadState());
            if (threadsBean.isThreadContentionMonitoringEnabled()) {
                t.put(STAT_BLOCKED_TIME, ti.getBlockedTime());
                t.put(STAT_BLOCKED_COUNT, ti.getBlockedCount());
                t.put(STAT_WAITED_TIME, ti.getWaitedTime());
                t.put(STAT_WAITED_COUNT, ti.getWaitedCount());
            }
            t.put(STAT_LOCK_NAME, ti.getLockName());
            t.put(STAT_LOCK_OWNER_ID, ti.getLockOwnerId());
            t.put(STAT_LOCK_OWNER_NAME, ti.getLockOwnerName());
            if (threadsBean.isThreadCpuTimeSupported() && threadsBean.isThreadCpuTimeEnabled()) {
                long tid = ti.getThreadId(), t0, t1;
                // Cpu
                t1 = threadsBean.getThreadCpuTime(tid);
                t0 = (Long) previous.computeIfAbsent(tid, id -> new HashMap<>()).getOrDefault(STAT_CPU_TIME, t1);
                t.put(STAT_CPU_TIME, t1);
                t.put(STAT_CPU_TIME_PERC, (delta != 0) ? (t1 - t0) / ((double) delta * 1000000) : 0.0d);
                // User
                t1 = threadsBean.getThreadUserTime(tid);
                t0 = (Long) previous.computeIfAbsent(tid, id -> new HashMap<>()).getOrDefault(STAT_USER_TIME, t1);
                t.put(STAT_USER_TIME, t1);
                t.put(STAT_USER_TIME_PERC, (delta != 0) ? (t1 - t0) / ((double) delta * 1000000) : 0.0d);
            }
            threads.add(t);
        }
        return threads;
    }

    private void align(AttributedStringBuilder sb, String val, int width, Align align) {
        if (align == Align.Left) {
            sb.append(val);
            for (int i = 0; i < width - val.length(); i++) {
                sb.append(' ');
            }
        } else {
            for (int i = 0; i < width - val.length(); i++) {
                sb.append(' ');
            }
            sb.append(val);
        }
    }

    private synchronized void display() throws IOException {
        long now = System.currentTimeMillis();

        display.resize(size.getRows(), size.getColumns());

        List<AttributedString> lines = new ArrayList<>();
        AttributedStringBuilder sb = new AttributedStringBuilder(size.getColumns());

        // Top headers
        sb.style(sb.style().bold());
        sb.append("ttop");
        sb.style(sb.style().boldOff());
        sb.append(" - ");
        sb.append(String.format("%8tT", new Date()));
        sb.append(".");

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        String osinfo = "OS: " + os.getName() + " " + os.getVersion() + ", " + os.getArch() + ", "
                + os.getAvailableProcessors() + " cpus.";
        if (sb.length() + 1 + osinfo.length() < size.getColumns()) {
            sb.append(" ");
        } else {
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }
        sb.append(osinfo);

        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        String clsinfo = "Classes: " + cl.getLoadedClassCount() + " loaded, " + cl.getUnloadedClassCount()
                + " unloaded, " + cl.getTotalLoadedClassCount() + " loaded total.";
        if (sb.length() + 1 + clsinfo.length() < size.getColumns()) {
            sb.append(" ");
        } else {
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }
        sb.append(clsinfo);

        ThreadMXBean th = ManagementFactory.getThreadMXBean();
        String thinfo = "Threads: " + th.getThreadCount() + ", peak: " + th.getPeakThreadCount() + ", started: "
                + th.getTotalStartedThreadCount() + ".";
        if (sb.length() + 1 + thinfo.length() < size.getColumns()) {
            sb.append(" ");
        } else {
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }
        sb.append(thinfo);

        MemoryMXBean me = ManagementFactory.getMemoryMXBean();
        String meinfo = "Memory: " + "heap: "
                + memory(
                        me.getHeapMemoryUsage().getUsed(),
                        me.getHeapMemoryUsage().getMax()) + ", non heap: "
                + memory(
                        me.getNonHeapMemoryUsage().getUsed(),
                        me.getNonHeapMemoryUsage().getMax()) + ".";
        if (sb.length() + 1 + meinfo.length() < size.getColumns()) {
            sb.append(" ");
        } else {
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }
        sb.append(meinfo);

        StringBuilder sbc = new StringBuilder();
        sbc.append("GC: ");
        boolean first = true;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (first) {
                first = false;
            } else {
                sbc.append(", ");
            }
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            sbc.append(gc.getName())
                    .append(": ")
                    .append(count)
                    .append(" col. / ")
                    .append(String.format("%d", time / 1000))
                    .append(".")
                    .append(String.format("%03d", time % 1000))
                    .append(" s");
        }
        sbc.append(".");
        if (sb.length() + 1 + sbc.length() < size.getColumns()) {
            sb.append(" ");
        } else {
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }
        sb.append(sbc);
        lines.add(sb.toAttributedString());
        sb.setLength(0);

        lines.add(sb.toAttributedString());

        // Threads
        List<Map<String, Comparable<?>>> threads = infos();
        Collections.sort(threads, comparator);
        int nb = Math.min(size.getRows() - lines.size() - 2, nthreads > 0 ? nthreads : threads.size());
        // Compute values
        List<Map<String, String>> values = threads.subList(0, nb).stream()
                .map(thread -> stats.stream().collect(Collectors.toMap(Function.identity(), key -> columns.get(key)
                        .format
                        .apply(thread.get(key)))))
                .collect(Collectors.toList());
        for (String key : stats) {
            int width =
                    values.stream().mapToInt(map -> map.get(key).length()).max().orElse(0);
            widths.put(key, Math.max(columns.get(key).header.length(), Math.max(width, widths.getOrDefault(key, 0))));
        }
        List<String> cstats;
        if (widths.values().stream().mapToInt(Integer::intValue).sum() + stats.size() - 1 < size.getColumns()) {
            cstats = stats;
        } else {
            cstats = new ArrayList<>();
            int sz = 0;
            for (String stat : stats) {
                int nsz = sz;
                if (nsz > 0) {
                    nsz++;
                }
                nsz += widths.get(stat);
                if (nsz < size.getColumns()) {
                    sz = nsz;
                    cstats.add(stat);
                } else {
                    break;
                }
            }
        }
        // Headers
        for (String key : cstats) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            Column col = columns.get(key);
            align(sb, col.header, widths.get(key), col.align);
        }
        lines.add(sb.toAttributedString());
        sb.setLength(0);
        // Threads
        for (int i = 0; i < nb; i++) {
            Map<String, Comparable<?>> thread = threads.get(i);
            long tid = (Long) thread.get(STAT_TID);
            for (String key : cstats) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                long last;
                Object cur = thread.get(key);
                Object prv =
                        previous.computeIfAbsent(tid, id -> new HashMap<>()).put(key, cur);
                if (prv != null && !prv.equals(cur)) {
                    changes.computeIfAbsent(tid, id -> new HashMap<>()).put(key, now);
                    last = now;
                } else {
                    last = changes.computeIfAbsent(tid, id -> new HashMap<>()).getOrDefault(key, 0L);
                }
                long fade = delay * 24;
                if (now - last < fade) {
                    int r = (int) ((now - last) / (fade / 24));
                    sb.style(sb.style().foreground(255 - r).background(9));
                }
                align(sb, values.get(i).get(key), widths.get(key), columns.get(key).align);
                sb.style(sb.style().backgroundOff().foregroundOff());
            }
            lines.add(sb.toAttributedString());
            sb.setLength(0);
        }

        display.update(lines, 0);
    }

    private Comparator<Map<String, Comparable<?>>> buildComparator(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            sort = Collections.singletonList(STAT_TID);
        }
        Comparator<Map<String, Comparable<?>>> comparator = null;
        for (String key : sort) {
            String fkey;
            boolean asc;
            if (key.startsWith("+")) {
                fkey = key.substring(1);
                asc = true;
            } else if (key.startsWith("-")) {
                fkey = key.substring(1);
                asc = false;
            } else {
                fkey = key;
                asc = true;
            }
            if (!columns.containsKey(fkey)) {
                throw new IllegalArgumentException("Unsupported sort key: " + fkey);
            }
            @SuppressWarnings("unchecked")
            Comparator<Map<String, Comparable<?>>> comp = Comparator.comparing(m -> (Comparable) m.get(fkey));
            if (asc) {
                comp = comp.reversed();
            }
            if (comparator != null) {
                comparator = comparator.thenComparing(comp);
            } else {
                comparator = comp;
            }
        }
        return comparator;
    }

    private void register(String name, Align align, String header, Function<Object, String> format) {
        columns.put(name, new Column(name, align, header, format));
    }

    private static String nanos(long nanos) {
        return millis(nanos / 1_000_000L);
    }

    private static String millis(long millis) {
        long secs = millis / 1_000;
        millis = millis % 1000;
        long mins = secs / 60;
        secs = secs % 60;
        long hours = mins / 60;
        mins = mins % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d.%03d", hours, mins, secs, millis);
        } else if (mins > 0) {
            return String.format("%d:%02d.%03d", mins, secs, millis);
        } else {
            return String.format("%d.%03d", secs, millis);
        }
    }

    private static Function<Object, String> padcut(int nb) {
        return o -> padcut(o.toString(), nb);
    }

    private static String padcut(String str, int nb) {
        if (str.length() <= nb) {
            StringBuilder sb = new StringBuilder(nb);
            sb.append(str);
            while (sb.length() < nb) {
                sb.append(' ');
            }
            return sb.toString();
        } else {
            return str.substring(0, nb - 3) + "...";
        }
    }

    private static String memory(long cur, long max) {
        if (max > 0) {
            String smax = humanReadableByteCount(max, false);
            String cmax = humanReadableByteCount(cur, false);
            StringBuilder sb = new StringBuilder(smax.length() * 2 + 3);
            for (int i = cmax.length(); i < smax.length(); i++) {
                sb.append(' ');
            }
            sb.append(cmax).append(" / ").append(smax);
            return sb.toString();
        } else {
            return humanReadableByteCount(cur, false);
        }
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * This is for long running commands to be interrupted by ctrl-c
     */
    private void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void bindKeys(KeyMap<Operation> map) {
        map.bind(Operation.HELP, "h", "?");
        map.bind(Operation.EXIT, "q", ":q", "Q", ":Q", "ZZ");
        map.bind(Operation.INCREASE_DELAY, "+");
        map.bind(Operation.DECREASE_DELAY, "-");
        map.bind(Operation.CLEAR, KeyMap.ctrl('L'));
        map.bind(Operation.REVERSE, "r");
    }

    private static class Column {
        final String name;
        final Align align;
        final String header;
        final Function<Object, String> format;

        Column(String name, Align align, String header, Function<Object, String> format) {
            this.name = name;
            this.align = align;
            this.header = header;
            this.format = format;
        }
    }
}
