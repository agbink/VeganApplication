package com.vegan.api.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
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
    public Map<String, String> upload(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.");
        }

        // 원본 확장자 유지, UUID로 파일명 충돌 방지
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + ext;

        // 업로드 디렉토리 생성 (없으면 자동 생성)
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        String url = baseUrl + "/uploads/" + fileName;
        return Map.of("url", url);
    }
}
