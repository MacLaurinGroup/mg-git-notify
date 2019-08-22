/**
 * (c) MacLaurin Group 2019
 */
package io.maclaurin.git.notify.commit;

public class CodeBuildGitCommitUrlGenerator implements GitCommitUrlGenerator {

	private static final String BASE_URL = "https://console.aws.amazon.com/codesuite/codecommit/repositories/";
	private String repoName, awsRegion;
	
	public CodeBuildGitCommitUrlGenerator(String repoName, String awsRegion) {
		this.repoName = repoName;
		this.awsRegion = awsRegion;
	}

	@Override
	public String getUrl(String commitId) {
		return String.format("%1$s%2$s/commit/%3$s?region=%4$s",BASE_URL,repoName,commitId,awsRegion);
	}
	 
}