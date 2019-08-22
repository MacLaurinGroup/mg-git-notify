/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.config;

import java.util.Properties;

public class BranchConfiguration {

  private String branchName;

  private String[] subscriberEmails;

  protected BranchConfiguration(String repoName, String branchName, Properties props) {
    this.branchName = branchName;

    String emailList = props.getProperty("git." + repoName + "." + branchName + ".subscriber.emails");
    if( emailList == null || emailList.trim().isEmpty() ) {
      subscriberEmails = new String[0];
    } else {
      subscriberEmails = emailList.split(",");
    }
  }

  public String getBranchName() {
    return branchName;
  }

  public String[] getSubscriberEmails() {
    return subscriberEmails;
  }

  public boolean isMatch(String name) {
    if( name == null ) {
      return false;
    }
    
    if (branchName.equals("*")) {
      return true;
    }

    if (branchName.equalsIgnoreCase(name)) {
      return true;
    }

    if (branchName.startsWith("*") && branchName.endsWith("*")) {
      String searchTerm = branchName.substring(1, branchName.length() - 1);
      if (name.contains(searchTerm)) {
        return true;
      }
    } else if (branchName.startsWith("*")) {
      String searchTerm = branchName.substring(1);
      if (name.endsWith(searchTerm)) {
        return true;
      }
    } else if (branchName.endsWith("*")) {
      String searchTerm = branchName.substring(0, branchName.length() - 1);
      if (name.startsWith(searchTerm)){
        return true;
      }
    }

    return false;
  }
}