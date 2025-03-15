#!/bin/sh

REPO_NAME=eclipse-tapestry5-plugin
PACKAGE_NAME=update-site
VERSION_NAME=2.13.7

SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )

cd ${SCRIPT_PATH}

rm -rf ${VERSION_NAME}

mkdir ${VERSION_NAME}

cp artifacts.jar ${VERSION_NAME}
cp content.jar ${VERSION_NAME}
mv features ${VERSION_NAME}
mv plugins ${VERSION_NAME}

unzip artifacts.jar

sed -i'.bak' -e "s/\/plugins\/\\$/\/${VERSION_NAME}\/plugins\/$/g" artifacts.xml
sed -i'.bak' -e "s/\/features\/\\$/\/${VERSION_NAME}\/features\/$/g" artifacts.xml
sed -i'.bak' -e "s/\/binary\/\\$/\/${VERSION_NAME}\/binary\/$/g" artifacts.xml

zip artifacts.jar artifacts.xml

rm artifacts.xml artifacts.xml.bak
