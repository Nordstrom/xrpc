#!/bin/bash -e

LINE_BREAK="======================================"
REQUEST_HEADERS="Content-Type"
REQUEST_METHOD="GET"


function exitError {

	echo "Error: $1" >&2
	exit 1
}

function usage {

	cat <<EOM
Usage: $(basename "$0") [OPTION]...

  -r URL       request URL
  -o DOMAIN    origin domain
  -h           display help
EOM

	exit 2
}

# read arguments
requestURL="https://localhost:8080/people"
originDomain="test.domain"

while getopts ":r:o:h" optKey; do
	case $optKey in
		r)
			requestURL=$OPTARG
			;;
		o)
			originDomain=$OPTARG
			;;
		h|*)
			usage
			;;
	esac
done

# validation of arguments
[[ -z $requestURL ]] && exitError "Missing request URL"
[[ -z $originDomain ]] && exitError "Missing origin domain"

# make OPTIONS call
curl \
  -k \
  --http1.1 \
	--dump-header - \
	--header "Access-Control-Request-Headers: $REQUEST_HEADERS" \
	--header "Access-Control-Request-Method: $REQUEST_METHOD" \
	--header "Origin: $originDomain" \
	--output /dev/null \
	--request OPTIONS \
	--silent \
	"$requestURL"

echo "======================================"
echo

# make GET call
curl \
  -k \
  --http1.1 \
	--dump-header - \
	--header "Origin: $originDomain" \
	--request $REQUEST_METHOD \
	--silent \
	"$requestURL"
