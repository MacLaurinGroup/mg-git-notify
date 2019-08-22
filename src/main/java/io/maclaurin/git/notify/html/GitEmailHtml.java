/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.html;

import static j2html.TagCreator.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.stream.Collectors;

import io.maclaurin.git.notify.exception.GitEmailException;

public class GitEmailHtml implements GitHtmlI {

  private GitBranchHtml branchHtml;

  private String repoName,branchName;

  private Date sinceDt, untilDt;

  private String css;

  public GitEmailHtml(String repoName, String branchName, GitBranchHtml branchHtml, Date sinceDt, Date untilDt) throws GitEmailException {
    this.repoName = repoName;
    this.branchName = branchName;
    this.branchHtml = branchHtml;
    this.sinceDt = sinceDt;
    this.untilDt = untilDt;

    try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("email.css"), "UTF-8"))) {
      css = br.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch(IOException e) {
      throw new GitEmailException("failed to load email css", e);
    }
  }

  @Override
  public String toHtml() {
    
    return html(

      head(
        style(rawHtml(css)).withType("text/css")
      ),

      body(

        div(
          h1("Daily Git Summary")
        ).withClass("title"),

        div(
          dl(
            dt(b("Repository:")),
            dd(i(repoName)),
            dt(b("Branch:")),
            dd(i(branchName)),
            dt(b("Summary Start Date:")),
            dd(i(String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", sinceDt))),
            dt(b("Summary End Date:")),
            dd(i(String.format("%1$tB %1$te, %1$tY at %1$tH:%1$tM:%1$tS (%1$tZ)", untilDt)))
          ).withClass("menu-container"),
          hr(),
          h2("Commits"),
          rawHtml(branchHtml.toHtml())
        ).withClass("body-container"),

        footer(
          p(
            join(
              "Git Notify built by MacLaurin Group | \u00a9 2019 MacLaurin Group | ",
              a("maclaurin.group").withHref("https://maclaurin.group/")
            )
          )
        )
      )
    ).render();
    
  }
}