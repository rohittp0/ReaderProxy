#!/bin/bash
# increment_version.sh
# This script increments the VERSION and VERSION_CODE in gradle.properties

set -e

GRADLE_PROPERTIES="gradle.properties"

if [ ! -f "$GRADLE_PROPERTIES" ]; then
    echo "gradle.properties file not found!"
    exit 1
fi

# Read the current values from gradle.properties
CURRENT_VERSION=$(grep '^VERSION=' "$GRADLE_PROPERTIES" | cut -d'=' -f2)
CURRENT_VERSION_CODE=$(grep '^VERSION_CODE=' "$GRADLE_PROPERTIES" | cut -d'=' -f2)

echo "Current VERSION: $CURRENT_VERSION"
echo "Current VERSION_CODE: $CURRENT_VERSION_CODE"

# Increment the version code
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

# Increment the patch version assuming VERSION format is X.Y.Z
IFS='.' read -r major minor patch <<< "$CURRENT_VERSION"
NEW_PATCH=$((patch + 1))
NEW_VERSION="${major}.${minor}.${NEW_PATCH}"

echo "New VERSION: $NEW_VERSION"
echo "New VERSION_CODE: $NEW_VERSION_CODE"

# Update gradle.properties using sed with a backup file (works on GNU and BSD)
sed -i.bak "s/^VERSION=.*/VERSION=${NEW_VERSION}/" "$GRADLE_PROPERTIES"
sed -i.bak "s/^VERSION_CODE=.*/VERSION_CODE=${NEW_VERSION_CODE}/" "$GRADLE_PROPERTIES"

# Remove the backup file
rm "$GRADLE_PROPERTIES.bak"

echo "gradle.properties updated successfully."

# Export the new version as a step output for GitHub Actions.
# GitHub Actions automatically creates the file referenced by the GITHUB_OUTPUT environment variable.
echo "new_version=${NEW_VERSION}" >> "$GITHUB_OUTPUT"