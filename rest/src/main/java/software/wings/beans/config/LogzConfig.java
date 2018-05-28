package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "token")
public class LogzConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Logz.io URL", required = true) @NotEmpty private String logzUrl;

  @Attributes(title = "Token", required = true) @Encrypted private char[] token;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedToken;
  /**
   * Instantiates a new Splunk config.
   */
  public LogzConfig() {
    super(SettingVariableTypes.LOGZ.name());
  }

  public String getLogzUrl() {
    return logzUrl;
  }

  public void setLogzUrl(String logzUrl) {
    this.logzUrl = logzUrl;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public char[] getToken() {
    return token;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setToken(char[] token) {
    this.token = token;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String logzUrl;
    private String token;

    @Builder
    public Yaml(
        String type, String harnessApiVersion, String logzUrl, String token, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.logzUrl = logzUrl;
      this.token = token;
    }
  }
}
