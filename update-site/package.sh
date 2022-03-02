#!/bin/sh

REPO_NAME=eclipse-tapestry5-plugin
PACKAGE_NAME=update-site
VERSION_NAME=2.13.4

SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )

cd ${SCRIPT_PATH}

ARCHIVE_NAME=${REPO_NAME}-${PACKAGE_NAME}-${VERSION_NAME}.zip

rm ${ARCHIVE_NAME}

zip -r ${ARCHIVE_NAME} \
  artifacts.jar \
  content.jar \
  features \
  plugins
