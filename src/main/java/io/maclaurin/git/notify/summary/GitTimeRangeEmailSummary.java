/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.summary;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.RevisionSyntaxException;

import io.maclaurin.git.notify.commit.CodeBuildGitCommitUrlGenerator;
import io.maclaurin.git.notify.commit.GitCommitUrlGenerator;
import io.maclaurin.git.notify.config.BranchConfiguration;
import io.maclaurin.git.notify.config.GlobalConfiguration;
import io.maclaurin.git.notify.config.RepoConfiguration;
import io.maclaurin.git.notify.exception.GitEmailException;
import io.maclaurin.git.notify.html.GitBranchHtml;
import io.maclaurin.git.notify.html.GitCommitHtml;
import io.maclaurin.git.notify.html.GitEmailHtml;
import io.maclaurin.git.notify.html.GitFileDiffHtml;
import io.maclaurin.git.notify.html.GitFileDiffSummaryHtml;

import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitTimeRangeEmailSummary {

  private static final Logger LOG = LoggerFactory.getLogger(GitTimeRangeEmailSummary.class);

  private GlobalConfiguration globalConfig;

  private Date since, until;

  public GitTimeRangeEmailSummary(GlobalConfiguration globalConfig, Date since, Date until) {
    this.globalConfig = globalConfig;
    this.since = since;
    this.until = until;
  }

  public void send() {
    for (RepoConfiguration repoConfig : globalConfig.getRepoConfigurations()) {
      try (Git git = initGit(repoConfig)) {

        LOG.info("processing git repo: " + repoConfig.getRepoName() + " (" + repoConfig.getCloneUrl() + ")");
        
        List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();
        for (Ref branch : branches) {
          processBranch(repoConfig, git, branch);
        }

      } catch(Exception e) {
        LOG.error("failed to process repository: " + repoConfig.getRepoName() + " (" + repoConfig.getCloneUrl() + ")", e);
      }
    }
  }

  private Git initGit(RepoConfiguration repoConfig) throws GitAPIException, IOException {
    String gitDirName = repoConfig.getCloneUrl().substring(repoConfig.getCloneUrl().lastIndexOf("/"));
    File gitDir = new File(globalConfig.getWorkDir(), gitDirName);
    gitDir.mkdirs();
    File gitFile = new File(gitDir, ".git");

    Git git;
    if (!gitFile.exists()) {
            
      LOG.info("git repo must be checked out" );
      git = Git.cloneRepository()
              .setURI(repoConfig.getCloneUrl())
              .setCredentialsProvider(globalConfig.getGitCredentialsProvider())
              .setDirectory(gitDir)
              .setCloneAllBranches(true)
              .call();

    } else {

      LOG.info("git repo already checked out");
      git = Git.open(gitFile);

      git.fetch()
        .setCredentialsProvider(globalConfig.getGitCredentialsProvider())
        .call();

    }

    return git;
  }

  private void processBranch(RepoConfiguration repoConfig, Git git, Ref branch) throws GitEmailException {
    if (!branch.getName().startsWith("refs/remotes/origin/")) {
      LOG.info("skipping over non-remote origin branch: " + branch.getName());
      return;
    }

    String shortBranchName = branch.getName().substring("refs/remotes/origin/".length());
    BranchConfiguration matchingBranchConfig = null;
    for (BranchConfiguration branchConfig : repoConfig.getBranchConfigurations()) {
      if (branchConfig.isMatch(shortBranchName)) {
        matchingBranchConfig = branchConfig;
        break;
      }
    }

    if (matchingBranchConfig == null) {
      LOG.info("skipping over branch:" + branch.getName());
      return;
    }
    
    sendForBranch(git, repoConfig.getRepoName(), branch, matchingBranchConfig.getSubscriberEmails());
    LOG.info("finished processing branch: " + branch.getName());
  }

  public void sendForBranch(Git git, String repoName, Ref branch, String[] emailAddresses) throws GitEmailException {
    try {

      GitBranchHtml branchHtml = new GitBranchHtml();
      Repository repository = git.getRepository();

      Iterable<RevCommit> commits = git.log()
          .setRevFilter(CommitTimeRevFilter.between(since, until))
          .add(repository.resolve(branch.getName()))
          .call();
      
      processCommits(commits, branchHtml, git, repoName);
      
      if (branchHtml.getNumberOfCommits() == 0) {
        LOG.info("branch [" + branch.getName() + "] has no commits in the specified time range");
        return;
      }

      String emailSubject = new StringBuilder("Git Summary - ")
        .append(repoName)
        .append(":")
        .append(branch.getName().replace("refs/remotes/origin/",""))
        .append(" (")
        .append(String.format("%1$tY-%1$tm-%1$td", since))
        .append(" to ")
        .append(String.format("%1$tY-%1$tm-%1$td", until))
        .append(")")
        .toString();

      String emailHtml = new GitEmailHtml(repoName, branch.getName(), branchHtml, since, until).toHtml();
      
      for (String emailAddr : emailAddresses){
        Email email = EmailBuilder.startingBlank()
          .to(emailAddr)
          .from(globalConfig.getEmailFromAddr())
          .withSubject(emailSubject)
          .withHTMLText(emailHtml)
          .buildEmail();

        globalConfig.getMailer().sendMail(email);
        LOG.info("sending summary email for branch [" + branch.getName() + "] to email [" + emailAddr + "]");
      }

    } catch (GitAPIException | RevisionSyntaxException | IOException e) {
      throw new GitEmailException("encountered an error processing branch", e);
    }
  }

  private void processCommits(Iterable<RevCommit> commits, GitBranchHtml branchHtml, Git git, String repoName) throws GitAPIException, IOException {
    
    GitCommitUrlGenerator gitCommitUrlGenerator = null;
    switch (globalConfig.getGitProvider()) {
      case codebuild:
        gitCommitUrlGenerator = new CodeBuildGitCommitUrlGenerator(repoName, globalConfig.getAwsRegion());
        break;
    }

    for (RevCommit commit : commits) {

      GitCommitHtml commitHtml = new GitCommitHtml(commit, gitCommitUrlGenerator);
      branchHtml.addCommit(commitHtml);

      try (ObjectReader reader = git.getRepository().newObjectReader()) {

        AbstractTreeIterator oldTreeIter;
        if (commit.getParentCount() == 0) {
          oldTreeIter = new EmptyTreeIterator();
        } else {
          oldTreeIter = new CanonicalTreeParser();
          ((CanonicalTreeParser) oldTreeIter).reset(reader, commit.getParent(0).getTree());
        }

        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, commit.getTree());

        List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
        commitHtml.addFileDiffSummaryHtml(new GitFileDiffSummaryHtml(diffs));
        commitHtml.addFileDiffHtml(new GitFileDiffHtml(diffs));

      }

    }

  }

}