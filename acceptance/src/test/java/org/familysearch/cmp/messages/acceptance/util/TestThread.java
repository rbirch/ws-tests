package org.familysearch.cmp.messages.acceptance.util;

import com.jayway.jsonpath.JsonPath;

import org.familysearch.cmp.messages.acceptance.MessageEndpointsAT;
import org.familysearch.cmp.qa.util.TestUser;

import java.net.URL;
import java.util.*;

/**
 * Created by rob birch on 3/8/2016.
 */
public class TestThread {
  private String requestJson;
  private String threadId;
  private String subject;
  private String about;
  private String aboutUrl;

  private URL threadResource;
  private ArrayList<String> participantIds = new ArrayList<>();
  private LinkedHashMap<String, TestMessage> messages = new LinkedHashMap<>();
  private TestHelper testHelper;

  public TestThread(String threadJson) {
    this.testHelper = MessageEndpointsAT.testHelper;

    this.requestJson = threadJson;
    this.subject = JsonPath.read(threadJson, "$.subject");
    this.aboutUrl = JsonPath.read(threadJson, "$.aboutUrl");
    this.about = JsonPath.read(threadJson, "$.about");
    this.participantIds = new ArrayList(JsonPath.read(threadJson, "$.participantIds"));

    String authorId = JsonPath.read(threadJson, "$.firstMessage.authorId");
    if(!participantIds.contains(authorId)) {
      this.participantIds.add(authorId);
    }

    try {
      String newThreadPath = post();
      this.threadResource = new URL(newThreadPath);
    } catch(Exception ex) { throw new RuntimeException(ex); }

    this.threadId = getThreadIdFromThreadResource(this.threadResource);

    TestMessage.createFirstMessage(this, JsonPath.read(threadJson, "$.firstMessage").toString());
  }

  public String getThreadId() {
    return threadId;
  }

  public String getThreadSubject() {
    return subject;
  }

  public String getThreadAboutUrl() {
    return aboutUrl;
  }

  public String getThreadAbout() {
    return about;
  }

  public URL getThreadResource() {
    return threadResource;
  }

  public ArrayList<String> getParticipantIds() {
    return participantIds;
  }

  public void addMessage(TestMessage message) {
    if(message.getMessageId() != null) {
      messages.put(message.getMessageId(), message);
    }
  }

  public LinkedHashMap<String, TestMessage> getMessages() {
    return messages;
  }

  public TestMessage getMessage(String messageId) {
    return messages.get(messageId);
  }

  public TestMessage getFirstMessage() {
    return (TestMessage)messages.values().toArray()[0];
  }

  // post the thread through the API
  private String post() {
    String resourcePath = UriConstants.getThreadsUri();
    String authorId = JsonPath.read(this.requestJson, "$.firstMessage.authorId");
    TestUser user = testHelper.getTestUser(authorId);

    return testHelper.getResourceLocationFrom201Response(testHelper.getHttpResponse(resourcePath, user,
      TestHelper.Method.POST, this.requestJson));
  }

  // get the thread through the API
  public String getRepresentation() {
    String resourcePath = UriConstants.getThreadSpecificUri(getThreadId());
    TestUser user = testHelper.getTestUser(JsonPath.read(this.requestJson, "$.firstMessage.authorId"));
    return testHelper.getHttpResponseEntity(resourcePath, user, TestHelper.Method.GET);
  }

  public String getMessagesRepresentation() {
    String resourcePath = UriConstants.getThreadMessagesUri(this.threadId);
    TestUser user = testHelper.getTestUser(JsonPath.read(this.requestJson, "$.firstMessage.authorId"));
    return testHelper.getHttpResponseEntity(resourcePath, user, TestHelper.Method.GET);
  }

  private String getThreadIdFromThreadResource(URL threadResource) {
    String s = threadResource.toString();
    return s.substring(s.lastIndexOf('/') + 1);
  }

}


