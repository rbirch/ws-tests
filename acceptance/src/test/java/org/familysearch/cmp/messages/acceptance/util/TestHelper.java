package org.familysearch.cmp.messages.acceptance.util;

import com.jayway.jsonpath.JsonPath;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.familysearch.cmp.messages.acceptance.MessageEndpointsAT;
import org.familysearch.cmp.qa.util.TestUser;
import org.familysearch.cmp.qa.util.BaseTestHelper;
import org.familysearch.qa.testuserprovider.TestUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by rbirch on 3/10/2016.
 */
public class TestHelper extends BaseTestHelper {

  private static final Logger log = LoggerFactory.getLogger(MessageEndpointsAT.class);
  private static final int MESSAGE_THREADS_TO_CREATE = 20;
  private static final int TEST_GROUPS_TO_CREATE = 10;
  private static final String GROUP_NAME = "test_group";
  private static final String GROUP_DESCRIPTION = "acceptance test group";

  public Map<String, TestThread> testThreads = new LinkedHashMap<>();
  public Map<String, TestThread> deletedThreads = new LinkedHashMap<>();

  public Map<String, TestGroup> testGroups = new LinkedHashMap<>();

  private Map<String, TestUser> allTestUsers;
  private TestUser commonUser;

  public TestHelper(String basePath) {
    super(basePath);
    
    TestUserProvider provider = new TestUserProvider("U2MS-Encrypt-Key", "messaging_test_users.encrypted");

    List<org.familysearch.qa.testuserprovider.TestUser> credentials = provider.getByAttribute("integration-member");
    allTestUsers = createTestUsers(credentials);

    credentials = provider.getByAttribute("integration-non-member");
    allTestUsers.putAll(createTestUsers(credentials));

    int idx = new Random().nextInt(allTestUsers.keySet().size());
    String key = allTestUsers.keySet().toArray()[idx].toString();
    commonUser = allTestUsers.get(key);
  }

  public  void setup() {
    // delete all TestUser threads
    runMethodForAllTestUsers(testUser -> softDeleteAllThreadsForUser(testUser));

    // delete all private groups for all test users
    runMethodForAllTestUsers(testUser -> deletePrivateGroupsForUser(testUser));

    // create TestGroups with multiple members
    iterateMethod(i -> {
        TestGroup testGroup = putNewGroup(GROUP_NAME + "_" + i, GROUP_DESCRIPTION);
        testGroup.addMembers(getRandomTestUsersBesides(testGroup.getOwner()));
    }, TEST_GROUPS_TO_CREATE);

    // create TestThreads with multiple messages
    iterateMethod(blah -> createThreadWithMultipleMessages(), MESSAGE_THREADS_TO_CREATE);
  }

  public Map getAllTestUsers() {
    return allTestUsers;
  }

  public TestUser getCommonUser() {
    return commonUser;
  }

  // ThreadResource endpoints
  public TestThread postNewThread() {
    TestUser author = getRandomUser();
    int minParticipants = 1;
    int maxParticipants = allTestUsers.size() - 2; // need to leave at least two users as a non participants

    int participantCount = ThreadLocalRandom.current().nextInt(minParticipants, maxParticipants);
    List<String> receivers = getRandomUserIds(participantCount);
    if(!author.getUserId().equals(commonUser.getUserId()) && !receivers.contains(commonUser.getUserId())) {
      receivers.add(commonUser.getUserId());
    }
    return postNewThread(author, receivers);
  }

  public ClientResponse postNewThreadByNonParticipantAuthor() {
    TestUser author = getRandomUser();
    int minParticipants = 1;
    int maxParticipants = allTestUsers.size() - 1;

    int participantCount = ThreadLocalRandom.current().nextInt(minParticipants, maxParticipants + 1);
    List<String> participantIds = getRandomUserIds(participantCount);
    if(!author.getUserId().equals(commonUser.getUserId()) && !participantIds.contains(commonUser.getUserId())) {
      participantIds.add(commonUser.getUserId());
    }

    String threadSubject = "web test subject " + getRandomString(50);
    String threadAbout = "about";
    String aboutUrl = "http://familysearch.org/lyrics/hokeypokey.html";
    String threadBody = getRandomThreadMessageForUser(author);

    String requestJson = JsonMapper.createJsonThreadsSnippet(author.getUserId(), participantIds, threadSubject, threadAbout, aboutUrl, threadBody);

    String resourcePath = UriConstants.getThreadsUri();
    TestUser bogusAuthor = getTestUserBesides(author);

    return getHttpResponse(resourcePath, bogusAuthor, Method.POST, requestJson);
  }

  public TestThread postNewThread(TestUser author, List<String> participantIds) {
    String threadSubject = "web test subject " + getRandomString(50);
    String threadAbout = "about";
    String aboutUrl = "http://familysearch.org/lyrics/hokeypokey.html";
    String threadBody = getRandomThreadMessageForUser(author);

    TestThread testThread = null;
    String requestJson = JsonMapper.createJsonThreadsSnippet(author.getUserId(), participantIds, threadSubject, threadAbout, aboutUrl, threadBody);
    testThread = new TestThread(requestJson);

    log.info(String.format("Created new TestThread - threadId = %s", testThread.getThreadId()));

    testThreads.put(testThread.getThreadId(), testThread);
    return testThread;
  }

  // END ThreadResource

  // UserEndpoints
  // get user threads
  public String getUserThreads(TestUser user, String query) {
    String resourcePath = UriConstants.getUserThreadsUri(user.getUserId()) + query;
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  // get user counters
  public String getUserCounters(TestUser user) {
    String resourcePath = UriConstants.getUserCountersUri(user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  // get user contacts
  public String getGroupContacts(TestUser user) {
    String resourcePath = UriConstants.getGroupContactsUri(user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  // get the groups the user is a member of
  public String getGroupsForUser(TestUser user) {
    String resourcePath = UriConstants.getGroupsUri(user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  // create a new group with a random owner
  public TestGroup putNewGroup() {
    return putNewGroup(GROUP_NAME, GROUP_DESCRIPTION);
  }

  // create a new group with this owner
  public TestGroup putNewGroup(TestUser owner) {
    return putNewGroup(GROUP_NAME, GROUP_DESCRIPTION, owner);
  }

  // create a new group with a random owner, but this name and description
  public TestGroup putNewGroup(String name, String description) {
    TestUser user = getRandomUser();
    return putNewGroup(name, description, user);
  }

  // create a new group with this name, description and owner
  public TestGroup putNewGroup(String name, String description, TestUser owner) {
    TestGroup testGroup = null;
    try {
      String requestJson = JsonMapper.createJsonGroupSnippet(name, description);
      testGroup = new TestGroup(owner, requestJson);

      log.info(String.format("Created new TestGroup - name: %s, owner: %s", testGroup.getName(), testGroup.getOwner().getUserId()));
    } catch(Exception ex) { throw new RuntimeException(ex); }

    testGroups.put(testGroup.getName(), testGroup);
    return testGroup;
  }

  // get user thread state
  public String getUserThreadState(TestThread testThread) {
    TestUser user = getParticipantForThread(testThread);
    return getUserThreadState(testThread, user);
  }

  public String getUserThreadState(TestThread testThread, TestUser user) {
    String resourcePath = UriConstants.getThreadStateUri(testThread.getThreadId(), user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  public String setUserThreadStateToTrash(TestThread testThread, TestUser user) {
    String requestJson = JsonMapper.createJsonThreadStateSnippet(JsonMapper.State.TRASH);
    String resourcePath = UriConstants.getThreadStateUri(testThread.getThreadId(), user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.PUT, requestJson);
  }

  public String setUserThreadStateToUnread(TestThread testThread, TestUser user) {
    String requestJson = JsonMapper.createJsonThreadStateSnippet(JsonMapper.State.UNREAD);
    String resourcePath = UriConstants.getThreadStateUri(testThread.getThreadId(), user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.PUT, requestJson);
  }

  public String setUserThreadStateToRead(TestThread testThread, TestUser user) {
    String requestJson = JsonMapper.createJsonThreadStateSnippet(JsonMapper.State.READ);
    String resourcePath = UriConstants.getThreadStateUri(testThread.getThreadId(), user.getUserId());
    return getHttpResponseEntity(resourcePath, user, Method.PUT, requestJson);
  }
  // END UserEndpoints

  // MessageResource endpoints
  public String getThreadMessages(TestThread testThread, String query) {
    TestUser user = getParticipantForThread(testThread);
    return getThreadMessages(testThread, query, user);
  }

  public String getThreadMessages(TestThread testThread, String query, TestUser user) {
    String resourcePath = UriConstants.getThreadMessagesUri(testThread.getThreadId()) + query;
    return getHttpResponseEntity(resourcePath, user, Method.GET);
  }

  // END MessageResource

  public String getHttpResponseEntity(String resourcePath, TestUser user, Method method, String... payload) {
    try {
      ClientResponse response = getHttpResponse(resourcePath, user, method, payload);
      return response.getEntity(String.class);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public String getResourceLocationFrom201Response(ClientResponse response) {
    if(response.getStatus() == Response.Status.CREATED.getStatusCode()) {
      return UriConstants.getNewResourceLocation(response);
    }

    throw new RuntimeException("Expected http response of 201 but got " + response.getStatus());
  }

  public ClientResponse getHttpResponse(String resourcePath, TestUser user, Method method, String... payload) {
    WebResource resource = resourceForPath(resourcePath);
    return getResponse(resource, method, user.getSessionId(), payload);
  }

  // verifies timeStamps are ordered from newest to oldest.. if oldest to newest is required,
  // reverse the collection order before calling this
  public static boolean verifySortOrder(List<String> timeStamps) {
    if (timeStamps.size() < 2) { // need at least 2 to compare
      return true;
    }

    boolean ok = true;

    for (int idx = 0; idx < timeStamps.size() - 1; idx++) {
      ZonedDateTime thisTime = ZonedDateTime.parse(timeStamps.get(idx));
      ZonedDateTime nextTime = ZonedDateTime.parse(timeStamps.get(idx + 1));

      ok &= !thisTime.isBefore(nextTime); // == thisTime >= nextTime
    }
    return ok;
  }

  public TestUser getNonParticipantForThread(TestThread testThread) {
    String userId = allTestUsers.keySet().stream()
      .filter(id -> !testThread.getParticipantIds().contains(id))
      .findAny().get();

    return allTestUsers.get(userId);
  }

  public TestUser getNonParticipantForThreadBesides(TestThread testThread, TestUser user) {
    String userId = allTestUsers.keySet().stream()
      .filter(id -> !testThread.getParticipantIds().contains(id))
      .filter(id -> !id.equals(user.getUserId()))
      .findAny().get();

    return allTestUsers.get(userId);
  }

  public TestUser getFirstMessageAuthorForThread(TestThread testThread) {
    return allTestUsers.get(testThread.getFirstMessage().getAuthorId());
  }

  public TestUser getParticipantForThread(TestThread testThread) {
    List<String> userIds = testThread.getParticipantIds();
    Collections.shuffle(userIds);
    String userId = userIds.stream().findAny().get();
    return allTestUsers.get(userId);
  }

  public TestUser getParticipantForThreadBesides(TestThread testThread, TestUser user) {
      List<String> userIds = testThread.getParticipantIds();
    Collections.shuffle(userIds);
    String userId = userIds.stream()
      .filter(s -> !s.equals(user.getUserId()))
      .findAny().get();

    return allTestUsers.get(userId);
  }

  public TestUser getTestUser(String userId) {
    return (TestUser)getAllTestUsers().get(userId);
  }

  public TestUser getRandomUser() {
    return getRandomUsers(1).get(0);
  }

  private List<String> getRandomUserIds(int count) {
    return getRandomUsers(count).stream().map(TestUser::getUserId).collect(Collectors.toList());
  }

  public List<TestUser> getRandomUsers(int count) {
    return getRandomUsers(count, new ArrayList<>(allTestUsers.keySet()));
  }

  public TestUser getTestUserBesides(TestUser user) {
    List<String> userIds = new ArrayList<>(allTestUsers.keySet());
    userIds.remove(user.getUserId());
    return getRandomUsers(1, userIds).get(0);
  }

  public List<TestUser> getRandomTestUsersBesides(TestUser user) {
    int count = ThreadLocalRandom.current().nextInt(2, getAllTestUsers().size());
    List<String> userIds = new ArrayList<>(allTestUsers.keySet());
    userIds.remove(user.getUserId());
    return getRandomUsers(count, userIds);
  }

  private List<TestUser> getRandomUsers(int count, List<String> userIds) {
    List<TestUser> returnUsers = new ArrayList<>();

    IntStream.range(0, count).forEach(idx -> {
        Collections.shuffle(userIds);
        String userId = userIds.get(0);
        userIds.remove(0);
        returnUsers.add(allTestUsers.get(userId));
      });

    return returnUsers;
  }

  public TestThread getRandomTestThread() {
    List<TestThread> threads = new ArrayList<>(testThreads.values());
    Collections.shuffle(threads);
    return threads.get(0);
  }

  public TestGroup getRandomTestGroup() {
    List<TestGroup> groups = new ArrayList<>(testGroups.values());
    Collections.shuffle(groups);
    return groups.get(0);
  }

  // if it doesn't matter who the message author is..
  public TestMessage createNewMessageForThread(TestThread testThread) {
    TestUser user = getParticipantForThread(testThread);
    return createNewMessageForThread(testThread, user);
  }

  // if it matters who the message author is..
  public TestMessage createNewMessageForThread(TestThread testThread, TestUser author) {
    String messageBody = getRandomThreadMessageForUser(author);

    String requestJson = JsonMapper.createJsonMessageSnippet(author.getUserId(), messageBody);
    TestMessage testMessage = TestMessage.createMessage(testThread, requestJson);

    log.info(String.format("Added message %s to thread %s", testMessage.getMessageId(), testThread.getThreadId()));
    return testMessage;
  }

  public void createThreadWithMultipleMessages()  {
    TestThread testThread = postNewThread();
    // testThread already has first message - add 9 more for a total of 10 messages
    IntStream.range(0, 9).forEach(idx -> {
      try {
        createNewMessageForThread(testThread);
      }catch(Exception ex) { throw new RuntimeException(ex); }
    });
  }

  // run in parallel to make execution faster..
  public void iterateMethod(Consumer method, int iterations) {
    List<Runnable> tasks = new ArrayList<>();
    IntStream.range(0, iterations).forEach(i ->
      tasks.add(() -> method.accept(i))
    );
    runTasksInParallel(tasks);
  }

  // used to call any method for all the test users
  public void runMethodForAllTestUsers(Consumer<TestUser> method) {
    List<Runnable> tasks = new ArrayList<>();
    allTestUsers.values().stream().forEach(testUser ->
      tasks.add(() -> method.accept(testUser))
    );

    runTasksInParallel(tasks);
  }

  // run the task in parallel using a thread pool
  private static void runTasksInParallel(List<Runnable> tasks) {
    //ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
    ExecutorService executor = Executors.newCachedThreadPool();
    tasks.stream().forEach(task -> executor.submit(task));

    executor.shutdown();
    try {
      executor.awaitTermination(120, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getRandomThreadMessageForUser(TestUser user) {
    return String.format("message from user %s\n%s\n%s",
      user.getUserName(), ZonedDateTime.now().toString(), getRandomString(100));
  }

  public void deletePrivateGroupsForUser(TestUser user) {
    WebResource resource = resourceForPath(UriConstants.getGroupsUri(user.getUserId()));
    ClientResponse response = getResponse(resource, Method.GET, user.getSessionId());

    if(response.getStatus() == Response.Status.OK.getStatusCode()) {
      String responseJson = response.getEntity(String.class);
      List<String> groupNames = JsonPath.read(responseJson, "$.groups[*].name");

      List<String> deletedGroups = groupNames.stream()
        .filter(name -> name.startsWith(GROUP_NAME))
        .map(name -> {
          WebResource webResource = resourceForPath(UriConstants.getGroupUri(user.getUserId(), name));
          ClientResponse clientResponse = getResponse(webResource, Method.DELETE, user.getSessionId());
          if (clientResponse.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            log.error(String.format("Error deleting group %s for user %s", GROUP_NAME, user.getUserId()));
          }
          return name;
        })
        .collect(Collectors.toList());

      log.info(String.format("deleted groups %s for user %s", deletedGroups, user.getUserId()));
    }
  }

  public void softDeleteAllThreadsForUser(TestUser user) {
    WebResource resource = resourceForPath(UriConstants.getUserCountersUri(user.getUserId()));
    ClientResponse response = getResponse(resource, Method.GET, user.getSessionId());
    assertResponse(response, Response.Status.OK.getStatusCode());
    String responseJson = response.getEntity(String.class);
    int pageSize = JsonPath.read(responseJson, "$.totalThreads");

    resource = resourceForPath(String.format(UriConstants.getUserThreadsUri(user.getUserId()) + "?pageSize=%s", pageSize));
    response = getResponse(resource, Method.GET, user.getSessionId());
    if(response.getStatus() == Response.Status.OK.getStatusCode()) {
      responseJson = response.getEntity(String.class);
      List<String> threadIds = JsonPath.read(responseJson, "$.userThreadSummaries[*].threadId");

      List<String> deletedThreadIds = threadIds.stream()
        .map(threadId -> {
          WebResource webResource = resourceForPath(UriConstants.getThreadStateUri(threadId, user.getUserId()));

          String requestJson = null;
          try {
            requestJson = JsonMapper.createJsonThreadStateSnippet(JsonMapper.State.TRASH);
          } catch(Exception ex) { throw new RuntimeException(ex); }

          ClientResponse clientResponse = getResponse(webResource, Method.PUT, user.getSessionId(), requestJson);
          if(clientResponse.getStatus() == Response.Status.OK.getStatusCode()) {
            if(clientResponse.getEntity(String.class).contains("TRASH")) {
              return threadId;
            }
            else {
              log.warn(String.format("Thread %s didn't have a status of 'TRASH' after soft deleting..", threadId));
            }
          }
          return null;
        })
        .collect(Collectors.toList());
      log.info(String.format("soft deleted %s threads for user %s", deletedThreadIds.size(), user.getUserId()));
    }
  }

}
