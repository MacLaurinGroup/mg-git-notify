/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.html;

import static j2html.TagCreator.*;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import io.maclaurin.git.notify.commit.GitCommitUrlGenerator;
import j2html.tags.ContainerTag;

public class GitCommitHtml implements GitHtmlI {

  private RevCommit commit;

  private GitFileDiffHtml fileDiffHtml;

  private GitFileDiffSummaryHtml fileDiffSummaryHtml;

  private GitCommitUrlGenerator gitCommitUrlGenerator;

  public GitCommitHtml(RevCommit commit, GitCommitUrlGenerator gitCommitUrlGenerator) {
    this.commit = commit;
    this.gitCommitUrlGenerator = gitCommitUrlGenerator;
  }

  public void addFileDiffHtml(GitFileDiffHtml fileDiffHtml) {
    this.fileDiffHtml = fileDiffHtml;
  }

  public void addFileDiffSummaryHtml(GitFileDiffSummaryHtml fileDiffSummaryHtml) {
    this.fileDiffSummaryHtml = fileDiffSummaryHtml;
  }

  @Override
  public String toHtml() {

    PersonIdent authorIdent = commit.getAuthorIdent();
    PersonIdent committerIdent = commit.getCommitterIdent();

    ContainerTag commitName = dd();
    if (gitCommitUrlGenerator == null) {
      commitName.with(
        i(
          commit.name()
        )
      );
    } else {
      commitName.with(
        a(
          i(
            commit.name()
          )
        ).withHref( gitCommitUrlGenerator.getUrl( commit.name() ) ) );
    }

    return div(
      div(
        dl(
          dt(b("Commit: ")), 
          commitName,
          dt(b("Message: ")), 
          dd(i(commit.getFullMessage().trim())),
          dt(b("Author: ")), 
          dd(
            i(
              join(
                authorIdent.getName(), 
                " (",
                authorIdent.getEmailAddress(), 
                ") on ", 
                String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", authorIdent.getWhen())
              )
            )
          ),
          dt(b("Committer: ")), 
          dd(
            i(
              join(
                committerIdent.getName(), 
                " (",
                committerIdent.getEmailAddress(), 
                ") on ", 
                String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", committerIdent.getWhen())
              )
            )
          )
        )
      ),
      div(rawHtml(fileDiffSummaryHtml.toHtml())),
      div(rawHtml(fileDiffHtml.toHtml()))
    ).render();
  }

}