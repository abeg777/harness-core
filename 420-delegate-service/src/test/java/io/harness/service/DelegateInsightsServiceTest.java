package io.harness.service;

import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateInsightsServiceImpl;

import software.wings.beans.DelegateInsightsBarDetails;
import software.wings.beans.DelegateInsightsDetails;
import software.wings.beans.DelegateInsightsSummary;
import software.wings.beans.DelegateInsightsType;
import software.wings.beans.DelegateTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights.DelegateTaskUsageInsightsKeys;
import software.wings.beans.DelegateTaskUsageInsightsEventType;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateInsightsServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";
  private static final String TEST_TASK_ID = "testTaskId";
  private static final String TEST_DELEGATE_ID = "testDelegateId";
  private static final String TEST_DELEGATE_GROUP_ID = "testDelegateGroupId";

  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks @Inject private DelegateInsightsServiceImpl delegateInsightsService;
  @Inject private HPersistence persistence;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskAssignedFeatureFlagDisabled() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(false);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNull();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskAssignedValidValues() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsCreateEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.STARTED);
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.UNKNOWN);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedValidValuesSucceeded() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.SUCCEEDED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsSucceededEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.SUCCEEDED);

    assertThat(delegateTaskUsageInsightsSucceededEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsSucceededEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.SUCCEEDED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedValidValuesFailed() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.FAILED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsFailedEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.FAILED);

    assertThat(delegateTaskUsageInsightsFailedEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsFailedEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.FAILED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedFeatureFlagDisabled() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.FAILED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsFailedEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.FAILED);

    assertThat(delegateTaskUsageInsightsFailedEvent).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRetrieveDelegateInsightsDetailsWithFFDisabled() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateGroupId = UUIDGenerator.generateUuid();
    long timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, accountId)).thenReturn(false);

    DelegateInsightsSummary insightsSummary1 =
        DelegateInsightsSummary.builder()
            .accountId(accountId)
            .insightsType(DelegateInsightsType.SUCCESSFUL)
            .delegateGroupId(delegateGroupId)
            .periodStartTime(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(8))
            .count(1)
            .build();
    persistence.save(insightsSummary1);

    DelegateInsightsDetails delegateInsightsDetails =
        delegateInsightsService.retrieveDelegateInsightsDetails(accountId, delegateGroupId, timestamp);
    assertThat(delegateInsightsDetails).isNotNull();
    assertThat(delegateInsightsDetails.getInsights()).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRetrieveDelegateInsightsDetailsWithFFEnabled() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateGroupId = UUIDGenerator.generateUuid();
    long timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, accountId)).thenReturn(true);

    // Insights Bar 1
    long bar1Timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(8);
    DelegateInsightsSummary insightsSummary1 = DelegateInsightsSummary.builder()
                                                   .accountId(accountId)
                                                   .insightsType(DelegateInsightsType.SUCCESSFUL)
                                                   .delegateGroupId(delegateGroupId)
                                                   .periodStartTime(bar1Timestamp)
                                                   .count(1)
                                                   .build();
    persistence.save(insightsSummary1);

    DelegateInsightsSummary insightsSummary2 = DelegateInsightsSummary.builder()
                                                   .accountId(accountId)
                                                   .insightsType(DelegateInsightsType.FAILED)
                                                   .delegateGroupId(delegateGroupId)
                                                   .periodStartTime(bar1Timestamp)
                                                   .count(2)
                                                   .build();
    persistence.save(insightsSummary2);

    // Insights Bar 2
    long bar2Timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
    DelegateInsightsSummary insightsSummary3 = DelegateInsightsSummary.builder()
                                                   .accountId(accountId)
                                                   .insightsType(DelegateInsightsType.IN_PROGRESS)
                                                   .delegateGroupId(delegateGroupId)
                                                   .periodStartTime(bar2Timestamp)
                                                   .count(3)
                                                   .build();
    persistence.save(insightsSummary3);

    // Summary that should not be fetched
    DelegateInsightsSummary insightsSummary4 =
        DelegateInsightsSummary.builder()
            .accountId(accountId)
            .insightsType(DelegateInsightsType.IN_PROGRESS)
            .delegateGroupId(delegateGroupId)
            .periodStartTime(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(61))
            .count(4)
            .build();
    persistence.save(insightsSummary4);

    DelegateInsightsDetails delegateInsightsDetails =
        delegateInsightsService.retrieveDelegateInsightsDetails(accountId, delegateGroupId, timestamp);
    assertThat(delegateInsightsDetails).isNotNull();
    assertThat(delegateInsightsDetails.getInsights()).hasSize(2);
    assertThat(delegateInsightsDetails.getInsights().stream().allMatch(
                   barInfo -> ImmutableList.of(bar1Timestamp, bar2Timestamp).contains(barInfo.getTimeStamp())))
        .isTrue();

    for (DelegateInsightsBarDetails barDetails : delegateInsightsDetails.getInsights()) {
      if (barDetails.getTimeStamp() == bar1Timestamp) {
        assertThat(barDetails.getCounts()).hasSize(2);
        assertThat(barDetails.getCounts().get(0).getLeft())
            .isIn(DelegateInsightsType.SUCCESSFUL, DelegateInsightsType.FAILED);
        assertThat(barDetails.getCounts().get(0).getRight()).isIn(1L, 2L);
        assertThat(barDetails.getCounts().get(1).getLeft())
            .isIn(DelegateInsightsType.SUCCESSFUL, DelegateInsightsType.FAILED);
        assertThat(barDetails.getCounts().get(1).getRight()).isIn(1L, 2L);
      } else {
        assertThat(barDetails.getCounts()).hasSize(1);
        assertThat(barDetails.getCounts().get(0).getLeft()).isEqualTo(DelegateInsightsType.IN_PROGRESS);
        assertThat(barDetails.getCounts().get(0).getRight()).isEqualTo(3L);
      }
    }
  }

  private DelegateTaskUsageInsights getDefaultDelegateTaskUsageInsightsFromDB(
      DelegateTaskUsageInsightsEventType eventType) {
    return persistence.createQuery(DelegateTaskUsageInsights.class)
        .filter(DelegateTaskUsageInsightsKeys.accountId, TEST_ACCOUNT_ID)
        .filter(DelegateTaskUsageInsightsKeys.taskId, TEST_TASK_ID)
        .filter(DelegateTaskUsageInsightsKeys.delegateId, TEST_DELEGATE_ID)
        .filter(DelegateTaskUsageInsightsKeys.delegateGroupId, TEST_DELEGATE_GROUP_ID)
        .filter(DelegateTaskUsageInsightsKeys.eventType, eventType)
        .get();
  }
}
