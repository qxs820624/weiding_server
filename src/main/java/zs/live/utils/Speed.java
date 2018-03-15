package zs.live.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Speed implements Filter {
    private static Logger access = LoggerFactory.getLogger("speed-access");
    private static Logger causes = LoggerFactory.getLogger("speed-causes");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static boolean enable = true;

    private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private ConcurrentHashMap<Long, Status> monitoring;
    public long monitorInterval = 5, slowRequest = 150, startTime = 0;
    private LinkedBlockingQueue<Status> logAccessQueue, logCausesQueue;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        long ss = System.currentTimeMillis();
        ((HttpServletResponse)servletResponse).addHeader("BgnTime", String.valueOf(ss));
        if (!enable || ss < startTime) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            long id = Thread.currentThread().getId();
            try {
                monitoring.putIfAbsent(id, new Status());
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                Status status = monitoring.remove(id);
                if (status != null) {
                    long tm = System.currentTimeMillis() - ss;
                    HttpServletRequest req = (HttpServletRequest) servletRequest;
                    status.requestStart = ss;
                    status.requestTime = tm;
                    status.requestURI = req.getRequestURI();
                    status.requestQuery = req.getQueryString();
                    logAccessQueue.offer(status);
                    if (tm > slowRequest) logCausesQueue.offer(status);
                }
            }
        }
    }

    private Thread startThread(ThreadGroup group, boolean daemon, Runnable runnable) {
        Thread thread = new Thread(group, runnable);
        if (daemon) thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 启动30秒以后才开始抓取。刚启动的时候，一些class loading的东西抓出来没意义
        startTime = System.currentTimeMillis() + 30000;
        monitoring = new ConcurrentHashMap<Long, Status>();
        logAccessQueue = new LinkedBlockingQueue<Status>();
        logCausesQueue = new LinkedBlockingQueue<Status>();

        causes.info("[{}] start to find slow method call", sdf.format(startTime));

        ThreadGroup group = new ThreadGroup("speed-mon");
        startThread(group, true, new Runnable() {
            @Override
            public void run() {
                while (true)
                    try {
                        LinkedBlockingQueue<Status> q = logAccessQueue;
                        if (q == null) break;
                        Status status = q.poll(5, TimeUnit.SECONDS);
                        if (status == null) continue;
                        access.info(
                                "[{}] {}ms, uri={}, query={}",
                                sdf.format(status.requestStart),
                                status.requestTime,
                                status.requestURI,
                                status.requestQuery);
                    } catch (Throwable t) {
                        causes.error("log-access", t);
                    }
            }
        });
        startThread(group, true, new Runnable() {
            @Override
            public void run() {
                while (true)
                    try {
                        LinkedBlockingQueue<Status> q = logCausesQueue;
                        if (q == null) break;
                        Status status = q.poll(5, TimeUnit.SECONDS);
                        if (status == null
                                || status.maxCallDepth < 1
                                || status.infos == null
                                || status.infos.size() < 1)
                            continue;

                        CaptureInfo info = status.infos.get(0);
                        if (info == null) continue;
                        int startDepth = 0;
                        while (startDepth < status.maxCallDepth
                                && info.getAt(startDepth) != null
                                && !Speed.class.getName().equals(info.getAt(startDepth).getClassName()))
                            startDepth++;

                        Tracker tracker = new Tracker();
                        for (int depth = startDepth; depth < status.maxCallDepth; depth++) {
                            tracker.setDepth(depth);
                            for (CaptureInfo i : status.infos)
                                tracker.track(i);
                        }

                        causes.info(
                                "[{}] {}ms, uri={}, query={}, collected={}\n{}\n",
                                sdf.format(status.requestStart),
                                status.requestTime,
                                status.requestURI,
                                status.requestQuery,
                                status.infos.size(),
                                tracker.out.toString());
                    } catch (Throwable t) {
                        causes.error("log-causes", t);
                    }
            }
        });
        startThread(group, true, new Runnable() {
            private long mostlyBlockTime = -1;

            @Override
            public void run() {
                while (true)
                    try {
                        Thread.sleep(monitorInterval);

                        ConcurrentHashMap<Long, Status> m = monitoring;
                        if (m == null) break;
                        int sz = m.size(), blockCount = 0;
                        if (sz < 1) continue;

                        long[] ids = new long[sz];
                        Iterator<Long> it = m.keySet().iterator();
                        for (int i = 0; i < ids.length; i++) {
                            ids[i] = it.hasNext() ? it.next() : 1;
                        }
                        ThreadInfo[] infos = threadMXBean.getThreadInfo(ids, Integer.MAX_VALUE);
                        long now = System.currentTimeMillis();
                        if (infos != null && infos.length > 0) {
                            for (ThreadInfo info : infos) {
                                if (info == null) continue;
                                // 检查是否 block
                                if (Thread.State.BLOCKED == info.getThreadState()) {
                                    blockCount++;
                                }
                                // 当前运行状态
                                long id = info.getThreadId();
                                Status status = m.get(id);
                                if (status != null) {
                                    CaptureInfo captureInfo = new CaptureInfo();
                                    captureInfo.time = now;
                                    captureInfo.threadInfo = info;
                                    captureInfo.stackTraceElements = info.getStackTrace();
                                    status.addInfo(captureInfo);
                                }
                            }
                        }
                        if (sz > 50)
                            causes.error("monitor total={}, blocked={}", sz, blockCount);
                        if (sz > 200 && blockCount > 100) {
                            // 有足够的服务线程，但是大多被长时间阻塞，应当重启
                            causes.error("exit for total={}, blocked={}", sz, blockCount);
                            System.exit(1);
                        }
                        if (blockCount > 50 && 100.0 * blockCount / sz > 90) {
                            if (mostlyBlockTime < 0)
                                mostlyBlockTime = now;
                        } else {
                            mostlyBlockTime = -1;
                        }
                        if (mostlyBlockTime > 0 &&
                                now - mostlyBlockTime > TimeUnit.MINUTES.toMillis(10)) {
                            causes.error("exit for total={}, blocked={}, exceed 90% since {}",
                                    sz, blockCount, sdf.format(mostlyBlockTime));
                            System.exit(2);
                        }
                    } catch (Throwable t) {
                        causes.error("log-causes", t);
                    }
            }
        });
    }

    @Override
    public void destroy() {
        monitoring = null;
        logAccessQueue = null;
        logCausesQueue = null;
    }

    private static class Status {
        long requestStart;
        long requestTime;
        String requestURI;
        String requestQuery;

        ArrayList<CaptureInfo> infos;
        int maxCallDepth = -1;

        void addInfo(CaptureInfo info) {
            if (infos == null) infos = new ArrayList<CaptureInfo>();

            info.index = infos.size();
            infos.add(info);
            maxCallDepth = Math.max(maxCallDepth, info.stackTraceElements.length);
        }
    }

    private static class CaptureInfo {
        int index;
        long time;
        ThreadInfo threadInfo;
        StackTraceElement[] stackTraceElements;

        StackTraceElement getAt(int n) {
            if (stackTraceElements == null || n < 0 || n >= stackTraceElements.length) return null;
            return stackTraceElements[stackTraceElements.length - 1 - n];
        }
    }

    private static class Tracker {
        StringBuilder out = new StringBuilder();
        private int depth = -1;
        private CaptureInfo s, e;
        private StackTraceElement tracking;
        private static String[] ignorePrefix = {
                "org.codehaus.groovy.",
                "groovy.lang.",
                "groovy.util.",
                "org.springframework.",
                "org.apache.catalina.",
                "java.lang.",
                "sun."};

        private static boolean ignore(StackTraceElement element) {
            if (element == null) return true;
            if ((element.getFileName() == null || element.getLineNumber() < 0)
                    && !element.isNativeMethod()) return true;

            String className = element.getClassName();
            if (className == null) return true;
            for (String pfx : ignorePrefix)
                if (className.startsWith(pfx))
                    return true;
            return false;
        }

        void end() {
            if (s != null && e != null && tracking != null) {
                long tm = e.time - s.time;
                if (tm > 20 && !ignore(tracking)) out
                        .append(String.format("%8dms, ", tm))
                        .append(String.format("%4d:[%d,%d], ", depth, s.index, e.index))
                        .append(tracking).append("\n");
            }
            s = null;
            e = null;
            tracking = null;
        }

        void setDepth(int depth) {
            if (this.depth != depth) end();
            this.depth = depth;
        }

        void track(CaptureInfo info) {
            StackTraceElement element = info.getAt(depth);
            if (element == null) {
                end();
            } else if (element.equals(tracking)) {
                this.e = info;
            } else {
                end();
                s = info;
                e = info;
                tracking = element;
            }
        }
    }
}
