package com.roleplay.phone.model;

import com.roleplay.phone.sms.model.TextMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PhoneProfile {
   private final String phoneNumber;
   private UUID creatorUuid;
   private final Map<String, String> contacts;
   private boolean silentMode;
   private boolean airplaneMode;
   private final List<TextMessage> messages;

   public PhoneProfile(String phoneNumber, UUID creatorUuid) {
      this.phoneNumber = phoneNumber;
      this.creatorUuid = creatorUuid;
      this.contacts = new LinkedHashMap();
      this.silentMode = false;
      this.airplaneMode = false;
      this.messages = new CopyOnWriteArrayList();
   }

   public PhoneProfile(String phoneNumber, UUID creatorUuid, Map<String, String> contacts, boolean silentMode, boolean airplaneMode) {
      this.phoneNumber = phoneNumber;
      this.creatorUuid = creatorUuid;
      this.contacts = new LinkedHashMap(contacts);
      this.silentMode = silentMode;
      this.airplaneMode = airplaneMode;
      this.messages = new CopyOnWriteArrayList();
   }

   public String getPhoneNumber() {
      return this.phoneNumber;
   }

   public UUID getCreatorUuid() {
      return this.creatorUuid;
   }

   public void setCreatorUuid(UUID creatorUuid) {
      this.creatorUuid = creatorUuid;
   }

   public Map<String, String> getContacts() {
      return Collections.unmodifiableMap(this.contacts);
   }

   public void addContact(String name, String number) {
      this.contacts.put(name, number);
   }

   public boolean removeContact(String name) {
      return this.contacts.remove(name) != null;
   }

   public String resolveContactName(String number) {
      for(Map.Entry<String, String> entry : this.contacts.entrySet()) {
         if (((String)entry.getValue()).equalsIgnoreCase(number)) {
            return (String)entry.getKey();
         }
      }

      return number;
   }

   public boolean isSilentMode() {
      return this.silentMode;
   }

   public void setSilentMode(boolean silentMode) {
      this.silentMode = silentMode;
   }

   public boolean isAirplaneMode() {
      return this.airplaneMode;
   }

   public void setAirplaneMode(boolean airplaneMode) {
      this.airplaneMode = airplaneMode;
   }

   public List<TextMessage> getMessages() {
      return Collections.unmodifiableList(this.messages);
   }

   public void addMessage(TextMessage message) {
      this.messages.add(message);
   }

   public Map<String, List<TextMessage>> getConversations() {
      Map<String, List<TextMessage>> threads = new LinkedHashMap();

      for(TextMessage msg : this.messages) {
         String partner = msg.getOtherParty(this.phoneNumber);
         ((List)threads.computeIfAbsent(partner, (k) -> new ArrayList())).add(msg);
      }

      return threads;
   }

   public List<TextMessage> getMessagesWith(String otherNumber) {
      List<TextMessage> thread = new ArrayList();

      for(TextMessage msg : this.messages) {
         if (msg.getOtherParty(this.phoneNumber).equalsIgnoreCase(otherNumber)) {
            thread.add(msg);
         }
      }

      return thread;
   }

   public int getUnreadCount() {
      int count = 0;

      for(TextMessage msg : this.messages) {
         if (!msg.isOutgoing(this.phoneNumber) && !msg.isRead()) {
            ++count;
         }
      }

      return count;
   }

   public int getUnreadCountWith(String otherNumber) {
      int count = 0;

      for(TextMessage msg : this.messages) {
         if (msg.getOtherParty(this.phoneNumber).equalsIgnoreCase(otherNumber) && !msg.isOutgoing(this.phoneNumber) && !msg.isRead()) {
            ++count;
         }
      }

      return count;
   }

   public void markConversationAsRead(String otherNumber) {
      for(TextMessage msg : this.messages) {
         if (msg.getOtherParty(this.phoneNumber).equalsIgnoreCase(otherNumber) && !msg.isOutgoing(this.phoneNumber)) {
            msg.setRead(true);
         }
      }

   }
}
