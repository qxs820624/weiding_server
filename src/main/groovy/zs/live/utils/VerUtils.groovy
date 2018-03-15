package zs.live.utils

import java.util.regex.Pattern

class VerUtils {
    static def PATTERN_VER_1 = Pattern.compile('^(\\d+).(\\d+)$')
    static def PATTERN_VER_2 = Pattern.compile('^(\\d+).(\\d+).(\\d+)$')
    static def PATTERN_VER_3 = Pattern.compile('^(\\d+).(\\d+).(\\d+).(\\d+)$')

    static int toIntVer(String ver) {
        def v1 = 0
        def v2 = 0
        def v3 = 0
        def v4 = 0
        if (ver != null) {
            ver ='4.2.2.1'.equals(ver)?'4.2.2':ver
            def finder = PATTERN_VER_1.matcher(ver)
            if (finder.find()) {
                v1 = finder.group(1).toInteger()
                v2 = finder.group(2).toInteger()
            } else {
                finder = PATTERN_VER_2.matcher(ver)
                if (finder.find()) {
                    v1 = finder.group(1).toInteger()
                    v2 = finder.group(2).toInteger()
                    v3 = finder.group(3).toInteger()
                    if(v3>9){
                        v3 = (v3+"").substring(0,1);
                        v3 = Integer.parseInt(v3)
                    }
                }
            }
        }
        return v1 * 1000000 + v2 * 10000 + v3 * 100 + v4
    }
}
