package com.vegan.api;

import java.util.List;

/**
 * 백엔드 Spring Data Page 응답 매핑용 DTO.
 * GET /api/products?page=0&size=20 형태 요청의 응답:
 * { "content": [...], "totalPages": 5, "totalElements": 93, "number": 0, "last": false }
 */
public class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int number;   // 현재 페이지 번호 (0부터)
    private boolean last; // 마지막 페이지 여부

    public List<T> getContent() { return content; }
    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public int getNumber() { return number; }
    public boolean isLast() { return last; }
}
