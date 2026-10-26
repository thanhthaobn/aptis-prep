package com.aptisprep.service.admin;

import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.QuestionMediaRepository;
import com.aptisprep.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

/** Media validation/storage collaborator; the caller owns the catalog transaction. */
final class QuestionMediaWriter {
    private final QuestionMediaRepository questionMediaRepository;
    private final StorageService storage;

    QuestionMediaWriter(QuestionMediaRepository questionMediaRepository, StorageService storage) {
        this.questionMediaRepository = questionMediaRepository;
        this.storage = storage;
    }

    void save(Integer questionId, MultipartFile file) {
        String type =
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!(type.startsWith("image/") || type.startsWith("audio/"))) {
            throw new BusinessRuleException("Media câu hỏi chỉ nhận ảnh hoặc âm thanh");
        }
        boolean audio = type.startsWith("audio/");
        long maxBytes = audio ? 25L * 1024 * 1024 : 8L * 1024 * 1024;
        if (file.getSize() <= 0 || file.getSize() > maxBytes) {
            throw new BusinessRuleException("Media vượt quá dung lượng cho phép");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            if (audio ? !looksLikeAudio(header) : !looksLikeImage(header)) {
                throw new BusinessRuleException("Nội dung file không khớp với định dạng media");
            }
        } catch (IOException exception) {
            throw new BusinessRuleException("Không thể đọc file media");
        }
        QuestionMediaEntity value = new QuestionMediaEntity();
        value.setQuestionId(questionId);
        value.setMediaType(audio ? "AUDIO" : "IMAGE");
        value.setMediaUrl(storage.store(file, "questions/"));
        value.setOriginalFilename(cleanFilename(file.getOriginalFilename()));
        value.setContentType(type);
        value.setFileSize(file.getSize());
        value.setMaxPlayCount(audio ? 2 : null);
        value.setAltText(cleanFilename(file.getOriginalFilename()));
        value.setPosition(
                questionMediaRepository.findByQuestionIdOrderByPosition(questionId).size() + 1);
        questionMediaRepository.save(value);
    }

    private boolean looksLikeAudio(byte[] bytes) {
        return bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WAVE")
                || bytes.length >= 4 && ascii(bytes, 0, "OggS")
                || bytes.length >= 3 && ascii(bytes, 0, "ID3")
                || bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0
                || bytes.length >= 8 && ascii(bytes, 4, "ftyp");
    }

    private boolean looksLikeImage(byte[] bytes) {
        return bytes.length >= 8
                        && (bytes[0] & 0xFF) == 0x89
                        && bytes[1] == 0x50
                        && bytes[2] == 0x4E
                        && bytes[3] == 0x47
                || bytes.length >= 3
                        && (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xFF) == 0xD8
                        && (bytes[2] & 0xFF) == 0xFF
                || bytes.length >= 6 && (ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a"))
                || bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++)
            if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        return true;
    }

    private String cleanFilename(String value) {
        if (value == null || value.isBlank()) return "media";
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
