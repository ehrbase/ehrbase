#!/bin/bash

# store arguments in a special array
args=("$@")
# get number of elements
ELEMENTS=${#args[@]}

cd tests

# clean old report
rm -Rf ./results/${suite}

# echo each element in array
# for loop
for (( i=0;i<$ELEMENTS;i++)); do
    suite=${args[${i}]}
    echo "Running test suite [${suite}]"

    robot -v nodocker \
          -v NODENAME:${SERVER_NODENAME} \
          -v BASEURL:${EHRBASE_BASE_URL}/ehrbase/rest/openehr/v1 \
          --dotted \
          --console quiet \
          --flattenkeywords for \
          --flattenkeywords foritem \
          --flattenkeywords name:_resources.* \
          --report NONE \
          --outputdir results/${suite} \
          --skiponfailure not-ready -L TRACE \
          robot/${suite}
done
