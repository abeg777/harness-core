package software.wings.app;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.List;

/**
 * The Class PortalConfig.
 */
public class PortalConfig {
  @JsonProperty(defaultValue = "https://localhost:8000") private String url = "https://localhost:8000";
  private List<String> allowedDomains = Lists.newArrayList();
  private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty(defaultValue = "") private String companyName = "";
  @JsonProperty(defaultValue = "/register/verify") private String verificationUrl = "/register/verify";
  @JsonProperty(defaultValue = "/app/%s/overview") private String applicationOverviewUrlPattern = "/app/%s/overview";
  @JsonProperty(defaultValue = "/app/%s/env/%s/execution/%s/detail")
  private String executionUrlPattern = "/app/%s/env/%s/execution/%s/detail";
  private String jwtPasswordSecret;
  private String jwtExternalServiceSecret;
  private Long authTokenExpiryInMillis = 24 * 60 * 60 * 1000L;

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets allowed domains.
   *
   * @return the allowed domains
   */
  @JsonProperty(defaultValue = "")
  public String getAllowedDomains() {
    return Joiner.on(",").join(allowedDomains);
  }

  /**
   * Sets allowed domains.
   *
   * @param allowedDomains the allowed domains
   */
  public void setAllowedDomains(String allowedDomains) {
    this.allowedDomains = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedDomains);
  }

  /**
   * Gets allowed domains list.
   *
   * @return the allowed domains list
   */
  public List<String> getAllowedDomainsList() {
    return isEmpty(allowedDomains) ? Lists.newArrayList() : allowedDomains;
  }

  /**
   * Gets allowed origins.
   * @return the allowed origins
   */
  @JsonProperty(defaultValue = "")
  public String getAllowedOrigins() {
    return Joiner.on(",").join(allowedOrigins);
  }

  /**
   * Gets Allowed origins list
   * @return the allowed origins list
   */
  public List<String> getAllowedOriginsList() {
    return isEmpty(allowedOrigins) ? Lists.newArrayList() : allowedOrigins;
  }

  /**
   * Sets allowed orgins.
   *
   * @param allowedOrigins
   */
  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedOrigins);
  }

  /**
   * Gets company name.
   *
   * @return the company name
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Sets company name.
   *
   * @param companyName the company name
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  /**
   * Getter for property 'verificationUrl'.
   *
   * @return Value for property 'verificationUrl'.
   */
  public String getVerificationUrl() {
    return verificationUrl;
  }

  /**
   * Setter for property 'verificationUrl'.
   *
   * @param verificationUrl Value to set for property 'verificationUrl'.
   */
  public void setVerificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
  }

  /**
   * Gets auth token expiry in millis.
   *
   * @return the auth token expiry in millis
   */
  public Long getAuthTokenExpiryInMillis() {
    return authTokenExpiryInMillis;
  }

  /**
   * Sets auth token expiry in millis.
   *
   * @param authTokenExpiryInMillis the auth token expiry in millis
   */
  public void setAuthTokenExpiryInMillis(Long authTokenExpiryInMillis) {
    this.authTokenExpiryInMillis = authTokenExpiryInMillis;
  }

  /**
   * Gets application overview url pattern.
   *
   * @return the application overview url pattern
   */
  public String getApplicationOverviewUrlPattern() {
    return applicationOverviewUrlPattern;
  }

  /**
   * Sets application overview url pattern.
   *
   * @param applicationOverviewUrlPattern the application overview url pattern
   */
  public void setApplicationOverviewUrlPattern(String applicationOverviewUrlPattern) {
    this.applicationOverviewUrlPattern = applicationOverviewUrlPattern;
  }

  public String getJwtPasswordSecret() {
    return jwtPasswordSecret;
  }

  public void setJwtPasswordSecret(String jwtPasswordSecret) {
    this.jwtPasswordSecret = jwtPasswordSecret;
  }

  public String getJwtExternalServiceSecret() {
    return jwtExternalServiceSecret;
  }

  public void setJwtExternalServiceSecret(String jwtExternalServiceSecret) {
    this.jwtExternalServiceSecret = jwtExternalServiceSecret;
  }

  /**
   * The type Array converter.
   */
  public static class ArrayConverter extends StdConverter<List<String>, String> {
    @Override
    public String convert(List<String> value) {
      return Joiner.on(",").join(value);
    }
  }

  /**
   * Gets execution url pattern.
   *
   * @return the execution url pattern
   */
  public String getExecutionUrlPattern() {
    return executionUrlPattern;
  }

  /**
   * Sets execution url pattern.
   *
   * @param executionUrlPattern the execution url pattern
   */
  public void setExecutionUrlPattern(String executionUrlPattern) {
    this.executionUrlPattern = executionUrlPattern;
  }
}
