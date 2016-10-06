package software.wings.service.impl;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.ErrorCodes.ACCESS_DENIED;
import static software.wings.beans.ErrorCodes.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_APP_ID;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_ENV_ID;

import com.google.inject.Singleton;

import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.service.intfc.AuthService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
@Singleton
public class AuthServiceImpl implements AuthService {
  private GenericDbCache dbCache;

  /**
   * Instantiates a new Auth service.
   *
   * @param dbCache the db cache
   */
  @Inject
  public AuthServiceImpl(GenericDbCache dbCache) {
    this.dbCache = dbCache;
  }

  @Override
  public AuthToken validateToken(String tokenString) {
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);
    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN);
    } else if (authToken.getExpireAt() <= System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN);
    }
    return authToken;
  }

  @Override
  public void authorize(String appId, String envId, User user, List<PermissionAttribute> permissionAttributes,
      PageRequestType requestType) {
    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(appId, envId, permissionAttribute, user.getRoles(), requestType)) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  private boolean authorizeAccessType(String appId, String envId, PermissionAttribute permissionAttribute,
      List<Role> roles, PageRequestType requestType) {
    return roles.stream()
        .filter(role -> roleAuthorizedWithAccessType(role, permissionAttribute, appId, envId, requestType))
        .findFirst()
        .isPresent();
  }

  private boolean roleAuthorizedWithAccessType(
      Role role, PermissionAttribute permissionAttribute, String appId, String envId, PageRequestType requestType) {
    ResourceType reqResourceType = permissionAttribute.getResourceType();
    Action reqAction = permissionAttribute.getAction();
    boolean requiresEnvironmentPermission = permissionAttribute.getScope().equals(PermissionScope.ENV);
    for (Permission permission : role.getPermissions()) {
      if (hasMatchingPermissionType(requiresEnvironmentPermission, permission.getPermissionScope())
          && hasResourceAccess(reqResourceType, permission) && canPerformAction(reqAction, permission)
          && allowedInEnv(envId, requiresEnvironmentPermission, permission, requestType)
          && forApplication(appId, permission, requestType)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasMatchingPermissionType(boolean requiresEnvironmentPermission, PermissionScope permissionScope) {
    return requiresEnvironmentPermission ? permissionScope.equals(PermissionScope.ENV)
                                         : permissionScope.equals(PermissionScope.APP);
  }

  private boolean forApplication(String appId, Permission permission, PageRequestType requestType) {
    return requestType.equals(LIST_WITHOUT_APP_ID) || GLOBAL_APP_ID.equals(permission.getAppId())
        || (appId != null && appId.equals(permission.getAppId()));
  }

  private boolean allowedInEnv(
      String envId, boolean requiresEnvironmentPermission, Permission permission, PageRequestType requestType) {
    return !requiresEnvironmentPermission || requestType.equals(LIST_WITHOUT_ENV_ID)
        || allowedInSpecificEnvironment(envId, permission);
  }

  private boolean allowedInSpecificEnvironment(String envId, Permission permission) {
    if (envId != null) {
      Environment environment = dbCache.get(Environment.class, envId);
      return hasAccessByEnvType(environment, permission) || hasAccessByEnvId(environment, permission);
    }
    return false;
  }

  private boolean hasAccessByEnvId(Environment environment, Permission permission) {
    return GLOBAL_ENV_ID.equals(permission.getEnvId())
        || (environment != null && environment.getUuid().equals(permission.getEnvId()));
  }

  private boolean hasAccessByEnvType(Environment environment, Permission permission) {
    return ALL.equals(permission.getEnvironmentType())
        || (environment != null && environment.getEnvironmentType().equals(permission.getEnvironmentType()));
  }

  private boolean canPerformAction(Action reqAction, Permission permission) {
    return Action.ALL.equals(permission.getAction()) || (reqAction.equals(permission.getAction()));
  }

  private boolean hasResourceAccess(ResourceType reqResource, Permission permission) {
    return ResourceType.ANY.equals(permission.getResourceType()) || (reqResource.equals(permission.getResourceType()));
  }
}
