/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.states.V1;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.FeatureName.CIE_HOSTED_VMS;
import static io.harness.beans.outcomes.LiteEnginePodDetailsOutcome.POD_DETAILS_OUTCOME;
import static io.harness.beans.outcomes.VmDetailsOutcome.VM_DETAILS_OUTCOME;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.INITIALIZE_EXECUTION;
import static io.harness.ci.commonconstants.CIExecutionConstants.MAXIMUM_EXPANSION_LIMIT;
import static io.harness.ci.commonconstants.CIExecutionConstants.MAXIMUM_EXPANSION_LIMIT_FREE_ACCOUNT;
import static io.harness.ci.states.InitializeTaskStep.TASK_BUFFER_TIMEOUT_MILLIS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;
import static io.harness.delegate.beans.ci.CIInitializeTaskParams.Type.DLITE_VM;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.steps.StepUtils.buildAbstractions;
import static io.harness.steps.StepUtils.generateLogAbstractions;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.dependencies.ServiceDependency;
import io.harness.beans.environment.ServiceDefinitionInfo;
import io.harness.beans.outcomes.DependencyOutcome;
import io.harness.beans.outcomes.LiteEnginePodDetailsOutcome;
import io.harness.beans.outcomes.VmDetailsOutcome;
import io.harness.beans.outcomes.VmDetailsOutcome.VmDetailsOutcomeBuilder;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.sweepingoutputs.InitializeExecutionSweepingOutput;
import io.harness.beans.yaml.extended.infrastrucutre.DockerInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.HostedVmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.buildstate.BuildSetupUtils;
import io.harness.ci.buildstate.ConnectorUtils;
import io.harness.ci.execution.BackgroundTaskUtility;
import io.harness.ci.ff.CIFeatureFlagService;
import io.harness.ci.integrationstage.DockerInitializeTaskParamsBuilder;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.ci.integrationstage.K8InitializeServiceUtils;
import io.harness.ci.integrationstage.VmInitializeTaskParamsBuilder;
import io.harness.ci.license.CILicenseService;
import io.harness.ci.states.CIDelegateTaskExecutor;
import io.harness.ci.utils.CIStagePlanCreationUtils;
import io.harness.ci.validation.CIYAMLSanitizationService;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.vm.VmServiceStatus;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.task.HDelegateTask;
import io.harness.encryption.Scope;
import io.harness.eraro.Level;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.exception.ngexception.CILiteEngineException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.helper.SerializedResponseDataHelper;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.repositories.CIAccountExecutionMetadataRepository;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.executable.AsyncExecutableWithRbac;
import io.harness.steps.matrix.ExpandedExecutionWrapperInfo;
import io.harness.steps.matrix.StrategyExpansionData;
import io.harness.steps.matrix.StrategyHelper;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.SerializationFormat;
import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CI)
public class InitializeTaskStepV2 implements AsyncExecutableWithRbac<StepElementParameters> {
  @Inject private ExceptionManager exceptionManager;

  @Inject private CIDelegateTaskExecutor ciDelegateTaskExecutor;

  @Inject private ConnectorUtils connectorUtils;
  @Inject private CIFeatureFlagService ciFeatureFlagService;
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private SerializedResponseDataHelper serializedResponseDataHelper;
  @Inject private K8InitializeServiceUtils k8InitializeServiceUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private VmInitializeTaskParamsBuilder vmInitializeTaskParamsBuilder;
  @Inject private DockerInitializeTaskParamsBuilder dockerInitializeTaskParamsBuilder;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private CIYAMLSanitizationService sanitizationService;
  @Inject private BackgroundTaskUtility backgroundTaskUtility;
  @Inject private CILicenseService ciLicenseService;
  @Inject private StrategyHelper strategyHelper;
  @Inject CIAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  private static final String DEPENDENCY_OUTCOME = "dependencies";

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return null;
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepElementParameters stepParameters, AsyncExecutableResponse executableResponse) {}

  @Override
  public AsyncExecutableResponse executeAsyncAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    log.info("start executeAsyncAfterRbac for initialize step");
    InitializeStepInfo initializeStepInfo = (InitializeStepInfo) stepParameters.getSpec();

    String logPrefix = getLogPrefix(ambiance);
    CIStagePlanCreationUtils.validateFreeAccountStageExecutionLimit(
        accountExecutionMetadataRepository, ciLicenseService, AmbianceUtils.getAccountId(ambiance));

    populateStrategyExpansion(initializeStepInfo, ambiance);
    CIInitializeTaskParams buildSetupTaskParams =
        buildSetupUtils.getBuildSetupTaskParams(initializeStepInfo, ambiance, logPrefix);
    boolean executeOnHarnessHostedDelegates = false;
    boolean emitEvent = false;
    String stageId = ambiance.getStageExecutionId();
    List<TaskSelector> taskSelectors = new ArrayList<>();

    // Secrets are in decrypted format for DLITE_VM type
    if (buildSetupTaskParams.getType() != DLITE_VM) {
      log.info("Created params for build task: {}", buildSetupTaskParams);
    }
    if (buildSetupTaskParams.getType() == DLITE_VM) {
      HostedVmInfraYaml hostedVmInfraYaml = (HostedVmInfraYaml) initializeStepInfo.getInfrastructure();
      String platformSelector = vmInitializeTaskParamsBuilder.getHostedPoolId(
          hostedVmInfraYaml.getSpec().getPlatform(), AmbianceUtils.getAccountId(ambiance));
      TaskSelector taskSelector = TaskSelector.newBuilder().setSelector(platformSelector).build();
      taskSelectors.add(taskSelector);
      executeOnHarnessHostedDelegates = true;

      emitEvent = true;
    } else if (initializeStepInfo.getInfrastructure().getType() == Infrastructure.Type.DOCKER) {
      DockerInfraYaml dockerInfraYaml = (DockerInfraYaml) initializeStepInfo.getInfrastructure();
      String platformSelector =
          dockerInitializeTaskParamsBuilder.getHostedPoolId(dockerInfraYaml.getSpec().getPlatform());
      TaskSelector taskSelector = TaskSelector.newBuilder().setSelector(platformSelector).build();
      taskSelectors.add(taskSelector);
      // TODO: start emitting & processing event for Docker as well
      // emitEvent = true;
    }

    TaskData taskData = getTaskData(stepParameters, buildSetupTaskParams);
    String accountId = AmbianceUtils.getAccountId(ambiance);

    Map<String, String> abstractions = buildAbstractions(ambiance, Scope.PROJECT);

    HDelegateTask task = (HDelegateTask) StepUtils.prepareDelegateTaskInput(
        accountId, taskData, abstractions, generateLogAbstractions(ambiance));

    String taskId = ciDelegateTaskExecutor.queueTask(abstractions, task,
        taskSelectors.stream().map(TaskSelector::getSelector).collect(Collectors.toList()), new ArrayList<>(),
        executeOnHarnessHostedDelegates, emitEvent, generateLogAbstractions(ambiance));
    String logKey = getLogKey(ambiance);

    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(taskId)
        .addAllLogKeys(CollectionUtils.emptyIfNull(singletonList(logKey)))
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    // If any of the responses are in serialized format, deserialize them
    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    log.info("Received response for step {}", stepIdentifier);

    backgroundTaskUtility.queueJob(() -> saveInitialiseExecutionSweepingOutput(ambiance));

    ResponseData responseData = responseDataMap.entrySet().iterator().next().getValue();
    responseData = serializedResponseDataHelper.deserialize(responseData);
    if (responseData instanceof ErrorNotifyResponseData) {
      FailureData failureData =
          FailureData.newBuilder()
              .addFailureTypes(FailureType.APPLICATION_FAILURE)
              .setLevel(Level.ERROR.name())
              .setCode(GENERAL_ERROR.name())
              .setMessage(emptyIfNull(ExceptionUtils.getMessage(exceptionManager.processException(
                  new CILiteEngineException(((ErrorNotifyResponseData) responseData).getErrorMessage())))))
              .build();

      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage("Delegate is not able to connect to created build farm")
                           .addFailureData(failureData)
                           .build())
          .build();
    }
    CITaskExecutionResponse ciTaskExecutionResponse = (CITaskExecutionResponse) responseData;
    CITaskExecutionResponse.Type type = ciTaskExecutionResponse.getType();
    if (type == CITaskExecutionResponse.Type.K8) {
      return handleK8TaskResponse(ambiance, stepParameters, ciTaskExecutionResponse);
    } else if (type == CITaskExecutionResponse.Type.VM || type == CITaskExecutionResponse.Type.DOCKER) {
      return handleVmTaskResponse(ciTaskExecutionResponse);
    } else {
      throw new CIStageExecutionException(format("Invalid infra type for task response: %s", type));
    }
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepElementParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      return;
    }

    InitializeStepInfo initializeStepInfo = (InitializeStepInfo) stepElementParameters.getSpec();
    List<EntityDetail> connectorsEntityDetails =
        getConnectorIdentifiers(initializeStepInfo, accountIdentifier, projectIdentifier, orgIdentifier);

    if (isNotEmpty(connectorsEntityDetails)) {
      pipelineRbacHelper.checkRuntimePermissions(ambiance, connectorsEntityDetails, true);
    }

    validateFeatureFlags(initializeStepInfo, accountIdentifier);
    validateConnectors(
        initializeStepInfo, connectorsEntityDetails, accountIdentifier, orgIdentifier, projectIdentifier);
    sanitizeExecution(initializeStepInfo);
  }

  private void sanitizeExecution(InitializeStepInfo initializeStepInfo) {
    List<ExecutionWrapperConfig> steps = initializeStepInfo.getExecutionElementConfig().getSteps();
    if (initializeStepInfo.getInfrastructure().getType() == Infrastructure.Type.KUBERNETES_HOSTED
        || initializeStepInfo.getInfrastructure().getType() == Infrastructure.Type.HOSTED_VM) {
      sanitizationService.validate(steps);
    }
  }
  private void validateConnectors(InitializeStepInfo initializeStepInfo, List<EntityDetail> connectorEntitiesList,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (initializeStepInfo.getInfrastructure().getType() != Infrastructure.Type.HOSTED_VM) {
      return;
    }

    // For hosted VMs, we need to validate whether all the connectors connect via platform or not
    List<ConnectorDetails> connectorDetailsList =
        getConnectorDetails(connectorEntitiesList, accountIdentifier, projectIdentifier, orgIdentifier);
    Set<String> invalidIdentifiers = new HashSet<>();
    for (ConnectorDetails connectorDetails : connectorDetailsList) {
      if (connectorDetails.getExecuteOnDelegate() != null) {
        if (connectorDetails.getExecuteOnDelegate()) {
          invalidIdentifiers.add(connectorDetails.getIdentifier());
        }
      } else {
        log.warn("Connector type: {} has executeOnDelegate set as null", connectorDetails.getConnectorType());
        invalidIdentifiers.add(connectorDetails.getIdentifier());
      }
    }
    if (!isEmpty(invalidIdentifiers)) {
      throw new CIStageExecutionException(format(
          "While using hosted infrastructure, all connectors should be configured to go via the Harness platform instead of via the delegate. "
              + "Please update the connectors: %s to connect via the Harness platform instead. This can be done by "
              + "editing the connector and updating the connectivity to go via the Harness platform.",
          invalidIdentifiers));
    }
  }

  private List<ConnectorDetails> getConnectorDetails(
      List<EntityDetail> entityDetails, String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    List<ConnectorDetails> connectorDetailsList = new ArrayList<>();
    BaseNGAccess ngAccess = IntegrationStageUtils.getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    for (EntityDetail entityDetail : entityDetails) {
      if (!EntityType.CONNECTORS.equals(entityDetail.getType())) {
        continue;
      }
      ConnectorDetails connectorDetails =
          connectorUtils.getConnectorDetailsWithIdentifier(ngAccess, (IdentifierRef) entityDetail.getEntityRef());
      connectorDetailsList.add(connectorDetails);
    }
    return connectorDetailsList;
  }
  private void validateFeatureFlags(InitializeStepInfo initializeStepInfo, String accountIdentifier) {
    if (initializeStepInfo.getInfrastructure().getType() != Infrastructure.Type.HOSTED_VM) {
      return;
    }

    // For hosted VMs, we need to check whether the feature flag is enabled or not
    Boolean isEnabled = ciFeatureFlagService.isEnabled(CIE_HOSTED_VMS, accountIdentifier);
    if (!isEnabled) {
      throw new CIStageExecutionException(
          "Hosted builds are not enabled for this account. Please contact Harness support.");
    }
  }
  private String getLogKey(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = StepUtils.generateLogAbstractions(ambiance);
    return LogStreamingHelper.generateLogBaseKey(logAbstractions);
  }
  public TaskData getTaskData(
      StepElementParameters stepElementParameters, CIInitializeTaskParams buildSetupTaskParams) {
    long timeout =
        Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
    SerializationFormat serializationFormat = SerializationFormat.KRYO;
    String taskType = TaskType.INITIALIZATION_PHASE.getDisplayName();
    if (buildSetupTaskParams.getType() == DLITE_VM) {
      serializationFormat = SerializationFormat.JSON;
      taskType = TaskType.DLITE_CI_VM_INITIALIZE_TASK.getDisplayName();
    }

    return TaskData.builder()
        .async(true)
        .timeout(timeout + TASK_BUFFER_TIMEOUT_MILLIS)
        .taskType(taskType)
        .serializationFormat(serializationFormat)
        .parameters(new Object[] {buildSetupTaskParams})
        .build();
  }

  private void saveInitialiseExecutionSweepingOutput(Ambiance ambiance) {
    long startTime = AmbianceUtils.getCurrentLevelStartTs(ambiance);
    long currentTime = System.currentTimeMillis();

    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(INITIALIZE_EXECUTION));
    if (!optionalSweepingOutput.isFound()) {
      try {
        InitializeExecutionSweepingOutput initializeExecutionSweepingOutput =
            InitializeExecutionSweepingOutput.builder().initialiseExecutionTime(currentTime - startTime).build();
        executionSweepingOutputResolver.consume(
            ambiance, INITIALIZE_EXECUTION, initializeExecutionSweepingOutput, StepOutcomeGroup.STAGE.name());
      } catch (Exception e) {
        log.error("Error while consuming initialize execution sweeping output", e);
      }
    }
  }

  private StepResponse handleK8TaskResponse(
      Ambiance ambiance, StepElementParameters stepElementParameters, CITaskExecutionResponse ciTaskExecutionResponse) {
    K8sTaskExecutionResponse k8sTaskExecutionResponse = (K8sTaskExecutionResponse) ciTaskExecutionResponse;
    InitializeStepInfo initializeStepInfo = (InitializeStepInfo) stepElementParameters.getSpec();

    DependencyOutcome dependencyOutcome =
        getK8DependencyOutcome(ambiance, initializeStepInfo, k8sTaskExecutionResponse.getK8sTaskResponse());
    LiteEnginePodDetailsOutcome liteEnginePodDetailsOutcome =
        getPodDetailsOutcome(k8sTaskExecutionResponse.getK8sTaskResponse());

    StepResponse.StepOutcome stepOutcome =
        StepResponse.StepOutcome.builder().name(DEPENDENCY_OUTCOME).outcome(dependencyOutcome).build();
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      log.info(
          "LiteEngineTaskStep pod creation task executed successfully with response [{}]", k8sTaskExecutionResponse);
      if (liteEnginePodDetailsOutcome == null) {
        throw new CIStageExecutionException("Failed to get pod local ipAddress details");
      }
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(stepOutcome)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(POD_DETAILS_OUTCOME)
                           .group(StepOutcomeGroup.STAGE.name())
                           .outcome(liteEnginePodDetailsOutcome)
                           .build())
          .build();

    } else {
      log.error("LiteEngineTaskStep execution finished with status [{}] and response [{}]",
          k8sTaskExecutionResponse.getCommandExecutionStatus(), k8sTaskExecutionResponse);

      StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED).stepOutcome(stepOutcome);
      if (k8sTaskExecutionResponse.getErrorMessage() != null) {
        stepResponseBuilder.failureInfo(
            FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build());
      }
      return stepResponseBuilder.build();
    }
  }

  private void populateStrategyExpansion(InitializeStepInfo initializeStepInfo, Ambiance ambiance) {
    ExecutionElementConfig executionElement = initializeStepInfo.getExecutionElementConfig();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    List<ExecutionWrapperConfig> expandedExecutionElement = new ArrayList<>();
    Map<String, StrategyExpansionData> strategyExpansionMap = new HashMap<>();

    LicensesWithSummaryDTO licensesWithSummaryDTO = ciLicenseService.getLicenseSummary(accountId);
    Optional<Integer> maxExpansionLimit = Optional.of(Integer.valueOf(MAXIMUM_EXPANSION_LIMIT));
    if (licensesWithSummaryDTO != null && licensesWithSummaryDTO.getEdition() == Edition.FREE) {
      maxExpansionLimit = Optional.of(Integer.valueOf(MAXIMUM_EXPANSION_LIMIT_FREE_ACCOUNT));
    }

    for (ExecutionWrapperConfig config : executionElement.getSteps()) {
      // Inject the envVariables before calling strategy expansion
      IntegrationStageUtils.injectLoopEnvVariables(config);
      ExpandedExecutionWrapperInfo expandedExecutionWrapperInfo =
          strategyHelper.expandExecutionWrapperConfig(config, maxExpansionLimit);
      expandedExecutionElement.addAll(expandedExecutionWrapperInfo.getExpandedExecutionConfigs());
      strategyExpansionMap.putAll(expandedExecutionWrapperInfo.getUuidToStrategyExpansionData());
    }

    initializeStepInfo.setExecutionElementConfig(
        ExecutionElementConfig.builder().steps(expandedExecutionElement).build());
    initializeStepInfo.setStrategyExpansionMap(strategyExpansionMap);
  }

  private StepResponse handleVmTaskResponse(CITaskExecutionResponse ciTaskExecutionResponse) {
    VmTaskExecutionResponse vmTaskExecutionResponse = (VmTaskExecutionResponse) ciTaskExecutionResponse;
    DependencyOutcome dependencyOutcome = getVmDependencyOutcome(vmTaskExecutionResponse);
    StepResponse.StepOutcome stepOutcome =
        StepResponse.StepOutcome.builder().name(DEPENDENCY_OUTCOME).outcome(dependencyOutcome).build();

    if (vmTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(stepOutcome)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(VM_DETAILS_OUTCOME)
                           .group(StepOutcomeGroup.STAGE.name())
                           .outcome(getVmDetailsOutcome(vmTaskExecutionResponse))
                           .build())
          .build();
    } else {
      log.error("VM initialize step execution finished with status [{}] and response [{}]",
          vmTaskExecutionResponse.getCommandExecutionStatus(), vmTaskExecutionResponse);
      StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED).stepOutcome(stepOutcome);
      if (vmTaskExecutionResponse.getErrorMessage() != null) {
        stepResponseBuilder.failureInfo(
            FailureInfo.newBuilder().setErrorMessage(vmTaskExecutionResponse.getErrorMessage()).build());
      }
      return stepResponseBuilder.build();
    }
  }

  private VmDetailsOutcome getVmDetailsOutcome(VmTaskExecutionResponse vmTaskExecutionResponse) {
    VmDetailsOutcomeBuilder builder = VmDetailsOutcome.builder().ipAddress(vmTaskExecutionResponse.getIpAddress());
    if (vmTaskExecutionResponse.getDelegateMetaInfo() == null
        || isEmpty(vmTaskExecutionResponse.getDelegateMetaInfo().getId())) {
      return builder.build();
    }

    return builder.delegateId(vmTaskExecutionResponse.getDelegateMetaInfo().getId()).build();
  }

  private DependencyOutcome getVmDependencyOutcome(VmTaskExecutionResponse vmTaskExecutionResponse) {
    List<ServiceDependency> serviceDependencyList = new ArrayList<>();

    List<VmServiceStatus> serviceStatuses = vmTaskExecutionResponse.getServiceStatuses();
    if (isEmpty(serviceStatuses)) {
      return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
    }

    for (VmServiceStatus serviceStatus : serviceStatuses) {
      ServiceDependency.Status status = ServiceDependency.Status.SUCCESS;
      if (serviceStatus.getStatus() == VmServiceStatus.Status.ERROR) {
        status = ServiceDependency.Status.ERROR;
      }
      serviceDependencyList.add(ServiceDependency.builder()
                                    .identifier(serviceStatus.getIdentifier())
                                    .name(serviceStatus.getName())
                                    .image(serviceStatus.getImage())
                                    .errorMessage(serviceStatus.getErrorMessage())
                                    .status(status.getDisplayName())
                                    .logKeys(Collections.singletonList(serviceStatus.getLogKey()))
                                    .build());
    }
    return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
  }

  private DependencyOutcome getK8DependencyOutcome(
      Ambiance ambiance, InitializeStepInfo stepParameters, CiK8sTaskResponse ciK8sTaskResponse) {
    List<ServiceDefinitionInfo> serviceDefinitionInfos =
        k8InitializeServiceUtils.getServiceInfos(stepParameters.getStageElementConfig());
    List<ServiceDependency> serviceDependencyList = new ArrayList<>();
    if (serviceDefinitionInfos == null) {
      return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
    }

    Map<String, CIContainerStatus> containerStatusMap = new HashMap<>();
    if (ciK8sTaskResponse != null && ciK8sTaskResponse.getPodStatus() != null
        && ciK8sTaskResponse.getPodStatus().getCiContainerStatusList() != null) {
      for (CIContainerStatus containerStatus : ciK8sTaskResponse.getPodStatus().getCiContainerStatusList()) {
        containerStatusMap.put(containerStatus.getName(), containerStatus);
      }
    }

    String logPrefix = getLogPrefix(ambiance);
    for (ServiceDefinitionInfo serviceDefinitionInfo : serviceDefinitionInfos) {
      String logKey = format("%s/serviceId:%s", logPrefix, serviceDefinitionInfo.getIdentifier());
      String containerName = serviceDefinitionInfo.getContainerName();
      if (containerStatusMap.containsKey(containerName)) {
        CIContainerStatus containerStatus = containerStatusMap.get(containerName);

        ServiceDependency.Status status = ServiceDependency.Status.SUCCESS;
        if (containerStatus.getStatus() == CIContainerStatus.Status.ERROR) {
          status = ServiceDependency.Status.ERROR;
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceDefinitionInfo.getIdentifier())
                                      .name(serviceDefinitionInfo.getName())
                                      .image(containerStatus.getImage())
                                      .startTime(containerStatus.getStartTime())
                                      .endTime(containerStatus.getEndTime())
                                      .errorMessage(containerStatus.getErrorMsg())
                                      .status(status.getDisplayName())
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      } else {
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceDefinitionInfo.getIdentifier())
                                      .name(serviceDefinitionInfo.getName())
                                      .image(serviceDefinitionInfo.getImage())
                                      .errorMessage("Unknown")
                                      .status(ServiceDependency.Status.ERROR.getDisplayName())
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      }
    }
    return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
  }

  private String getLogPrefix(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = StepUtils.generateLogAbstractions(ambiance, "STAGE");
    return LogStreamingHelper.generateLogBaseKey(logAbstractions);
  }

  private LiteEnginePodDetailsOutcome getPodDetailsOutcome(CiK8sTaskResponse ciK8sTaskResponse) {
    if (ciK8sTaskResponse != null && ciK8sTaskResponse.getPodStatus() != null) {
      String ip = ciK8sTaskResponse.getPodStatus().getIp();
      String namespace = ciK8sTaskResponse.getPodNamespace();
      return LiteEnginePodDetailsOutcome.builder().ipAddress(ip).namespace(namespace).build();
    }
    return null;
  }
  private List<EntityDetail> getConnectorIdentifiers(
      InitializeStepInfo initializeStepInfo, String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    Infrastructure infrastructure = initializeStepInfo.getInfrastructure();
    if (infrastructure == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }
    List<EntityDetail> entityDetails = new ArrayList<>();
    // Add git clone connector
    if (!initializeStepInfo.isSkipGitClone()) {
      if (initializeStepInfo.getCiCodebase() == null) {
        throw new CIStageExecutionException("Codebase is mandatory with enabled cloneCodebase flag");
      }
      if (isEmpty(initializeStepInfo.getCiCodebase().getConnectorRef().getValue())) {
        throw new CIStageExecutionException("Git connector is mandatory with enabled cloneCodebase flag");
      }

      entityDetails.add(createEntityDetails(initializeStepInfo.getCiCodebase().getConnectorRef().getValue(),
          accountIdentifier, projectIdentifier, orgIdentifier));
    }

    List<String> connectorRefs =
        IntegrationStageUtils.getStageConnectorRefs(initializeStepInfo.getStageElementConfig());
    if (infrastructure.getType() == Infrastructure.Type.VM || infrastructure.getType() == Infrastructure.Type.DOCKER
        || infrastructure.getType() == Infrastructure.Type.HOSTED_VM) {
      if (!isEmpty(connectorRefs)) {
        entityDetails.addAll(
            connectorRefs.stream()
                .map(connectorIdentifier
                    -> createEntityDetails(connectorIdentifier, accountIdentifier, projectIdentifier, orgIdentifier))
                .collect(Collectors.toList()));
      }
      return entityDetails;
    }

    if (infrastructure.getType() != Infrastructure.Type.KUBERNETES_HOSTED) {
      if (((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
        throw new CIStageExecutionException("Input infrastructure can not be empty");
      }
      String infraConnectorRef = ((K8sDirectInfraYaml) infrastructure).getSpec().getConnectorRef().getValue();
      entityDetails.add(createEntityDetails(infraConnectorRef, accountIdentifier, projectIdentifier, orgIdentifier));
    }

    entityDetails.addAll(connectorRefs.stream()
                             .map(connectorIdentifier -> {
                               return createEntityDetails(
                                   connectorIdentifier, accountIdentifier, projectIdentifier, orgIdentifier);
                             })
                             .collect(Collectors.toList()));

    return entityDetails;
  }
  private EntityDetail createEntityDetails(
      String connectorIdentifier, String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return EntityDetail.builder().entityRef(connectorRef).type(EntityType.CONNECTORS).build();
  }
}