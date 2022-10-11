#!/usr/bin/env bash
RELEASE_NOTES="$(git log -1 --pretty=short)"

app="zxilly/Notify"

appcenter distribute release \
  --app "$app" \
  --group "public" \
  --file "app/build/outputs/apk/appcenter/release/app-appcenter-release.apk" \
  --release-notes "$RELEASE_NOTES" \
  --token "$APPCENTER_TOKEN"

appcenter crashes upload-mappings \
  --app "$app" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --mapping "app/build/outputs/mapping/appcenterRelease/mapping.txt" \
  --token "$APPCENTER_TOKEN"

appcenter crashes upload-mappings \
  --app "$app" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --mapping "app/build/outputs/mapping/appcenterRelease/mapping.txt" \
  --token "$APPCENTER_TOKEN"

artifacts=("appcenterRelease" "freeRelease" "githubRelease" "playRelease")

for artifact in "${artifacts[@]}"; do
  appcenter crashes upload-mappings \
    --app "$app" \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" \
    --mapping "app/build/outputs/mapping/$artifact/mapping.txt" \
    --token "$APPCENTER_TOKEN"
done