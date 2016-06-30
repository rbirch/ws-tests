package org.familysearch.cmp.messages.acceptance.util;



import com.jayway.jsonpath.JsonPath;
import com.sun.jersey.api.client.ClientResponse;
import org.familysearch.cmp.messages.acceptance.MessageEndpointsAT;
import org.familysearch.cmp.qa.util.TestUser;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rob birch on 4/4/2016.
 */
public class TestGroup {
  private String requestJson;
  private String name;
  private String description;
  private TestUser owner;
  private URL groupResource;
  private ArrayList<String> members = new ArrayList<>();
  private TestHelper testHelper;
  
  public TestGroup(TestUser owner, String groupJson) {
    this.testHelper = MessageEndpointsAT.testHelper;
    
    this.requestJson = groupJson;
    this.owner = owner;

    this.name = JsonPath.read(groupJson, "$.name");
    this.description = JsonPath.read(groupJson, "$.description");
    String newGroupPath = put();
    try {
      this.groupResource = new URL(newGroupPath);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public String getRepresentation() {
    String resourcePath = UriConstants.getGroupUri(owner.getUserId(), name);
    return testHelper.getHttpResponseEntity(resourcePath, owner, TestHelper.Method.GET);
  }

  private String put() {
    String resourcePath = UriConstants.getGroupUri(owner.getUserId(), name);
    String newGroupPath = testHelper.getResourceLocationFrom201Response(testHelper.getHttpResponse(resourcePath, owner, TestHelper.Method.PUT, requestJson));

    return newGroupPath;
  }

  public void delete() {
    ClientResponse response = delete(owner);
    testHelper.assertResponse(response, Response.Status.NO_CONTENT.getStatusCode());
  }

  public String deleteByNonOwner() {
    TestUser user = testHelper.getTestUserBesides(owner);

    ClientResponse response = delete(user);
    testHelper.assertResponse(response, Response.Status.FORBIDDEN.getStatusCode());
    return response.getEntity(String.class);
  }

  private ClientResponse delete(TestUser user) {
    String resourcePath = UriConstants.getGroupUri(owner.getUserId(), name);
    return testHelper.getHttpResponse(resourcePath, user, TestHelper.Method.DELETE, requestJson);
  }

  public URL getGroupResource() {
    return groupResource;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public TestUser getOwner() {
    return owner;
  }

  public String addMember(String userId) {
    String resourcePath = getGroupMemberUri(userId);
    String newMemberPath = testHelper.getResourceLocationFrom201Response(testHelper.getHttpResponse(resourcePath, getOwner(), TestHelper.Method.PUT));
    members.add(userId);
    return newMemberPath;
  }

  public boolean deleteMember(String userId) {
    String resourcePath = getGroupMemberUri(userId);
    ClientResponse response = testHelper.getHttpResponse(resourcePath, getOwner(), TestHelper.Method.DELETE);
    testHelper.assertResponse(response, Response.Status.NO_CONTENT.getStatusCode());
    return true;
  }

  public void addMembers(List<TestUser> users) {
    users.stream().forEach(member -> {
      try {
        addMember(member.getUserId());
      } catch (Exception ex) { throw new RuntimeException(ex); }
    });
  }

  public void deleteAllMembers() {
    members.stream().forEach(userId -> {
      try {
        deleteMember(userId);
      } catch (Exception ex) { throw new RuntimeException(ex); }
    });
  }

  public String getMembersRepresentation() {
    String resourcePath = UriConstants.getGroupMembersUri(getOwner().getUserId(), getName());
    ClientResponse response = testHelper.getHttpResponse(resourcePath, getOwner(), TestHelper.Method.GET);
    testHelper.assertResponse(response, Response.Status.OK.getStatusCode());
    return response.getEntity(String.class);
  }

  public ArrayList<String> getMembers() {
    return members;
  }

  private String getGroupMemberUri(String userId) {
    return UriConstants.getGroupMemberUri(getOwner().getUserId(), getName(), userId);
  }
}
