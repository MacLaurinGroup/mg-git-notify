/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.html;

import static j2html.TagCreator.*;

import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

public class GitFileDiffSummaryHtml implements GitHtmlI {

  private List<DiffEntry> diffEntries;

  public GitFileDiffSummaryHtml(List<DiffEntry> diffEntries) {
    this.diffEntries = diffEntries;
  }

  @Override
  public String toHtml() {

    int addCnt = 0;
    int copyCnt = 0;
    int deleteCnt = 0;
    int modifyCnt = 0;
    int renameCnt = 0;

    for (DiffEntry diff : diffEntries) {
      switch (diff.getChangeType()) {
      case ADD:
        addCnt++;
        break;
      case COPY:
        copyCnt++;
        break;
      case DELETE:
        deleteCnt++;
        break;
      case MODIFY:
        modifyCnt++;
        break;
      case RENAME:
        renameCnt++;
        break;
      }
    }

    return table(tr(th("Added"), th("Modified"), th("Deleted"), th("Copied"), th("Renamed")),
        tr(td(String.valueOf(addCnt)), td(String.valueOf(modifyCnt)), td(String.valueOf(deleteCnt)),
            td(String.valueOf(copyCnt)), td(String.valueOf(renameCnt)))).render();
  }

}