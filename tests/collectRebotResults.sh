#!/bin/bash
#set -u

base=$(pwd)
dirResults="$base/results"
dirReport="$base/report"


############################################################
# Ensures we have a clean report directory                 #
############################################################

mkdir -p ${dirReport}
rm -Rf ${dirReport}/*


############################################################
# Run rebot                                                #
############################################################

echo "------------------------------------------------------"
echo "Collecting Robot Test-Results:"
echo "  - $(ls -m ${dirResults} | sed -e $'s/, /\\\n  - /g')"
echo "------------------------------------------------------"

# run rebot
rebot \
    --name EHRbase \
    --outputdir ${dirReport} \
    --log Final_Log.html \
    --report Final_Report.html \
    --output output.xml \
    ${dirResults}/*/output.xml

# allow to read/write the result for everyone
chmod -R ugo+rw  ${dirReport}

exit 0
