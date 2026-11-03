#!/bin/bash
#
# Swagger Monitor
#

APP_HOME=$(cd "$(dirname "$0")" && pwd)

JAR_NAME=swagger-monitor-0.0.1-SNAPSHOT.jar

PID_FILE=$APP_HOME/swagger-monitor.pid

#
# Ajustar según la instalación realizada por Infraestructura
#
export JAVA_HOME=/opt/java/jdk-11
export BM_HOME=/opt/BM_HOME

start_app() {

    echo " ---> Iniciando Swagger Monitor"

    if [ -f "$PID_FILE" ]; then

        PID=$(cat "$PID_FILE")

        if ps -p "$PID" > /dev/null 2>&1; then
            echo " ---> Swagger Monitor ya se encuentra ejecutándose. PID=$PID"
            exit 0
        fi
    fi

    nohup $JAVA_HOME/bin/java \
        -DBM_HOME=$BM_HOME \
        -jar $APP_HOME/$JAR_NAME \
        > $BM_HOME/appl/swagger-monitor/logs/swagger-monitor.log 2>&1 &

    echo $! > "$PID_FILE"

    echo " ---> Swagger Monitor iniciado."
    echo " ---> PID: $(cat "$PID_FILE")"
}

stop_app() {

    echo " ---> Deteniendo Swagger Monitor"

    if [ ! -f "$PID_FILE" ]; then
        echo " ---> No existe archivo PID."
        exit 0
    fi

    PID=$(cat "$PID_FILE")

    if ps -p "$PID" > /dev/null 2>&1; then

        kill "$PID"

        echo " ---> Proceso detenido. PID=$PID"

    else

        echo " ---> El proceso no se encuentra activo."

    fi

    rm -f "$PID_FILE"
}

status_app() {

    if [ -f "$PID_FILE" ]; then

        PID=$(cat "$PID_FILE")

        if ps -p "$PID" > /dev/null 2>&1; then

            echo "Swagger Monitor ACTIVO"
            echo "PID: $PID"
            exit 0
        fi
    fi

    echo "Swagger Monitor DETENIDO"
}

usage() {

    echo "Uso:"
    echo "./swagger-monitor.sh start"
    echo "./swagger-monitor.sh stop"
    echo "./swagger-monitor.sh status"
}

COMMAND=$1

case "$COMMAND" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    status)
        status_app
        ;;
    *)
        usage
        exit 1
        ;;
esac

exit 0