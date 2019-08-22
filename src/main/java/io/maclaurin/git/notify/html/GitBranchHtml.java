/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.html;

import static j2html.TagCreator.*;

import java.util.ArrayList;
import java.util.List;

public class GitBranchHtml implements GitHtmlI {

  private List<GitCommitHtml> commits;
  
  public GitBranchHtml() {
    commits = new ArrayList<>();
  }

  public int getNumberOfCommits() {
    return commits.size();
  }

  public void addCommit(GitCommitHtml commitHtml) {
    commits.add(commitHtml);
  }

  @Override
  public String toHtml() {
    return div( 
      each(commits, commitHtml -> 
        div( rawHtml( commitHtml.toHtml() ), hr() )
      )
    ).render();
  }

}