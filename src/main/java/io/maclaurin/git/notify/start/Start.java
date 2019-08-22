/**
 * Main start up class
 * 
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.start;

import io.maclaurin.git.notify.config.GlobalConfiguration;
import io.maclaurin.git.notify.summary.GitTimeRangeEmailSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Start {

  private static final Logger LOG = LoggerFactory.getLogger(Start.class);

  /**
   * Main method for starting the Git Notify tool.
   * 
   * 
   * Requires a single command line argument at start up
   * pointing to the properties file containing configuration
   * settings. Argument should have the following syntax
   * depending on the file source:
   *  - File
   *      file:///var/tmp/properties.ini
   *  - S3
   *      s3://somebucket/folder/properties.ini
   *  - AWS Parameter Store
   *      ssm-ps:///some/path/properties.ini
   * 
   * 
   * By default when it runs it will perform the necessary Git
   * analysis and send out a summary email regarding any commits
   * made yesterday (midnight to midnight).
   * 
   * 
   * However, to provide a custom date-time range for the commits to
   * include in the email provide either a system property or
   * set an environment variable. The format of the date-time range
   * values should be something like "2019-08-22T01:00:0Z".
   * 
   *  - System properties
   *      git.notify.since
   *      git.notify.until
   *  - Environment variables
   *      GIT_NOTIFY_SINCE
   *      GIT_NOTIFY_UNTIL
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {

      if (args.length < 1) {
        LOG.error("missing required command line argument with properties location");
        System.exit(1);
      }
      GlobalConfiguration globalConfig = new GlobalConfiguration(args[0]);

      ZoneId zoneId = ZoneId.of(globalConfig.getTimeZone());
      LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(zoneId), LocalTime.MIDNIGHT);
      LocalDateTime yesterdayMidnight = todayMidnight.minusDays(1);

      Date since;
      String propSinceStr = System.getProperty("git.notify.since");
      String envSinceStr = System.getenv("GIT_NOTIFY_SINCE");
      if (propSinceStr != null && !propSinceStr.trim().isEmpty()) {
        since = Date.from(Instant.parse(propSinceStr).atZone(zoneId).toInstant());
      } else if (envSinceStr != null && !envSinceStr.trim().isEmpty()) {
        since = Date.from(Instant.parse(envSinceStr).atZone(zoneId).toInstant());
      } else {
        since = Date.from(yesterdayMidnight.atZone(zoneId).toInstant());
      }

      Date until;
      String propUntilStr = System.getProperty("git.notify.until");
      String envUntilStr = System.getenv("GIT_NOTIFY_UNTIL");
      if (propUntilStr != null && !propUntilStr.trim().isEmpty()) {
        until = Date.from(Instant.parse(propUntilStr).atZone(zoneId).toInstant());
      } else if (envUntilStr != null && !envUntilStr.trim().isEmpty()) {
        until = Date.from(Instant.parse(envUntilStr).atZone(zoneId).toInstant());
      } else {
        until = Date.from(todayMidnight.atZone(zoneId).toInstant());
      }

      LOG.info("will generate summary emails for git changes between " + String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", since) + " and " + String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", until) );
      
      GitTimeRangeEmailSummary emailSummary = new GitTimeRangeEmailSummary(globalConfig, since, until);
      emailSummary.send();

    } catch (Exception e) {
      LOG.error("encountered unexpected failure", e);
    }
  }

}