#!/bin/bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

	if [[ $TRAVIS_BRANCH == "master" && $(cat version.sbt) != *"SNAPSHOT"* ]]
	then
		eval "$(ssh-agent -s)"
		chmod 600 local.deploy_key.pem
		ssh-add local.deploy_key.pem
		git config --global user.name "Monadless CI"
		git config --global user.email "ci@monadless.io"
		git remote set-url origin git@github.com:monadless/monadless.git
		git fetch
		git checkout master || git checkout -b master
		git reset --hard origin/master
		git push --delete origin website
		sbt +tut +'release with-defaults'
	elif [[ $TRAVIS_BRANCH == "master" ]]
	then
		sbt ++2.11.8 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
		sbt ++2.11.8 coverageOff publish

		sbt ++2.12.1 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
		sbt ++2.12.1 coverageOff publish
	else
		sbt ++2.11.8 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
		sbt ++2.12.1 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
		echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
		sbt ++2.11.8 coverageOff publish
		sbt ++2.12.1 coverageOff publish
	fi
else
	sbt ++2.11.8 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
	sbt ++2.12.1 clean coverage test tut coverageReport coverageAggregate checkUnformattedFiles
fi
