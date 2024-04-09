#!/bin/bash
set -u

base=$(pwd)
dirResults="$base/results"


############################################################
# Help                                                     #
############################################################
showHelp()
{
   # Display Help
   echo "Run a given robot integration tests suite."
   echo
   echo "Syntax:  runRobotTests [h|-n|-p|-t|-s]"
   echo
   echo "Example: runRobotTests --name SANITY --path SANITY_TESTS --tags Sanity -t TEST"
   echo
   echo "options:"
   echo "n|name   Name of the suite also used as result sub directory."
   echo "p|path   Path of the suite to run."
   echo "t|tags   Include tags."
   echo "s|suite  SUT param defaults to 'TEST'."
   echo
}

name=0
path=0
tags=0
suite='TEST'
POSITIONAL_ARGS=()

############################################################
# parse command line args                                  #
############################################################

while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      showHelp
      exit
      ;;
    -n|--name)
      name="$2"
      shift # past argument
      shift # past value
      ;;
    -p|--path)
      path="$2"
      shift # past argument
      shift # past value
      ;;
    -t|--tags)
      tags="$2"
      shift # past argument
      shift # past value
      ;;
    -s|--suite)
      suite="$2"
      shift # past argument
      shift # past value
      ;;
    *)
      echo "Error: Invalid option [$1]"
      exit -1;;
  esac
done


############################################################
# Checks given parameters                                  #
############################################################

if [ $name == 0 ]; then
    echo "Option [name] not specified"
    exit -1
fi
if [ $path == 0 ]; then
    echo "Option [path] not specified"
    exit -1
fi
if [ $path == 0 ]; then
    echo "Option [path] not specified"
    exit -1
fi


############################################################
# Ensures we are in a clean result directory               #
############################################################

rm -Rf ${dirResults}/${name}


############################################################
# Run tests                                                #
############################################################

echo "---------------------------------------------------------------------------------------"
echo "Running Robot Test-Suite [name: ${name}, path: ${path}, tags: ${tags}, suite: ${suite}]"
echo "---------------------------------------------------------------------------------------"

cd tests
robot --include ${tags} \
      --skip TODO \
      --skip future \
      --loglevel INFO \
      -e ADMIN \
      -e SECURITY \
      --dotted \
      --console quiet \
      --skiponfailure not-ready -L TRACE \
      --flattenkeywords for \
      --flattenkeywords foritem \
      --flattenkeywords name:_resources.* \
      --flattenkeywords "name:composition_keywords.Load Json File With Composition" \
      --flattenkeywords "name:template_opt1.4_keywords.upload OPT file" \
      --removekeywords "name:JSONLibrary.Load Json From File" \
      --removekeywords "name:Change Json KeyValue and Save Back To File" \
      --removekeywords "name:JSONLibrary.Update Value To Json" \
      --removekeywords "name:JSONLibrary.Convert JSON To String" \
      --removekeywords "name:JSONLibrary.Get Value From Json" \
      --report NONE \
      --name ${name} \
      --outputdir ${dirResults}/${name} \
      -v SUT:${suite} \
      -v nodocker \
      -v NODENAME:${SERVER_NODENAME} \
      -v BASEURL:${EHRBASE_BASE_URL}/ehrbase/rest/openehr/v1 \
      robot/${path}

# --timestampoutputs \

#      -e obsolete \
#      -e libtest \
#      -v ALLOW-TEMPLATE-OVERWRITE:false \
#      robot/${suite[testSuitePath]}
