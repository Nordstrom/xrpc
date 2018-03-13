# Contributing

For people contributing to this project.

## Publishing

xrpc is published to [jCenter via Bintray](https://bintray.com/nordstromoss/oss_maven/xrpc).

### Publishing Steps

xrpc uses [travis-ci](https://travis-ci.org/Nordstrom/xrpc) and [gradle-release](https://github.com/researchgate/gradle-release) for publishing jars to Bintray.

In order to make a release, you **must** have write permissions to the master branch of the xrpc repository in Github.
The release process will automatically push commits & the release tag to the repository.

To publish:

1. Ensure that you have a `github.com/Nordstrom/xrpc` remote in your git repository named
   `upstream`.
2. Check out a branch called `master`, synced to `upstream/master`.
3. (Optional) Update [gradle.properties](./gradle.properties) if you want this to be a non-patch release.
3. Run `./gradlew release`. Watch the output closely for prompts, and follow instructions.
4. Edit the [release notes for the tag](https://github.com/Nordstrom/xrpc/releases), and mark it as released.
