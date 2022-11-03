package io.harness.gitsync.common.scmerrorhandling.handlers.github;

import static io.harness.exception.SCMExceptionErrorMessages.BRANCH_NOT_FOUND_ERROR;
import static io.harness.rule.OwnerRule.ADITHYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.scmerrorhandling.dtos.ErrorMetadata;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class GithubGetBranchHeadCommitScmApiErrorHandlerTest extends GitSyncTestBase {
  private static final String errorMessage = "errorMessage";
  @Inject GithubGetBranchHeadCommitScmApiErrorHandler githubGetBranchHeadCommitScmApiErrorHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthorizedResponse() {
    try {
      githubGetBranchHeadCommitScmApiErrorHandler.handleError(401, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthenticatedResponse() {
    try {
      githubGetBranchHeadCommitScmApiErrorHandler.handleError(403, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnResourceNotFoundResponse() {
    try {
      githubGetBranchHeadCommitScmApiErrorHandler.handleError(404, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnWrongBranchResponse() {
    try {
      githubGetBranchHeadCommitScmApiErrorHandler.handleError(422, errorMessage, ErrorMetadata.builder().build());
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmBadRequestException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(BRANCH_NOT_FOUND_ERROR);
    }
  }
}