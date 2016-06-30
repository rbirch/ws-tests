package org.familysearch.cmp.messages.acceptance;

import com.jayway.jsonpath.JsonPath;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.familysearch.cmp.messages.acceptance.util.*;
import org.familysearch.cmp.qa.AbstractAcceptanceTest;
import org.familysearch.cmp.qa.util.BaseTestHelper;
import org.familysearch.cmp.qa.util.TestUser;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static com.googlecode.catchexception.CatchException.*;

/**
 * Created by rob birch on 12/31/2015.
 */
public class MessageEndpointsAT extends AbstractAcceptanceTest {
  public static TestHelper testHelper;

  @BeforeClass(alwaysRun = true)
  public void setup() {
    this.testHelper = new TestHelper(getTargetUrl());
    testHelper.setup();
  }

  @Override
  protected String getBasePathWithRouting() {
    return "/fst/fs-messages";
  }

  @Override
  protected String getBasePathDirect() {
    return "";
  }

  @Override
  protected String getBlueprintServiceNameOfTargetService() {
    return "app";
  }

  //host should match amazon ec2 instance ip/name
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetHost() {
    boolean ok = true;
    String domain = getDomain().toLowerCase();
    String actual = null;
    for (TestThread testThread : testHelper.testThreads.values()) {
      String threadResource = testThread.getThreadResource().toString();
      ok &= threadResource.startsWith(domain);
      actual = threadResource.substring(0, threadResource.indexOf("/threads"));
    }

    assertTrue(ok, String.format("The actual host for the thread resources was incorrect.. expected: %s - actual: %s",
      domain, actual));
  }

  /***************************************************************************
   security tests -
   non-authenticated user
   ****************************************************************************/
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetThreadsForUserNoAuthentication() {
    String sessionId = null;
    TestUser user = testHelper.getRandomUser();
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getUserThreadsUri(user.getUserId())), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetMessagesForThreadNoAuthentication() {
    String threadId = testHelper.getRandomTestThread().getThreadId();
    String sessionId = null;
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadMessagesUri(threadId)), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testPostThreadAndMessageNoAuthentication() {
    String sessionId = null;
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadsUri()), BaseTestHelper.Method.POST, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testPostNewMessageToExistingThreadNoAuthentication() throws Exception {
    String sessionId = null;
    String threadId = testHelper.getRandomTestThread().getThreadId();
    TestUser user = testHelper.getCommonUser();

    String threadBody = testHelper.getRandomThreadMessageForUser(user);
    String requestJson = JsonMapper.createJsonMessageSnippet(user.getUserId(), threadBody);

    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadMessagesUri(threadId)), BaseTestHelper.Method.POST, sessionId, requestJson);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetMailboxStatisticsNoAuthentication() {
    String sessionId = null;
    TestUser user = testHelper.getRandomUser();
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getUserCountersUri(user.getUserId())), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetMessageNoAuthentication() {
    String threadId = testHelper.getRandomTestThread().getThreadId();
    String sessionId = null;

    Set msgKeys = testHelper.testThreads.get(threadId).getMessages().keySet();
    String msgId = msgKeys.toArray()[0].toString();
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadMessageUri(threadId,
      msgId)), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetThreadStateNoAuthentication() {
    String threadId = testHelper.getRandomTestThread().getThreadId();
    String sessionId = null;

    TestUser user = testHelper.getRandomUser();
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadStateUri(threadId,
      user.getUserId())), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testUpdateThreadStateNoAuthentication() {
    String sessionId = null;
    String threadId = testHelper.getRandomTestThread().getThreadId();
    TestUser user = testHelper.getRandomUser();
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadStateUri(threadId,
      user.getUserId())), BaseTestHelper.Method.PUT, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetThreadNoAuthentication() {
    String threadId = testHelper.getRandomTestThread().getThreadId();
    String sessionId = null;
    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadSpecificUri(threadId)), BaseTestHelper.Method.GET, sessionId);
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  /***************************************************************************
   security tests -
   1 user attempting to use another's session
   ****************************************************************************/
  // create a new thread and first message masquerading as some other author - author doesn't match session
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testPostThreadAsMasqueradingAuthor() {
    ClientResponse response = testHelper.postNewThreadByNonParticipantAuthor();
    TestHelper.assertResponse(response, Response.Status.FORBIDDEN.getStatusCode());
  }

  // same test as above, except sending a message on an existing thread = one participant using the other's session
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testSendMessageWithParticipantMasqueradingAsOtherParticipant() {
    TestThread testThread = testHelper.getRandomTestThread();
    TestUser participant1 = testHelper.getParticipantForThread(testThread); // impostor author
    TestUser participant2 = testHelper.getParticipantForThreadBesides(testThread, participant1); //intended author

    String requestJson = JsonMapper.createJsonMessageSnippet(participant1.getUserId(), testHelper.getRandomThreadMessageForUser(participant1));
    String resourcePath = UriConstants.getThreadMessagesUri(testThread.getThreadId());
    WebResource resource = testHelper.resourceForPath(resourcePath);
    ClientResponse response = testHelper.getResponse(resource, BaseTestHelper.Method.POST, participant2.getSessionId(), requestJson);
    TestHelper.assertResponse(response, Response.Status.FORBIDDEN.getStatusCode());
  }

  // send a message as a user that isn't a thread participant - should get 403
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testSendMessageAsNonParticipant() {
    // create a new thread
    TestThread testThread = testHelper.postNewThread();

    // get a test user that isn't a participant on the thread
    TestUser nonParticipant = testHelper.getNonParticipantForThread(testThread);
    // this call should throw an exception since there's a non-participant author..
    catchException(testHelper).createNewMessageForThread(testThread, nonParticipant);
    String message = caughtException().getMessage();
    assertTrue(message.contains("403"), String.format("The http response was incorrect - expected: 403, actual: %s", message));
  }

  @Test(groups = {"new"}, timeOut = 600000)
  public void getUserThreadsAsNonParticipantMasqueradingAsParticipant() {
    TestThread testThread = testHelper.getRandomTestThread();

    TestUser nonParticipant = testHelper.getNonParticipantForThread(testThread);
    TestUser participant = testHelper.getParticipantForThread(testThread);

    ClientResponse response = testHelper.getResponse(testHelper.resourceForPath(UriConstants.getThreadSpecificUri(testThread.getThreadId())),
      BaseTestHelper.Method.GET, nonParticipant.getSessionId());
    TestHelper.assertResponse(response, Response.Status.UNAUTHORIZED.getStatusCode());
  }

  /***************************************************************************
    authenticated tests - user matches session
  ****************************************************************************/

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetUserThreads() {
    TestUser user = testHelper.getRandomUser();

    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, "300");
    String responseJson = testHelper.getUserThreads(user, qb.createQuery());

    TestHelper.assertJsonResponseField(responseJson, "page");
    TestHelper.assertJsonResponseField(responseJson, "userThreadSummaries");

    List<JSONObject> threadSummaries = JsonPath.read(responseJson, "$.userThreadSummaries");
    String threadSummary = threadSummaries.get(new Random().nextInt(threadSummaries.size())).toString();

    JSONObject summary = JsonPath.read(threadSummary, "$.");
    assertEquals(summary.size(), 9, "The number of fields in the thread summary was incorrect");

    TestHelper.assertJsonResponseField(threadSummary, "threadId");
    TestHelper.assertJsonResponseField(threadSummary, "subject");
    TestHelper.assertJsonResponseField(threadSummary, "about");
    TestHelper.assertJsonResponseField(threadSummary, "aboutUrl");
    TestHelper.assertJsonResponseField(threadSummary, "msgCount");
    //testHelper.assertJsonResponseField(threadSummary, "unreadMsgCount");
    TestHelper.assertJsonResponseField(threadSummary, "lastModifiedTime");
    TestHelper.assertJsonResponseField(threadSummary, "participantIds");
    TestHelper.assertJsonResponseField(threadSummary, "userThreadState");

    //TODO: verify TestThread fields in threads
    assertEquals(JsonPath.read(responseJson, "$.page.pageNumber").toString(), "0", "The page element should have been 0 since no paging was done..");
    assertEquals(JsonPath.read(responseJson, "$.page.totalPages").toString(), "1", "the totalPages element should have been 1 since all threads were retrieved..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetAllMessagesForThread() {
    TestThread testThread = testHelper.getRandomTestThread();

    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, "50");

    String responseJson = testHelper.getThreadMessages(testThread, qb.createQuery());

    // top level fields
    TestHelper.assertJsonResponseField(responseJson, "page");
    TestHelper.assertJsonResponseField(responseJson, "messages");

    int pageNumber = JsonPath.read(responseJson, "$.page.pageNumber");
    assertEquals(pageNumber, 0, "The page element should have been 0 since no paging was done..");
    int totalPages = JsonPath.read(responseJson, "$.page.totalPages");
    assertEquals(totalPages, 1, "the totalPages element should have been 1 since all threads were retrieved..");

    ArrayList<TestMessage> expectedMessages = new ArrayList(testThread.getMessages().values());
    ArrayList<JSONObject> actualMessages = JsonPath.read(responseJson, "$.messages"); // actualThread.getJSONArray("messages");
    assertEquals(actualMessages.size(), expectedMessages.size(), "The number of messages for the thread was incorrect");

    //loop through thread messages and verify fields
    for (int idx = 0; idx < expectedMessages.size(); idx++) {
      TestMessage expected = expectedMessages.get(idx);

      JSONObject actual = actualMessages.get(idx);
      assertEquals(actual.get("id"), expected.getMessageId(), "Mismatched messageId's..");
      assertEquals(actual.get("authorId"), expected.getAuthorId(),
        String.format("The authorId for the message %s was incorrect for thread %s..", expected.getAuthorId(), testThread.getThreadId()));
      assertEquals(actual.get("body"), expected.getBody(),
        String.format("The body for the message %s was incorrect for thread %s..", expected.getBody(), testThread.getThreadId()));
    }
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testPostThreadAndFirstMessage() {
    TestThread testThread = testHelper.postNewThread();
    assertNotNull(testThread.getThreadId(), "The thread that was supposed to be created was null..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testPostNewMessageToExistingThread() {
    TestThread testThread = testHelper.getRandomTestThread();
    TestMessage testMessage = testHelper.createNewMessageForThread(testThread);
    assertNotNull(testMessage.getMessageResource(), "The message that was supposed to be created was null");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetMailboxStatistics() {
    TestUser user = testHelper.getRandomUser();

    String responseJson = testHelper.getUserCounters(user);

    int threadCount = JsonPath.read(responseJson, "$.totalThreads");
    int actualMsgCountTotal = JsonPath.read(responseJson, "$.totalMsgs");
    int actualUnreadMsgCountTotal = JsonPath.read(responseJson, "$.totalUnreadMsgs");
    int actualUnreadThreadCountTotal = JsonPath.read(responseJson, "$.totalUnreadThreads");

    //TODO: is this a valid way to get the user counts in order to compare to the values returned from the counters call?
    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, Integer.toString(threadCount));

    responseJson = testHelper.getUserThreads(user, qb.createQuery());

    int expectedMsgCountTotal = 0;
    int expectedUnreadMsgCountTotal = 0;
    int expectedUnreadThreadCountTotal = 0;

    JSONArray userThreads = JsonPath.read(responseJson, "$.userThreadSummaries");
    for (Object thread : userThreads) {
      expectedMsgCountTotal += (int)JsonPath.read(thread, "$.msgCount");
      int unreadMsgCount = (int)JsonPath.read(thread, "$.unreadMsgCount");
      expectedUnreadMsgCountTotal += unreadMsgCount;
      if (unreadMsgCount > 0) {
        expectedUnreadThreadCountTotal += 1;
      }
    }

    assertEquals(actualMsgCountTotal, expectedMsgCountTotal, "The total message count was incorrect..");
    assertEquals(actualUnreadMsgCountTotal, expectedUnreadMsgCountTotal, "The total unread message count was incorrect..");
    assertEquals(actualUnreadThreadCountTotal, expectedUnreadThreadCountTotal, "The total unread thread count was incorrect..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetThreadState() {
    TestThread testThread = testHelper.postNewThread();

    TestUser author = testHelper.getFirstMessageAuthorForThread(testThread);
    TestUser receiver = testHelper.getParticipantForThreadBesides(testThread, author);

    //get the thread state for the original message author
    String responseJson = testHelper.getUserThreadState(testThread, author);

    // initial thread state should display as 'READ' for sender
    String status = JsonPath.read(responseJson, "$.status");
    assertEquals(status, "READ", "The initial thread state for the message sender was incorrect..");

    // get the thread state for the receiver
    responseJson = testHelper.getUserThreadState(testThread, receiver);

    // initial thread state should display as 'UNREAD' for receiver
    status = JsonPath.read(responseJson, "$.status");
    assertEquals(status, "UNREAD", "The initial thread state for the message recipient was incorrect..");

    // have receiver respond to the message
    TestMessage testMessage = testHelper.createNewMessageForThread(testThread, receiver);
    String msgResource = testMessage.getMessageResource().toString();
    assertNotNull(msgResource, "The new message resource location was null..");

    // get the state for the original author again
    responseJson = testHelper.getUserThreadState(testThread, author);

    // now the thread status should display as 'UNREAD' for original author
    status = JsonPath.read(responseJson, "$.status");
    assertEquals(status, "UNREAD", "The thread state for the message sender after the recipient replied to a message was incorrect..");

    // get the state for the receiver again
    responseJson = testHelper.getUserThreadState(testThread, receiver);

    // now the thread status should display as 'READ' for receiver
    status = JsonPath.read(responseJson, "$.status");
    assertEquals(status, "UNREAD", "The thread state for the message recipient after the receiver replied to a message was incorrect..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testSoftDeleteThread() {
    //get a TestThread
    TestThread testThread = testHelper.getRandomTestThread();
    String threadId = testThread.getThreadId();

    TestUser user1 = testHelper.getCommonUser();
    TestUser user2 = testHelper.getParticipantForThreadBesides(testThread, user1);

    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, "100");
    String query = qb.createQuery();

    // get user threads for user1
    String responseJson = testHelper.getUserThreads(user1, query);

    // verify the threadId is included in the threads for user1
    List threadIds = JsonPath.read(responseJson, "$.userThreadSummaries[*].threadId");
    assertTrue(threadIds.contains(testThread.getThreadId()),
      String.format("The expected thread %s wasn't included in the threads for the user %s before deleting..", threadId, user1.getUserId()));

    // get thread state
    responseJson = testHelper.getUserThreadState(testThread, user1);
    // verify that the status isn't already 'TRASH'
    // TODO: can the status of 'TRASH' even be returned here?  If so, should we just get another threadId until we get one that isn't 'TRASH'
    assertFalse(JsonPath.read(responseJson, "$.status").equals("TRASH"), String.format("The status for thread %s was already 'TRASH'..", threadId));

    // set the status to 'TRASH' for user1
    responseJson = testHelper.setUserThreadStateToTrash(testThread, user1);
    assertEquals(JsonPath.read(responseJson, "$.status"), "TRASH", "The status wasn't set to 'TRASH'..");

    // get the threads again for user1 and verify the deleted thread isn't there anymore
    responseJson = testHelper.getUserThreads(user1, query);
    threadIds = JsonPath.read(responseJson, "$.userThreadSummaries[*].threadId");
    assertFalse(threadIds.contains(threadId), "The deleted thread was still returned in the user's threads after being soft deleted..");

    // now verify that the deleted thread is still there for user2
    responseJson = testHelper.getUserThreads(user2, query);
    threadIds = JsonPath.read(responseJson, "$.userThreadSummaries[*].threadId");
    assertTrue(threadIds.contains(threadId), "The deleted thread was not included in the receiver's threads after being soft deleted by the sender..");

    testHelper.testThreads.remove(threadId);
    testHelper.deletedThreads.put(threadId, testThread);
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testSetThreadState() {
    TestThread testThread = testHelper.postNewThread();
    TestUser user = testHelper.getFirstMessageAuthorForThread(testThread);

    // set the status as 'READ'
    String responseJson = testHelper.setUserThreadStateToRead(testThread, user);
    assertEquals(JsonPath.read(responseJson, "$.status"), "READ", "The status wasn't set to 'READ'..");

    // double check by getting the thread state again
    responseJson = testHelper.getUserThreadState(testThread, user);
    assertEquals(JsonPath.read(responseJson, "$.status"), "READ", "The status wasn't set to 'READ'..");

    // now check that the status is unchanged for a different uer on the thread
    TestUser otherUser = testHelper.getParticipantForThreadBesides(testThread, user);
    responseJson = testHelper.getUserThreadState(testThread, otherUser);
    assertEquals(JsonPath.read(responseJson, "$.status"), "UNREAD", "The status for another thread participant wasn't still 'UNREAD'..");

    // triple check by getting the thread object
    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, "100");
    String query = qb.createQuery();
    responseJson = testHelper.getUserThreads(user, query);

    List<JSONObject> userThreads = JsonPath.read(responseJson, "$.userThreadSummaries");
    JSONObject userThread = userThreads.stream()
      .filter(obj -> obj.get("threadId").equals(testThread.getThreadId()))
      .findFirst().get();

    String status = userThread.get("userThreadState").toString();
    assertEquals(status, "READ", "The status wasn't set to 'READ'..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetThread() {
    TestThread testThread = testHelper.getRandomTestThread();

    String responseJson = testThread.getRepresentation();
    String thread = JsonPath.read(responseJson, "$.").toString();

    //TODO: verify threadParticipants, author, and (resource?)
    // primary fields
    TestHelper.assertJsonResponseField(thread, "id");
    TestHelper.assertJsonResponseField(thread, "participantIds");
    TestHelper.assertJsonResponseField(thread, "subject");
    TestHelper.assertJsonResponseField(thread, "about");
    TestHelper.assertJsonResponseField(thread, "aboutUrl");
    TestHelper.assertJsonResponseField(thread, "firstMessage");
    
    String firstMessage = JsonPath.read(thread, "$.firstMessage").toString();
    TestHelper.assertJsonResponseField(firstMessage, "id");
    TestHelper.assertJsonResponseField(firstMessage, "authorId");
    TestHelper.assertJsonResponseField(firstMessage, "body");
    TestHelper.assertJsonResponseField(firstMessage, "created");

    String actualAuthorId = JsonPath.read(thread, "$.firstMessage.authorId");
    assertEquals(actualAuthorId, testThread.getFirstMessage().getAuthorId(), "The first message author didn't match..");

    JSONArray actualParticipants = JsonPath.read(thread, "$.participantIds");
    assertEquals(new HashSet<>(actualParticipants), new HashSet<>(testThread.getParticipantIds()), "The actual participantId's were incorrect..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testThreadPaging() throws Exception {
    int pageNumber = 0;
    int pageSize = 3;
    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, Integer.toString(pageSize));

    //TODO: possibly add another test where the number of threads isn't evenly divisible by the pageSize - should have to then + 1 to the expectedPages
    int expectedPages = (testHelper.testThreads.size() % pageSize == 0) ? testHelper.testThreads.size() / pageSize : testHelper.testThreads.size() / pageSize + 1;
    int actualPages = 0;

    TestUser user = testHelper.getCommonUser();

    String responseJson = testHelper.getUserThreads(user, qb.createQuery());
    int actualTotalElements = JsonPath.read(responseJson, "$.page.totalElements");
    assertEquals(actualTotalElements, testHelper.testThreads.size(), "The total elements returned was incorrect..");

    int totalPages = JsonPath.read(responseJson, "$.page.totalPages");
    assertEquals(totalPages, expectedPages, "The totalPages element didn't match the expected total pages..");

    // pageNumber starts with 0, so while needs to be (actualPages < totalPages)
    do {
      responseJson = testHelper.getUserThreads(user, qb.createQuery());
      //TODO: do we want to do anything in this loop?
      pageNumber = JsonPath.read(responseJson, "$.page.pageNumber");
      qb.addParameter(UriConstants.QueryBuilder.QueryParameters.page, Integer.toString(++actualPages));
    }
    while (actualPages < totalPages);

    assertEquals(actualPages, expectedPages,
      String.format("The number of pages used was incorrect for %s threads with a pageSize of %s..", testHelper.testThreads.size(), pageSize));

    //TODO: should this be part of this test? or in it's own test - posting a new message to a thread and verifying it shows up in the correct place?
    // post a new message to the thread - the thread should move to the top
    TestThread expectedTestThread = testHelper.getRandomTestThread();

    TestMessage testMessage = testHelper.createNewMessageForThread(expectedTestThread, user);
    assertNotNull(testMessage.getMessageResource(), "The new message resource location was null..");

    // get the threads again
    responseJson = testHelper.getUserThreads(user, "");
    List<String> actualThreadIds = JsonPath.read(responseJson, "$.userThreadSummaries[*].threadId");
    assertEquals(actualThreadIds.get(0), expectedTestThread.getThreadId(), "The updated thread wasn't the first one returned in the user threads..");
  }

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testMessagePaging() throws Exception {
    int pageSize = 3;
    int pageNumber = 0;
    int actualPages = 0;
    String queryParam = "?pageSize=" + pageSize;

    //TestUser user = sendingUser;
    TestUser user = testHelper.getCommonUser();
    TestThread testThread = testHelper.getRandomTestThread();
    Map messages = testThread.getMessages();

    int expectedPages = (messages.size() % pageSize == 0) ? messages.size() / pageSize : messages.size() / pageSize + 1;

    String resourcePath = UriConstants.getThreadMessagesUri(testThread.getThreadId()) + queryParam;
    String responseJson = testHelper.getHttpResponseEntity(resourcePath, user, BaseTestHelper.Method.GET);

    int actualTotalElements = JsonPath.read(responseJson, "$.page.totalElements");
    assertEquals(actualTotalElements, messages.size(), String.format("The total elements returned for thread %s was incorrect..", messages.toString()));

    int totalPages = JsonPath.read(responseJson, "$.page.totalPages");

    // pageNumber starts with 0, so while needs to be (actualPages < totalPages)
    do {
      responseJson = testHelper.getHttpResponseEntity(resourcePath, user, BaseTestHelper.Method.GET);
      pageNumber = JsonPath.read(responseJson, "$.page.pageNumber");

      resourcePath = String.format(UriConstants.getThreadMessagesUri(testThread.getThreadId()) + "?pageSize=%s&page=%s", pageSize, ++actualPages);
    }
    while (actualPages < totalPages);

    assertEquals(actualPages, expectedPages,
      String.format("The number of pages used was incorrect for %s messages with a pageSize of %s..", messages.size(), pageSize));

    // post a new message to the thread
    //TODO: should this be part of this test? or in it's own test - posting a new message to a thread and verifying it shows up in the correct place?
    TestMessage testMessage = testHelper.createNewMessageForThread(testThread, user);

    // get the messages again
    resourcePath = UriConstants.getThreadMessagesUri(testThread.getThreadId()) + "?pageSize=" + testThread.getMessages().size();
    responseJson = testHelper.getHttpResponseEntity(resourcePath, user, BaseTestHelper.Method.GET);
    List<String> actualMessageIds = JsonPath.read(responseJson, "$.messages[*].id");
    assertEquals(actualMessageIds.get(actualMessageIds.size() - 1), testMessage.getMessageId(), "The new message wasn't the last one returned in the thread messages..");
  }

  // TODO: split into 3 tests, default, explicit asc, explicit desc
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testThreadSorting() throws Exception {
    TestThread testThread = testHelper.getRandomTestThread();

    TestUser user = testHelper.getParticipantForThread(testThread);

    String resourcePath = UriConstants.getUserCountersUri(user.getUserId());
    String responseJson = testHelper.getHttpResponseEntity(resourcePath, user, BaseTestHelper.Method.GET);
    String threadCount = JsonPath.read(responseJson, "$.totalThreads").toString();

    // default sorting - descending
    UriConstants.QueryBuilder query = new UriConstants.QueryBuilder();
    query.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, threadCount);
    responseJson = testHelper.getUserThreads(user, query.createQuery());

    List<String> timeStamps = JsonPath.read(responseJson, "$.userThreadSummaries[*].lastModifiedTime");
    boolean ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The threads weren't sorted in the default descending order..");

    // explicit ascending order sort
    query.addParameter(UriConstants.QueryBuilder.QueryParameters.sort, UriConstants.QueryBuilder.SortParamValues.ASC.toString());
    responseJson = testHelper.getUserThreads(user, query.createQuery());

    timeStamps = JsonPath.read(responseJson, "$.userThreadSummaries[*].lastModifiedTime");
    Collections.reverse(timeStamps);
    ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The threads weren't sorted in specified ascending order..");

    // explicit descending order sort
    query.addParameter(UriConstants.QueryBuilder.QueryParameters.sort, UriConstants.QueryBuilder.SortParamValues.DESC.toString());
    responseJson = testHelper.getUserThreads(user, query.createQuery());

    timeStamps = JsonPath.read(responseJson, "$.userThreadSummaries[*].lastModifiedTime");
    ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The threads weren't sorted in specified descending order..");
  }

  // TODO: split into 3 tests, default, explicit ASC, explicit DESC
  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testMessageSorting() throws Exception {
    TestThread testThread = testHelper.getRandomTestThread();
    TestUser user = testHelper.getParticipantForThread(testThread);

    String responseJson = testHelper.getUserCounters(user);
    String msgTotal = JsonPath.read(responseJson, "$.totalMsgs").toString();

    // default sort order - descending
    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, msgTotal);
    // TODO: returning only 1 message
    responseJson = testHelper.getThreadMessages(testThread, qb.createQuery(), user);

    List<String> timeStamps = JsonPath.read(responseJson, "$.messages[*].created");
    Collections.reverse(timeStamps);
    boolean ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The messages weren't sorted in ascending order when called with default sorting..");

    // explicit sort order - ascending
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.sort, UriConstants.QueryBuilder.SortParamValues.ASC.toString());
    responseJson = testHelper.getThreadMessages(testThread, qb.createQuery(), user);

    timeStamps = JsonPath.read(responseJson, "$.messages[*].created");
    Collections.reverse(timeStamps);
    ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The messages weren't sorted in ascending order after explicitly setting the order to 'ASC'..");

    // explicit sort order - descending
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.sort, UriConstants.QueryBuilder.SortParamValues.DESC.toString());
    responseJson = testHelper.getThreadMessages(testThread, qb.createQuery(), user);

    timeStamps = JsonPath.read(responseJson, "$.messages[*].created");
    ok = TestHelper.verifySortOrder(timeStamps);
    assertTrue(ok, "The messages weren't sorted in descending order after explicitly setting the order to 'DESC'..");
  }

  /***************************************************************************
   security tests
   ****************************************************************************/



  //need some group endpoint security tests - making api call as one user with another's session

  @Test(groups = {"unsafe"}, timeOut = 600000)
  public void testGetUserContacts() {
    TestUser user = testHelper.getCommonUser();
    // build a list of participants that will all be the user's contacts
    Set<String> expectedContacts = new HashSet<>();

    testHelper.testThreads.values().stream().forEach(thread -> {
        if(thread.getParticipantIds().contains(user.getUserId())) {
          expectedContacts.addAll(thread.getParticipantIds());
        }
      });

    //remove the user
    expectedContacts.remove(user.getUserId());

    UriConstants.QueryBuilder qb = new UriConstants.QueryBuilder();
    qb.addParameter(UriConstants.QueryBuilder.QueryParameters.pageSize, "1000");
    String responseJson = testHelper.getGroupContacts(user);

    List<String> actualContacts = JsonPath.read(responseJson, "$.contacts[*].userId");
    boolean ok = actualContacts.containsAll(expectedContacts);

    assertTrue(ok, String.format("The contacts returned for user %s were incorrect - expected: %s, actual: %s", user.getUserId(), expectedContacts, actualContacts));
  }

  @Test(groups = {"unsafe"})
  public void testPrivateGroups() {
    // get a testGroup
    TestGroup testGroup = testHelper.getRandomTestGroup();
    TestUser owner = testGroup.getOwner();

    List<String> expectedMemberIds = testGroup.getMembers();

    // get the group members - should contain the contacts added in previous step
    String responseJson = testGroup.getMembersRepresentation();
    List<String> actualMemberIds = JsonPath.read(responseJson, "$.contacts[*].userId");
    boolean ok = actualMemberIds.containsAll(expectedMemberIds);
    assertTrue(ok, String.format("The group members returned for group %s was incorrect - expected: %s, actual: %s", testGroup.getName(), expectedMemberIds, actualMemberIds));

    // attempt to delete the group with a user besides the owner
    String response = testGroup.deleteByNonOwner();
    assertTrue(response.contains("403"),
      String.format("A non 403 response was returned when a user that wasn't the group owner attempted to delete the group.. response: %s", response));

    // get the groups the user belongs to - should include testGroup group name
    responseJson = testHelper.getGroupsForUser(owner);
    List<String> groupNames = JsonPath.read(responseJson, "$.groups[*].name");
    assertTrue(groupNames.contains(testGroup.getName()),
      String.format("The groups returned that user %s is a member of didn't contain the test group created..", owner.getUserId(), testGroup.getName()));

    // delete the members of the group
    testGroup.deleteAllMembers();

    // get the members of the group again  - should be empty list
    responseJson = testGroup.getMembersRepresentation();
    try { // if the collection is empty, it isn't returned, just the count is returned
      int contactCount = JsonPath.read(responseJson, "$.group.contactCount");
      assertEquals(contactCount, 0, String.format("The group members weren't all deleted as expected.. response json after delete: %s", responseJson));
    }
    catch(Exception ignore) {
      actualMemberIds = JsonPath.read(responseJson, "$.contacts[*].userId");
      assertEquals(actualMemberIds.size(), 0, String.format("the group members weren't all deleted as expected.. response json after delete: %s", responseJson));
    }

    // get the group - member count should be 0
    responseJson = testGroup.getRepresentation();
    int contactCount = JsonPath.read(responseJson, "$.group.contactCount");
    assertEquals(contactCount, 0, String.format("The group members weren't all deleted as expected.. response json after delete: %s", responseJson));

    // have the group owner delete the group
    testGroup.delete();

    // get the group representation - should get 404
    responseJson = testGroup.getRepresentation();
    int status = JsonPath.read(responseJson, "$.status");
    assertEquals(status, 404,
      String.format("The group %s was still able to be retrieved after deleting it..", testGroup.getName()));
  }

}
