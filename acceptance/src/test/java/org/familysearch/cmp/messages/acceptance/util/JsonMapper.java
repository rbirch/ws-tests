package org.familysearch.cmp.messages.acceptance.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rob birch on 1/5/2016.
 */
public class JsonMapper {
  public enum State { READ, UNREAD, TRASH }
  private static ObjectMapper mapper = new ObjectMapper();

  public static String createJsonThreadsSnippet(String senderId, List<String> receiverIds, String subject, String about, String aboutUrl,
                                            String messageBody) {

    Map<String, Object> map = new HashMap<>();

    List<String> participantIds = new ArrayList(receiverIds);
    if(!participantIds.contains(senderId)) {
      participantIds.add(senderId);
    }

    map.put("participantIds", participantIds);
    map.put("subject", subject);
    map.put("about", about);
    map.put("aboutUrl", aboutUrl);
    Map<String, Object> messageMap = new HashMap<>();
    messageMap.put("authorId", senderId);
    messageMap.put("body", messageBody);

    map.put("firstMessage", messageMap);

    try {
      return mapper.writeValueAsString(map);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public static String createJsonMessageSnippet(String authorId, String body) {
    Map<String, Object> map = new HashMap<>();

    map.put("authorId", authorId);
    map.put("body", body);
    try {
      return mapper.writeValueAsString(map);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public static String createJsonThreadStateSnippet(State status) {
    Map<String, Object> map = new HashMap<>();

    map.put("status", status);
    try {
      return mapper.writeValueAsString(map);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

  public static String createJsonGroupSnippet(String name, String description) {
    Map<String, Object> map = new HashMap<>();
    map.put("name", name);
    map.put("description", description);
    try {
      return mapper.writeValueAsString(map);
    } catch(Exception ex) { throw new RuntimeException(ex); }
  }

}
