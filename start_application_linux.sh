#!/bin/bash

# Path to play install folder
PLAY_HOME=./play-1.2.4

# Path to the application
APPLICATION_PATH=./AgentMaster

case "$1" in
    start)
        echo -en "Starting Superman service... \n"
        rm -f ${APPLICATION_PATH}/server.pid
        python $PLAY_HOME/play start ${APPLICATION_PATH} >/dev/null
        sleep 3
        echo -en "Superman started! \n"
        ;;

    stop)
        echo -en "Shutting down Superman service... \n"
        python $PLAY_HOME/play stop ${APPLICATION_PATH} > /dev/null
        sleep 3
        echo -en "Superman stopped! \n"
        ;;
    status)
        python $PLAY_HOME/play status ${APPLICATION_PATH}
        ;;
    *)
        echo "Usage: start_application_linux.sh {start|stop|status}" >&2
        exit 1
        ;;
esac
exit 0

