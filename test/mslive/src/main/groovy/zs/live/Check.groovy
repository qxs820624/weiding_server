package zs.live

import org.codehaus.groovy.runtime.StackTraceUtils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class Check {
    public List<CheckResult> check(Map<String, Closure> codes, int timeoutSeconds) {
        if (!checklock.isLocked()) updateCheckResult(codes)
        if (codes?.size() > 0 && timeoutSeconds > 0) await(timeoutSeconds)
        return lastCheckResult
    }

    public List<CheckResult> await(int seconds) {
        cdl.await(seconds, TimeUnit.SECONDS)
        return lastCheckResult
    }

    private void updateCheckResult(Map<String, Closure> codes) {
        if (codes == null || codes.size() < 1 || checklock.isLocked()) return
        Thread checker = new Thread(new Runnable() {
            @Override
            void run() {
                def ret = []
                if (checklock.tryLock())
                    try {
                        //noinspection GroovyAssignabilityCheck
                        codes?.each { String resource, Closure closure ->
                            CheckResult result = new CheckResult()
                            result.resource = resource
                            try {
                                closure.call()
                                result.hasError = false
                            } catch (e) {
                                result.hasError = true
                                StackTraceUtils.deepSanitize(e)
                                result.exception = e
                            }
                            ret.add(result)
                        }
                    } finally {
                        if (ret.size() > 0) lastCheckResult = ret
                        cdl.countDown()
                        cdl = new CountDownLatch(1)
                        checklock.unlock()
                    }
            }
        })
        checker.setDaemon(true)
        checker.start()
    }

    private transient CountDownLatch cdl = new CountDownLatch(1)
    private ReentrantLock checklock = new ReentrantLock()
    private List<CheckResult> lastCheckResult = []
    private static ConcurrentHashMap<String, Check> checks = new ConcurrentHashMap<String, Check>()

    public static Check get(String name) {
        Check check = checks.get(name)
        if (check == null) {
            check = new Check()
            Check oldCheck = checks.putIfAbsent(name, check)
            if (oldCheck != null) check = oldCheck
        }
        return check
    }
}

class CheckResult {
    boolean hasError
    String resource, message
    Throwable exception

    @Override
    public String toString() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        pw.print('resource=')
        pw.print(resource)
        pw.print(', hasError=')
        pw.print(hasError)
        if (hasError) {
            if (message) {
                pw.print(', message=')
                pw.print(message)
            }
            pw.println('')
            exception.printStackTrace(pw)
        }
        pw.flush()
        pw.close()
        return sw.toString()
    }
}
