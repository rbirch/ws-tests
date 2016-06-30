package org.familysearch.cmp.messages.acceptance.util;

import com.jayway.jsonpath.JsonPath;
import org.familysearch.cmp.messages.acceptance.MessageEndpointsAT;
import org.familysearch.cmp.qa.util.TestUser;

import java.net.URL;

/**
 * Created by rob birch on 3/8/2016.
 */
public class TestMessage {
  private TestThread parentThread;
  private String requestJson;
  private String messageId;
  private String authorId;
  private String body;
  private URL messageResource;
  private TestHelper testHelper;


  private TestMessage(TestThread parentThread, String messageJson) {
    this.testHelper = MessageEndpointsAT.testHelper;

    this.parentThread = parentThread;
    this.requestJson = messageJson;
    this.authorId = JsonPath.read(messageJson, "$.authorId");
    this.body = JsonPath.read(messageJson, "$.body");
  }

  //call this from everywhere except TestThread
  public static TestMessage createMessage(TestThread parentThread, String messageJson) {
    TestMessage testMessage = new TestMessage(parentThread, messageJson);

    String newMessagePath = testMessage.post();
    try {
      testMessage.messageResource = new URL(newMessagePath);
    } catch(Exception ex) { throw new IllegalArgumentException(ex); }

    testMessage.messageId = newMessagePath.substring(newMessagePath.lastIndexOf('/') + 1);
    parentThread.addMessage(testMessage);
    return testMessage;
  }

  // call from inside TestThread only after it is created
  public static TestMessage createFirstMessage(TestThread parentThread, String messageJson) {
    try {
      String messageId = JsonPath.read(parentThread.getRepresentation(), "$.firstMessage.id");
      TestMessage testMessage = new TestMessage(parentThread, messageJson);
      testMessage.messageId = messageId;
      testMessage.messageResource = new URL(parentThread.getThreadResource() + "/messages/" + messageId);
      parentThread.addMessage(testMessage);
      return testMessage;
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public URL getMessageResource() {
    return messageResource;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getAuthorId() {
    return authorId;
  }

  public String getBody() {
    return body;
  }

  public TestThread getParentThread() {
    return parentThread;
  }

  public String getRepresentation() {
    String resourcePath = UriConstants.getThreadMessageUri(getParentThread().getThreadId(), getMessageId());
    TestUser user = testHelper.getTestUser(JsonPath.read(this.requestJson, "$.firstMessage.authorId"));
    return testHelper.getHttpResponseEntity(resourcePath, user, TestHelper.Method.GET);
  }

  private String post() {
    String resourcePath = UriConstants.getThreadMessagesUri(parentThread.getThreadId());
    TestUser author = testHelper.getTestUser(authorId);
    return testHelper.getResourceLocationFrom201Response(testHelper.getHttpResponse(resourcePath, author, TestHelper.Method.POST, requestJson));
  }

}
