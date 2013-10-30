#!/bin/sh

REPO_NAME=eclipse-tapestry5-plugin
PACKAGE_NAME=update-site
VERSION_NAME=1.3.2

curl -vT content.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/

curl -vT artifacts.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/

curl -vT features/com.anjlab.eclipse.tapestry5.feature_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/features/

curl -vT plugins/com.anjlab.eclipse.tapestry5_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/plugins/

curl -vT features/com.anjlab.eclipse.e4.tapestry5.feature_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/features/

curl -vT plugins/com.anjlab.eclipse.e4.tapestry5_$VERSION_NAME.jar \
    -udmitrygusev:$bintray_api_key \
    https://api.bintray.com/content/anjlab/$REPO_NAME/$PACKAGE_NAME/$VERSION_NAME/plugins/

