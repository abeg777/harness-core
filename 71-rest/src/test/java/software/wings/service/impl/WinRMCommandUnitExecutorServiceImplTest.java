package software.wings.service.impl;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.rule.OwnerRule.SAHIL;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.PUBLIC_DNS;
import static software.wings.utils.WingsTestConstants.SSH_USER_NAME;
import static software.wings.utils.WingsTestConstants.SSH_USER_PASSWORD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Injector;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.TimeoutException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import java.util.HashMap;

public class WinRMCommandUnitExecutorServiceImplTest extends WingsBaseTest {
  private static final String EXEC_CMD = "ls";
  private static final SettingAttribute HOST_CONN_ATTR_PWD = aSettingAttribute()
                                                                 .withUuid(HOST_CONN_ATTR_ID)
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withAccessType(USER_PASSWORD)
                                                                                .withUserName(SSH_USER_NAME)
                                                                                .withSshPassword(SSH_USER_PASSWORD)
                                                                                .build())
                                                                 .build();

  private static final ExecCommandUnit EXEC_COMMAND_UNIT = anExecCommandUnit().withCommandString(EXEC_CMD).build();

  @Mock DelegateLogService delegateLogService;

  @Mock Injector injector;

  @Mock private WinRmExecutorFactory winRmExecutorFactory;

  @Mock private WinRmExecutor winRmExecutor;

  @InjectMocks
  private CommandUnitExecutorService winRMCommandUnitExecutorService = new WinRMCommandUnitExecutorServiceImpl();

  private Host.Builder builder = aHost().withAppId(APP_ID).withHostName(HOST_NAME).withPublicDns(PUBLIC_DNS);
  private CommandExecutionContext.Builder commandExecutionContextBuider =
      aCommandExecutionContext()
          .appId(APP_ID)
          .activityId(ACTIVITY_ID)
          .runtimePath("/tmp/runtime")
          .backupPath("/tmp/backup")
          .stagingPath("/tmp/staging")
          .executionCredential(aSSHExecutionCredential().withSshUser(SSH_USER_NAME).build())
          .artifactFiles(Lists.newArrayList(anArtifactFile().withName("artifact.war").withFileUuid(FILE_ID).build()))
          .serviceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "aSecret"))
          .safeDisplayServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "*****"))
          .host(builder.build())
          .accountId(ACCOUNT_ID);

  @Before
  public void setup() {
    on(winRMCommandUnitExecutorService).set("timeLimiter", new FakeTimeLimiter());
  }

  /**
   * test execute when username and password given in winrmConnectionAttributes
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteWithUserNamePassword() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(false)
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(false)
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();

    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    winRMCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but()
            .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .winRmConnectionAttributes(winRmConnectionAttributes)
            .build());

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
  }

  /**
   * test execute when keyTab given
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteWithKeyTab() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(true)
                                                              .keyTabFilePath("/etc/test.keyTab")
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(true)
                                                .keyTabFilePath("/etc/test.keyTab")
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();

    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    winRMCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but()
            .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .winRmConnectionAttributes(winRmConnectionAttributes)
            .build());

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
  }

  /**
   * test execute when it throws TimeOutException
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteTimeOutException() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(true)
                                                              .keyTabFilePath("/etc/test.keyTab")
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(true)
                                                .keyTabFilePath("/etc/test.keyTab")
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();
    CommandExecutionContext commandExecutionContext = commandExecutionContextBuider.but()
                                                          .timeout(0)
                                                          .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
                                                          .winRmConnectionAttributes(winRmConnectionAttributes)
                                                          .build();
    CommandUnit commandUnit = mock(CommandUnit.class);

    when(commandUnit.execute(any()))
        .thenThrow(new TimeoutException(
            "Timed out waiting for tasks to be in running state", "Timeout", null, WingsException.SRE));
    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> winRMCommandUnitExecutorService.execute(commandUnit, commandExecutionContext))
        .withMessage("UNKNOWN_ERROR");

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
    verify(delegateLogService)
        .save(ACCOUNT_ID,
            aLog()
                .withAppId(APP_ID)
                .withHostName(PUBLIC_DNS)
                .withActivityId(ACTIVITY_ID)
                .withLogLevel(INFO)
                .withCommandUnitName(commandUnit.getName())
                .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
                .withExecutionResult(RUNNING)
                .build());
    verify(delegateLogService)
        .save(ACCOUNT_ID,
            aLog()
                .withAppId(APP_ID)
                .withActivityId(ACTIVITY_ID)
                .withHostName(PUBLIC_DNS)
                .withLogLevel(ERROR)
                .withLogLine("Command execution failed")
                .withCommandUnitName(commandUnit.getName())
                .withExecutionResult(FAILURE)
                .build());
  }

  /**
   * test execute when it throws WingsException
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteWingsException() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(true)
                                                              .keyTabFilePath("/etc/test.keyTab")
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(true)
                                                .keyTabFilePath("/etc/test.keyTab")
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();
    CommandExecutionContext commandExecutionContext = commandExecutionContextBuider.but()
                                                          .timeout(0)
                                                          .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
                                                          .winRmConnectionAttributes(winRmConnectionAttributes)
                                                          .build();
    CommandUnit commandUnit = mock(CommandUnit.class);

    when(commandUnit.execute(any()))
        .thenThrow(new WingsException(ErrorCode.INVALID_KEY, "Test error", WingsException.USER_SRE));
    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> winRMCommandUnitExecutorService.execute(commandUnit, commandExecutionContext))
        .withMessage("Test error");

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
  }

  /**
   * test execute when it throws InterruptedException
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteInterruptedException() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(true)
                                                              .keyTabFilePath("/etc/test.keyTab")
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(true)
                                                .keyTabFilePath("/etc/test.keyTab")
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();
    CommandExecutionContext commandExecutionContext = commandExecutionContextBuider.but()
                                                          .timeout(0)
                                                          .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
                                                          .winRmConnectionAttributes(winRmConnectionAttributes)
                                                          .build();
    CommandUnit commandUnit = mock(CommandUnit.class);

    when(commandUnit.execute(any())).thenThrow(new NullPointerException("Test Exception"));
    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> winRMCommandUnitExecutorService.execute(commandUnit, commandExecutionContext))
        .withMessage("UNKNOWN_ERROR");

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
  }

  /**
   * test execute when it throws UncheckedTimeOutException
   */
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteUnCheckedTimeoutException() {
    WinRmConnectionAttributes winRmConnectionAttributes = WinRmConnectionAttributes.builder()
                                                              .domain("TEST.LOCAL")
                                                              .username("testUser")
                                                              .password(new char[0])
                                                              .useKeyTab(true)
                                                              .keyTabFilePath("/etc/test.keyTab")
                                                              .build();
    WinRmSessionConfig winRmSessionConfig = WinRmSessionConfig.builder()
                                                .accountId(ACCOUNT_ID)
                                                .environment(new HashMap<>())
                                                .appId(APP_ID)
                                                .executionId(ACTIVITY_ID)
                                                .hostname(PUBLIC_DNS)
                                                .domain("TEST.LOCAL")
                                                .username("testUser")
                                                .password("")
                                                .useKeyTab(true)
                                                .keyTabFilePath("/etc/test.keyTab")
                                                .port(0)
                                                .useSSL(false)
                                                .skipCertChecks(false)
                                                .workingDirectory(null)
                                                .build();
    CommandExecutionContext commandExecutionContext = commandExecutionContextBuider.but()
                                                          .timeout(0)
                                                          .hostConnectionAttributes(HOST_CONN_ATTR_PWD)
                                                          .winRmConnectionAttributes(winRmConnectionAttributes)
                                                          .build();
    CommandUnit commandUnit = mock(CommandUnit.class);

    when(commandUnit.execute(any())).thenThrow(new UncheckedTimeoutException());
    when(winRmExecutorFactory.getExecutor(winRmSessionConfig, false)).thenReturn(winRmExecutor);

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> winRMCommandUnitExecutorService.execute(commandUnit, commandExecutionContext))
        .withMessage("SOCKET_CONNECTION_TIMEOUT");

    verify(winRmExecutorFactory).getExecutor(winRmSessionConfig, false);
  }
}