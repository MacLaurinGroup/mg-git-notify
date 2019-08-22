package io.maclaurin.git.notify.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class BranchConfigurationTest {

  @Test
  public void testExactMatch() {
    BranchConfiguration bConfig = new BranchConfiguration("repoName", "test", new Properties());
    assertTrue("exact match test failure", bConfig.isMatch("test"));
    assertFalse("exact match test failure", bConfig.isMatch("test1"));
    assertFalse("exact match test failure", bConfig.isMatch("*"));
    assertFalse("exact match test failure", bConfig.isMatch("tes"));
    assertFalse("exact match test failure", bConfig.isMatch(""));
    assertFalse("exact match test failure", bConfig.isMatch(null));
    assertFalse("exact match test failure", bConfig.isMatch("refs/remote/origin/test"));
  }

  @Test
  public void testEverythingMatch() {
    BranchConfiguration bConfig = new BranchConfiguration("repoName", "*", new Properties());
    assertTrue("everything match test failure", bConfig.isMatch("test"));
    assertTrue("everything match test failure", bConfig.isMatch("test1"));
    assertTrue("everything match test failure", bConfig.isMatch("*"));
    assertTrue("everything match test failure", bConfig.isMatch("tes"));
    assertTrue("everything match test failure", bConfig.isMatch(""));
    assertFalse("everything match test failure", bConfig.isMatch(null));
    assertTrue("everything match test failure", bConfig.isMatch("refs/remote/origin/test"));
  }

  @Test
  public void testPrefixMatch() {
    BranchConfiguration bConfig = new BranchConfiguration("repoName", "test*", new Properties());
    assertTrue("prefix match test failure", bConfig.isMatch("test"));
    assertTrue("prefix match test failure", bConfig.isMatch("test1"));
    assertFalse("prefix match test failure", bConfig.isMatch("*"));
    assertFalse("prefix match test failure", bConfig.isMatch("tes"));
    assertFalse("prefix match test failure", bConfig.isMatch(""));
    assertFalse("prefix match test failure", bConfig.isMatch(null));
    assertFalse("prefix match test failure", bConfig.isMatch("refs/remote/origin/test"));
  }

  @Test
  public void testSuffixMatch() {
    BranchConfiguration bConfig = new BranchConfiguration("repoName", "*test", new Properties());
    assertTrue("suffix match test failure", bConfig.isMatch("test"));
    assertFalse("suffix match test failure", bConfig.isMatch("test1"));
    assertFalse("suffix match test failure", bConfig.isMatch("*"));
    assertFalse("suffix match test failure", bConfig.isMatch("tes"));
    assertFalse("suffix match test failure", bConfig.isMatch(""));
    assertFalse("suffix match test failure", bConfig.isMatch(null));
    assertTrue("suffix match test failure", bConfig.isMatch("refs/remote/origin/test"));
  }

  @Test
  public void testContainsMatch() {
    BranchConfiguration bConfig = new BranchConfiguration("repoName", "*test*", new Properties());
    assertTrue("everything match test failure", bConfig.isMatch("test"));
    assertTrue("everything match test failure", bConfig.isMatch("test1"));
    assertFalse("everything match test failure", bConfig.isMatch("*"));
    assertFalse("everything match test failure", bConfig.isMatch("tes"));
    assertFalse("everything match test failure", bConfig.isMatch(""));
    assertFalse("everything match test failure", bConfig.isMatch(null));
    assertTrue("everything match test failure", bConfig.isMatch("refs/remote/origin/test"));
    assertTrue("everything match test failure", bConfig.isMatch("refs/remote/origin/test1"));
  }
  
}