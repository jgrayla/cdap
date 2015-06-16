#!/usr/bin/env bash

# Copyright © 2014-2015 Cask Data, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
  
# Build script for docs
# Builds the docs (all except javadocs and PDFs) from the .rst source files using Sphinx
# Builds the javadocs and copies them into place
# Zips everything up so it can be staged
# REST PDF is built as a separate target and checked in, as it is only used in SDK and not website
# Target for building the SDK
# Targets for both a limited and complete set of javadocs
# Targets not included in usage are intended for internal usage by script

API="cdap-api"
APIDOCS="apidocs"
APIS="apis"
BUILD="build"
BUILD_PDF="build-pdf"
CDAP_DOCS="cdap-docs"
HTML="html"
INCLUDES="_includes"
JAVADOCS="javadocs"
LICENSES="licenses"
LICENSES_PDF="licenses-pdf"
PROJECT="cdap"
PROJECT_CAPS="CDAP"
REFERENCE="reference-manual"
SOURCE="source"
SPHINX_MESSAGES="warnings.txt"

FALSE="false"
TRUE="true"

# Redirect placed in top to redirect to 'en' directory
REDIRECT_EN_HTML=`cat <<EOF
<!DOCTYPE HTML>
<html lang="en-US">
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0;url=en/index.html">
        <script type="text/javascript">
            window.location.href = "en/index.html"
        </script>
        <title></title>
    </head>
    <body>
    </body>
</html>
EOF`

SCRIPT=`basename ${0}`
SCRIPT_PATH=`pwd`
MANUAL=`basename ${SCRIPT_PATH}`

DOC_GEN_PY="${SCRIPT_PATH}/../tools/doc-gen.py"
BUILD_PATH="${SCRIPT_PATH}/${BUILD}"
HTML_PATH="${BUILD_PATH}/${HTML}"
SOURCE_PATH="${SCRIPT_PATH}/${SOURCE}"

if [ "x${2}" == "x" ]; then
  PROJECT_PATH="${SCRIPT_PATH}/../../"
else
  PROJECT_PATH="${SCRIPT_PATH}/../../../${2}"
fi

SDK_JAVADOCS="${PROJECT_PATH}/target/site/${APIDOCS}"

CHECK_INCLUDES="false"
TEST_INCLUDES_LOCAL="local"
TEST_INCLUDES_REMOTE="remote"
if [ "x${3}" == "x" ]; then
  TEST_INCLUDES="${TEST_INCLUDES_REMOTE}"
else
  TEST_INCLUDES="${3}"
fi

RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'
WARNING="${RED}${BOLD}WARNING:${NC}"

function echo_red_bold() {
  echo -e "${RED}${BOLD}${1}${NC}${2}"
}

function echo_clean_colors() {
  local c="${1//${RED}/}"
  c="${c//${BOLD}/}"
  c="${c//${NC}/}"
  echo -e "${c}"
}

# Hash of file with "Not Found"; returned by GitHub
NOT_FOUND_HASH="9d1ead73e678fa2f51a70a933b0bf017"

ZIP_FILE_NAME=$HTML
ZIP="${ZIP_FILE_NAME}.zip"

# Set Google Analytics Codes

# Corporate Docs Code
GOOGLE_ANALYTICS_WEB="UA-55077523-3"
WEB="web"

# CDAP Project Code
GOOGLE_ANALYTICS_GITHUB="UA-55081520-2"
GITHUB="github"


function usage() {
  cd $PROJECT_PATH
  PROJECT_PATH=`pwd`
  echo "Build script for '${PROJECT_CAPS}' docs"
  echo "Usage: ${SCRIPT} < option > [source test_includes]"
  echo ""
  echo "  Options (select one)"
  echo "    build          Clean build of javadocs and HTML docs, copy javadocs and PDFs into place, zip results"
  echo "    build-github   Clean build and zip for placing on GitHub"
  echo "    build-web      Clean build and zip for placing on docs.cask.co webserver"
  echo ""
  echo "    docs           Clean build of docs"
  echo "    javadocs       Clean build of javadocs ($API module only) for SDK and website"
  echo "    javadocs-full  Clean build of javadocs for all modules"
  echo "    license-pdfs   Clean build of License Dependency PDFs"
  echo ""
  echo "    check-includes Check if included files have changed from source"
  echo "    depends        Build Site listing dependencies"
  echo "    sdk            Build SDK"
  echo "  with"
  echo "    source         Path to $PROJECT source for javadocs, if not $PROJECT_PATH"
  echo "    test_includes  local, remote or neither (default: remote); must specify source if used"
  echo " "
}

function clean() {
  cd ${SCRIPT_PATH}
  rm -rf ${SCRIPT_PATH}/${BUILD}
  mkdir -p ${SCRIPT_PATH}/${BUILD}
}

function build_docs() {
  clean
  cd ${SCRIPT_PATH}
  check_includes
  sphinx-build -w ${BUILD}/${SPHINX_MESSAGES} -b html -d ${BUILD}/doctrees source ${BUILD}/html
  display_any_messages
}

function build_docs_google() {
  clean
  cd ${SCRIPT_PATH}
  check_includes
  sphinx-build -w ${BUILD}/${SPHINX_MESSAGES} -D googleanalytics_id=$1 -D googleanalytics_enabled=1 -b html -d ${BUILD}/doctrees source ${BUILD}/html
  display_any_messages
}

function build_javadocs_full() {
  cd ${PROJECT_PATH}
  set_mvn_environment
  echo "Currently not implemented"
  return
  # MAVEN_OPTS="-Xmx512m" mvn clean site -DskipTests
}

function build_javadocs_api() {
  cd ${PROJECT_PATH}
  set_mvn_environment
  MAVEN_OPTS="-Xmx1024m" mvn clean install -P examples,templates,release -DskipTests -Dgpg.skip=true && mvn clean site -DskipTests -P templates -DisOffline=false
}

function build_javadocs_sdk() {
  build_javadocs_api
  copy_javadocs_sdk
}

function copy_javadocs_sdk() {
  cd ${BUILD_PATH}/${HTML}
  rm -rf ${JAVADOCS}
  cp -r ${SDK_JAVADOCS} .
  mv -f ${APIDOCS} ${JAVADOCS}
}

function build_license_pdfs() {
  version
  cd ${SCRIPT_PATH}
  PROJECT_VERSION_TRIMMED=${PROJECT_VERSION%%-SNAPSHOT*}
  rm -rf ${SCRIPT_PATH}/${LICENSES_PDF}
  mkdir ${SCRIPT_PATH}/${LICENSES_PDF}
  E_DEP="cdap-enterprise-dependencies"
  L_DEP="cdap-level-1-dependencies"
  S_DEP="cdap-standalone-dependencies"
  # paths are relative to location of $DOC_GEN_PY script
  LIC_PDF="../../../${REFERENCE}/${LICENSES_PDF}"
  LIC_RST="../${REFERENCE}/source/${LICENSES}"
  echo ""
  echo "Building ${E_DEP}"
  python ${DOC_GEN_PY} -g pdf -o ${LIC_PDF}/${E_DEP}.pdf -b ${PROJECT_VERSION_TRIMMED} ${LIC_RST}/${E_DEP}.rst
  echo ""
  echo "Building $L_DEP"
  python ${DOC_GEN_PY} -g pdf -o ${LIC_PDF}/${L_DEP}.pdf -b ${PROJECT_VERSION_TRIMMED} ${LIC_RST}/${L_DEP}.rst
  echo ""
  echo "Building $S_DEP"
  python ${DOC_GEN_PY} -g pdf -o ${LIC_PDF}/${S_DEP}.pdf -b ${PROJECT_VERSION_TRIMMED} ${LIC_RST}/${S_DEP}.rst
}

function copy_license_pdfs() {
  cd ${BUILD_PATH}/${HTML}/${LICENSES}
  cp ${SCRIPT_PATH}/${LICENSES_PDF}/* .
}

function make_zip() {
  version
  if [ "x${1}" == "x" ]; then
    ZIP_DIR_NAME="${PROJECT}-docs-${PROJECT_VERSION}"
  else
    ZIP_DIR_NAME="${PROJECT}-docs-${PROJECT_VERSION}-$1"
  fi
  cd ${SCRIPT_PATH}/${BUILD}
  mkdir ${PROJECT_VERSION}
  mv ${HTML} ${PROJECT_VERSION}/en
  # Add a redirect index.html file
  echo "${REDIRECT_EN_HTML}" > ${PROJECT_VERSION}/index.html
  # Zip everything
  zip -qr ${ZIP_DIR_NAME}.zip ${PROJECT_VERSION}/* --exclude .DS_Store
}

function build_extras() {
  # Over-ride this function in guides where Javadocs or licenses are being built or copied.
  # Currently performed in reference-manual
  echo "No extras being built."
}

function build() {
  build_docs
  build_extras
}

function build_github() {
  build_docs_google ${GOOGLE_ANALYTICS_GITHUB}
  build_extras
}

function build_web() {
  build_docs_google ${GOOGLE_ANALYTICS_WEB}
  build_extras
}

function set_mvn_environment() {
  if [ "$(uname)" == "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 1.7)
  fi
}

function check_includes() {
  if [ "${CHECK_INCLUDES}" == "${TRUE}" ]; then
    echo "Downloading and checking includes."
    # Build includes
    BUILD_INCLUDES_DIR=${SCRIPT_PATH}/${BUILD}/${INCLUDES}
    rm -rf ${BUILD_INCLUDES_DIR}
    mkdir ${BUILD_INCLUDES_DIR}
    download_includes ${BUILD_INCLUDES_DIR}
    # Test included files
    test_includes
  else
    echo "No includes to be checked."
  fi
}

function download_includes() {
  # $1 passed is the directory to which the downloaded files are to be written.
  # For an example of over-riding this function, see developer/build.sh
  echo "No includes to be downloaded."
}

function test_includes () {
  # For an example of over-riding this function, see developer/build.sh
  echo "No includes to be tested."
}

function test_an_include() {
  # Tests a file and checks that it hasn't changed.
  # Uses md5 hashes to monitor if any files have changed.
  local md5_hash=${1}
  local target=${2}
  local new_md5_hash
  
  local file_name=`basename ${target}`
  
  if [[ "${OSTYPE}" == "darwin"* ]]; then
    new_md5_hash=`md5 -q ${target}`
  else
    new_md5_hash=`md5sum ${target} | awk '{print $1}'`
  fi
  
  local m
  
  if [[ "${new_md5_hash}" == "${NOT_FOUND_HASH}" ]]; then
    m="${WARNING} ${RED}${BOLD}${file_name} not found!${NC}"  
    m="${m}\nfile: ${target}"  
  elif [[ "${new_md5_hash}" != "${md5_hash}" ]]; then
    m="${WARNING} ${RED}${BOLD}${file_name} has changed! Compare files and update hash!${NC}"   
    m="${m}\nfile: ${target}"   
    m="${m}\nOld MD5 Hash: ${md5_hash} New MD5 Hash: ${RED}${BOLD}${new_md5_hash}${NC}"   
  fi
  if [ "x${m}" != "x" ]; then
    set_message "${m}"
  else
    m="MD5 Hash for ${file_name} matches"
  fi
  echo -e "${m}"
}

function build_standalone() {
  cd ${PROJECT_PATH}
  set_mvn_environment
  MAVEN_OPTS="-Xmx1024m" mvn clean package -DskipTests -P examples,templates -pl cdap-examples,cdap-app-templates/cdap-etl -am -amd && MAVEN_OPTS="-Xmx1024m" mvn package -pl cdap-standalone -am -DskipTests -P dist,release
}

function build_sdk() {
  build_standalone
}

function build_dependencies() {
  cd $PROJECT_PATH
  set_mvn_environment
  mvn clean package site -am -Pjavadocs -DskipTests
}

function version() {
  OIFS="${IFS}"
  local current_directory=`pwd`
  cd ${PROJECT_PATH}
  PROJECT_VERSION=`grep "<version>" pom.xml`
  PROJECT_VERSION=${PROJECT_VERSION#*<version>}
  PROJECT_VERSION=${PROJECT_VERSION%%</version>*}
  PROJECT_LONG_VERSION=`expr "${PROJECT_VERSION}" : '\([0-9]*\.[0-9]*\.[0-9]*\)'`
  PROJECT_SHORT_VERSION=`expr "${PROJECT_VERSION}" : '\([0-9]*\.[0-9]*\)'`
  local full_branch=`git rev-parse --abbrev-ref HEAD`
  IFS=/ read -a branch <<< "${full_branch}"
  GIT_BRANCH="${branch[1]}"
  # Determine branch and branch type: one of develop, master, release, develop-feature, release-feature
  if [ "${full_branch}" == "develop" -o  "${full_branch}" == "master" ]; then
    GIT_BRANCH="${full_branch}"
    GIT_BRANCH_TYPE=${GIT_BRANCH}
  elif [ "${GIT_BRANCH:0:7}" == "release" ]; then
    GIT_BRANCH_TYPE="release"
  else
    # We are on a feature branch: but from develop or release?
    if [[ "x${GIT_BRANCH_PARENT}" == "x" ]]; then
      GIT_BRANCH_PARENT=`git show-branch | grep '*' | grep -v "$(git rev-parse --abbrev-ref HEAD)" | head -n1 | sed 's/.*\[\(.*\)\].*/\1/' | sed 's/[\^~].*//'`  
    fi
    if [ "${GIT_BRANCH_PARENT}" == "release" ]; then
      GIT_BRANCH_TYPE="release-feature"
    else
      GIT_BRANCH_TYPE="develop-feature"
    fi
  fi
  cd $current_directory
  IFS="${OIFS}"
}

function display_version() {
  version
  echo ""
  echo "PROJECT_PATH: ${PROJECT_PATH}"
  echo "PROJECT_VERSION: ${PROJECT_VERSION}"
  echo "PROJECT_LONG_VERSION: ${PROJECT_LONG_VERSION}"
  echo "PROJECT_SHORT_VERSION: ${PROJECT_SHORT_VERSION}"
  echo "GIT_BRANCH: ${GIT_BRANCH}"
  echo "GIT_BRANCH_TYPE: ${GIT_BRANCH_TYPE}"
  echo "GIT_BRANCH_PARENT: ${GIT_BRANCH_PARENT}"
}

function set_messages_file() {
  TMP_MESSAGES_FILE="/tmp/$(basename $0).$$.tmp"
  export TMP_MESSAGES_FILE
}

function cleanup_messages_file() {
  rm -f ${TMP_MESSAGES_FILE}
}

function clear_messages() {
  MESSAGES=
  set_messages_file
}

function set_message() {
  if [ "x${MESSAGES}" == "x" ]; then
    MESSAGES=${*}
  else
    MESSAGES="${MESSAGES}\n\n${*}"
  fi
  if [ "x${TMP_MESSAGES_FILE}" != "x" ]; then
    local clean_m=`echo_clean_colors "${*}"`
    if [ -e ${TMP_MESSAGES_FILE} ]; then
      echo >> ${TMP_MESSAGES_FILE}
    fi
    echo -e "Warning Message for \"${MANUAL}\":\n${clean_m}" >> ${TMP_MESSAGES_FILE}
  fi
}

function display_any_messages() {
  if [[ "x${MESSAGES}" != "x" || -s ${SPHINX_MESSAGES} ]]; then
    local m="Warning Messages for \"${MANUAL}\":"
    if [ "x${MESSAGES}" != "x" ]; then
      echo ""
      echo_red_bold "${m}"
      echo ""
      echo -e "${MESSAGES}"
    fi
    if [ -s ${SPHINX_MESSAGES} ]; then
      m="Sphinx ${m}"
      echo ""
      echo_red_bold "${m}"
      echo ${m} >> ${TMP_MESSAGES_FILE}
      cat ${SPHINX_MESSAGES} | while read line
      do
        echo ${line}
        echo ${line} >> ${TMP_MESSAGES_FILE}
      done
    fi
  fi
}

function display_messages_file() {
  if [[ "x${TMP_MESSAGES_FILE}" != "x" && -a ${TMP_MESSAGES_FILE} ]]; then
    echo ""
    echo "Warning Messages: ${TMP_MESSAGES_FILE}"
    echo ""
    echo >> ${TMP_MESSAGES_FILE}
    cat ${TMP_MESSAGES_FILE} | while read line
    do
      echo -e "${line}"
    done
    return 1 # Indicates warning messages present
  else
    return 0
  fi
}

function rewrite() {
  # Substitutes text in file $1 and outputting to file $2, replacing text $3 with text $4
  # or if $4=="", substitutes text in-place in file $1, replacing text $2 with text $3
  # or if $3 & $4=="", substitutes text in-place in file $1, using sed command $2
  cd ${SCRIPT_PATH}
  local rewrite_source=${1}
  echo "Re-writing"
  echo "    $rewrite_source"
  if [ "x${3}" == "x" ]; then
    local sub_string=${2}
    echo "  $sub_string"
    if [ "$(uname)" == "Darwin" ]; then
      sed -i '.bak' "${sub_string}" ${rewrite_source}
      rm ${rewrite_source}.bak
    else
      sed -i "${sub_string}" ${rewrite_source}
    fi
  elif [ "x${4}" == "x" ]; then
    local sub_string=${2}
    local new_sub_string=${3}
    echo "  ${sub_string} -> ${new_sub_string} "
    if [ "$(uname)" == "Darwin" ]; then
      sed -i '.bak' "s|${sub_string}|${new_sub_string}|g" ${rewrite_source}
      rm ${rewrite_source}.bak
    else
      sed -i "s|${sub_string}|${new_sub_string}|g" ${rewrite_source}
    fi
  else
    local rewrite_target=${2}
    local sub_string=${3}
    local new_sub_string=${4}
    echo "  to"
    echo "    ${rewrite_target}"
    echo "  ${sub_string} -> ${new_sub_string} "
    sed -e "s|${sub_string}|${new_sub_string}|g" ${rewrite_source} > ${rewrite_target}
  fi
}

function run_command() {
  case "${1}" in
    build )             build;;
    build-github )      build_github;;
    build-web )         build_web;;
    check-includes )    check_includes;;
    docs )              build_docs;;
    license-pdfs )      build_license_pdfs;;
    build-standalone )  build_standalone;;
    copy-javadocs )     copy_javadocs;;
    copy-license-pdfs ) copy_license_pdfs;;
    javadocs )          build_javadocs_sdk;;
    javadocs-full )     build_javadocs_full;;
    depends )           build_dependencies;;
    sdk )               build_sdk;;
    version )           display_version;;
    * )                 usage;;
  esac
}
