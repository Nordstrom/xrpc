#!/usr/bin/env bash

xrpc_pid=$$
echo $xrpc_pid

path_separator=':'
is_windows=`uname -o | grep -ic cygwin`
script_full_path=$(readlink -f "$0")
if [ $is_windows == 1 ]; then
        path_separator=';'
        script_full_path=`cygpath -wm $script_full_path`
fi
base_dir=$(dirname $script_full_path)
pid_file=/var/run/xrpc.pid

INPUT_CP=
INPUT_CONF=${base_dir}/application.conf

help() {
clear
cat << EOF

    Usage:
    start.sh [Options] [Conf file (default: $INPUT_CONF)]

    ----------------------------------------------------------------
    Options:

    --cp
      Class Path ($INPUT_CP)

    --help
      Display this help and exit.

    --occ
      OCC handling (possible values: none (default), passthru, or bidirectional)

    --env
      ENV for running xrpc. For e.g. msmaster1int or msmaster2int

    --vip
      VIP per pool feature is enabled by default unless this flag is set to "false"

    --zookeeper
      Zookeeper endpoints. If not specified, will be picked up from application.conf

EOF
}

PROP_OCC_HANDLING=
PROP_ENV=
PROP_ZOOKEEPER=
SHOW_HELP=
PROP_VIP=

OPTS=`getopt -o h --long cp:,help,occ:,env:,vip:,zookeeper: -- "$@"`
if [ \( $? != 0 \) -a \( "$1" != "--ignore-unknown-opt" \) ]; then
  echo "Use -h option to display usage"
  exit 1
fi
eval set -- "$OPTS"

while true
do
  case "$1" in
    --cp) INPUT_CP="$2"; shift 2;;
    --occ) PROP_OCC_HANDLING="-DOCC_HANDLING=$2"; shift 2;;
    --env) PROP_ENV="-DENV=$2"; shift 2;;
    --zookeeper) PROP_ZOOKEEPER="-DZOOKEEPER=$2"; shift 2;;
    --vip) PROP_VIP="-DVIP=$2"; shift 2;;
    -h|--help) SHOW_HELP=1; shift 1;;

    --) shift; break;;
    *) echo "internal error on option: \`$1'!" ; \
       echo "terminating xrpc..." ; exit 1 ;;
  esac
done

if [ "$1" != '' ]; then
  INPUT_CONF=$1
fi

if [ "$INPUT_CP" == '' ] && [ "$SHOW_HELP" == '' ]; then
  all_jars=( `cd $base_dir; ls -t *.jar 2>/dev/null` )
  if [ "$all_jars" == '' ]; then
    echo "Error: No jar found in $base_dir!!!"
    echo "Please provide --cp option (-h for help)."
    exit 1
  fi
  for one_jar in "${all_jars[@]}"
  do
    if [ "$INPUT_CP" != "" ]; then
      INPUT_CP=${INPUT_CP}${path_separator}
    fi
    INPUT_CP=${INPUT_CP}${base_dir}/${one_jar}
  done
fi

if [ "$SHOW_HELP" != '' ]; then
  help
  exit 1
fi

java_path=java
if [ "$JAVA_HOME" != '' ]; then
  java_path=$JAVA_HOME/bin/java
fi
java_version=$($java_path -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '/\./ {print $1"."$2}')
if [[ "$java_version" < "1.8" ]]; then
  echo "xrpc requires Java version 1.8 or later!!!"
fi

LOG_DIR="/var/log/xrpc"
if [ ! -d "$LOG_DIR" ]; then
  `mkdir -p "$LOG_DIR"; chmod 666 "$LOG_DIR"`
fi

echo $xrpc_pid > $pid_file
$java_path  -ea          \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -Dio.netty.allocator.type=pooled \
  -XX:+UseStringDeduplication     \
  -XX:+UseTLAB                    \
  -XX:+AggressiveOpts             \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:ReservedCodeCacheSize=128m  \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -XX:MaxDirectMemorySize=8G      \
  -Xss8M                          \
  -Xms512M                        \
  -Xmx4G                          \
  -server                         \
  $PROP_OCC_HANDLING \
  $PROP_ENV          \
  $PROP_ZOOKEEPER    \
  $PROP_VIP          \
  -Dcom.sun.management.jmxremote                    \
  -Dcom.sun.management.jmxremote.port=9010          \
  -Dcom.sun.management.jmxremote.local.only=false   \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false          \
  -Dserver.port=$PORT                               \
  -Dconfig.file=application.conf                    \
  -jar app.jar

rm $pid_file
