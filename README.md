# mg-git-notify

A utility built using Java for generating and sending daily Git activity summary emails for Git branches.

## Configuration

Details about the various configuration options for the utility can be found below.

### Properties File

The majority of the configuration is handled in a properties file. The location of the properties file is passed to the utility via a command line argument on startup.

Property | Required | Description
-------- | -------- | -----------
work.dir | false | The local file directory to utilize for checking out the Git repositories. Defaults to `/var/tmp/git-notify/`
time.zone | false | The time zone to use when looking for commits within the summary time range.
email.from.address | true | The from email address to utilize when sending out summary emails.
smtp.host | true | The SMTP host to use when sending out emails.
smtp.port | true | The SMTP port to use when sending out emails.
smtp.username | true | The SMTP username to use when sending out emails.
smtp.password | true | The SMTP password to use when sending out emails.
smtp.transport.strategy | false | The SMTP transport strategy to use when sending out emails. Supported values are `SMTP`, `SMTPS`, `SMTP_TLS`
aws.region | false | If the `git.provider` is set to `codebuild` this provides the relevant AWS regions to utilize when building the commit url.
git.provider | false | If utilizing a supported Git provider the email will contain a link to the commit. Supported values are `codebuild`
git.username | false | The Git username to use when cloning the Git repositories.
git.password | false | The Git password to use when cloning the Git repositories.
git.repos | true | A comma separated list of Git repositories names to clone.
git.{repo_name}.clone_url | true | The HTTPS clone url for the Git repository.
git.{repo_name}.branches | true | The particular branches of the Git repository to check for commits to email about. Supports exact branch names and basic wildcard matching via `*` at the front and/or end. Providing just `*` will match all branches.
git.{repo_name}.{branch}.subscriber.emails | true | The email addresses to send a summary email to for this particular branch

### Summary Date Range

By default the utility will look for commits that occurred the previous day from midnight to midnight in the configured time zone. However, it is possible to tell the utility to look across a different time period by setting up either environment variables or system properties.

You have the option of providing a single side of the time range. For example if you only provided a `since` date-time the utility will still utilize the normal default until date-time of midnight (00:00) today for the configured time zone.

When providing a `since` or `until` date-time override the format of the string should be something like `2019-08-22T10:00:00Z` to represent August 22, 2019 at 10:00:00 am UTC time.

#### Environment Variables

* GIT_NOTIFY_SINCE
* GIT_NOTIFY_UNTIL

#### System Properties

* git.notify.since
* git.notify.until

## Running

To run the utility you simply need to provide it with an environment that has a Java 8+ runtime installed and enough disk space to checkout the configured Git repositories.

```
java -jar git-notify.jar s3://bucket/folder/properties.ini
```

_*Note: If the utility detects that the Git repository is already checked out to the local disk from a previous run it will simply perform a fetch instead of a fresh checkout.*_

### AWS CodeBuild + SES

Our preferred setup for this tool is to utilize the AWS CodeBuild service to execute the utility and AWS Simple Email Service (SES) to send the emails. The approach is not only easy to setup but is also extremely cost effective by allowing the utility to normally run for just one or two pennies a day (if not less).

Generally our setup goes something like this:

1. Acquire the latest `git-notify` release JAR
2. Upload the release JAR to a location on S3 along with a buildspec.yml file
  * We have an example buildspec.yml file under the codebuild directory
3. Configure SES to be able to send emails to the given recipients if not already configured
  * See the [AWS SES verification documentation](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/verify-email-addresses.html)
4. Setup SES SMTP access credentials for the utility
  * See the [AWS SES SMTP credentials documentation](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/smtp-credentials.html)
5. Setup Git credentials that allow read access to the relevant Git repositories
6. Setup the properties.ini file and store it on S3 or in an AWS Systems Manager Parameter Store parameter
7. Create a CodeBuild project 
  * Configured with an S3 source that points to the files uploaded in step 1
  * Use the default Amazon Linux 2 environment image
  * Ensure that the IAM role associated to or created by CodeBuild has access to the source files and the properties.ini 
8. Run a CodeBuild job to confirm everything works as expected
9. Define a CodeBuild trigger to run a fresh CodeBuild job as desired
  * See the [AWS CodeBuild trigger documentation](https://docs.aws.amazon.com/codebuild/latest/userguide/trigger-create.html)
