#!/bin/sh

openssl req -x509 -nodes -sha256 -days 365 -newkey rsa:2048 -keyout xjeffrose.com.key -out xjeffrose.com.crt