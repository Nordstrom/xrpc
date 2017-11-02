# Contributing

For people contributing to this project.

## Publishing

xrpc is published to [the central repository](https://oss.sonatype.org/#nexus-search;quick~com.nordstrom.xrpc).

### Setup

1. [Create a Sontatype JIRA account](https://issues.sonatype.org/secure/Signup!default.jspa), if you don't have one. This will be what you log in to the central repository with.
2. Get added to the nordstrom.com group. This means contacting one of the administrators of the group, and filing a ticket, as described in [this issue](https://issues.sonatype.org/browse/OSSRH-13276).
3. Create a gpg key, if you don't already have one. [This help article](http://central.sonatype.org/pages/working-with-pgp-signatures.html) has detailed instructions.
4. If you used gpg v2.1 or greater, create a gradle-compatible old-style private keyring:

    ```bash
    touch ~/.gnupg/secring.gpg
    chmod 600 ~/.gnupg/secring.gpg
    gpg --export-secret-keys > ~/.gnupg/secring.gpg
    ```

5. If you haven't configured gradle to use this key yet, note the four-byte ID of your key:

    ```bash
    $ gpg --keyid-format SHORT -k {PUBLIC_KEY_ID}
    pub   rsa2048/{FOUR_BYTES_HEX} 2017-01-01 [SC] [expires: 2019-01-01]
          1234567890ABCDEF1234567890ABCDEF12345678
    uid         [ultimate] Your Name <your.name@email.com>
    sub   rsa2048/IGNORED 2017-01-01 [E] [expires: 2019-01-01]
    ```

    The string where `{FOUR_BYTES_HEX}` is above should be your four-byte key ID. The public ID of your key can be shown bu running `gpg --list-keys`.
6. Create a private `gradle.properties` file:

    ```bash
    mkdir -p ~/.gradle
    touch ~/.gradle/gradle.properties
    chmod 600 ~/.gradle/gradle.properties
    ```
7. If you haven't configured the gradle plugin for signing, follow [these instructions](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials). Use the four-byte value from step 5 for `keyId`, and set `secretKeyRingFile` to the full path to `~/.gnupg/secring.gpg`.
8. If you haven't configured the gradle plugin for signing, add your JIRA credentials to `gradle.properties`:

    ```
    ossrhUsername={username}
    ossrhPassword={password}
    ```

### Publishing Steps

xrpc uses [gradle-release](https://github.com/researchgate/gradle-release) for publishing jars to
Maven Central.

In order to use the plugin, you **must** have write permissions on this repository in GitHub. It
will automatically push commits & the release tag to the repository.

To publish:

1. Ensure that you have a `github.com/Nordstrom/xrpc` remote in your git repository named
   `upstream`.
2. Check out a branch called `master`, synced to `upstream/master`.
3. Run `./gradlew release`. Watch the output closely for prompts, and follow instructions.
4. Follow the [release instructions at Sonatype](http://central.sonatype.org/pages/releasing-the-deployment.html).
5. Edit the [release notes for the tag](https://github.com/Nordstrom/xrpc/releases), and mark it as released.
