package com.vegan.api.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegan.api.cart.dto.CartAddRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 장바구니에 같은 상품을 다시 담으면 새 행을 만들지 않고 수량만 늘어나는지 검증한다.
 */
@DataJpaTest
@Import(CartService.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_cart_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    CartService cartService;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void addingSameProductAgainOnlyIncreasesQuantity() {
        User user = saveUser("cart-same@example.com");
        Product product = saveProduct("크리스피 두부스틱 (400g)", 10);

        CartItem first = cartService.addItem(user.getId(), request(product.getId(), 2));
        CartItem second = cartService.addItem(user.getId(), request(product.getId(), 3));
        flushAndClear();

        List<CartItem> cart = cartItemRepository.findByUser_Id(user.getId());
        assertEquals(1, cart.size(), "같은 상품은 장바구니 행이 하나만 있어야 합니다.");
        assertEquals(first.getId(), second.getId(), "두 번째 담기도 같은 장바구니 항목이어야 합니다.");
        assertEquals(5, cart.get(0).getSelectedQuantity(), "수량은 2 + 3 = 5여야 합니다.");
    }

    @Test
    void addingBeyondStockIsRejectedAndKeepsQuantity() {
        User user = saveUser("cart-stock@example.com");
        Product product = saveProduct("비건 립밤", 5);

        cartService.addItem(user.getId(), request(product.getId(), 4));
        assertThrows(ResponseStatusException.class,
                () -> cartService.addItem(user.getId(), request(product.getId(), 2)),
                "합계 6개는 재고 5개를 넘으므로 거절되어야 합니다.");
        flushAndClear();

        List<CartItem> cart = cartItemRepository.findByUser_Id(user.getId());
        assertEquals(1, cart.size());
        assertEquals(4, cart.get(0).getSelectedQuantity(), "거절된 요청은 수량을 바꾸면 안 됩니다.");
    }

    @Test
    void differentProductsAndDifferentUsersAreSeparateRows() {
        User user = saveUser("cart-a@example.com");
        User other = saveUser("cart-b@example.com");
        Product tofu = saveProduct("두부스틱", 10);
        Product lipBalm = saveProduct("립밤", 10);

        cartService.addItem(user.getId(), request(tofu.getId(), 1));
        cartService.addItem(user.getId(), request(lipBalm.getId(), 1));
        cartService.addItem(other.getId(), request(tofu.getId(), 1));
        flushAndClear();

        assertEquals(2, cartItemRepository.findByUser_Id(user.getId()).size(), "다른 상품은 별도 행이어야 합니다.");
        assertEquals(1, cartItemRepository.findByUser_Id(other.getId()).size(), "다른 사용자의 장바구니와 섞이면 안 됩니다.");
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(new User(email, "encoded-password", "사용자", "010-0000-0000", "서울"));
    }

    private Product saveProduct(String name, int stock) {
        return productRepository.saveAndFlush(new Product(
                name, "테스트브랜드", 5_000, "image", "detail", stock, 101, "장바구니 테스트 상품"));
    }

    private CartAddRequest request(Long productId, int quantity) {
        return OBJECT_MAPPER.convertValue(
                Map.of("productId", productId, "selectedQuantity", quantity), CartAddRequest.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
