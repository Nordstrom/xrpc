# Contributing

For people contributing to this project.

## Publishing

xrpc is published to [jCenter via Bintray](https://bintray.com/nordstromoss/oss_maven/xrpc).

### Setup

1. [Create a bintray account](https://bintray.com), if you don't have one. It's recommended that you
   use github OAuth to create the account.
2. Get added to the [NordstromOSS organization](https://bintray.com/nordstromoss). Any admin should
   be able to add you to the organization if you send them your username.
3. Create a private `gradle.properties` file:

    ```bash
    mkdir -p ~/.gradle
    touch ~/.gradle/gradle.properties
    chmod 600 ~/.gradle/gradle.properties
    ```
4. Find your bintray key in the ["Edit Profile" screen](https://bintray.com/profile/edit), under the
   "API Key" tab on the left.
5. Add your bintray username and key to the private `gradle.properties` file:

    ```
    bintrayUsername={username}
    bintrayKey={API key}
    ```

### Publishing Steps

xrpc uses [gradle-release](https://github.com/researchgate/gradle-release) for publishing jars to
Bintray.

In order to use the plugin, you **must** have write permissions on the xrpc repository in GitHub.
The release process will automatically push commits & the release tag to the repository.

To publish:

1. Ensure that you have a `github.com/Nordstrom/xrpc` remote in your git repository named
   `upstream`.
2. Check out a branch called `master`, synced to `upstream/master`.
3. Run `./gradlew release`. Watch the output closely for prompts, and follow instructions.
4. Edit the [release notes for the tag](https://github.com/Nordstrom/xrpc/releases), and mark it as released.
