#!/bin/bash

# Define constants
RUNNING_CODE='200'
CURL_TIMEOUT=10
CONTAINER_NAME='kason-service_kason-crawler_1'
SERVICE_URL='http://kason-crawler:6001/actuator/info'
LONG_SLEEP=60
SHORT_SLEEP=15

# Log function to standardize output
log_message() {
    echo "$(date "+%Y-%m-%d %H:%M:%S") $1"
}

while true
do
    # Get the HTTP status code with timeout protection
    crawler_code=$(curl -I -m ${CURL_TIMEOUT} -o /dev/null -s -w %{http_code} ${SERVICE_URL})

    # Check if the service needs to be restarted
    if [ "$crawler_code" != "$RUNNING_CODE" ]; then
        log_message "Word-Crawler Service is down (code: ${crawler_code}), restarting..."

        # Get the container ID and restart it
        container_id=$(docker ps -a | grep ${CONTAINER_NAME} | awk '{print $1}')

        # Check if we found a container ID
        if [ -n "$container_id" ]; then
            docker container start "$container_id"
            log_message "Word-Crawler Service has been restarted, sleeping for ${LONG_SLEEP}s"
        else
            log_message "ERROR: Could not find container ${CONTAINER_NAME}"
        fi

        sleep ${LONG_SLEEP}
    else
        log_message "Word-Crawler Service is running, code = ${crawler_code}"
        sleep ${SHORT_SLEEP}
    fi
done