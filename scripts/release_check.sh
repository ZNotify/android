#!/bin/bash

# get last git commit message
git_commit_message=$(git log -1 --pretty=%B)

if [[ $git_commit_message == *"[release:"* ]]; then
    echo "release=true" >> "$GITHUB_OUTPUT"
    echo "Should release"
    # get version from [release: x.x.x]
    version=$(echo "$git_commit_message" | grep -o "\[release:.*\]" | sed "s/\[release:\s*//g" | sed "s/\s*\]//g")
    if [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "version=$version" >> "$GITHUB_OUTPUT"
        echo "Version is $version"
    else
        echo "Version format is not correct"
        echo "Version format should be x.x.x"
        echo "But got $version"
        exit 1
    fi

else
    echo "release=false" >> "$GITHUB_OUTPUT"
    echo "Should not release"
fi
