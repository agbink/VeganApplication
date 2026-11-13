package com.vegan.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.vegan.R;

/**
 * 앱 공통 이미지 로딩.
 * 불러오는 중(placeholder), 실패(error), 주소 없음(fallback) 모두 같은 기본 이미지를 보여줘서
 * 사진이 없는 상품도 빈칸으로 보이지 않게 한다.
 */
public final class ImageLoader {

    private ImageLoader() {
    }

    public static void load(ImageView target, String url) {
        Glide.with(target)
                .load(url == null || url.trim().isEmpty() ? null : url)
                .placeholder(R.drawable.img_placeholder)
                .error(R.drawable.img_placeholder)
                .fallback(R.drawable.img_placeholder)
                .into(target);
    }
}
