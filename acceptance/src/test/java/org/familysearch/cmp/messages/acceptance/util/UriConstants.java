package org.familysearch.cmp.messages.acceptance.util;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.client.utils.URIBuilder;


/**
 * Created by rob birch on 1/5/2016.
 */

public class UriConstants {

  private static final String THREAD_ID_PLACEHOLDER = "{tid}";
  private static final String USER_ID_PLACEHOLDER = "{uid}";
  private static final String MESSAGE_ID_PLACEHOLDER = "{mid}";
  private static final String GROUP_NAME_PLACEHOLDER = "{groupName}";
  private static final String MEMBER_ID_PLACEHOLDER = "{memberId";

  // AdminEndpoints
  private static final String ADMIN_THREADS_URI = String.format("/admin/threads/%s", THREAD_ID_PLACEHOLDER);
  private static final String ADMIN_USERS_URI   = String.format("/admin/threads/users/%s", USER_ID_PLACEHOLDER);

  //  GroupEndpoints
  private static final String GROUP_URI           = String.format("/users/%s/groups/%s", USER_ID_PLACEHOLDER, GROUP_NAME_PLACEHOLDER);
  private static final String GROUPS_URI          = String.format("/users/%s/groups", USER_ID_PLACEHOLDER);
  private static final String GROUP_MEMBER_URI    = String.format("/users/%s/groups/%s/members/%s", USER_ID_PLACEHOLDER, GROUP_NAME_PLACEHOLDER, MEMBER_ID_PLACEHOLDER);
  private static final String GROUP_MEMBERS_URI   = String.format("/users/%s/groups/%s/members", USER_ID_PLACEHOLDER, GROUP_NAME_PLACEHOLDER);
  private static final String GROUP_CONTACTS_URI  = String.format("/users/%s/contacts", USER_ID_PLACEHOLDER);

  // HealthCheckEndpoints
  private static final String HEALTHCHECK_ENV_URI       = "/healthcheck/env";
  private static final String HEALTHCHECK_VITALS_URI    = "/healthcheck/vital";
  private static final String HEALTHCHECK_STATS_URI     = "/healthcheck/stats";
  private static final String HEALTHCHECK_HEARTBEAT_URI = "/healthcheck/heartbeat";

  // MessageResource
  private static final String THREAD_MESSAGE_URI  = String.format("/threads/%s/messages/%s", THREAD_ID_PLACEHOLDER, MESSAGE_ID_PLACEHOLDER);
  private static final String THREAD_MESSAGES_URI = String.format("/threads/%s/messages", THREAD_ID_PLACEHOLDER);

  // ThreadResource
  private static final String THREAD_SPECIFIC_URI = String.format("/threads/%s", THREAD_ID_PLACEHOLDER);
  private static final String THREADS_URI         = "/threads";

  // UserEndpoints
  private static final String USER_COUNTERS_URI   = String.format("/users/%s/counters", USER_ID_PLACEHOLDER);
  private static final String USER_THREADS_URI    = String.format("/users/%s/threads", USER_ID_PLACEHOLDER);

  // UserThreadStateResource
  private static final String THREAD_STATE_URI    = String.format("/threads/%s/users/%s/state", THREAD_ID_PLACEHOLDER, USER_ID_PLACEHOLDER);

  // SubscriptionProfileEndpoints
  private static final String SUBSCRIPTION_URI = "/profile/unsubscribe";


  // admin
  public static String getAdminThreadsUri(String threadId) {
    return ADMIN_THREADS_URI.replace(THREAD_ID_PLACEHOLDER, threadId);
  }

  public static String getAdminUsersUri(String userId) {
    return ADMIN_USERS_URI.replace(USER_ID_PLACEHOLDER, userId);
  }

  // group
  public static String getGroupUri(String userId, String groupName) {
    return GROUP_URI.replace(USER_ID_PLACEHOLDER, userId).replace(GROUP_NAME_PLACEHOLDER, groupName);
  }

  public static String getGroupsUri(String userId) {
    return GROUPS_URI.replace(USER_ID_PLACEHOLDER, userId);
  }

  public static String getGroupMemberUri(String userId, String groupName, String memberId) {
    return GROUP_MEMBER_URI.replace(USER_ID_PLACEHOLDER, userId).replace(GROUP_NAME_PLACEHOLDER, groupName).replace(MEMBER_ID_PLACEHOLDER, memberId);
  }

  public static String getGroupMembersUri(String userId, String groupName) {
    return GROUP_MEMBERS_URI.replace(USER_ID_PLACEHOLDER, userId).replace(GROUP_NAME_PLACEHOLDER, groupName);
  }

  public static String getGroupContactsUri(String userId) {
    return GROUP_CONTACTS_URI.replace(USER_ID_PLACEHOLDER, userId);
  }

  // healthcheck
  public static String getHealthcheckEnvUri() {
    return HEALTHCHECK_ENV_URI;
  }

  public static String GetHealthcheckVitalsUri() {
    return HEALTHCHECK_VITALS_URI;
  }

  public static String getHealthcheckStatusUri() {
    return HEALTHCHECK_STATS_URI;
  }

  public static String getHealthcheckHeartbeatUri() {
    return HEALTHCHECK_HEARTBEAT_URI;
  }

  // messages
  public static String getThreadMessageUri(String threadId, String messageId) {
    return THREAD_MESSAGE_URI.replace(THREAD_ID_PLACEHOLDER, threadId).replace(MESSAGE_ID_PLACEHOLDER, messageId);
  }

  public static String getThreadMessagesUri(String threadId) {
    return THREAD_MESSAGES_URI.replace(THREAD_ID_PLACEHOLDER, threadId);
  }

  // subscription
  public static String getSubscriptionUri() {
    return SUBSCRIPTION_URI;
  }

  // thread
  public static String getThreadSpecificUri(String threadId) {
    return THREAD_SPECIFIC_URI.replace(THREAD_ID_PLACEHOLDER, threadId);
  }

  public static String getThreadsUri() {
    return THREADS_URI;
  }

  // user
  public static String getUserCountersUri(String userId) {
    return USER_COUNTERS_URI.replace(USER_ID_PLACEHOLDER, userId);
  }

  public static String getUserThreadsUri(String userId) {
    return USER_THREADS_URI.replace(USER_ID_PLACEHOLDER, userId);
  }

  // user thread state
  public static String getThreadStateUri(String threadId, String userId) {
    return THREAD_STATE_URI.replace(THREAD_ID_PLACEHOLDER, threadId).replace(USER_ID_PLACEHOLDER, userId);
  }

  public static String getNewResourceLocation(ClientResponse response) {
    return response.getHeaders().get("Location").get(0);
  }

  public static class QueryBuilder {

    public enum QueryParameters {
      page,
      pageSize,
      sort
    }

    public enum SortParamValues {
      ASC,
      DESC
    }

    private URIBuilder builder = new URIBuilder();

    public QueryBuilder addParameter(QueryParameters name, String value) {
      if(builder.toString().matches(".*(\\?|&)" + name.toString() + "=.*")) {
        builder.setParameter(name.toString(), value);
      }
      else {
        builder.addParameter(name.toString(), value);
      }

      return this;
    }

    public String createQuery() {
      return builder.toString();
    }
  }

}
