package com.vegan.api.product;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품 목록 페이지 응답.
 *
 * Spring Data의 SliceImpl은 Redis(JSON) 캐시에서 역직렬화할 수 없어서
 * 필요한 필드만 담은 단순 객체로 변환해 응답·캐시 모두에 사용한다.
 * 무한 스크롤은 다음 페이지 여부(last)만 필요하므로 전체 개수(COUNT)는 담지 않는다.
 */
@Getter
public class ProductSliceResponse {

    private List<Product> content;
    private int number;
    private int size;
    private boolean first;
    private boolean last;

    // Redis 캐시 역직렬화용
    protected ProductSliceResponse() {
    }

    private ProductSliceResponse(List<Product> content, int number, int size, boolean last) {
        // Slice.getContent()는 수정 불가 리스트라 캐시 역직렬화가 가능한 ArrayList로 복사한다.
        this.content = new ArrayList<>(content);
        this.number = number;
        this.size = size;
        this.first = number == 0;
        this.last = last;
    }

    public static ProductSliceResponse from(Slice<Product> slice) {
        return new ProductSliceResponse(slice.getContent(), slice.getNumber(), slice.getSize(), slice.isLast());
    }

    public static ProductSliceResponse of(List<Product> content, int number, int size, boolean last) {
        return new ProductSliceResponse(content, number, size, last);
    }
}
