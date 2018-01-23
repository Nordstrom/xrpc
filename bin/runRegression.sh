#!/usr/bin/env bash

set -e

if ! [ -x "$(command -v wrk)" ]; then
  echo 'Error: wrk is not installed. Please install wrk to run regressions' >&2
  exit 1
fi

wrk -c 15 -d 60 -t 4 https://localhost:8080/people --latency
