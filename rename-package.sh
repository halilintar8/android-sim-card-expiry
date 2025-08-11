#!/bin/bash
# rename-package.sh
# Run from the root of your Android project

OLD_PKG="com.example.simexpiry"
NEW_PKG="com.halilintar8.simexpiry"

OLD_DIR="app/src/main/java/com/example/simexpiry"
NEW_DIR="app/src/main/java/com/halilintar8/simexpiry"

echo "=== Backing up project ==="
tar czf backup_before_package_rename.tar.gz app/src/main/java

echo "=== Replacing package declarations ==="
find . -type f -name "*.kt" -o -name "*.xml" \
    | xargs sed -i "s/^package ${OLD_PKG}/package ${NEW_PKG}/g"

echo "=== Replacing imports ==="
find . -type f -name "*.kt" -o -name "*.xml" \
    | xargs sed -i "s/import ${OLD_PKG}/import ${NEW_PKG}/g"

echo "=== Replacing subpackage references ==="
find . -type f -name "*.kt" -o -name "*.xml" -o -name "*.gradle" \
    | xargs sed -i "s/${OLD_PKG}\./${NEW_PKG}\./g"

echo "=== Updating Gradle namespace and applicationId ==="
sed -i "s/namespace *= *\"${OLD_PKG}\"/namespace = \"${NEW_PKG}\"/g" app/build.gradle
sed -i "s/applicationId *= *\"${OLD_PKG}\"/applicationId = \"${NEW_PKG}\"/g" app/build.gradle

echo "=== Moving source files to new directory ==="
mkdir -p "${NEW_DIR%/*}"
mv "$OLD_DIR" "$NEW_DIR"

echo "=== Done! Cleaning and rebuilding ==="
./gradlew clean build

