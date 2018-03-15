#!/usr/bin/env bash
# variables:
#   MEMS     -> java -Xms$$G
#   MEMX     -> java -Xmx$$G
#   profiles -> java -Dspring.profiles.active
#   SYENV    -> java -Dcfg.url=file:/path/to/souyue.${SYENV}.properties
#   service  -> ${service}-${version:-3.0.0}.jar

[ -f env ] && source ./env

[ -n "$DEBUG" ] && set -x
[ "$UID" == 0 ] && exit 1

[ -z "$LC_ALL" ] && export LC_ALL=zh_CN.utf8
SY_LOG_DIR=${LOG_DIR:-../logs}
SY_TMP_DIR=${TMP_DIR:-../tmp}
mkdir -p "$SY_LOG_DIR" "$SY_TMP_DIR"

JOPTS=(-server)
#-Xms16G -Xmx16G
[ -n "$MEMS" ] && JOPTS+=("-Xms${MEMS}G")
[ -n "$MEMX" ] && JOPTS+=("-Xmx${MEMX}G")
# jdk 1.8 g1gc
# JOPTS+=(-XX:+UseG1GC)
# jdk 1.6 cms
JOPTS+=(-XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+CMSScavengeBeforeRemark -XX:+DisableExplicitGC)
JOPTS+=(-verbose:gc -XX:+PrintGCDetails -Xloggc:"${SY_LOG_DIR}/gc.log")
JOPTS+=(-XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime)
JOPTS+=(-XX:+AggressiveOpts -XX:+OptimizeStringConcat)
JOPTS+=(-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom)
JOPTS+=(-Djava.io.tmpdir="$SY_TMP_DIR" -Dlogging.path="$SY_LOG_DIR")
[ -d lib ] && JOPTS+=(-Djava.library.path=./lib)
[ -n "$profiles" ] && JOPTS+=(-Dspring.profiles.active="$profiles")

FN=$(ls -1 *.jar | tail -1)
exec java $JAVA_OPTS ${JOPTS[@]} -jar "$FN" "$@"
