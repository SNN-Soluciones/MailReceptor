package digital.samyx.mailreceptor.dto;

import lombok.Data;

@Data
public class EmailAttachmentDTO {
    private String fileName;
    private String filePath;
    private String extension;
    private String emailFrom;
}