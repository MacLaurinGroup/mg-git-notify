/**
 * Class for representing the "global"
 * configuration options provided by
 * the properties file.
 * 
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

import io.maclaurin.git.notify.exception.GitEmailException;

public class GlobalConfiguration {

  private static final String FILE_URL_PREFIX = "file://",
    S3_URL_PREFIX = "s3://",
    SSM_PS_URL_PREFIX = "ssm-ps://";

  public enum GitProvider {
    codebuild;
  }

  private Properties props;

  private Mailer mailer;
  
  private CredentialsProvider gitCredentialsProvider;

  private String emailFromAddr;

  private File workDir;

  private List<RepoConfiguration> repoConfigurations;

  private GitProvider gitProvider;

  private String awsRegion;

  private String timeZone;

  public GlobalConfiguration(String configUrl) throws GitEmailException {
    if (configUrl == null || configUrl.trim().isEmpty()) {
      throw new GitEmailException("missing configuration url");
    }

    if (configUrl.startsWith(S3_URL_PREFIX)) {
      props = fetchS3Properties(configUrl);
    } else if (configUrl.startsWith(FILE_URL_PREFIX)) {
      props = fetchFileProperties(configUrl);
    } else if (configUrl.startsWith(SSM_PS_URL_PREFIX)) {
      props = fetchParameterStoreProperties(configUrl);
    } else {
      throw new GitEmailException("invalid/unsupported configuration url - supports s3, file, and ssm-ps");
    }

    loadProperties();
  }

  public File getWorkDir() {
    return workDir;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public String getEmailFromAddr() {
    return emailFromAddr;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public Mailer getMailer() {
    return mailer;
  }

  public CredentialsProvider getGitCredentialsProvider() {
    return gitCredentialsProvider;
  }

  public List<RepoConfiguration> getRepoConfigurations() {
    return repoConfigurations;
  }

  public GitProvider getGitProvider() {
    return gitProvider;
  }

  private Properties fetchS3Properties(String url) throws GitEmailException {
    int bucketEndIndex = url.indexOf("/",S3_URL_PREFIX.length() + 1); 
    String s3Bucket = url.substring(S3_URL_PREFIX.length() + 1, bucketEndIndex);
    String s3Key = url.substring(bucketEndIndex+1);
    try {
      AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
      S3Object s3Object = s3Client.getObject(new GetObjectRequest(s3Bucket,s3Key));
      Properties props = new Properties();
      props.load(s3Object.getObjectContent());
      return props;
    } catch (Exception e) {
      throw new GitEmailException("encountered an error loading properties from s3", e);
    }
  }

  private Properties fetchFileProperties(String url) throws GitEmailException {
    String filePath = url.replace(FILE_URL_PREFIX, "");
    try (InputStream input = new FileInputStream(filePath)) {
      Properties props = new Properties();
      props.load(input);
      return props;
    } catch (IOException e) {
      throw new GitEmailException("encountered an error loading properties file", e);
    }
  } 

  private Properties fetchParameterStoreProperties(String url) throws GitEmailException {
    try {
      AWSSimpleSystemsManagement client= AWSSimpleSystemsManagementClientBuilder.defaultClient();
      GetParameterResult result = client.getParameter(new GetParameterRequest()
          .withName(url.replace(SSM_PS_URL_PREFIX, ""))
          .withWithDecryption(true));

      Properties props = new Properties();
      props.load(new ByteArrayInputStream(result.getParameter().getValue().getBytes("UTF-8")));
      return props;
    } catch (Exception e) {
      throw new GitEmailException("encountered an error loading properties from parameter store", e);
    }
  }

  private void loadProperties() throws GitEmailException {

    String workDirStr = getStringProperty("work.dir", "/var/tmp/git-notify/");
    workDir = new File(workDirStr);
    workDir.mkdirs();

    awsRegion = getStringProperty("aws.region", "us-east-1");

    timeZone = getStringProperty("time.zone", "UTC");

    emailFromAddr = getStringProperty("email.from.address", "");
    if (emailFromAddr.isEmpty()){
      throw new GitEmailException("missing required property [email.from.address]");
    }

    // SMTP
    String smtpHost = getStringProperty("smtp.host", "");
    if (smtpHost.isEmpty()) {
      throw new GitEmailException("missing required property [smtp.host]");
    }
    
    int smtpPort = getIntProperty("smtp.port", -1 );
    if (smtpPort <= 0) {
      throw new GitEmailException("missing required property [smtp.port]");
    }

    String smtpUser = getStringProperty("smtp.username", "");
    if (smtpUser.isEmpty()) {
      throw new GitEmailException("missing required property [smtp.username]");
    }

    String smtpPass = getStringProperty("smtp.password", "");
    if (smtpPass.isEmpty()) {
      throw new GitEmailException("missing required property [smtp.password]");
    }

    String smtpTS = getStringProperty("smtp.transport.strategy", "SMTP_TLS");

    mailer = MailerBuilder
      .withSMTPServer(smtpHost, smtpPort, smtpUser, smtpPass)
      .withTransportStrategy(TransportStrategy.valueOf(smtpTS))
      .withProperty("mail.smtp.starttls.enable", "true")
      .withProperty("mail.smtp.ssl.enable", "true")
      .withProperty("mail.smtp.auth", "true")
      .buildMailer();

    // Git General
    gitProvider = GitProvider.valueOf(getStringProperty("git.provider", "").toLowerCase());

    // Git Credentials
    String gitUser = getStringProperty("git.username", "");
    String gitPass = getStringProperty("git.password", "");
    gitCredentialsProvider = new UsernamePasswordCredentialsProvider(gitUser,gitPass);

    // Git Repo Configurations
    repoConfigurations = new ArrayList<>();
    String repoList = getStringProperty("git.repos", "");
    if (repoList.isEmpty()) {
      throw new GitEmailException("missing required property [git.repos]");
    }
    String[] repos = repoList.split(",");
    for (String repo : repos) {
      repoConfigurations.add(new RepoConfiguration(repo, props));
    }

  }

  private String getStringProperty( String key, String defaultValue ) {
    String value = props.getProperty(key);
    if ( value == null || value.trim().isEmpty()){
      return defaultValue;
    }
    return value.trim();
  }

  private int getIntProperty( String key, int defaultValue ) {
    try {
      return Integer.parseInt(props.getProperty(key));
    } catch( NumberFormatException e ) {
      return defaultValue;
    }
  }

}