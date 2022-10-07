#!/bin/bash

# get last git commit message
git_commit_message=$(git log -1 --pretty=%B)

if [[ $git_commit_message == *"[release:"* ]]; then
    echo "::set-output name=release::true"
    echo "Should release"
    # get version from [release: x.x.x]
    version=$(echo "$git_commit_message" | grep -o "\[release:.*\]" | sed "s/\[release:\s*//g" | sed "s/\s*\]//g")
    if [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "::set-output name=version::$version"
        echo "Version is $version"
    else
        echo "Version format is not correct"
        echo "Version format should be x.x.x"
        echo "But got $version"
        exit 1
    fi

else
    echo "::set-output name=release::false"
    echo "Should not release"
fi
