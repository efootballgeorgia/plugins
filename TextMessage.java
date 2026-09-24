package com.roleplay.phone.sms.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class TextMessage {
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());
   private final UUID id;
   private final String senderNumber;
   private final String recipientNumber;
   private final String content;
   private final long timestampMillis;
   private boolean read;

   public TextMessage(String senderNumber, String recipientNumber, String content) {
      this(UUID.randomUUID(), senderNumber, recipientNumber, content, System.currentTimeMillis(), false);
   }

   public TextMessage(UUID id, String senderNumber, String recipientNumber, String content, long timestampMillis, boolean read) {
      this.id = id != null ? id : UUID.randomUUID();
      this.senderNumber = senderNumber;
      this.recipientNumber = recipientNumber;
      this.content = content != null ? content : "";
      this.timestampMillis = timestampMillis;
      this.read = read;
   }

   public UUID getId() {
      return this.id;
   }

   public String getSenderNumber() {
      return this.senderNumber;
   }

   public String getRecipientNumber() {
      return this.recipientNumber;
   }

   public String getContent() {
      return this.content;
   }

   public long getTimestampMillis() {
      return this.timestampMillis;
   }

   public boolean isRead() {
      return this.read;
   }

   public void setRead(boolean read) {
      this.read = read;
   }

   public String getFormattedTimestamp() {
      return TIME_FORMATTER.format(Instant.ofEpochMilli(this.timestampMillis));
   }

   public String getOtherParty(String myNumber) {
      return this.senderNumber.equalsIgnoreCase(myNumber) ? this.recipientNumber : this.senderNumber;
   }

   public boolean isOutgoing(String myNumber) {
      return this.senderNumber.equalsIgnoreCase(myNumber);
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o instanceof TextMessage) {
         TextMessage that = (TextMessage)o;
         return Objects.equals(this.id, that.id);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.id});
   }
}
