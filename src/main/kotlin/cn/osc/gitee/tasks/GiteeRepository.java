// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.tasks;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import cn.osc.gitee.api.GiteeApiRequestExecutor;
import cn.osc.gitee.api.GiteeApiRequests;
import cn.osc.gitee.api.GiteeServerPath;
import cn.osc.gitee.api.data.GiteeIssue;
import cn.osc.gitee.api.data.GiteeIssueBase;
import cn.osc.gitee.api.data.GiteeIssueCommentWithHtml;
import cn.osc.gitee.api.data.GiteeIssueState;
import cn.osc.gitee.api.util.GiteeApiPagesLoader;
import cn.osc.gitee.exceptions.GiteeAuthenticationException;
import cn.osc.gitee.exceptions.GiteeJsonException;
import cn.osc.gitee.exceptions.GiteeRateLimitExceededException;
import cn.osc.gitee.exceptions.GiteeStatusCodeException;
import cn.osc.gitee.issue.GiteeIssuesLoadingHelper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag("GitHub")
final class GiteeRepository extends BaseRepository {

  private Pattern myPattern = Pattern.compile("($^)");
  @NotNull private String myRepoAuthor = "";
  @NotNull private String myRepoName = "";
  @NotNull private String myUser = "";
  private boolean myAssignedIssuesOnly = false;

  @SuppressWarnings({"UnusedDeclaration"})
  GiteeRepository() {
  }

  GiteeRepository(GiteeRepository other) {
    super(other);
    setRepoName(other.myRepoName);
    setRepoAuthor(other.myRepoAuthor);
    setAssignedIssuesOnly(other.myAssignedIssuesOnly);
  }

  GiteeRepository(GiteeRepositoryType type) {
    super(type);
    setUrl("https://" + GiteeServerPath.DEFAULT_HOST);
  }

  @NotNull
  @Override
  public CancellableConnection createCancellableConnection() {
    return new CancellableConnection() {
      private final GiteeApiRequestExecutor myExecutor = getExecutor();
      private final ProgressIndicator myIndicator = new EmptyProgressIndicator();

      @Override
      protected void doTest() throws Exception {
        try {
          myExecutor.execute(myIndicator, GiteeApiRequests.Repos.get(getServer(), getRepoAuthor(), getRepoName()));
        }
        catch (ProcessCanceledException ignore) {
        }
      }

      @Override
      public void cancel() {
        myIndicator.cancel();
      }
    };
  }

  @Override
  public boolean isConfigured() {
    return super.isConfigured() &&
           !StringUtil.isEmptyOrSpaces(getRepoAuthor()) &&
           !StringUtil.isEmptyOrSpaces(getRepoName()) &&
           !StringUtil.isEmptyOrSpaces(getPassword());
  }

  @Override
  public String getPresentableName() {
    final String name = super.getPresentableName();
    return name +
           (!StringUtil.isEmpty(getRepoAuthor()) ? "/" + getRepoAuthor() : "") +
           (!StringUtil.isEmpty(getRepoName()) ? "/" + getRepoName() : "");
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed) throws Exception {
    try {
      return getIssues(query, offset + limit, withClosed);
    }
    catch (GiteeRateLimitExceededException e) {
      return Task.EMPTY_ARRAY;
    }
    catch (GiteeAuthenticationException | GiteeStatusCodeException e) {
      throw new Exception(e.getMessage(), e); // Wrap to show error message
    }
    catch (GiteeJsonException e) {
      throw new Exception("Bad response format", e);
    }
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled)
    throws Exception {
    return getIssues(query, offset, limit, withClosed);
  }

  private Task @NotNull [] getIssues(@Nullable String query, int max, boolean withClosed) throws Exception {
    GiteeApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    GiteeServerPath server = getServer();

    String assigned = null;
    if (myAssignedIssuesOnly) {
      if (StringUtil.isEmptyOrSpaces(myUser)) {
        myUser = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).getLogin();
      }
      assigned = myUser;
    }

    List<? extends GiteeIssueBase> issues;
    if (StringUtil.isEmptyOrSpaces(query)) {
      // search queries have way smaller request number limit
      issues = GiteeIssuesLoadingHelper.load(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, max, assigned);
    }
    else {
      issues = GiteeIssuesLoadingHelper.search(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, assigned, query);
    }
    List<Task> tasks = new ArrayList<>();

    for (GiteeIssueBase issue : issues) {
      List<GiteeIssueCommentWithHtml> comments = GiteeApiPagesLoader
        .loadAll(executor, indicator, GiteeApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
      tasks.add(createTask(issue, comments));
    }

    return tasks.toArray(Task.EMPTY_ARRAY);
  }

  @NotNull
  private Task createTask(@NotNull GiteeIssueBase issue, @NotNull List<GiteeIssueCommentWithHtml> comments) {
    return new Task() {
      @NotNull private final String myRepoName = getRepoName();
      private final Comment @NotNull [] myComments =
        ContainerUtil.map2Array(comments, Comment.class, comment -> new GiteeComment(comment.getCreatedAt(),
                                                                                      comment.getUser().getLogin(),
                                                                                      comment.getBodyHtml(),
                                                                                      comment.getUser().getAvatarUrl(),
                                                                                      comment.getUser().getHtmlUrl()));

      @Override
      public boolean isIssue() {
        return true;
      }

      @Override
      public String getIssueUrl() {
        return issue.getHtmlUrl();
      }

      @Override
      public @NlsSafe @NotNull String getId() {
        return myRepoName + "-" + issue.getNumber();
      }

      @NotNull
      @Override
      public String getSummary() {
        return issue.getTitle();
      }

      @Override
      public String getDescription() {
        return issue.getBody();
      }

      @Override
      public Comment @NotNull [] getComments() {
        return myComments;
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return AllIcons.Vcs.Vendors.Github;
      }

      @NotNull
      @Override
      public TaskType getType() {
        return TaskType.BUG;
      }

      @Override
      public Date getUpdated() {
        return issue.getUpdatedAt();
      }

      @Override
      public Date getCreated() {
        return issue.getCreatedAt();
      }

      @Override
      public boolean isClosed() {
        return issue.getState() == GiteeIssueState.closed;
      }

      @Override
      public TaskRepository getRepository() {
        return GiteeRepository.this;
      }

      @Override
      public String getPresentableName() {
        return getId() + ": " + getSummary();
      }
    };
  }

  @Override
  @Nullable
  public String extractId(@NotNull String taskName) {
    Matcher matcher = myPattern.matcher(taskName);
    return matcher.find() ? matcher.group(1) : null;
  }

  @Nullable
  @Override
  public Task findTask(@NotNull String id) throws Exception {
    final int index = id.lastIndexOf("-");
    if (index < 0) {
      return null;
    }
    final String numericId = id.substring(index + 1);
    GiteeApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    GiteeIssue issue = executor.execute(indicator,
                                         GiteeApiRequests.Repos.Issues.get(getServer(), getRepoAuthor(), getRepoName(), numericId));
    if (issue == null) return null;
    List<GiteeIssueCommentWithHtml> comments = GiteeApiPagesLoader
      .loadAll(executor, indicator, GiteeApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
    return createTask(issue, comments);
  }

  @Override
  public void setTaskState(@NotNull Task task, @NotNull TaskState state) throws Exception {
    boolean isOpen = switch (state) {
      case OPEN -> true;
      case RESOLVED -> false;
      default -> throw new IllegalStateException("Unknown state: " + state);
    };
    GiteeApiRequestExecutor executor = getExecutor();
    GiteeServerPath server = getServer();
    String repoAuthor = getRepoAuthor();
    String repoName = getRepoName();

    ProgressIndicator indicator = getProgressIndicator();
    executor.execute(indicator,
                     GiteeApiRequests.Repos.Issues.updateState(server, repoAuthor, repoName, task.getNumber(), isOpen));
  }

  @NotNull
  @Override
  public BaseRepository clone() {
    return new GiteeRepository(this);
  }

  public @NlsSafe @NotNull String getRepoName() {
    return myRepoName;
  }

  public void setRepoName(@NotNull String repoName) {
    myRepoName = repoName;
    myPattern = Pattern.compile("(" + StringUtil.escapeToRegexp(repoName) + "\\-\\d+)");
  }

  public @NlsSafe @NotNull String getRepoAuthor() {
    return myRepoAuthor;
  }

  public void setRepoAuthor(@NotNull String repoAuthor) {
    myRepoAuthor = repoAuthor;
  }

  public @NlsSafe @NotNull String getUser() {
    return myUser;
  }

  public void setUser(@NotNull String user) {
    myUser = user;
  }

  /**
   * Stores access token
   */
  @Override
  public void setPassword(String password) {
    super.setPassword(password);
    setUser("");
  }

  public boolean isAssignedIssuesOnly() {
    return myAssignedIssuesOnly;
  }

  public void setAssignedIssuesOnly(boolean value) {
    myAssignedIssuesOnly = value;
  }

  @Override
  @NotNull
  protected CredentialAttributes getAttributes() {
    String serviceName = CredentialAttributesKt.generateServiceName("Tasks", getRepositoryType().getName() + " " + getPresentableName());
    return new CredentialAttributes(serviceName, "GitHub OAuth token");
  }

  @NotNull
  private GiteeApiRequestExecutor getExecutor() {
    return GiteeApiRequestExecutor.Factory.getInstance().create(getPassword(), myUseProxy);
  }

  @NotNull
  private static ProgressIndicator getProgressIndicator() {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator == null) indicator = new EmptyProgressIndicator();
    return indicator;
  }

  @NotNull
  private GiteeServerPath getServer() {
    return GiteeServerPath.from(getUrl());
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof GiteeRepository)) return false;

    GiteeRepository that = (GiteeRepository)o;
    if (!Objects.equals(getRepoAuthor(), that.getRepoAuthor())) return false;
    if (!Objects.equals(getRepoName(), that.getRepoName())) return false;
    if (!Comparing.equal(isAssignedIssuesOnly(), that.isAssignedIssuesOnly())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return StringUtil.stringHashCode(getRepoName()) +
           31 * StringUtil.stringHashCode(getRepoAuthor());
  }

  @Override
  protected int getFeatures() {
    return super.getFeatures() | STATE_UPDATING;
  }
}
