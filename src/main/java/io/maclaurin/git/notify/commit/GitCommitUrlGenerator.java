/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.commit;

public interface GitCommitUrlGenerator {

  public String getUrl( String commitId );
  
}