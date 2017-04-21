
Pull requests are welcome! :) 

Feel free to create the pull request even before it is ready for review so the maintainers can give you early feedback. Please add the prefix [WIP] to the title if it is sill in progress.

We are looking for developers interested in becoming a maintainer. After a few contributions, please reach out to one of the maintainers to join the team if you are interested. Note that contributions are not necessarily pull requests; they can be help on the gitter channel, issues triage, documentation improvements, and others.

Building the project
====================

The pre-requisites are [SBT](http://www.scala-sbt.org/) and Java 8. Go to the project directory and run this command to build the library and run the tests:

```
sbt test
```

Releases and snapshots
======================

All artifacts are published to maven central by the travis ci build. The [build script](https://github.com/monadless/monadless/blob/master/build/build.sh) is configured to:

- Publish a new snapshot version on each master build with the version specified by [version.sbt](https://github.com/monadless/monadless/blob/master/version.sbt).
- Make a release if `version.sbt` has a non-snaphot version. Note: anyone can submit a pull request to trigger a new release by updating [version.sbt](https://github.com/monadless/monadless/blob/master/version.sbt).
- Release a snapshot on each branch build with the version `THE_BRANCH_NAME-SNAPSHOT`.
- Don't release or publish artifacts if it's a pull request build.
