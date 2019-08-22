/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.html;

import static j2html.TagCreator.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

import j2html.tags.ContainerTag;

public class GitFileDiffHtml implements GitHtmlI {

  private List<DiffEntry> diffEntries;

  public GitFileDiffHtml(List<DiffEntry> diffEntries) {
    this.diffEntries = diffEntries;
  }

  @Override
  public String toHtml() {

    List<DiffEntry> addEntries = new ArrayList<>();
    List<DiffEntry> copyEntries = new ArrayList<>();
    List<DiffEntry> deleteEntries = new ArrayList<>();
    List<DiffEntry> modifyEntries = new ArrayList<>();
    List<DiffEntry> renameEntries = new ArrayList<>();

    for (DiffEntry diff : diffEntries) {
      switch (diff.getChangeType()) {
      case ADD:
        addEntries.add(diff);
        break;
      case COPY:
        copyEntries.add(diff);
        break;
      case DELETE:
        deleteEntries.add(diff);
        break;
      case MODIFY:
        modifyEntries.add(diff);
        break;
      case RENAME:
        renameEntries.add(diff);
        break;
      }
    }

    ContainerTag dl = dl();
    if (!addEntries.isEmpty()) {
      dl.with(
        dt(b("Added:")),
        each(addEntries, addEntry ->
          dd( 
            join(
              " - ",
              addEntry.getNewPath().equals("/dev/null") ? addEntry.getOldPath() : addEntry.getNewPath() 
            )
          )
        )
      );
    }

    if (!modifyEntries.isEmpty()) {
      dl.with(
        dt(b("Modified:")),
        each(modifyEntries, modifyEntry ->
          dd( 
            join(
              " - ",
              modifyEntry.getNewPath().equals("/dev/null") ? modifyEntry.getOldPath() : modifyEntry.getNewPath() 
            )
          )
        )
      );
    }

    if (!deleteEntries.isEmpty()) {
      dl.with(
        dt(b("Deleted:")),
        each(deleteEntries, deleteEntry ->
          dd( 
            join(
              " - ",
              deleteEntry.getNewPath().equals("/dev/null") ? deleteEntry.getOldPath() : deleteEntry.getNewPath() 
            )
          )
        )
      );
    }

    if (!copyEntries.isEmpty()) {
      dl.with(
        dt(b("Copied:")),
        each(copyEntries, copyEntry ->
          dd( 
            join(
              " - ",
              copyEntry.getNewPath().equals("/dev/null") ? copyEntry.getOldPath() : copyEntry.getNewPath()
            )
          )
        )
      );
    }

    if (!renameEntries.isEmpty()) {
      dl.with(
        dt(b("Renamed:")),
        each(renameEntries, renameEntry ->
          dd( 
            join(
              " - ",
              renameEntry.getNewPath().equals("/dev/null") ? renameEntry.getOldPath() : renameEntry.getNewPath() 
            )
          )
        )
      );
    }

    return dl.render();

  }

}