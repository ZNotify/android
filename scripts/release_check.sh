#!/bin/bash

# get last git commit message
git_commit_message=$(git log -1 --pretty=%B)

#if commit message contains "[release]" then set output
if [[ $git_commit_message == *"[release]"* ]]; then
    echo "::set-output name=release::true"
    echo "Should release"
else
    echo "::set-output name=release::false"
    echo "Should not release"
fi
