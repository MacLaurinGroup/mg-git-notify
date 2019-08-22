/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.maclaurin.git.notify.exception.GitEmailException;

public class RepoConfiguration {

  private String repoName;

  private String cloneUrl;

  private List<BranchConfiguration> branchConfigurations;
  
  protected RepoConfiguration( String repoName, Properties props ) throws GitEmailException {
    this.repoName = repoName;

    cloneUrl = props.getProperty("git." + repoName + ".clone_url");
    if (cloneUrl == null || cloneUrl.trim().isEmpty()) {
      throw new GitEmailException("missing required property [git." + repoName + ".clone_url]");
    }
    cloneUrl = cloneUrl.trim();

    String branchList = props.getProperty("git." + repoName + ".branches");
    if (branchList == null || branchList.trim().isEmpty()){
      throw new GitEmailException("missing required property [git." + repoName + ".branches]");
    }

    branchConfigurations = new ArrayList<>();
    String[] branches = branchList.split(",");
    for (String branch : branches) {
      branchConfigurations.add(new BranchConfiguration(repoName, branch, props));
    }
  }

  public String getRepoName() {
    return repoName;
  }

  public String getCloneUrl() {
    return cloneUrl;
  }

  public List<BranchConfiguration> getBranchConfigurations() {
    return branchConfigurations;
  }

}