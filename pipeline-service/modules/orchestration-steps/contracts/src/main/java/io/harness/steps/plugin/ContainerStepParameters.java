package io.harness.steps.plugin;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("ContainerStepParameters")
@RecasterAlias("io.harness.steps.plugin.ContainerStepParameters")
public class ContainerStepParameters extends ContainerBaseStepInfo implements SpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public ContainerStepParameters(ParameterField<String> image, ParameterField<String> connectorRef,
      ParameterField<ImagePullPolicy> imagePullPolicy, ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      ParameterField<List<String>> entrypoint) {
    super(image, connectorRef, imagePullPolicy, delegateSelectors, entrypoint);
  }

  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(ContainerCommandUnitConstants.InitContainer, ContainerCommandUnitConstants.ContainerStep,
        ContainerCommandUnitConstants.CleanContainer);
  }
}