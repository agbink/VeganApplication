package com.vegan.api.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String baseUrl;

    /**
     * POST /api/upload
     * multipart/form-data 형식으로 "image" 필드에 이미지 파일을 받아
     * 서버 로컬에 저장 후 접근 가능한 URL을 반환합니다.
     *
     * 응답: { "url": "http://서버주소:8080/uploads/파일명.jpg" }
     */
    @PostMapping
    public Map<String, String> upload(@RequestAttribute(required = false) Long userId,
                                      @RequestParam("image") MultipartFile file) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 10MB 이하여야 합니다.");
        }

        String ext = extensionFor(file.getContentType());
        String fileName = UUID.randomUUID().toString() + ext;

        try {
            Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path destination = dir.resolve(fileName).normalize();
            if (!destination.startsWith(dir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
            }
            file.transferTo(destination);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        String url = baseUrl + "/uploads/" + fileName;
        return Map.of("url", url);
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 형식을 확인할 수 없습니다.");
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "JPEG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        };
    }
}
