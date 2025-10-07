package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.dto.EmailAttachmentDTO;
import jakarta.mail.Message;
import jakarta.mail.Part;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface IFileProcessorService {
    // Métodos originales del paso 1
    String saveAttachment(Part part, String saveDirectory, String prefix) throws Exception;
    List<File> unzipFile(String zipFilePath, String destDirectory) throws Exception;
    String getFileExtension(String fileName);
    boolean isValidFileType(String extension);

    // Métodos nuevos para el paso 2
    Map<String, List<EmailAttachmentDTO>> processEmailAttachments(Message message, String saveDirectory) throws Exception;
    void asociarPDFsConXMLs(List<EmailAttachmentDTO> xmlFiles, List<EmailAttachmentDTO> pdfFiles);
}