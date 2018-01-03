#!/usr/bin/env bash

set -e

wrk -c 15 -d 60 -t 4 https://localhost:8080/people --latency
