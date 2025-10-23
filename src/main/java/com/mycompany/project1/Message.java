package com.mycompany.project1;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane; 
import java.util.stream.Collectors;

/**
 */
public class Message {

    /**
     * Internal, private class to hold all message data (content and metadata) together.
     * This replaces the need for parallel arrays and simplifies searching/deleting.
     */
    private static class MessageObject {
        // Core data
        public final String recipient;
        public final String messageContent;
        public final String flag; // "Sent", "Stored", or "JSON_LOADED"
        
        // Metadata
        public final String messageID;
        public final String messageHash;

        public MessageObject(String recipient, String messageContent, String flag, String messageID, String messageHash) {
            this.recipient = recipient;
            this.messageContent = messageContent;
            this.flag = flag;
            this.messageID = messageID;
            this.messageHash = messageHash;
        }
    }
    
    // Helper class for JSON deserialization
    class MessageData {
        String recipient;
        String message;
        String flag;
    }

    private String currentRecipient;
    private String currentMessageContent;
    private int totalMessagesSent = 0;
    
    // --- ROBUST DATA STRUCTURE: Single list for all messages (Sent, Stored, JSON_LOADED) 
    private List<MessageObject> allMessages = new ArrayList<>();
    
    // Separate list for disregarded messages, as they have no ID/Hash
    private List<String> disregardedMessages = new ArrayList<>(); 
    
    // Used during ID generation for external test access
    String currentMessageID = "";

    public void setRecipient(String recipient) {
        this.currentRecipient = recipient;
    }

    public void setMessageContent(String messageContent) {
        this.currentMessageContent = messageContent;
    }
    
    // --- Message Validation Methods ---

    public boolean checkRecipientCall(String callNumber) {
        if (callNumber == null || callNumber.trim().isEmpty()) {
            return false;
        }
        return callNumber.matches("^[+]\\d{11}$");
    }

    public boolean checkMessageLength(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        return message.length() <= 250;
    }

    // --- Message ID and Hash Generation ---
    
    public String createMessageID() {
        Random rand = new Random();
        // Generates a random 10-digit number
        long tenDigitId = (long) (rand.nextDouble() * 9000000000L) + 1000000000L; 
        this.currentMessageID = String.valueOf(tenDigitId);
        return this.currentMessageID;
    }

    public String createMessageHash() {
        if (currentMessageContent == null || currentMessageContent.trim().isEmpty() || currentMessageID.length() < 2) {
            return "ERROR: INVALID_STATE";
        }
        String messageIDPrefix = currentMessageID.substring(0, 2);
        
        String cleanedContent = currentMessageContent.trim().toUpperCase().replaceAll("[^A-Z0-9\\s]", " ");
        String[] words = cleanedContent.split("\\s+");
        
        String firstWord = words.length > 0 && !words[0].isEmpty() ? words[0] : "";
        String lastWord = words.length > 1 ? words[words.length - 1] : firstWord;

        return messageIDPrefix + ":" + firstWord + lastWord;
    }

    // --- Core Message Processing ---

    /**
     * Adds a new MessageObject to the allMessages list based on the option.
     */
    public String send(String option, String messageID, String messageHash) {
        
        switch (option) {
            case "1":  // Sent Message
                totalMessagesSent++;
                allMessages.add(new MessageObject(currentRecipient, currentMessageContent, "Sent", messageID, messageHash));
                return "Message successfully sent.\nPress D to delete message.";
            case "2":  // Disregarded Message
                disregardedMessages.add(currentMessageContent);
                return "Message discarded.";
            case "3":  // Stored Message
                allMessages.add(new MessageObject(currentRecipient, currentMessageContent, "Stored", messageID, messageHash));
                return "Message successfully stored.";
            case "4":
                return "ChatGPT feature placeholder invoked.";
            default:
                return "Invalid option selected. Message discarded.";
        }
    }
    
    public int returnTotalMessageSent() {
        // Return totalMessagesSent only counts messages that were marked "Sent" at creation time.
        return totalMessagesSent;
    }
    
    // --- POE Array Functionality (Refactored to use MessageObject) ---

    // b) Display the longest sent message
    public String displayLongestMessage() {
        // Filter messages to include only "Sent" flags
        List<String> sentContents = allMessages.stream()
            .filter(msg -> "Sent".equals(msg.flag))
            .map(msg -> msg.messageContent)
            .collect(Collectors.toList());
        
        if (sentContents.isEmpty()) {
            return "No SENT messages available to find the longest message.";
        }
        
        String longestMessage = "";
        for (String msg : sentContents) {
            if (msg.length() > longestMessage.length()) {
                longestMessage = msg;
            }
        }
        return "Longest Message Found:\n" + longestMessage;
    }
    
    // c) Search for a message ID and display the corresponding recipient and message.
    public String searchForMessageID(String messageID) {
        for (MessageObject msg : allMessages) {
            if (msg.messageID.equals(messageID)) {
                 return String.format("--- Message Found ---\nID: %s\nRecipient: %s\nHash: %s\nMessage: %s\nFlag: %s",
                                     msg.messageID, msg.recipient, msg.messageHash, msg.messageContent, msg.flag);
            }
        }
        return "Message ID not found.";
    }
    
    // d) Search for all the messages sent to a particular recipient
    public String searchAllMessagesToRecipient(String recipient) {
        StringBuilder sb = new StringBuilder(" Messages Sent/Stored for Recipient: " + recipient + " -\n");
        boolean found = false;
        
        for (MessageObject msg : allMessages) {
            if (msg.recipient.equals(recipient)) {
                found = true;
                sb.append(String.format("Flag: %s | ID: %s | Hash: %s | Message: %s\n",
                                         msg.flag, msg.messageID, msg.messageHash, msg.messageContent));
            }
        }
        
        if (!found) {
            return "No messages found for that recipient.";
        }
        return sb.toString();
    }
    
    // e) Delete a message using the message hash
    public String deleteMessageByHash(String messageHash) {
        MessageObject messageToDelete = null;
        for (MessageObject msg : allMessages) {
            if (msg.messageHash.equals(messageHash)) {
                messageToDelete = msg;
                break;
            }
        }
        
        if (messageToDelete != null) {
            allMessages.remove(messageToDelete);
            
            // Adjust totalMessagesSent if a 'Sent' message was deleted
            if ("Sent".equals(messageToDelete.flag) && totalMessagesSent > 0) {
                totalMessagesSent--;
            }
            
            return String.format("Message with Hash %s (%s) successfully deleted.", messageHash, messageToDelete.flag);
        } else {
            return "Message Hash not found. No message deleted.";
        }
    }

    // f) Display a full report (All Sent, Stored, and Disregarded Messages)
    public String displayReport() {
        StringBuilder sb = new StringBuilder("--- Message System Report ---\n");
        
        sb.append(String.format("Total Messages Tracked (Sent/Stored/JSON): %d\n", allMessages.size()));
        sb.append(String.format("Total Disregarded Messages: %d\n", disregardedMessages.size()));
        sb.append(String.format("Total Messages Sent (via option 1): %d\n", totalMessagesSent)); 
        
        sb.append("\n- Tracking Data (Sent/Stored/JSON) -\n");
        if (allMessages.isEmpty()) {
             sb.append("No Sent, Stored, or JSON-loaded messages tracked.\n");
        } else {
            for (MessageObject msg : allMessages) {
                sb.append(String.format("%s | ID: %s | Hash: %s | Recipient: %s\n",
                                        msg.flag, msg.messageID, msg.messageHash, msg.recipient));
            }
        }
        
        sb.append("\n--- Disregarded Message Content -\n");
        if (disregardedMessages.isEmpty()) {
            sb.append("No messages were disregarded.\n");
        } else {
            for (String msg : disregardedMessages) {
                sb.append("-> ").append(msg.substring(0, Math.min(msg.length(), 40))).append("...\n");
            }
        }
        
        return sb.toString();
    }
    
    // --- JSON Utility Method (Refactored to use MessageObject) 
    
    public void readJsonIntoArray() {
        // NOTE: This assumes a file named 'messages.json' exists in the project root.
        String jsonFilePath = "messages.json"; 
        
        try (FileReader reader = new FileReader(jsonFilePath)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<MessageData>>(){}.getType();
            List<MessageData> messagesFromJSON = gson.fromJson(reader, listType);
            
            if (messagesFromJSON != null) {
                for (MessageData data : messagesFromJSON) {
                    // Check if message is valid before loading
                    if (checkRecipientCall(data.recipient) && checkMessageLength(data.message)) {
                        
                        // Set current object properties temporarily to generate ID/Hash
                        setRecipient(data.recipient);
                        setMessageContent(data.message);
                        String generatedID = createMessageID();
                        String generatedHash = createMessageHash();
                        
                        // Add the message as a single, complete object
                        allMessages.add(new MessageObject(data.recipient, data.message, "JSON_LOADED", generatedID, generatedHash));
                    }
                }
            }
        } catch (IOException e) {
            // Display a non-critical error for the user that JSON file couldn't be loaded
            JOptionPane.showMessageDialog(null, "Warning: Could not read 'messages.json'. Starting with empty message arrays.", "JSON Load Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}