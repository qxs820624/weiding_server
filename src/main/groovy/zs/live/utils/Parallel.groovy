package zs.live.utils

import org.codehaus.groovy.runtime.StackTraceUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.*

class Parallel {
    public static void run(List list, Closure closure, long timeout) {
        if (closure && list?.size() > 0) {
//            CountDownLatch countDownLatch = new CountDownLatch(list.size())
//            list.each {
//                executorService.submit(c(closure, it, countDownLatch)) }
//            countDownLatch.await(timeout, TimeUnit.MILLISECONDS)
            tfSubmit(closure,list)
        }
    }

    public static void runWait(List list, Closure closure, long timeout) {
        if (closure && list?.size() > 0) {
            CountDownLatch countDownLatch = new CountDownLatch(list.size())
            countDownLatch.await(timeout, TimeUnit.MILLISECONDS)
            list.each {
                executorService.submit(c(closure, it, countDownLatch)) }
        }
    }

    public static void run(int count, List list, Closure closure) {
        if (closure && list?.size() > 0 && count > 0) {
            List nl = []
            (1..list.size()).each { int i ->
                nl.add(list[i - 1])
                if (nl.size() == count) {
                    executorService.invokeAll(nl.collect { c(closure, it) })
                    nl.clear()
                }
            }
            if (nl.size() > 0)
                executorService.invokeAll(nl.collect { c(closure, it) })
        }
    }

    public static void submit(Closure closure, Object arg) {
       executorService.submit(c(closure, arg))
    }

    public static void tfSubmit(Closure closure, Object arg) {
        tfexecutorService.submit(c(closure, arg))
    }

    private static Callable c(Closure closure, Object arg, CountDownLatch countDownLatch) {
        return new Callable() {
            @Override
            Object call() {
                Object r = null
                try {
                    r = closure?.call(arg)
                } catch (e) {
                    log.error("background task: args=${arg}", StackTraceUtils.deepSanitize(e))
                } finally {
                    countDownLatch.countDown()
                }
                return r
            }
        }
    }

    private static Callable c(Closure closure, Object arg) {
        return new Callable() {
            @Override
            Object call() {
                try {
                    return closure?.call(arg)
                } catch (e) {
                    log.error("background task: args=${arg}", StackTraceUtils.deepSanitize(e))
                }
                return null
            }
        }
    }

    private static Logger log = LoggerFactory.getLogger(Parallel)
    private static ThreadFactory tf = Executors.defaultThreadFactory()
    private static ThreadFactory dtf = new ThreadFactory() {
        @Override
        Thread newThread(Runnable r) {
            Thread t = tf.newThread(r)
            t.setDaemon(true)
            return t
        }
    }
    private static ExecutorService executorService = new ThreadPoolExecutor(
            10, 500,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            dtf);

    static {
        addShutdownHook { executorService.shutdownNow() }
    }

    private static ExecutorService tfexecutorService = new ThreadPoolExecutor(
            10, 500,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            tf);
    static {
        addShutdownHook { tfexecutorService.shutdownNow() }
    }

//    public static void main(String[] args){
//        String str = 1
//        for (int i = 0;i < 520 ; i++){
//            Parallel.submit({println("----------${it}------------"+str)},i)
//        }
//    }
}
