/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.pms.pipeline.api;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.MANKRIT;

import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.ngexception.beans.yamlschema.NodeErrorInfo;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorDTO;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorWrapperDTO;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.pms.pipeline.ExecutionSummaryInfoDTO;
import io.harness.pms.pipeline.PMSPipelineSummaryResponseDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.rule.Owner;
import io.harness.spec.server.pipeline.model.GitDetails;
import io.harness.spec.server.pipeline.model.PipelineGetResponseBody;
import io.harness.spec.server.pipeline.model.PipelineListResponseBody;
import io.harness.spec.server.pipeline.model.PipelineListResponseBody.StoreTypeEnum;
import io.harness.spec.server.pipeline.model.YAMLSchemaErrorWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PipelinesApiUtilsTest extends CategoryTest {
  String slug = randomAlphabetic(10);
  String name = randomAlphabetic(10);

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testGetGitDetails() {
    EntityGitDetails entityGitDetails = EntityGitDetails.builder()
                                            .objectId("objectId")
                                            .branch("branch")
                                            .commitId("commitId")
                                            .filePath("filePath")
                                            .fileUrl("fileUrl")
                                            .repoUrl("repoUrl")
                                            .repoName("repoName")
                                            .build();
    GitDetails gitDetails = PipelinesApiUtils.getGitDetails(entityGitDetails);
    assertEquals("objectId", gitDetails.getEntityIdentifier());
    assertEquals("branch", gitDetails.getBranchName());
    assertEquals("commitId", gitDetails.getCommitId());
    assertEquals("filePath", gitDetails.getFilePath());
    assertEquals("fileUrl", gitDetails.getFileUrl());
    assertEquals("repoUrl", gitDetails.getRepoUrl());
    assertEquals("repoName", gitDetails.getRepoName());
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testGetYAMLSchemaWrapper() {
    YamlSchemaErrorWrapperDTO yamlSchemaErrorWrapperDTO =
        YamlSchemaErrorWrapperDTO.builder()
            .schemaErrors(Collections.singletonList(YamlSchemaErrorDTO.builder()
                                                        .message("errorMessage")
                                                        .fqn("$.inputSet")
                                                        .stageInfo(NodeErrorInfo.builder().identifier("stage1").build())
                                                        .stepInfo(NodeErrorInfo.builder().identifier("step1").build())
                                                        .hintMessage("trySomething")
                                                        .build()))
            .build();
    List<YAMLSchemaErrorWrapper> yamlSchemaErrorWrappers =
        PipelinesApiUtils.getListYAMLErrorWrapper(yamlSchemaErrorWrapperDTO);
    assertEquals(1, yamlSchemaErrorWrappers.size());
    YAMLSchemaErrorWrapper yamlSchemaErrorWrapper = yamlSchemaErrorWrappers.get(0);
    assertEquals("errorMessage", yamlSchemaErrorWrapper.getMessage());
    assertEquals("$.inputSet", yamlSchemaErrorWrapper.getFqn());
    assertEquals("stage1", yamlSchemaErrorWrapper.getStageInfo().getSlug());
    assertEquals("step1", yamlSchemaErrorWrapper.getStepInfo().getSlug());
    assertEquals("trySomething", yamlSchemaErrorWrapper.getHintMessage());
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testGetResponseBody() {
    PipelineEntity pipelineEntity =
        PipelineEntity.builder().yaml("yaml").createdAt(123456L).lastUpdatedAt(987654L).build();
    PipelineGetResponseBody responseBody = PipelinesApiUtils.getGetResponseBody(pipelineEntity);
    assertEquals("yaml", responseBody.getPipelineYaml());
    assertEquals(123456L, responseBody.getCreated().longValue());
    assertEquals(987654L, responseBody.getUpdated().longValue());
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testGetFilterProperties() {
    List<String> tags = new ArrayList<>();
    tags.add("key:value");
    tags.add("key2");
    PipelineFilterPropertiesDto pipelineFilterPropertiesDto =
        PipelinesApiUtils.getFilterProperties(Collections.singletonList("pipelineId"), "name", null, tags,
            Collections.singletonList("service"), Collections.singletonList("envs"), "deploymentType", "repo");
    assertEquals(pipelineFilterPropertiesDto.getPipelineIdentifiers().get(0), "pipelineId");
    assertEquals(pipelineFilterPropertiesDto.getName(), "name");
    assertEquals(
        pipelineFilterPropertiesDto.getPipelineTags().get(0), NGTag.builder().key("key").value("value").build());
    assertEquals(pipelineFilterPropertiesDto.getTags().get("key2"), null);
  }

  @Test
  @Owner(developers = MANKRIT)
  @Category(UnitTests.class)
  public void testGetPipelines() {
    PMSPipelineSummaryResponseDTO pmsPipelineSummaryResponseDTO =
        PMSPipelineSummaryResponseDTO.builder()
            .identifier(slug)
            .name(name)
            .createdAt(123456L)
            .lastUpdatedAt(987654L)
            .executionSummaryInfo(ExecutionSummaryInfoDTO.builder().deployments(Collections.singletonList(1)).build())
            .storeType(StoreType.INLINE)
            .build();
    PipelineListResponseBody listResponseBody = PipelinesApiUtils.getPipelines(pmsPipelineSummaryResponseDTO);
    assertEquals(listResponseBody.getCreated().longValue(), 123456L);
    assertEquals(listResponseBody.getUpdated().longValue(), 987654L);
    assertEquals(listResponseBody.getSlug(), slug);
    assertEquals(listResponseBody.getName(), name);
    assertEquals(listResponseBody.getStoreType(), StoreTypeEnum.INLINE);
    assertEquals(listResponseBody.getExecutionSummary().getDeploymentsCount().get(0).intValue(), 1);
  }
}
