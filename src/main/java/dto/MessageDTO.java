package dto;

import java.util.List;

public class MessageDTO {
    public Long messageId;
    public String senderDisplayName;
    public String receiverDisplayName;
    public String content;
    public Long replyToMessageId;
    public String sentAt;
    public List<String> mediaUrls;
}