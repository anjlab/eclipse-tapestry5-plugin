#!/bin/sh

PACKAGE_NAME=eclipse-tapestry5-plugin
VERSION_NAME=1.1.0

curl -vT content.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/eclipse/$PACKAGE_NAME/$VERSION_NAME/

curl -vT artifacts.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/eclipse/$PACKAGE_NAME/$VERSION_NAME/

curl -vT features/com.anjlab.eclipse.tapestry5.feature_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/eclipse/$PACKAGE_NAME/$VERSION_NAME/features/

curl -vT plugins/com.anjlab.eclipse.tapestry5_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/eclipse/$PACKAGE_NAME/$VERSION_NAME/plugins/

