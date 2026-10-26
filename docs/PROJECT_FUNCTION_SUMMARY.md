# Tổng hợp hàm trong dự án product_be

**Phạm vi snapshot:** 2026-10-26. Tài liệu liệt kê các hàm/phương thức được khai báo tường minh trong mã nguồn hiện có, gồm backend Java, mã kiểm thử Java và hai script trong `docs/tools`.

| Phạm vi | Tệp | Hàm/phương thức |
|---|---:|---:|
| Java runtime (`src/main/java`) | 346 | 1211 |
| Java test (`src/test/java`) | 70 | 481 |
| Python tool | 1 | 8 |
| Google Apps Script | 1 | 13 |
| **Tổng** | **418** | **1713** |

## Cách đọc

- `Hàm/phương thức` ghi tên khai báo và danh sách tham số để phân biệt các overload. `Giá trị trả về` là kiểu trả về khai báo trong source; hàm JavaScript không khai báo kiểu trả về được ghi rõ.
- `Mục đích` ưu tiên JavaDoc/docstring hiện có; nếu không có, đây là diễn giải ngắn dựa trên tên hàm và lớp chứa. Xem liên kết source để biết điều kiện, quyền truy cập và side effect đầy đủ.
- Danh sách gồm method private/public/protected, method interface/annotation và hàm kiểm thử. Không gồm constructor, lambda/anonymous callback, method do Lombok/framework sinh ra hoặc method kế thừa mà source không khai báo.
- Trong thư mục `frontend` hiện chỉ có `nginx.conf`, không có source Angular/TypeScript/JavaScript để lập danh sách hàm. Các file build trong `target` và thư viện bên ngoài không thuộc snapshot source.

## Danh sách hàm theo tệp

### [docs/tools/generate_code_design_workbook.py:36](tools/generate_code_design_workbook.py#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`generate_code_design_workbook.title`](tools/generate_code_design_workbook.py#L36)(`ws, str text, str \| None subtitle, int columns`) | `không khai báo (Python)` | Thực hiện tạo code design workbook. |
| [`generate_code_design_workbook.paragraph`](tools/generate_code_design_workbook.py#L52)(`ws, int row, str text, int columns, str fill`) | `không khai báo (Python)` | Thực hiện tạo code design workbook. |
| [`generate_code_design_workbook.section`](tools/generate_code_design_workbook.py#L62)(`ws, int row, str text, int columns`) | `không khai báo (Python)` | Thực hiện tạo code design workbook. |
| [`generate_code_design_workbook.table`](tools/generate_code_design_workbook.py#L71)(`ws, int start_row, list[str] headers, list[list] rows, str name, widths`) | `không khai báo (Python)` | Thực hiện tạo code design workbook. |
| [`generate_code_design_workbook.add_sheet`](tools/generate_code_design_workbook.py#L108)(`wb, str name, str heading, str subtitle, list[str] headers, list[list] rows, widths`) | `không khai báo (Python)` | Thêm sheet. |
| [`generate_code_design_workbook.obj`](tools/generate_code_design_workbook.py#L122)(`kind, filename, package, status, phase, workflows, purpose, dependencies`) | `không khai báo (Python)` | Tạo một bản ghi dữ liệu chuẩn dùng để đưa vào các sheet workbook. |
| [`generate_code_design_workbook.build_workbook`](tools/generate_code_design_workbook.py#L474)() | `không khai báo (Python)` | Tạo workbook Excel gồm các sheet tổng hợp thiết kế mã nguồn và endpoint. |
| [`generate_code_design_workbook.verify_workbook`](tools/generate_code_design_workbook.py#L582)() | `không khai báo (Python)` | Mở workbook đầu ra và xác minh sheet, dữ liệu và cấu trúc mong đợi. |

### [docs/tools/update_google_doc_proposal.gs:15](tools/update_google_doc_proposal.gs#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`update_google_doc_proposal.previewGraduationProposal`](tools/update_google_doc_proposal.gs#L15)() | `không khai báo (JavaScript)` | Tạo Google Doc xem trước đề cương tốt nghiệp mà không sửa tài liệu đích. |
| [`update_google_doc_proposal.updateGraduationProposal`](tools/update_google_doc_proposal.gs#L21)() | `không khai báo (JavaScript)` | Sao lưu tài liệu đích rồi cập nhật nội dung đề cương tốt nghiệp. |
| [`update_google_doc_proposal.buildProposal_`](tools/update_google_doc_proposal.gs#L38)(`doc`) | `không khai báo (JavaScript)` | Dựng toàn bộ nội dung và định dạng đề cương trong Google Doc được truyền vào. |
| [`update_google_doc_proposal.appendInstitutionHeader_`](tools/update_google_doc_proposal.gs#L180)(`body`) | `không khai báo (JavaScript)` | Thêm phần tên cơ sở đào tạo vào đầu tài liệu. |
| [`update_google_doc_proposal.appendCenteredTitle_`](tools/update_google_doc_proposal.gs#L196)(`body, text, size`) | `không khai báo (JavaScript)` | Thêm tiêu đề căn giữa với cỡ chữ chỉ định. |
| [`update_google_doc_proposal.appendSectionTitle_`](tools/update_google_doc_proposal.gs#L204)(`body, text`) | `không khai báo (JavaScript)` | Thêm tiêu đề mục vào nội dung tài liệu. |
| [`update_google_doc_proposal.appendBodyParagraph_`](tools/update_google_doc_proposal.gs#L213)(`body, text`) | `không khai báo (JavaScript)` | Thêm đoạn văn nội dung vào tài liệu. |
| [`update_google_doc_proposal.appendLabeledBullet_`](tools/update_google_doc_proposal.gs#L223)(`body, label, content`) | `không khai báo (JavaScript)` | Thêm bullet có nhãn và phần nội dung tương ứng. |
| [`update_google_doc_proposal.appendNumberedItem_`](tools/update_google_doc_proposal.gs#L235)(`body, label, content`) | `không khai báo (JavaScript)` | Thêm mục được đánh số vào tài liệu. |
| [`update_google_doc_proposal.appendReference_`](tools/update_google_doc_proposal.gs#L247)(`body, text`) | `không khai báo (JavaScript)` | Thêm một tài liệu tham khảo vào danh sách tham khảo. |
| [`update_google_doc_proposal.appendTimelineTable_`](tools/update_google_doc_proposal.gs#L256)(`body`) | `không khai báo (JavaScript)` | Thêm bảng tiến độ thời gian vào đề cương. |
| [`update_google_doc_proposal.appendSignatureTable_`](tools/update_google_doc_proposal.gs#L296)(`body`) | `không khai báo (JavaScript)` | Thêm bảng khu vực chữ ký vào tài liệu. |
| [`update_google_doc_proposal.appendSpacer_`](tools/update_google_doc_proposal.gs#L315)(`body, count`) | `không khai báo (JavaScript)` | Thêm khoảng trống theo số dòng chỉ định. |

### [src/main/java/com/example/workflow/Application.java:11](../src/main/java/com/example/workflow/Application.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`Application.main`](../src/main/java/com/example/workflow/Application.java#L11)(`String... args`) | `void` | Điểm bắt đầu chạy chương trình/ứng dụng. |

### [src/main/java/com/example/workflow/cache/CacheKeys.java:12](../src/main/java/com/example/workflow/cache/CacheKeys.java#L12)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CacheKeys.userOrders`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L12)(`String userId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `String` | Thực hiện đơn hàng. |
| [`CacheKeys.userOrdersPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L16)(`String userId`) | `String` | Thực hiện đơn hàng tiền tố. |
| [`CacheKeys.userCancelledOrders`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L20)(`String userId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `String` | Thực hiện đã hủy đơn hàng. |
| [`CacheKeys.userCancelledOrdersPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L24)(`String userId`) | `String` | Thực hiện đã hủy đơn hàng tiền tố. |
| [`CacheKeys.managerPendingOrders`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L28)(`OrderStatus status, Pageable pageable`) | `String` | Thực hiện đang chờ đơn hàng. |
| [`CacheKeys.managerPendingOrdersPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L32)(`OrderStatus status`) | `String` | Thực hiện đang chờ đơn hàng tiền tố. |
| [`CacheKeys.warehousePendingOrders`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L36)(`Pageable pageable`) | `String` | Thực hiện đang chờ đơn hàng. |
| [`CacheKeys.warehousePendingOrdersPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L40)() | `String` | Thực hiện đang chờ đơn hàng tiền tố. |
| [`CacheKeys.staffAssignedOrders`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L44)(`String staffId, Pageable pageable`) | `String` | Thực hiện được phân công đơn hàng. |
| [`CacheKeys.staffAssignedOrdersPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L48)(`String staffId`) | `String` | Thực hiện được phân công đơn hàng tiền tố. |
| [`CacheKeys.reputationHistories`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L52)(`String userId, Pageable pageable`) | `String` | Thực hiện histories. |
| [`CacheKeys.reputationHistoriesPrefix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L56)(`String userId`) | `String` | Thực hiện histories tiền tố. |
| [`CacheKeys.priceAndPageSuffix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L60)(`BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `String` | Thực hiện và trang hậu tố. |
| [`CacheKeys.pageSuffix`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L64)(`Pageable pageable`) | `String` | Thực hiện hậu tố. |
| [`CacheKeys.safe`](../src/main/java/com/example/workflow/cache/CacheKeys.java#L70)(`Object value`) | `String` | Thực hiện cache khóa. |

### [src/main/java/com/example/workflow/cache/DeferredCacheEvict.java:11](../src/main/java/com/example/workflow/cache/DeferredCacheEvict.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DeferredCacheEvict.cacheName`](../src/main/java/com/example/workflow/cache/DeferredCacheEvict.java#L11)() | `String` | Tạo khóa cache tên. |
| [`DeferredCacheEvict.allEntries`](../src/main/java/com/example/workflow/cache/DeferredCacheEvict.java#L13)() | `boolean` | Thực hiện entries. |
| [`DeferredCacheEvict.key`](../src/main/java/com/example/workflow/cache/DeferredCacheEvict.java#L15)() | `String` | Thực hiện deferred cache evict. |

### [src/main/java/com/example/workflow/cache/DeferredCacheEvictAspect.java:29](../src/main/java/com/example/workflow/cache/DeferredCacheEvictAspect.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DeferredCacheEvictAspect.publishCacheEvictionsAfterSuccessfulReturn`](../src/main/java/com/example/workflow/cache/DeferredCacheEvictAspect.java#L29)(`ProceedingJoinPoint joinPoint, DeferredCacheEvicts deferredCacheEvicts`) | `Object` | Phát cache evictions sau thành công return. |
| [`DeferredCacheEvictAspect.buildEntries`](../src/main/java/com/example/workflow/cache/DeferredCacheEvictAspect.java#L42)(`ProceedingJoinPoint joinPoint, DeferredCacheEvicts deferredCacheEvicts, Object result`) | `List<CacheEvictionEntry>` | Tạo entries. |
| [`DeferredCacheEvictAspect.buildEvaluationContext`](../src/main/java/com/example/workflow/cache/DeferredCacheEvictAspect.java#L64)(`Method method, Object[] args, Object result`) | `StandardEvaluationContext` | Tạo evaluation context. |

### [src/main/java/com/example/workflow/cache/DeferredCacheEvicts.java:11](../src/main/java/com/example/workflow/cache/DeferredCacheEvicts.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DeferredCacheEvicts.value`](../src/main/java/com/example/workflow/cache/DeferredCacheEvicts.java#L11)() | `DeferredCacheEvict[]` | Thực hiện deferred cache evicts. |
| [`DeferredCacheEvicts.reason`](../src/main/java/com/example/workflow/cache/DeferredCacheEvicts.java#L13)() | `String` | Thực hiện deferred cache evicts. |

### [src/main/java/com/example/workflow/config/CacheClearer.java:13](../src/main/java/com/example/workflow/config/CacheClearer.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CacheClearer.run`](../src/main/java/com/example/workflow/config/CacheClearer.java#L13)(`String... args`) | `void` | Chạy cache clearer. |

### [src/main/java/com/example/workflow/config/CloudinaryConfig.java:22](../src/main/java/com/example/workflow/config/CloudinaryConfig.java#L22)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CloudinaryConfig.cloudinary`](../src/main/java/com/example/workflow/config/CloudinaryConfig.java#L22)() | `Cloudinary` | Thực hiện cloudinary. |

### [src/main/java/com/example/workflow/config/RedisConfig.java:24](../src/main/java/com/example/workflow/config/RedisConfig.java#L24)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisConfig.errorHandler`](../src/main/java/com/example/workflow/config/RedisConfig.java#L24)() | `CacheErrorHandler` | Thực hiện handler. |
| [`RedisConfig..handleCacheGetError`](../src/main/java/com/example/workflow/config/RedisConfig.java#L27)(`RuntimeException exception, Cache cache, Object key`) | `void` | Xử lý cache get lỗi. |
| [`RedisConfig..handleCachePutError`](../src/main/java/com/example/workflow/config/RedisConfig.java#L32)(`RuntimeException exception, Cache cache, Object key, Object value`) | `void` | Xử lý cache put lỗi. |
| [`RedisConfig..handleCacheEvictError`](../src/main/java/com/example/workflow/config/RedisConfig.java#L37)(`RuntimeException exception, Cache cache, Object key`) | `void` | Xử lý cache evict lỗi. |
| [`RedisConfig..handleCacheClearError`](../src/main/java/com/example/workflow/config/RedisConfig.java#L42)(`RuntimeException exception, Cache cache`) | `void` | Xử lý cache clear lỗi. |
| [`RedisConfig.logCacheFailure`](../src/main/java/com/example/workflow/config/RedisConfig.java#L49)(`String operation, RuntimeException exception, Cache cache, Object key`) | `void` | Thực hiện cache lỗi. |
| [`RedisConfig.redisTemplate`](../src/main/java/com/example/workflow/config/RedisConfig.java#L60)(`RedisConnectionFactory connectionFactory`) | `RedisTemplate<String, Object>` | Thực hiện template. |
| [`RedisConfig.redisCacheManagerBuilderCustomizer`](../src/main/java/com/example/workflow/config/RedisConfig.java#L75)() | `RedisCacheManagerBuilderCustomizer` | Thực hiện cache quản lý builder customizer. |
| [`RedisConfig.ttl`](../src/main/java/com/example/workflow/config/RedisConfig.java#L101)(`Duration ttl`) | `RedisCacheConfiguration` | Thực hiện redis. |

### [src/main/java/com/example/workflow/config/SecurityConfig.java:37](../src/main/java/com/example/workflow/config/SecurityConfig.java#L37)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`SecurityConfig.filterChain`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L37)(`HttpSecurity http`) | `SecurityFilterChain` | Thực hiện chain. |
| [`SecurityConfig.jwtAuthenticationConverter`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L68)() | `JwtAuthenticationConverter` | Thực hiện authentication converter. |
| [`SecurityConfig.keycloakRealmRoleConverter`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L76)() | `Converter<Jwt, Collection<GrantedAuthority>>` | Thực hiện realm vai trò converter. |
| [`SecurityConfig.corsConfigurationSource`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L98)() | `CorsConfigurationSource` | Thực hiện configuration nguồn. |
| [`SecurityConfig.passwordEncoder`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L111)() | `PasswordEncoder` | Thực hiện encoder. |
| [`SecurityConfig.parseAllowedOrigins`](../src/main/java/com/example/workflow/config/SecurityConfig.java#L116)() | `List<String>` | Phân tích được phép origins. |

### [src/main/java/com/example/workflow/config/WebMvcConfig.java:14](../src/main/java/com/example/workflow/config/WebMvcConfig.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WebMvcConfig.addInterceptors`](../src/main/java/com/example/workflow/config/WebMvcConfig.java#L14)(`InterceptorRegistry registry`) | `void` | Thêm interceptors. |

### [src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java:46](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L46)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WebSocketAuthInterceptor.preSend`](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L46)(`Message<?> message, MessageChannel channel`) | `Message<?>` | Thực hiện send. |
| [`WebSocketAuthInterceptor.authenticate`](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L65)(`StompHeaderAccessor accessor`) | `Authentication` | Xác thực web socket auth interceptor. |
| [`WebSocketAuthInterceptor.authorizeSubscription`](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L80)(`StompHeaderAccessor accessor`) | `void` | Kiểm tra quyền truy cập subscription. |
| [`WebSocketAuthInterceptor.authorizePersonalTopic`](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L111)(`String destination, String prefix, Authentication authentication`) | `void` | Kiểm tra quyền truy cập personal topic. |
| [`WebSocketAuthInterceptor.hasAdminFeedAccess`](../src/main/java/com/example/workflow/config/WebSocketAuthInterceptor.java#L122)(`Authentication authentication`) | `boolean` | Kiểm tra có quản trị viên feed access. |

### [src/main/java/com/example/workflow/config/WebSocketConfig.java:26](../src/main/java/com/example/workflow/config/WebSocketConfig.java#L26)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WebSocketConfig.configureMessageBroker`](../src/main/java/com/example/workflow/config/WebSocketConfig.java#L26)(`MessageBrokerRegistry config`) | `void` | Thực hiện tin nhắn broker. |
| [`WebSocketConfig.configureClientInboundChannel`](../src/main/java/com/example/workflow/config/WebSocketConfig.java#L32)(`ChannelRegistration registration`) | `void` | Thực hiện client inbound channel. |
| [`WebSocketConfig.registerStompEndpoints`](../src/main/java/com/example/workflow/config/WebSocketConfig.java#L37)(`StompEndpointRegistry registry`) | `void` | Đăng ký stomp endpoints. |
| [`WebSocketConfig.parseAllowedOrigins`](../src/main/java/com/example/workflow/config/WebSocketConfig.java#L44)() | `String[]` | Phân tích được phép origins. |

### [src/main/java/com/example/workflow/controller/AuthController.java:29](../src/main/java/com/example/workflow/controller/AuthController.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`AuthController.login`](../src/main/java/com/example/workflow/controller/AuthController.java#L29)(`@Valid  @RequestBody  LoginRequest request`) | `ResponseEntity<ApiResponse<AuthResponse>>` | Thực hiện auth. |
| [`AuthController.forgotPassword`](../src/main/java/com/example/workflow/controller/AuthController.java#L44)(`@Valid  @RequestBody  ForgotPasswordRequest request`) | `ResponseEntity<ApiResponse<Void>>` | Thực hiện mật khẩu. |
| [`AuthController.resetPassword`](../src/main/java/com/example/workflow/controller/AuthController.java#L50)(`@Valid  @RequestBody  ResetPasswordRequest request`) | `ResponseEntity<ApiResponse<Void>>` | Thực hiện mật khẩu. |

### [src/main/java/com/example/workflow/controller/CacheController.java:18](../src/main/java/com/example/workflow/controller/CacheController.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CacheController.clearAllCaches`](../src/main/java/com/example/workflow/controller/CacheController.java#L18)() | `ResponseEntity<ApiResponse<Void>>` | Xóa/làm trống tất cả caches. |

### [src/main/java/com/example/workflow/controller/CartController.java:23](../src/main/java/com/example/workflow/controller/CartController.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CartController.addToCart`](../src/main/java/com/example/workflow/controller/CartController.java#L23)(`@RequestHeader(value = GUEST_SESSION_HEADER, required = false)  String guestSessionId, @Positive(message = "Variant id must be positive")  @RequestParam("variantId")  Long variantId, @Min(value = 1, message = "Quantity must be at least 1")  @RequestParam("quantity")  int quantity`) | `ResponseEntity<ApiResponse<Void>>` | Thêm tới giỏ hàng. |
| [`CartController.update`](../src/main/java/com/example/workflow/controller/CartController.java#L33)(`@RequestHeader(value = GUEST_SESSION_HEADER, required = false)  String guestSessionId, @Positive(message = "Variant id must be positive")  @PathVariable  Long variantId, @Min(value = 0, message = "Quantity must be zero or positive")  @RequestParam  int quantity`) | `ResponseEntity<ApiResponse<Void>>` | Cập nhật giỏ hàng. |
| [`CartController.remove`](../src/main/java/com/example/workflow/controller/CartController.java#L43)(`@RequestHeader(value = GUEST_SESSION_HEADER, required = false)  String guestSessionId, @Positive(message = "Variant id must be positive")  @PathVariable  Long variantId`) | `ResponseEntity<ApiResponse<Void>>` | Xóa giỏ hàng. |
| [`CartController.getCart`](../src/main/java/com/example/workflow/controller/CartController.java#L52)(`@RequestHeader(value = GUEST_SESSION_HEADER, required = false)  String guestSessionId`) | `ResponseEntity<ApiResponse<CartResDTO>>` | Lấy giỏ hàng. |

### [src/main/java/com/example/workflow/controller/CheckoutController.java:23](../src/main/java/com/example/workflow/controller/CheckoutController.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutController.checkout`](../src/main/java/com/example/workflow/controller/CheckoutController.java#L23)(`@RequestHeader("Idempotency-Key")  String idempotencyKey, @Valid  @RequestBody  CheckoutRequest request`) | `ResponseEntity<ApiResponse<CheckoutResponseDTO>>` | Thực hiện checkout. |

### [src/main/java/com/example/workflow/controller/CustomRequestController.java:41](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L41)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestController.create`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L41)(`@Valid  @RequestBody  CreateCustomRequest request, @RequestHeader(name = "Idempotency-Key", required = false)  String idempotencyKey`) | `ResponseEntity<ApiResponse<CustomRequestDTO>>` | Tạo custom yêu cầu. |
| [`CustomRequestController.list`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L51)(`@RequestParam(required = false)  CustomRequestStatus status, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<CustomRequestDTO>>>` | Liệt kê custom yêu cầu. |
| [`CustomRequestController.get`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L59)(`@Positive  @PathVariable  Long id`) | `ResponseEntity<ApiResponse<CustomRequestDTO>>` | Lấy custom yêu cầu. |
| [`CustomRequestController.update`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L64)(`@Positive  @PathVariable  Long id, @Valid  @RequestBody  UpdateCustomRequest request`) | `ResponseEntity<ApiResponse<CustomRequestDTO>>` | Cập nhật custom yêu cầu. |
| [`CustomRequestController.delete`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L72)(`@Positive  @PathVariable  Long id, @PositiveOrZero  @RequestParam  Long expectedVersion, @RequestHeader("Idempotency-Key")  String idempotencyKey`) | `ResponseEntity<Void>` | Xóa custom yêu cầu. |
| [`CustomRequestController.submit`](../src/main/java/com/example/workflow/controller/CustomRequestController.java#L82)(`@Positive  @PathVariable  Long id, @Valid  @RequestBody  SubmitCustomRequest request, @RequestHeader("Idempotency-Key")  String idempotencyKey`) | `ResponseEntity<ApiResponse<CustomRequestSubmissionDTO>>` | Gửi custom yêu cầu. |

### [src/main/java/com/example/workflow/controller/DashboardController.java:18](../src/main/java/com/example/workflow/controller/DashboardController.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DashboardController.getDashboardStats`](../src/main/java/com/example/workflow/controller/DashboardController.java#L18)() | `ResponseEntity<ApiResponse<DashboardStatsDTO>>` | Lấy dashboard stats. |

### [src/main/java/com/example/workflow/controller/GuestCheckoutController.java:26](../src/main/java/com/example/workflow/controller/GuestCheckoutController.java#L26)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestCheckoutController.checkout`](../src/main/java/com/example/workflow/controller/GuestCheckoutController.java#L26)(`@RequestHeader(GUEST_SESSION_HEADER)  String guestSessionId, @RequestHeader("Idempotency-Key")  String idempotencyKey, @Valid  @RequestBody  GuestCheckoutRequest request`) | `ResponseEntity<ApiResponse<CheckoutResponseDTO>>` | Thực hiện khách checkout. |

### [src/main/java/com/example/workflow/controller/GuestOrderController.java:45](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L45)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestOrderController.cancellationView`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L45)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token`) | `ResponseEntity<ApiResponse<GuestOrderCancellationViewDTO>>` | Safe link target: reading an email link never mutates the order. |
| [`GuestOrderController.cancel`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L53)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  CancelOrderRequest request`) | `ResponseEntity<ApiResponse<OrderCancellationResultDTO>>` | Hủy đơn hàng của khách. |
| [`GuestOrderController.getGuestOrder`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L64)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token`) | `ResponseEntity<ApiResponse<OrderDTO>>` | Lấy đơn hàng của khách. |
| [`GuestOrderController.getGuestOrderHistory`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L72)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token`) | `ResponseEntity<ApiResponse<List<OrderStatusHistoryDTO>>>` | Lấy đơn hàng của khách lịch sử. |
| [`GuestOrderController.getGuestProductionCheckpoints`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L80)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token`) | `ResponseEntity<ApiResponse<List<OrderItemProductionDTO>>>` | Lấy khách sản xuất checkpoint. |
| [`GuestOrderController.getGuestReceiptView`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L90)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token`) | `ResponseEntity<ApiResponse<ReceiptViewDTO>>` | Lấy khách biên nhận dữ liệu hiển thị. |
| [`GuestOrderController.confirmGuestReceipt`](../src/main/java/com/example/workflow/controller/GuestOrderController.java#L98)(`@Positive  @PathVariable  Long orderId, @NotBlank  @RequestParam("token")  String token, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ReceiptConfirmRequest request`) | `ResponseEntity<ApiResponse<ReceiptConfirmResponse>>` | Thực hiện khách biên nhận. |

### [src/main/java/com/example/workflow/controller/ManagerOrderController.java:45](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L45)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerOrderController.getPendingReviews`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L45)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ManagerOrderReviewSummaryDTO>>>` | Lấy đang chờ duyệt/đánh giá. |
| [`ManagerOrderController.getReviewDetail`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L53)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<ManagerOrderReviewDetailDTO>>` | Lấy duyệt/đánh giá chi tiết. |
| [`ManagerOrderController.reviewOrder`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L61)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ManagerReviewRequest request`) | `ResponseEntity<ApiResponse<ManagerReviewResultDTO>>` | Duyệt/đánh giá đơn hàng. |
| [`ManagerOrderController.getAvailableStaff`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L74)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<AvailableStaffDTO>>>` | Lấy khả dụng nhân viên. |
| [`ManagerOrderController.getAssignableOrders`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L82)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<OrderAssignmentQueueDTO>>>` | Lấy assignable đơn hàng. |
| [`ManagerOrderController.assignStaffToOrder`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L90)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  AssignOrderRequest request`) | `ResponseEntity<ApiResponse<OrderAssignmentResultDTO>>` | Phân công nhân viên tới đơn hàng. |
| [`ManagerOrderController.reviewProductionCheckpoint`](../src/main/java/com/example/workflow/controller/ManagerOrderController.java#L103)(`@Positive  @PathVariable  Long orderId, @Positive  @PathVariable  Long checkpointId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ReviewProductionCheckpointRequest request`) | `ResponseEntity<ApiResponse<ReviewProductionCheckpointResultDTO>>` | Duyệt/đánh giá checkpoint sản xuất. |

### [src/main/java/com/example/workflow/controller/NotificationController.java:21](../src/main/java/com/example/workflow/controller/NotificationController.java#L21)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`NotificationController.getCurrentUserNotifications`](../src/main/java/com/example/workflow/controller/NotificationController.java#L21)(`@PageableDefault(size = 10)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<NotificationDTO>>>` | Lấy người dùng hiện tại thông báo. |
| [`NotificationController.getAdminNotifications`](../src/main/java/com/example/workflow/controller/NotificationController.java#L28)(`@PageableDefault(size = 10)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<NotificationDTO>>>` | Lấy quản trị viên thông báo. |
| [`NotificationController.getCurrentUserUnreadCount`](../src/main/java/com/example/workflow/controller/NotificationController.java#L36)() | `ResponseEntity<ApiResponse<Long>>` | Lấy người dùng hiện tại unread số lượng. |
| [`NotificationController.getAdminUnreadCount`](../src/main/java/com/example/workflow/controller/NotificationController.java#L41)() | `ResponseEntity<ApiResponse<Long>>` | Lấy quản trị viên unread số lượng. |
| [`NotificationController.markCurrentUserNotificationsAsRead`](../src/main/java/com/example/workflow/controller/NotificationController.java#L47)() | `ResponseEntity<ApiResponse<Void>>` | Đánh dấu người dùng hiện tại thông báo as đọc. |
| [`NotificationController.markAdminNotificationsAsRead`](../src/main/java/com/example/workflow/controller/NotificationController.java#L53)() | `ResponseEntity<ApiResponse<Void>>` | Đánh dấu quản trị viên thông báo as đọc. |

### [src/main/java/com/example/workflow/controller/OrderAgreementController.java:31](../src/main/java/com/example/workflow/controller/OrderAgreementController.java#L31)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAgreementController.getAgreement`](../src/main/java/com/example/workflow/controller/OrderAgreementController.java#L31)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<OrderAgreementDTO>>` | Lấy thỏa thuận. |
| [`OrderAgreementController.submit`](../src/main/java/com/example/workflow/controller/OrderAgreementController.java#L37)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  SubmitOrderAgreementRequest request`) | `ResponseEntity<ApiResponse<OrderAgreementDTO>>` | Gửi đơn hàng thỏa thuận. |
| [`OrderAgreementController.decide`](../src/main/java/com/example/workflow/controller/OrderAgreementController.java#L50)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  OrderAgreementDecisionRequest request`) | `ResponseEntity<ApiResponse<OrderAgreementDTO>>` | Ra quyết định đơn hàng thỏa thuận. |

### [src/main/java/com/example/workflow/controller/OrderChatController.java:36](../src/main/java/com/example/workflow/controller/OrderChatController.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderChatController.getThread`](../src/main/java/com/example/workflow/controller/OrderChatController.java#L36)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<OrderChatThreadDTO>>` | Lấy luồng chat. |
| [`OrderChatController.getMessages`](../src/main/java/com/example/workflow/controller/OrderChatController.java#L42)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<List<OrderChatMessageDTO>>>` | Lấy tin nhắn. |
| [`OrderChatController.sendRest`](../src/main/java/com/example/workflow/controller/OrderChatController.java#L48)(`@Positive  @PathVariable  Long orderId, @Valid  @RequestBody  SendOrderChatMessageRequest request, Authentication authentication`) | `ResponseEntity<ApiResponse<OrderChatMessageDTO>>` | Gửi rest. |
| [`OrderChatController.sendWebSocket`](../src/main/java/com/example/workflow/controller/OrderChatController.java#L62)(`@Valid  @Payload  SendOrderChatMessageRequest request, Authentication authentication`) | `void` | Gửi web socket. |

### [src/main/java/com/example/workflow/controller/OrderController.java:61](../src/main/java/com/example/workflow/controller/OrderController.java#L61)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderController.getMyOrders`](../src/main/java/com/example/workflow/controller/OrderController.java#L61)(`@DecimalMin(value = "0.0", message = "Minimum price must not be negative")  @RequestParam(required = false)  BigDecimal minPrice, @DecimalMin(value = "0.0", message = "Maximum price must not be negative")  @RequestParam(required = false)  BigDecimal maxPrice, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<OrderListDTO>>>` | Lấy của người dùng đơn hàng. |
| [`OrderController.getMyCancelledOrders`](../src/main/java/com/example/workflow/controller/OrderController.java#L76)(`@DecimalMin(value = "0.0", message = "Minimum price must not be negative")  @RequestParam(required = false)  BigDecimal minPrice, @DecimalMin(value = "0.0", message = "Maximum price must not be negative")  @RequestParam(required = false)  BigDecimal maxPrice, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<OrderListDTO>>>` | Lấy của người dùng đã hủy đơn hàng. |
| [`OrderController.getOrderById`](../src/main/java/com/example/workflow/controller/OrderController.java#L90)(`@Positive  @PathVariable("id")  Long id`) | `ResponseEntity<ApiResponse<OrderDTO>>` | Lấy đơn hàng theo ID. |
| [`OrderController.getOrderHistory`](../src/main/java/com/example/workflow/controller/OrderController.java#L96)(`@Positive  @PathVariable("order_id")  Long orderId`) | `ResponseEntity<ApiResponse<List<OrderStatusHistoryDTO>>>` | Lấy đơn hàng lịch sử. |
| [`OrderController.getProductionCheckpoints`](../src/main/java/com/example/workflow/controller/OrderController.java#L104)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<List<OrderItemProductionDTO>>>` | Lấy sản xuất checkpoint. |
| [`OrderController.getChangeRequests`](../src/main/java/com/example/workflow/controller/OrderController.java#L112)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<List<ChangeRequestDTO>>>` | Lấy change yêu cầu. |
| [`OrderController.createChangeRequest`](../src/main/java/com/example/workflow/controller/OrderController.java#L120)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  CreateChangeRequestRequest request`) | `ResponseEntity<ApiResponse<ChangeRequestDTO>>` | Tạo change yêu cầu. |
| [`OrderController.cancelOrder`](../src/main/java/com/example/workflow/controller/OrderController.java#L133)(`@Positive  @PathVariable("order_id")  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  CancelOrderRequest request`) | `ResponseEntity<ApiResponse<OrderCancellationResultDTO>>` | Hủy đơn hàng. |
| [`OrderController.reorderOrder`](../src/main/java/com/example/workflow/controller/OrderController.java#L144)(`@Positive  @PathVariable("order_id")  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey`) | `ResponseEntity<ApiResponse<ReorderResponseDTO>>` | Thực hiện đơn hàng. |
| [`OrderController.getReceiptView`](../src/main/java/com/example/workflow/controller/OrderController.java#L154)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<ReceiptViewDTO>>` | Lấy biên nhận dữ liệu hiển thị. |
| [`OrderController.confirmReceipt`](../src/main/java/com/example/workflow/controller/OrderController.java#L162)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ReceiptConfirmRequest request`) | `ResponseEntity<ApiResponse<ReceiptConfirmResponse>>` | Thực hiện biên nhận. |
| [`OrderController.validatePriceRange`](../src/main/java/com/example/workflow/controller/OrderController.java#L173)(`BigDecimal minPrice, BigDecimal maxPrice`) | `void` | Kiểm tra hợp lệ giá range. |

### [src/main/java/com/example/workflow/controller/PaymentController.java:32](../src/main/java/com/example/workflow/controller/PaymentController.java#L32)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentController.openPayment`](../src/main/java/com/example/workflow/controller/PaymentController.java#L32)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey`) | `ResponseEntity<ApiResponse<PaymentAttemptDTO>>` | Mở thanh toán. |
| [`PaymentController.paymentStatus`](../src/main/java/com/example/workflow/controller/PaymentController.java#L44)(`@Positive  @PathVariable  Long orderId`) | `ResponseEntity<ApiResponse<PaymentAttemptDTO>>` | Thực hiện trạng thái. |
| [`PaymentController.sePayWebhook`](../src/main/java/com/example/workflow/controller/PaymentController.java#L52)(`@RequestBody  byte[] rawBody, @RequestHeader  HttpHeaders headers`) | `ResponseEntity<Map<String, Boolean>>` | Thực hiện pay webhook. |

### [src/main/java/com/example/workflow/controller/ProductController.java:29](../src/main/java/com/example/workflow/controller/ProductController.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductController.createProduct`](../src/main/java/com/example/workflow/controller/ProductController.java#L29)(`@Valid  @RequestBody  ProductDTO dto`) | `ResponseEntity<ApiResponse<ProductDTO>>` | Tạo sản phẩm. |
| [`ProductController.getAllProducts`](../src/main/java/com/example/workflow/controller/ProductController.java#L38)(`@RequestParam(value = "keyword", required = false)  String keyword, @RequestParam(value = "minPrice", required = false)  BigDecimal minPrice, @RequestParam(value = "maxPrice", required = false)  BigDecimal maxPrice, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ProductDTO>>>` | Lấy tất cả sản phẩm. |
| [`ProductController.getBestSellingProducts`](../src/main/java/com/example/workflow/controller/ProductController.java#L48)(`@RequestParam(defaultValue = "day")  String period, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<BestSellerProductDTO>>>` | Lấy best selling sản phẩm. |
| [`ProductController.getProductById`](../src/main/java/com/example/workflow/controller/ProductController.java#L56)(`@Positive(message = "Product id must be positive")  @PathVariable  Long id`) | `ResponseEntity<ApiResponse<ProductDTO>>` | Lấy sản phẩm theo ID. |
| [`ProductController.updateProduct`](../src/main/java/com/example/workflow/controller/ProductController.java#L62)(`@Positive(message = "Product id must be positive")  @PathVariable  Long id, @Valid  @RequestBody  ProductDTO dto`) | `ResponseEntity<ApiResponse<Void>>` | Cập nhật sản phẩm. |
| [`ProductController.addVariant`](../src/main/java/com/example/workflow/controller/ProductController.java#L72)(`@Positive(message = "Product id must be positive")  @PathVariable  Long id, @Valid  @RequestBody  ProductVariantDTO dto`) | `ResponseEntity<ApiResponse<ProductDTO>>` | Thêm biến thể. |
| [`ProductController.deleteProduct`](../src/main/java/com/example/workflow/controller/ProductController.java#L82)(`@Positive(message = "Product id must be positive")  @PathVariable  Long id`) | `ResponseEntity<ApiResponse<Void>>` | Xóa sản phẩm. |

### [src/main/java/com/example/workflow/controller/ProductReviewController.java:34](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L34)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewController.createForOrderItem`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L34)(`@Positive  @PathVariable  Long orderItemId, @Valid  @RequestBody  ProductReviewRequest request`) | `ResponseEntity<ApiResponse<ProductReviewDTO>>` | Tạo cho mặt hàng của đơn hàng. |
| [`ProductReviewController.updateMyReview`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L46)(`@Positive  @PathVariable  Long reviewId, @Valid  @RequestBody  ProductReviewRequest request`) | `ResponseEntity<ApiResponse<ProductReviewDTO>>` | Cập nhật của người dùng duyệt/đánh giá. |
| [`ProductReviewController.getProductReviews`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L58)(`@Positive  @PathVariable  Long productId, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ProductReviewDTO>>>` | Lấy sản phẩm duyệt/đánh giá. |
| [`ProductReviewController.getProductSummary`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L66)(`@Positive  @PathVariable  Long productId`) | `ResponseEntity<ApiResponse<ProductReviewSummaryDTO>>` | Lấy sản phẩm tóm tắt. |
| [`ProductReviewController.getManageableVariantReviews`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L73)(`@Positive  @PathVariable  Long variantId, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ProductReviewDTO>>>` | Lấy có thể quản lý biến thể duyệt/đánh giá. |
| [`ProductReviewController.getVisibleVariantReviews`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L82)(`@Positive  @PathVariable  Long variantId, @PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ProductReviewDTO>>>` | Lấy được hiển thị biến thể duyệt/đánh giá. |
| [`ProductReviewController.getVariantSummary`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L90)(`@Positive  @PathVariable  Long variantId`) | `ResponseEntity<ApiResponse<ProductReviewSummaryDTO>>` | Lấy biến thể tóm tắt. |
| [`ProductReviewController.hideReview`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L97)(`@Positive  @PathVariable  Long reviewId, @Size(max = 500)  @RequestParam(required = false)  String reason`) | `ResponseEntity<ApiResponse<ProductReviewDTO>>` | Thực hiện duyệt/đánh giá. |
| [`ProductReviewController.restoreReview`](../src/main/java/com/example/workflow/controller/ProductReviewController.java#L109)(`@Positive  @PathVariable  Long reviewId`) | `ResponseEntity<ApiResponse<ProductReviewDTO>>` | Khôi phục duyệt/đánh giá. |

### [src/main/java/com/example/workflow/controller/ProductTypeController.java:24](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L24)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductTypeController.listActive`](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L24)() | `ResponseEntity<ApiResponse<List<ProductTypeDTO>>>` | Liệt kê đang hoạt động. |
| [`ProductTypeController.listAll`](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L29)() | `ResponseEntity<ApiResponse<List<ProductTypeDTO>>>` | Liệt kê tất cả. |
| [`ProductTypeController.get`](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L35)(`@Positive  @PathVariable  Long id`) | `ResponseEntity<ApiResponse<ProductTypeDTO>>` | Lấy sản phẩm loại. |
| [`ProductTypeController.create`](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L40)(`@Valid  @RequestBody  SaveProductTypeRequest request`) | `ResponseEntity<ApiResponse<ProductTypeDTO>>` | Tạo sản phẩm loại. |
| [`ProductTypeController.update`](../src/main/java/com/example/workflow/controller/ProductTypeController.java#L46)(`@Positive  @PathVariable  Long id, @Valid  @RequestBody  SaveProductTypeRequest request`) | `ResponseEntity<ApiResponse<ProductTypeDTO>>` | Cập nhật sản phẩm loại. |

### [src/main/java/com/example/workflow/controller/StaffOrderController.java:52](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L52)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`StaffOrderController.getClaimableOrders`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L52)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<OrderAssignmentQueueDTO>>>` | Lấy claimable đơn hàng. |
| [`StaffOrderController.getMyAssignedStaffOrders`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L60)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<OrderListDTO>>>` | Lấy của người dùng nhân viên được phân công đơn hàng. |
| [`StaffOrderController.claimOrder`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L68)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ClaimOrderRequest request`) | `ResponseEntity<ApiResponse<OrderAssignmentResultDTO>>` | Nhận xử lý đơn hàng. |
| [`StaffOrderController.startProduction`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L81)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey`) | `ResponseEntity<ApiResponse<StartProductionResultDTO>>` | Bắt đầu sản xuất. |
| [`StaffOrderController.submitProductionCheckpoint`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L93)(`@Positive  @PathVariable  Long orderId, @Positive  @PathVariable  Long orderItemId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  SubmitProductionCheckpointRequest request`) | `ResponseEntity<ApiResponse<ProductionCheckpointDTO>>` | Gửi checkpoint sản xuất. |
| [`StaffOrderController.decideChangeRequest`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L107)(`@Positive  @PathVariable  Long orderId, @Positive  @PathVariable  Long changeRequestId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ChangeRequestDecisionRequest request`) | `ResponseEntity<ApiResponse<ChangeRequestDTO>>` | Ra quyết định change yêu cầu. |
| [`StaffOrderController.handoverForShipping`](../src/main/java/com/example/workflow/controller/StaffOrderController.java#L121)(`@Positive  @PathVariable  Long orderId, @RequestHeader("Idempotency-Key")  @NotBlank  @Size(max = 200)  String idempotencyKey, @Valid  @RequestBody  ShippingHandoverRequest request`) | `ResponseEntity<ApiResponse<ShippingHandoverResponse>>` | Thực hiện cho vận chuyển. |

### [src/main/java/com/example/workflow/controller/UploadController.java:27](../src/main/java/com/example/workflow/controller/UploadController.java#L27)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UploadController.uploadImage`](../src/main/java/com/example/workflow/controller/UploadController.java#L27)(`@RequestParam("file")  MultipartFile file`) | `ResponseEntity<ApiResponse<Map<String, String>>>` | Thực hiện ảnh. |

### [src/main/java/com/example/workflow/controller/UserController.java:40](../src/main/java/com/example/workflow/controller/UserController.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserController.register`](../src/main/java/com/example/workflow/controller/UserController.java#L40)(`@Valid  @RequestBody  UserCreDTO dto`) | `ResponseEntity<ApiResponse<Void>>` | Đăng ký người dùng. |
| [`UserController.getAll`](../src/main/java/com/example/workflow/controller/UserController.java#L47)(`@Min(value = 0, message = "Page must be zero or positive")  @RequestParam(defaultValue = "0")  int page, @Min(value = 1, message = "Size must be positive")  @Max(value = 100, message = "Size must be at most 100")  @RequestParam(defaultValue = "10")  int size, @RequestParam(required = false)  List<Role> roles`) | `ResponseEntity<ApiResponse<Page<UserListDTO>>>` | Lấy tất cả. |
| [`UserController.getMe`](../src/main/java/com/example/workflow/controller/UserController.java#L59)() | `ResponseEntity<ApiResponse<UserResDTO>>` | Lấy me. |
| [`UserController.getMyReputationHistory`](../src/main/java/com/example/workflow/controller/UserController.java#L64)(`@PageableDefault(size = 5)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ReputationHistoryDTO>>>` | Lấy của người dùng reputation lịch sử. |
| [`UserController.updateMe`](../src/main/java/com/example/workflow/controller/UserController.java#L71)(`@Valid  @RequestBody  UserProfileUpdateDTO request`) | `ResponseEntity<ApiResponse<UserResDTO>>` | Cập nhật me. |
| [`UserController.changeMyPassword`](../src/main/java/com/example/workflow/controller/UserController.java#L78)(`@Valid  @RequestBody  ChangePasswordRequest request`) | `ResponseEntity<ApiResponse<Void>>` | Thực hiện của người dùng mật khẩu. |
| [`UserController.getById`](../src/main/java/com/example/workflow/controller/UserController.java#L86)(`@PathVariable  String id`) | `ResponseEntity<ApiResponse<UserResDTO>>` | Lấy theo ID. |
| [`UserController.update`](../src/main/java/com/example/workflow/controller/UserController.java#L94)(`@PathVariable  String id, @Valid  @RequestBody  UserCreDTO request`) | `ResponseEntity<ApiResponse<UserResDTO>>` | Cập nhật người dùng. |
| [`UserController.delete`](../src/main/java/com/example/workflow/controller/UserController.java#L104)(`@PathVariable  String id`) | `ResponseEntity<ApiResponse<Void>>` | Xóa người dùng. |

### [src/main/java/com/example/workflow/controller/VoucherController.java:32](../src/main/java/com/example/workflow/controller/VoucherController.java#L32)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`VoucherController.getTemplates`](../src/main/java/com/example/workflow/controller/VoucherController.java#L32)() | `ResponseEntity<ApiResponse<List<VoucherTemplateDTO>>>` | Lấy templates. |
| [`VoucherController.getManagementTemplates`](../src/main/java/com/example/workflow/controller/VoucherController.java#L38)() | `ResponseEntity<ApiResponse<List<VoucherTemplateDTO>>>` | Lấy management templates. |
| [`VoucherController.getGuestVouchers`](../src/main/java/com/example/workflow/controller/VoucherController.java#L44)(`@RequestParam(value = "subtotal", defaultValue = "0")  BigDecimal subtotal, @RequestHeader(value = "X-Guest-Session-Id", required = false)  String guestSessionId`) | `ResponseEntity<ApiResponse<List<VoucherCartOptionDTO>>>` | Lấy khách vouchers. |
| [`VoucherController.getMyWallet`](../src/main/java/com/example/workflow/controller/VoucherController.java#L52)() | `ResponseEntity<ApiResponse<List<UserVoucherDTO>>>` | Lấy của người dùng wallet. |
| [`VoucherController.getMyCartVoucherOptions`](../src/main/java/com/example/workflow/controller/VoucherController.java#L58)(`@RequestParam(value = "subtotal", defaultValue = "0")  BigDecimal subtotal`) | `ResponseEntity<ApiResponse<CartVoucherOptionsDTO>>` | Lấy của người dùng giỏ hàng voucher options. |
| [`VoucherController.redeemMyVoucher`](../src/main/java/com/example/workflow/controller/VoucherController.java#L66)(`@Positive(message = "Template id must be positive")  @RequestParam("templateId")  Long templateId`) | `ResponseEntity<ApiResponse<UserVoucherDTO>>` | Đổi voucher của người dùng voucher. |
| [`VoucherController.createCampaign`](../src/main/java/com/example/workflow/controller/VoucherController.java#L81)(`@Valid  @RequestBody  CreateVoucherTemplateRequest template`) | `ResponseEntity<ApiResponse<VoucherTemplateDTO>>` | Tạo campaign. |

### [src/main/java/com/example/workflow/controller/WishlistController.java:33](../src/main/java/com/example/workflow/controller/WishlistController.java#L33)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WishlistController.getMyWishlist`](../src/main/java/com/example/workflow/controller/WishlistController.java#L33)(`@PageableDefault(size = 20)  Pageable pageable`) | `ResponseEntity<ApiResponse<Page<ProductDTO>>>` | Lấy của người dùng wishlist. |
| [`WishlistController.addToWishlist`](../src/main/java/com/example/workflow/controller/WishlistController.java#L40)(`@Positive  @PathVariable  Long productId`) | `ResponseEntity<ApiResponse<ProductDTO>>` | Thêm tới wishlist. |
| [`WishlistController.removeFromWishlist`](../src/main/java/com/example/workflow/controller/WishlistController.java#L47)(`@Positive  @PathVariable  Long productId`) | `ResponseEntity<ApiResponse<Void>>` | Xóa từ wishlist. |
| [`WishlistController.isInMyWishlist`](../src/main/java/com/example/workflow/controller/WishlistController.java#L55)(`@Positive  @PathVariable  Long productId`) | `ResponseEntity<ApiResponse<Boolean>>` | Kiểm tra điều kiện trong của người dùng wishlist. |
| [`WishlistController.getMyWishlistStatus`](../src/main/java/com/example/workflow/controller/WishlistController.java#L62)(`@RequestParam("productIds")  List<@Positive Long> productIds`) | `ResponseEntity<ApiResponse<Map<Long, Boolean>>>` | Lấy của người dùng wishlist trạng thái. |

### [src/main/java/com/example/workflow/controller/WorkflowOperationsController.java:35](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L35)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowOperationsController.summary`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L35)() | `ResponseEntity<ApiResponse<WorkflowOperationsSummaryDTO>>` | Thực hiện workflow thao tác. |
| [`WorkflowOperationsController.incidents`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L40)(`@RequestParam(defaultValue = "50")  @Min(1)  @Max(200)  int limit`) | `ResponseEntity<ApiResponse<List<WorkflowOutboxIncidentDTO>>>` | Thực hiện workflow thao tác. |
| [`WorkflowOperationsController.requeue`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L47)(`@PathVariable  @NotBlank  @Size(max = 36)  String eventId`) | `ResponseEntity<ApiResponse<Boolean>>` | Đưa lại vào hàng đợi workflow thao tác. |
| [`WorkflowOperationsController.dlqIncidents`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L58)(`@RequestParam(defaultValue = "50")  @Min(1)  @Max(200)  int limit`) | `ResponseEntity<ApiResponse<List<WorkflowDlqIncidentDTO>>>` | Thực hiện sự cố. |
| [`WorkflowOperationsController.dlqCount`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L65)() | `ResponseEntity<ApiResponse<Long>>` | Thực hiện số lượng. |
| [`WorkflowOperationsController.replayDlq`](../src/main/java/com/example/workflow/controller/WorkflowOperationsController.java#L70)(`@PathVariable  @NotBlank  @Size(max = 64)  String recordId`) | `ResponseEntity<ApiResponse<Boolean>>` | Phát lại DLQ. |

### [src/main/java/com/example/workflow/delegate/WorkflowTaskDelegate.java:15](../src/main/java/com/example/workflow/delegate/WorkflowTaskDelegate.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskDelegate.execute`](../src/main/java/com/example/workflow/delegate/WorkflowTaskDelegate.java#L15)(`DelegateExecution execution`) | `void` | Thực thi workflow tác vụ delegate. |

### [src/main/java/com/example/workflow/dto/ApiResponse.java:17](../src/main/java/com/example/workflow/dto/ApiResponse.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ApiResponse.success`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L17)(`T data`) | `ApiResponse<T>` | Thực hiện api phản hồi. |
| [`ApiResponse.success`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L21)(`String message, T data`) | `ApiResponse<T>` | Thực hiện api phản hồi. |
| [`ApiResponse.success`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L25)(`String message`) | `ApiResponse<Void>` | Thực hiện api phản hồi. |
| [`ApiResponse.success`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L29)(`HttpStatus status, String message`) | `ApiResponse<Void>` | Thực hiện api phản hồi. |
| [`ApiResponse.error`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L33)(`HttpStatus status, String message`) | `ApiResponse<T>` | Thực hiện api phản hồi. |
| [`ApiResponse.error`](../src/main/java/com/example/workflow/dto/ApiResponse.java#L37)(`HttpStatus status, String message, T data`) | `ApiResponse<T>` | Thực hiện api phản hồi. |

### [src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java:36](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChangeRequestDecisionRequest.captureUnknownField`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L36)(`String name, Object value`) | `void` | Thực hiện unknown trường. |
| [`ChangeRequestDecisionRequest.hasUnknownFields`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L38)() | `boolean` | Kiểm tra có unknown trường. |
| [`ChangeRequestDecisionRequest.orderVersion`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L40)() | `Long` | Thực hiện phiên bản. |
| [`ChangeRequestDecisionRequest.changeRequestVersion`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L41)() | `Long` | Thực hiện yêu cầu phiên bản. |
| [`ChangeRequestDecisionRequest.decision`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L42)() | `ChangeRequestDecision` | Thực hiện change yêu cầu quyết định yêu cầu. |
| [`ChangeRequestDecisionRequest.reason`](../src/main/java/com/example/workflow/dto/ChangeRequestDecisionRequest.java#L43)() | `String` | Thực hiện change yêu cầu quyết định yêu cầu. |

### [src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java:45](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L45)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CreateChangeRequestRequest.captureUnknownField`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L45)(`String name, Object value`) | `void` | Thực hiện unknown trường. |
| [`CreateChangeRequestRequest.hasUnknownFields`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L47)() | `boolean` | Kiểm tra có unknown trường. |
| [`CreateChangeRequestRequest.orderVersion`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L49)() | `Long` | Thực hiện phiên bản. |
| [`CreateChangeRequestRequest.orderItemId`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L50)() | `Long` | Thực hiện mặt hàng ID. |
| [`CreateChangeRequestRequest.baseSpecVersion`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L51)() | `Integer` | Thực hiện đặc tả phiên bản. |
| [`CreateChangeRequestRequest.proposedDescription`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L52)() | `String` | Thực hiện description. |
| [`CreateChangeRequestRequest.proposedImageUrls`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L53)() | `List<String>` | Thực hiện ảnh URL. |
| [`CreateChangeRequestRequest.reason`](../src/main/java/com/example/workflow/dto/CreateChangeRequestRequest.java#L54)() | `String` | Thực hiện create change yêu cầu yêu cầu. |

### [src/main/java/com/example/workflow/dto/ManagerReviewRequest.java:15](../src/main/java/com/example/workflow/dto/ManagerReviewRequest.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerReviewRequest.isDecisionValid`](../src/main/java/com/example/workflow/dto/ManagerReviewRequest.java#L15)() | `boolean` | Kiểm tra điều kiện quyết định valid. |

### [src/main/java/com/example/workflow/dto/OrderDTO.java:82](../src/main/java/com/example/workflow/dto/OrderDTO.java#L82)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderDTO.CustomerInfo.user`](../src/main/java/com/example/workflow/dto/OrderDTO.java#L82)(`String userId, String name, String email, String phone, String address`) | `CustomerInfo` | Thực hiện khách hàng info. |
| [`OrderDTO.CustomerInfo.guest`](../src/main/java/com/example/workflow/dto/OrderDTO.java#L86)(`String guestSessionId, String name, String email, String phone, String address`) | `CustomerInfo` | Thực hiện khách hàng info. |

### [src/main/java/com/example/workflow/entity/CustomRequest.java:95](../src/main/java/com/example/workflow/entity/CustomRequest.java#L95)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequest.isEditable`](../src/main/java/com/example/workflow/entity/CustomRequest.java#L95)() | `boolean` | Kiểm tra điều kiện editable. |
| [`CustomRequest.markSubmitted`](../src/main/java/com/example/workflow/entity/CustomRequest.java#L99)(`Long orderId, LocalDateTime submissionTime`) | `void` | Đánh dấu đã gửi. |
| [`CustomRequest.prePersist`](../src/main/java/com/example/workflow/entity/CustomRequest.java#L109)() | `void` | Thực hiện persist. |
| [`CustomRequest.preUpdate`](../src/main/java/com/example/workflow/entity/CustomRequest.java#L123)() | `void` | Thực hiện update. |

### [src/main/java/com/example/workflow/entity/Order.java:193](../src/main/java/com/example/workflow/entity/Order.java#L193)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`Order.getNote`](../src/main/java/com/example/workflow/entity/Order.java#L193)() | `String` | Lấy note. |
| [`Order.setNote`](../src/main/java/com/example/workflow/entity/Order.java#L197)(`String note`) | `void` | Đặt note. |
| [`Order.getEmail`](../src/main/java/com/example/workflow/entity/Order.java#L201)() | `String` | Lấy email. |
| [`Order.setEmail`](../src/main/java/com/example/workflow/entity/Order.java#L205)(`String email`) | `void` | Đặt email. |
| [`Order.getRecipientName`](../src/main/java/com/example/workflow/entity/Order.java#L209)() | `String` | Lấy recipient tên. |
| [`Order.setRecipientName`](../src/main/java/com/example/workflow/entity/Order.java#L213)(`String recipientName`) | `void` | Đặt recipient tên. |
| [`Order.getRecipientPhone`](../src/main/java/com/example/workflow/entity/Order.java#L217)() | `String` | Lấy recipient điện thoại. |
| [`Order.setRecipientPhone`](../src/main/java/com/example/workflow/entity/Order.java#L221)(`String recipientPhone`) | `void` | Đặt recipient điện thoại. |
| [`Order.getShippingAddress`](../src/main/java/com/example/workflow/entity/Order.java#L225)() | `String` | Lấy vận chuyển address. |
| [`Order.setShippingAddress`](../src/main/java/com/example/workflow/entity/Order.java#L229)(`String shippingAddress`) | `void` | Đặt vận chuyển address. |
| [`Order.ensureContactSnapshot`](../src/main/java/com/example/workflow/entity/Order.java#L233)() | `OrderContactSnapshot` | Thực hiện contact snapshot. |

### [src/main/java/com/example/workflow/entity/OrderAssignment.java:60](../src/main/java/com/example/workflow/entity/OrderAssignment.java#L60)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignment.occupy`](../src/main/java/com/example/workflow/entity/OrderAssignment.java#L60)(`Long orderId, String staffId, AssignmentSource source, String actorId, LocalDateTime at`) | `void` | Thực hiện đơn hàng phân công. |
| [`OrderAssignment.release`](../src/main/java/com/example/workflow/entity/OrderAssignment.java#L72)(`AssignmentReleaseReason reason, LocalDateTime at`) | `void` | Thực hiện đơn hàng phân công. |

### [src/main/java/com/example/workflow/entity/OrderItem.java:102](../src/main/java/com/example/workflow/entity/OrderItem.java#L102)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItem.attachAssignment`](../src/main/java/com/example/workflow/entity/OrderItem.java#L102)(`OrderItemAssignment orderItemAssignment`) | `void` | Thực hiện phân công. |
| [`OrderItem.addProductionCheckpoint`](../src/main/java/com/example/workflow/entity/OrderItem.java#L109)(`ProductionCheckpoint checkpoint`) | `void` | Thêm checkpoint sản xuất. |

### [src/main/java/com/example/workflow/entity/OrderItemAssignment.java:65](../src/main/java/com/example/workflow/entity/OrderItemAssignment.java#L65)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItemAssignment.prePersist`](../src/main/java/com/example/workflow/entity/OrderItemAssignment.java#L65)() | `void` | Thực hiện persist. |
| [`OrderItemAssignment.preUpdate`](../src/main/java/com/example/workflow/entity/OrderItemAssignment.java#L76)() | `void` | Thực hiện update. |

### [src/main/java/com/example/workflow/entity/ProductReview.java:88](../src/main/java/com/example/workflow/entity/ProductReview.java#L88)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReview.prePersist`](../src/main/java/com/example/workflow/entity/ProductReview.java#L88)() | `void` | Thực hiện persist. |
| [`ProductReview.preUpdate`](../src/main/java/com/example/workflow/entity/ProductReview.java#L95)() | `void` | Thực hiện update. |

### [src/main/java/com/example/workflow/entity/ProductionCheckpoint.java:96](../src/main/java/com/example/workflow/entity/ProductionCheckpoint.java#L96)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpoint.prePersist`](../src/main/java/com/example/workflow/entity/ProductionCheckpoint.java#L96)() | `void` | Thực hiện persist. |
| [`ProductionCheckpoint.addImage`](../src/main/java/com/example/workflow/entity/ProductionCheckpoint.java#L106)(`ProductionCheckpointImage image`) | `void` | Thêm ảnh. |
| [`ProductionCheckpoint.attachDecision`](../src/main/java/com/example/workflow/entity/ProductionCheckpoint.java#L114)(`ProductionDecision productionDecision`) | `void` | Thực hiện quyết định. |

### [src/main/java/com/example/workflow/entity/ProductionCheckpointImage.java:60](../src/main/java/com/example/workflow/entity/ProductionCheckpointImage.java#L60)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpointImage.prePersist`](../src/main/java/com/example/workflow/entity/ProductionCheckpointImage.java#L60)() | `void` | Thực hiện persist. |

### [src/main/java/com/example/workflow/entity/ProductionDecision.java:61](../src/main/java/com/example/workflow/entity/ProductionDecision.java#L61)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionDecision.prePersist`](../src/main/java/com/example/workflow/entity/ProductionDecision.java#L61)() | `void` | Thực hiện persist. |

### [src/main/java/com/example/workflow/entity/ReputationHistory.java:51](../src/main/java/com/example/workflow/entity/ReputationHistory.java#L51)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ReputationHistory.prePersist`](../src/main/java/com/example/workflow/entity/ReputationHistory.java#L51)() | `void` | Thực hiện persist. |

### [src/main/java/com/example/workflow/entity/WishlistItem.java:47](../src/main/java/com/example/workflow/entity/WishlistItem.java#L47)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WishlistItem.prePersist`](../src/main/java/com/example/workflow/entity/WishlistItem.java#L47)() | `void` | Thực hiện persist. |

### [src/main/java/com/example/workflow/entity/converter/StringListJsonConverter.java:18](../src/main/java/com/example/workflow/entity/converter/StringListJsonConverter.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`StringListJsonConverter.convertToDatabaseColumn`](../src/main/java/com/example/workflow/entity/converter/StringListJsonConverter.java#L18)(`List<String> attribute`) | `String` | Chuyển đổi tới database column. |
| [`StringListJsonConverter.convertToEntityAttribute`](../src/main/java/com/example/workflow/entity/converter/StringListJsonConverter.java#L27)(`String dbData`) | `List<String>` | Chuyển đổi tới entity attribute. |

### [src/main/java/com/example/workflow/event/payload/CacheEvictionEntry.java:4](../src/main/java/com/example/workflow/event/payload/CacheEvictionEntry.java#L4)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CacheEvictionEntry.allEntries`](../src/main/java/com/example/workflow/event/payload/CacheEvictionEntry.java#L4)(`String cacheName`) | `CacheEvictionEntry` | Thực hiện entries. |
| [`CacheEvictionEntry.key`](../src/main/java/com/example/workflow/event/payload/CacheEvictionEntry.java#L8)(`String cacheName, Object key`) | `CacheEvictionEntry` | Thực hiện cache eviction entry. |
| [`CacheEvictionEntry.prefix`](../src/main/java/com/example/workflow/event/payload/CacheEvictionEntry.java#L12)(`String cacheName, String keyPrefix`) | `CacheEvictionEntry` | Thực hiện cache eviction entry. |

### [src/main/java/com/example/workflow/exception/ConstantErrorCode.java:162](../src/main/java/com/example/workflow/exception/ConstantErrorCode.java#L162)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ConstantErrorCode.format`](../src/main/java/com/example/workflow/exception/ConstantErrorCode.java#L162)(`Object... args`) | `String` | Thực hiện constant lỗi code. |

### [src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java:15](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GlobalExceptionHandler.handleConcurrency`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L15)(`org.springframework.dao.ConcurrencyFailureException ex`) | `ResponseEntity<ApiResponse<Void>>` | Xử lý concurrency. |
| [`GlobalExceptionHandler.handleAppException`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L19)(`AppException ex`) | `ResponseEntity<ApiResponse<Void>>` | Xử lý app exception. |
| [`GlobalExceptionHandler.handleValidationExceptions`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L23)(`MethodArgumentNotValidException ex`) | `ResponseEntity<ApiResponse<Map<String, String>>>` | Xử lý validation exceptions. |
| [`GlobalExceptionHandler.handleConstraintViolationException`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L35)(`ConstraintViolationException ex`) | `ResponseEntity<ApiResponse<Map<String, String>>>` | Xử lý constraint violation exception. |
| [`GlobalExceptionHandler.handleGlobalException`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L47)(`Exception ex`) | `ResponseEntity<ApiResponse<Void>>` | Xử lý global exception. |
| [`GlobalExceptionHandler.findAppException`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L59)(`Throwable throwable`) | `AppException` | Tìm app exception. |
| [`GlobalExceptionHandler.buildAppExceptionResponse`](../src/main/java/com/example/workflow/exception/GlobalExceptionHandler.java#L70)(`AppException ex`) | `ResponseEntity<ApiResponse<Void>>` | Tạo app exception phản hồi. |

### [src/main/java/com/example/workflow/exception/RateLimitExceededException.java:17](../src/main/java/com/example/workflow/exception/RateLimitExceededException.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitExceededException.secondsCeil`](../src/main/java/com/example/workflow/exception/RateLimitExceededException.java#L17)(`Duration duration`) | `long` | Thực hiện ceil. |

### [src/main/java/com/example/workflow/mapper/CartMapper.java:17](../src/main/java/com/example/workflow/mapper/CartMapper.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CartMapper.toDto`](../src/main/java/com/example/workflow/mapper/CartMapper.java#L17)(`Cart cart`) | `CartResDTO` | Chuyển thành dto. |
| [`CartMapper.toDto`](../src/main/java/com/example/workflow/mapper/CartMapper.java#L21)(`CartItem cartItem`) | `CartItemDTO` | Chuyển thành dto. |
| [`CartMapper.toItemDtos`](../src/main/java/com/example/workflow/mapper/CartMapper.java#L28)(`List<CartItem> cartItems`) | `List<CartItemDTO>` | Chuyển thành mặt hàng dtos. |
| [`CartMapper.calculateTotalPrice`](../src/main/java/com/example/workflow/mapper/CartMapper.java#L30)(`Cart cart, @MappingTarget  CartResDTO dto`) | `void` | Tính total giá. |
| [`CartMapper.resolveImageUrl`](../src/main/java/com/example/workflow/mapper/CartMapper.java#L42)(`CartItem item`) | `String` | Xác định ảnh URL. |

### [src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java:11](../src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutResponseMapper.toCatalogResponse`](../src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java#L11)(`Order order, String lookupToken, String maskedEmail`) | `CheckoutResponseDTO` | Chuyển thành catalog phản hồi. |
| [`CheckoutResponseMapper.enumName`](../src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java#L32)(`Enum<?> value`) | `String` | Thực hiện tên. |
| [`CheckoutResponseMapper.resolveVoucherCode`](../src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java#L36)(`Order order`) | `String` | Xác định voucher code. |
| [`CheckoutResponseMapper.resolveVoucherName`](../src/main/java/com/example/workflow/mapper/CheckoutResponseMapper.java#L43)(`Order order`) | `String` | Xác định voucher tên. |

### [src/main/java/com/example/workflow/mapper/CustomRequestMapper.java:22](../src/main/java/com/example/workflow/mapper/CustomRequestMapper.java#L22)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestMapper.toDto`](../src/main/java/com/example/workflow/mapper/CustomRequestMapper.java#L22)(`CustomRequest request`) | `CustomRequestDTO` | Chuyển thành dto. |
| [`CustomRequestMapper.toDtoList`](../src/main/java/com/example/workflow/mapper/CustomRequestMapper.java#L44)(`List<CustomRequest> requests`) | `List<CustomRequestDTO>` | Chuyển thành dto list. |

### [src/main/java/com/example/workflow/mapper/CustomRequestSubmissionMapper.java:10](../src/main/java/com/example/workflow/mapper/CustomRequestSubmissionMapper.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestSubmissionMapper.toDto`](../src/main/java/com/example/workflow/mapper/CustomRequestSubmissionMapper.java#L10)(`Long draftId, Order order`) | `CustomRequestSubmissionDTO` | Chuyển thành dto. |
| [`CustomRequestSubmissionMapper.enumName`](../src/main/java/com/example/workflow/mapper/CustomRequestSubmissionMapper.java#L18)(`Enum<?> value`) | `String` | Thực hiện tên. |

### [src/main/java/com/example/workflow/mapper/ManagerReviewMapper.java:17](../src/main/java/com/example/workflow/mapper/ManagerReviewMapper.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerReviewMapper.toSummary`](../src/main/java/com/example/workflow/mapper/ManagerReviewMapper.java#L17)(`Order order`) | `ManagerOrderReviewSummaryDTO` | Chuyển thành tóm tắt. |
| [`ManagerReviewMapper.catalogFinalPrice`](../src/main/java/com/example/workflow/mapper/ManagerReviewMapper.java#L42)(`Order order`) | `BigDecimal` | Thực hiện cuối giá. |
| [`ManagerReviewMapper.paymentMethod`](../src/main/java/com/example/workflow/mapper/ManagerReviewMapper.java#L51)(`Order order`) | `PaymentMethod` | Thực hiện method. |

### [src/main/java/com/example/workflow/mapper/NotificationMapper.java:14](../src/main/java/com/example/workflow/mapper/NotificationMapper.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`NotificationMapper.toDto`](../src/main/java/com/example/workflow/mapper/NotificationMapper.java#L14)(`Notification notification, boolean read`) | `NotificationDTO` | Chuyển thành dto. |
| [`NotificationMapper.toInstant`](../src/main/java/com/example/workflow/mapper/NotificationMapper.java#L18)(`LocalDateTime value`) | `Instant` | Chuyển thành instant. |

### [src/main/java/com/example/workflow/mapper/OrderAssignmentMapper.java:13](../src/main/java/com/example/workflow/mapper/OrderAssignmentMapper.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignmentMapper.toDto`](../src/main/java/com/example/workflow/mapper/OrderAssignmentMapper.java#L13)(`OrderAssignment assignment, User staff`) | `OrderAssignmentDTO` | Chuyển thành dto. |
| [`OrderAssignmentMapper.toAvailableStaff`](../src/main/java/com/example/workflow/mapper/OrderAssignmentMapper.java#L30)(`User staff`) | `AvailableStaffDTO` | Chuyển thành khả dụng nhân viên. |

### [src/main/java/com/example/workflow/mapper/OrderItemMapper.java:25](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L25)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItemMapper.toDto`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L25)(`OrderItem orderItem`) | `OrderItemDTO` | Chuyển thành dto. |
| [`OrderItemMapper.mapCustomContent`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L43)(`OrderItem orderItem, @MappingTarget  OrderItemDTO target`) | `void` | Ánh xạ custom content. |
| [`OrderItemMapper.resolveProductName`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L59)(`OrderItem orderItem`) | `String` | Xác định sản phẩm tên. |
| [`OrderItemMapper.resolveVariantName`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L72)(`OrderItem orderItem`) | `String` | Xác định biến thể tên. |
| [`OrderItemMapper.resolveAttributes`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L82)(`OrderItem orderItem`) | `String` | Xác định attributes. |
| [`OrderItemMapper.resolveImageUrl`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L92)(`OrderItem orderItem`) | `String` | Xác định ảnh URL. |
| [`OrderItemMapper.toCheckRequest`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L103)(`OrderItem orderItem`) | `ItemCheckRequest` | Chuyển thành check yêu cầu. |
| [`OrderItemMapper.toCheckRequest`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L116)(`List<OrderItem> orderItems`) | `List<ItemCheckRequest>` | Chuyển thành check yêu cầu. |
| [`OrderItemMapper.parseCustomContent`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L125)(`String snapshot`) | `CustomContent` | Phân tích custom content. |
| [`OrderItemMapper.text`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L149)(`JsonNode node`) | `String` | Thực hiện mặt hàng của đơn hàng. |
| [`OrderItemMapper.imageUrls`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L153)(`JsonNode node`) | `List<String>` | Thực hiện URL. |
| [`OrderItemMapper.CustomContent.legacy`](../src/main/java/com/example/workflow/mapper/OrderItemMapper.java#L186)(`String description`) | `CustomContent` | Thực hiện custom content. |

### [src/main/java/com/example/workflow/mapper/OrderMapper.java:16](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderMapper.toDto`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L16)(`Order order`) | `OrderDTO` | Chuyển thành dto. |
| [`OrderMapper.resolveStaffId`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L30)(`Order order`) | `String` | Xác định nhân viên ID. |
| [`OrderMapper.resolveStaffName`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L36)(`Order order`) | `String` | Xác định nhân viên tên. |
| [`OrderMapper.resolveManagerName`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L42)(`Order order`) | `String` | Xác định quản lý tên. |
| [`OrderMapper.resolveCustomerName`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L46)(`Order order`) | `String` | Xác định khách hàng tên. |
| [`OrderMapper.resolveLastname`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L56)(`Order order`) | `String` | Xác định lastname. |
| [`OrderMapper.resolveCustomerInfo`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L63)(`Order order`) | `OrderDTO.CustomerInfo` | Xác định khách hàng info. |
| [`OrderMapper.resolveCustomerEmail`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L86)(`Order order`) | `String` | Xác định khách hàng email. |
| [`OrderMapper.resolveTotalPrice`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L93)(`Order order`) | `BigDecimal` | Xác định total giá. |
| [`OrderMapper.resolveFinalPrice`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L100)(`Order order`) | `BigDecimal` | Xác định cuối giá. |
| [`OrderMapper.resolveVoucherName`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L115)(`Order order`) | `String` | Xác định voucher tên. |
| [`OrderMapper.firstPresent`](../src/main/java/com/example/workflow/mapper/OrderMapper.java#L128)(`String primary, String fallback`) | `String` | Thực hiện present. |

### [src/main/java/com/example/workflow/mapper/OrderStatusHistoryMapper.java:10](../src/main/java/com/example/workflow/mapper/OrderStatusHistoryMapper.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderStatusHistoryMapper.toDto`](../src/main/java/com/example/workflow/mapper/OrderStatusHistoryMapper.java#L10)(`OrderStatusHistory entity`) | `OrderStatusHistoryDTO` | Chuyển thành dto. |

### [src/main/java/com/example/workflow/mapper/ProductMapper.java:19](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L19)(`Product product`) | `ProductDTO` | Chuyển thành dto. |
| [`ProductMapper.variantToDto`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L28)(`ProductVariant variant`) | `ProductVariantDTO` | Thực hiện tới dto. |
| [`ProductMapper.toEntity`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L32)(`ProductDTO dto`) | `Product` | Chuyển thành entity. |
| [`ProductMapper.variantToEntity`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L41)(`ProductVariantDTO dto`) | `ProductVariant` | Thực hiện tới entity. |
| [`ProductMapper.updateCatalogInfo`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L47)(`ProductDTO dto, @MappingTarget  Product entity`) | `void` | Cập nhật catalog info. |
| [`ProductMapper.updateVariant`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L55)(`ProductVariantDTO dto, @MappingTarget  ProductVariant entity`) | `void` | Cập nhật biến thể. |
| [`ProductMapper.filterDeletedVariants`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L63)(`Product entity, @MappingTarget  ProductDTO dto`) | `void` | Thực hiện deleted biến thể. |
| [`ProductMapper.linkVariants`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L80)(`@MappingTarget  Product product`) | `void` | Thực hiện biến thể. |
| [`ProductMapper.saveKeywords`](../src/main/java/com/example/workflow/mapper/ProductMapper.java#L89)(`ProductDTO dto, @MappingTarget  Product product`) | `void` | Lưu keywords. |

### [src/main/java/com/example/workflow/mapper/ProductReviewImageMapper.java:18](../src/main/java/com/example/workflow/mapper/ProductReviewImageMapper.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewImageMapper.toList`](../src/main/java/com/example/workflow/mapper/ProductReviewImageMapper.java#L18)(`String value`) | `List<String>` | Chuyển thành list. |
| [`ProductReviewImageMapper.toJson`](../src/main/java/com/example/workflow/mapper/ProductReviewImageMapper.java#L30)(`List<String> imageUrls`) | `String` | Chuyển thành json. |

### [src/main/java/com/example/workflow/mapper/ProductReviewMapper.java:10](../src/main/java/com/example/workflow/mapper/ProductReviewMapper.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductReviewMapper.java#L10)(`ProductReview review`) | `ProductReviewDTO` | Chuyển thành dto. |

### [src/main/java/com/example/workflow/mapper/ProductionMapper.java:18](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L18)(`OrderItemAssignment assignment`) | `OrderItemAssignmentDTO` | Chuyển thành dto. |
| [`ProductionMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L25)(`ProductionCheckpoint checkpoint`) | `ProductionCheckpointDTO` | Chuyển thành dto. |
| [`ProductionMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L30)(`ProductionCheckpointImage image`) | `ProductionCheckpointImageDTO` | Chuyển thành dto. |
| [`ProductionMapper.toDto`](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L32)(`ProductionDecision decision`) | `ProductionDecisionDTO` | Chuyển thành dto. |
| [`ProductionMapper.toProductionDto`](../src/main/java/com/example/workflow/mapper/ProductionMapper.java#L36)(`OrderItem orderItem`) | `OrderItemProductionDTO` | Chuyển thành sản xuất dto. |

### [src/main/java/com/example/workflow/mapper/ReputationHistoryMapper.java:9](../src/main/java/com/example/workflow/mapper/ReputationHistoryMapper.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ReputationHistoryMapper.toDto`](../src/main/java/com/example/workflow/mapper/ReputationHistoryMapper.java#L9)(`ReputationHistory history`) | `ReputationHistoryDTO` | Chuyển thành dto. |

### [src/main/java/com/example/workflow/mapper/UserMapper.java:13](../src/main/java/com/example/workflow/mapper/UserMapper.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserMapper.toKeycloakCreateRequest`](../src/main/java/com/example/workflow/mapper/UserMapper.java#L13)(`UserCreDTO source`) | `UserCreDTO` | Chuyển thành keycloak create yêu cầu. |
| [`UserMapper.toResponse`](../src/main/java/com/example/workflow/mapper/UserMapper.java#L20)(`User user`) | `UserResDTO` | Chuyển thành phản hồi. |
| [`UserMapper.updateUser`](../src/main/java/com/example/workflow/mapper/UserMapper.java#L24)(`@MappingTarget  User user, UserCreDTO request`) | `void` | Cập nhật người dùng. |
| [`UserMapper.updateProfile`](../src/main/java/com/example/workflow/mapper/UserMapper.java#L33)(`@MappingTarget  User user, UserProfileUpdateDTO request`) | `void` | Cập nhật hồ sơ. |

### [src/main/java/com/example/workflow/mapper/VoucherMapper.java:13](../src/main/java/com/example/workflow/mapper/VoucherMapper.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`VoucherMapper.toTemplateDto`](../src/main/java/com/example/workflow/mapper/VoucherMapper.java#L13)(`VoucherTemplate entity`) | `VoucherTemplateDTO` | Chuyển thành template dto. |
| [`VoucherMapper.toUserVoucherDto`](../src/main/java/com/example/workflow/mapper/VoucherMapper.java#L14)(`UserVoucher entity`) | `UserVoucherDTO` | Chuyển thành người dùng voucher dto. |
| [`VoucherMapper.toEntity`](../src/main/java/com/example/workflow/mapper/VoucherMapper.java#L16)(`CreateVoucherTemplateRequest request`) | `VoucherTemplate` | Chuyển thành entity. |

### [src/main/java/com/example/workflow/migration/LegacyOrderStateMapper.java:11](../src/main/java/com/example/workflow/migration/LegacyOrderStateMapper.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`LegacyOrderStateMapper.Assessment.requiresReview`](../src/main/java/com/example/workflow/migration/LegacyOrderStateMapper.java#L11)() | `boolean` | Thực hiện duyệt/đánh giá. |
| [`LegacyOrderStateMapper.assess`](../src/main/java/com/example/workflow/migration/LegacyOrderStateMapper.java#L14)(`OrderStatus oldStatus, String paymentMethod`) | `Assessment` | Thực hiện legacy đơn hàng trạng thái. |

### [src/main/java/com/example/workflow/ratelimit/RateLimitDecision.java:16](../src/main/java/com/example/workflow/ratelimit/RateLimitDecision.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitDecision.allowed`](../src/main/java/com/example/workflow/ratelimit/RateLimitDecision.java#L16)(`long limit, long remaining, Duration resetAfter`) | `RateLimitDecision` | Thực hiện rate limit quyết định. |
| [`RateLimitDecision.denied`](../src/main/java/com/example/workflow/ratelimit/RateLimitDecision.java#L20)(`HttpStatus status, String message, long limit, Duration retryAfter`) | `RateLimitDecision` | Thực hiện rate limit quyết định. |

### [src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java:29](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitInterceptor.preHandle`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L29)(`HttpServletRequest request, HttpServletResponse response, Object handler`) | `boolean` | Thực hiện handle. |
| [`RateLimitInterceptor.applyRule`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L40)(`RateLimitRule rule, HttpServletRequest request, HttpServletResponse response`) | `boolean` | Áp dụng rule. |
| [`RateLimitInterceptor.buildIdentityKey`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L60)(`RateLimitRule rule, HttpServletRequest request`) | `String` | Tạo identity khóa. |
| [`RateLimitInterceptor.authenticatedUserId`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L70)() | `String` | Thực hiện người dùng ID. |
| [`RateLimitInterceptor.clientIp`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L74)(`HttpServletRequest request`) | `String` | Thực hiện ip. |
| [`RateLimitInterceptor.sanitize`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L88)(`String value`) | `String` | Làm sạch rate limit interceptor. |
| [`RateLimitInterceptor.writeRateLimitHeaders`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L92)(`HttpServletResponse response, RateLimitDecision decision`) | `void` | Thực hiện rate limit headers. |
| [`RateLimitInterceptor.secondsCeil`](../src/main/java/com/example/workflow/ratelimit/RateLimitInterceptor.java#L101)(`Duration duration`) | `long` | Thực hiện ceil. |

### [src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java:40](../src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitPolicyResolver.resolve`](../src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java#L40)(`HttpServletRequest request`) | `Optional<RateLimitRule>` | Xác định rate limit policy resolver. |
| [`RateLimitPolicyResolver.fixed`](../src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java#L56)(`String group, Set<String> methods, Set<String> pathPatterns, long limit, Duration window, boolean failClosed, RateLimitIdentity identity`) | `RateLimitRule` | Thực hiện rate limit policy resolver. |
| [`RateLimitPolicyResolver.sliding`](../src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java#L68)(`String group, Set<String> methods, Set<String> pathPatterns, long limit, Duration window, boolean failClosed, RateLimitIdentity identity`) | `RateLimitRule` | Thực hiện rate limit policy resolver. |
| [`RateLimitPolicyResolver.normalizePath`](../src/main/java/com/example/workflow/ratelimit/RateLimitPolicyResolver.java#L80)(`HttpServletRequest request`) | `String` | Chuẩn hóa path. |

### [src/main/java/com/example/workflow/ratelimit/RateLimitRule.java:16](../src/main/java/com/example/workflow/ratelimit/RateLimitRule.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitRule.matches`](../src/main/java/com/example/workflow/ratelimit/RateLimitRule.java#L16)(`String method, String path, org.springframework.util.AntPathMatcher matcher`) | `boolean` | Thực hiện rate limit rule. |

### [src/main/java/com/example/workflow/repository/CartRepository.java:16](../src/main/java/com/example/workflow/repository/CartRepository.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CartRepository.findByUserId`](../src/main/java/com/example/workflow/repository/CartRepository.java#L16)(`@Param("userId")  String userId`) | `Optional<Cart>` | Tìm theo người dùng ID. |
| [`CartRepository.findByUserIdForUpdate`](../src/main/java/com/example/workflow/repository/CartRepository.java#L23)(`@Param("userId")  String userId`) | `Optional<Cart>` | Tìm theo người dùng ID cho update. |
| [`CartRepository.findByGuestSessionId`](../src/main/java/com/example/workflow/repository/CartRepository.java#L31)(`@Param("guestSessionId")  String guestSessionId`) | `Optional<Cart>` | Tìm theo phiên khách ID. |
| [`CartRepository.findByGuestSessionIdForUpdate`](../src/main/java/com/example/workflow/repository/CartRepository.java#L38)(`@Param("guestSessionId")  String guestSessionId`) | `Optional<Cart>` | Tìm theo phiên khách ID cho update. |

### [src/main/java/com/example/workflow/repository/ChangeRequestRepository.java:14](../src/main/java/com/example/workflow/repository/ChangeRequestRepository.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChangeRequestRepository.findByOrderIdOrderByCreatedAtDescIdDesc`](../src/main/java/com/example/workflow/repository/ChangeRequestRepository.java#L14)(`Long orderId`) | `List<ChangeRequest>` | Tìm theo đơn hàng ID đơn hàng theo đã tạo tại giảm dần ID giảm dần. |
| [`ChangeRequestRepository.findByIdAndOrderIdForUpdate`](../src/main/java/com/example/workflow/repository/ChangeRequestRepository.java#L16)(`@Param("id")  Long id, @Param("orderId")  Long orderId`) | `Optional<ChangeRequest>` | Tìm theo ID và đơn hàng ID cho update. |

### [src/main/java/com/example/workflow/repository/ChatMessageRepository.java:9](../src/main/java/com/example/workflow/repository/ChatMessageRepository.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChatMessageRepository.findByChatThreadIdOrderByTimestampAsc`](../src/main/java/com/example/workflow/repository/ChatMessageRepository.java#L9)(`Long chatThreadId`) | `List<ChatMessage>` | Tìm theo chat luồng chat ID đơn hàng theo timestamp tăng dần. |

### [src/main/java/com/example/workflow/repository/CustomRequestRepository.java:18](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestRepository.findAllByOwnerIdOrderByUpdatedAtDescIdDesc`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L18)(`String ownerId, Pageable pageable`) | `Page<CustomRequest>` | Tìm tất cả theo chủ sở hữu ID đơn hàng theo updated tại giảm dần ID giảm dần. |
| [`CustomRequestRepository.findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L23)(`String ownerId, CustomRequestStatus status, Pageable pageable`) | `Page<CustomRequest>` | Tìm tất cả theo chủ sở hữu ID và trạng thái đơn hàng theo updated tại giảm dần ID giảm dần. |
| [`CustomRequestRepository.findByIdAndOwnerId`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L29)(`Long id, String ownerId`) | `Optional<CustomRequest>` | Tìm theo ID và chủ sở hữu ID. |
| [`CustomRequestRepository.findByIdAndOwnerIdAndStatus`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L31)(`Long id, String ownerId, CustomRequestStatus status`) | `Optional<CustomRequest>` | Tìm theo ID và chủ sở hữu ID và trạng thái. |
| [`CustomRequestRepository.findByLinkedOrderId`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L37)(`Long linkedOrderId`) | `Optional<CustomRequest>` | Tìm theo linked đơn hàng ID. |
| [`CustomRequestRepository.existsByIdAndOwnerId`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L39)(`Long id, String ownerId`) | `boolean` | Thực hiện theo ID và chủ sở hữu ID. |
| [`CustomRequestRepository.findOwnedByIdForUpdate`](../src/main/java/com/example/workflow/repository/CustomRequestRepository.java#L41)(`@Param("id")  Long id, @Param("ownerId")  String ownerId`) | `Optional<CustomRequest>` | Tìm owned theo ID cho update. |

### [src/main/java/com/example/workflow/repository/GuestVoucherUsageRepository.java:7](../src/main/java/com/example/workflow/repository/GuestVoucherUsageRepository.java#L7)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestVoucherUsageRepository.existsByVoucherTemplateIdAndGuestSessionId`](../src/main/java/com/example/workflow/repository/GuestVoucherUsageRepository.java#L7)(`Long voucherTemplateId, String guestSessionId`) | `boolean` | Thực hiện theo voucher template ID và phiên khách ID. |
| [`GuestVoucherUsageRepository.existsByVoucherTemplateIdAndEmailHash`](../src/main/java/com/example/workflow/repository/GuestVoucherUsageRepository.java#L9)(`Long voucherTemplateId, String emailHash`) | `boolean` | Thực hiện theo voucher template ID và email hash. |
| [`GuestVoucherUsageRepository.existsByVoucherTemplateIdAndPhoneHash`](../src/main/java/com/example/workflow/repository/GuestVoucherUsageRepository.java#L11)(`Long voucherTemplateId, String phoneHash`) | `boolean` | Thực hiện theo voucher template ID và điện thoại hash. |

### [src/main/java/com/example/workflow/repository/NotificationReadRepository.java:10](../src/main/java/com/example/workflow/repository/NotificationReadRepository.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`NotificationReadRepository.findByUserIdAndNotification_IdIn`](../src/main/java/com/example/workflow/repository/NotificationReadRepository.java#L10)(`String userId, Collection<Long> notificationIds`) | `List<NotificationRead>` | Tìm theo người dùng ID và thông báo ID trong. |

### [src/main/java/com/example/workflow/repository/NotificationRepository.java:13](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`NotificationRepository.findByTargetUserIdOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L13)(`String targetUserId, Pageable pageable`) | `Page<Notification>` | Tìm theo đích người dùng ID đơn hàng theo đã tạo tại giảm dần. |
| [`NotificationRepository.findByTargetUserIdOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L15)(`String targetUserId`) | `List<Notification>` | Tìm theo đích người dùng ID đơn hàng theo đã tạo tại giảm dần. |
| [`NotificationRepository.countByTargetUserIdAndIsReadFalse`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L17)(`String targetUserId`) | `long` | Thực hiện theo đích người dùng ID và is đọc false. |
| [`NotificationRepository.findByTargetUserIdIsNullOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L19)(`Pageable pageable`) | `Page<Notification>` | Tìm theo đích người dùng ID is null đơn hàng theo đã tạo tại giảm dần. |
| [`NotificationRepository.findByTargetUserIdIsNullOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L21)() | `List<Notification>` | Tìm theo đích người dùng ID is null đơn hàng theo đã tạo tại giảm dần. |
| [`NotificationRepository.countUnreadAdminNotifications`](../src/main/java/com/example/workflow/repository/NotificationRepository.java#L23)(`@Param("userId")  String userId`) | `long` | Thực hiện unread quản trị viên thông báo. |

### [src/main/java/com/example/workflow/repository/OrderAgreementRepository.java:13](../src/main/java/com/example/workflow/repository/OrderAgreementRepository.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAgreementRepository.findFirstByOrderIdOrderByAgreementVersionDesc`](../src/main/java/com/example/workflow/repository/OrderAgreementRepository.java#L13)(`Long orderId`) | `Optional<OrderAgreement>` | Tìm first theo đơn hàng ID đơn hàng theo thỏa thuận phiên bản giảm dần. |
| [`OrderAgreementRepository.findByOrderIdAndAgreementVersion`](../src/main/java/com/example/workflow/repository/OrderAgreementRepository.java#L15)(`Long orderId, Integer agreementVersion`) | `Optional<OrderAgreement>` | Tìm theo đơn hàng ID và thỏa thuận phiên bản. |
| [`OrderAgreementRepository.existsByOrderIdAndSubmittedAtIsNotNull`](../src/main/java/com/example/workflow/repository/OrderAgreementRepository.java#L17)(`Long orderId`) | `boolean` | Thực hiện theo đơn hàng ID và đã gửi tại is không null. |
| [`OrderAgreementRepository.findByOrderIdAndAgreementVersionForUpdate`](../src/main/java/com/example/workflow/repository/OrderAgreementRepository.java#L19)(`@Param("orderId")  Long orderId, @Param("agreementVersion")  Integer agreementVersion`) | `Optional<OrderAgreement>` | Tìm theo đơn hàng ID và thỏa thuận phiên bản cho update. |

### [src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java:14](../src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignmentRepository.findActiveByOrderIdForUpdate`](../src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java#L14)(`@Param("orderId")  Long orderId`) | `Optional<OrderAssignment>` | Tìm đang hoạt động theo đơn hàng ID cho update. |
| [`OrderAssignmentRepository.findActiveByStaffIdForUpdate`](../src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java#L18)(`@Param("staffId")  String staffId`) | `Optional<OrderAssignment>` | Tìm đang hoạt động theo nhân viên ID cho update. |
| [`OrderAssignmentRepository.findActiveByOrderId`](../src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java#L22)(`@Param("orderId")  Long orderId`) | `Optional<OrderAssignment>` | Tìm đang hoạt động theo đơn hàng ID. |
| [`OrderAssignmentRepository.findByOrderIdOrderByAssignedAtDesc`](../src/main/java/com/example/workflow/repository/OrderAssignmentRepository.java#L25)(`Long orderId`) | `List<OrderAssignment>` | Tìm theo đơn hàng ID đơn hàng theo được phân công tại giảm dần. |

### [src/main/java/com/example/workflow/repository/OrderChatThreadRepository.java:9](../src/main/java/com/example/workflow/repository/OrderChatThreadRepository.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderChatThreadRepository.findByOrderId`](../src/main/java/com/example/workflow/repository/OrderChatThreadRepository.java#L9)(`Long orderId`) | `Optional<OrderChatThread>` | Tìm theo đơn hàng ID. |

### [src/main/java/com/example/workflow/repository/OrderItemAssignmentRepository.java:10](../src/main/java/com/example/workflow/repository/OrderItemAssignmentRepository.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItemAssignmentRepository.findByOrderItem_Id`](../src/main/java/com/example/workflow/repository/OrderItemAssignmentRepository.java#L10)(`Long orderItemId`) | `Optional<OrderItemAssignment>` | Tìm theo mặt hàng của đơn hàng ID. |
| [`OrderItemAssignmentRepository.existsByOrderItem_Id`](../src/main/java/com/example/workflow/repository/OrderItemAssignmentRepository.java#L13)(`Long orderItemId`) | `boolean` | Thực hiện theo mặt hàng của đơn hàng ID. |

### [src/main/java/com/example/workflow/repository/OrderItemRepository.java:16](../src/main/java/com/example/workflow/repository/OrderItemRepository.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItemRepository.findReviewTargetById`](../src/main/java/com/example/workflow/repository/OrderItemRepository.java#L16)(`@Param("id")  Long id`) | `Optional<OrderItem>` | Tìm duyệt/đánh giá đích theo ID. |
| [`OrderItemRepository.findBestSellingProducts`](../src/main/java/com/example/workflow/repository/OrderItemRepository.java#L24)(`@Param("status")  OrderStatus status, @Param("fromTime")  LocalDateTime fromTime, @Param("toTime")  LocalDateTime toTime, Pageable pageable`) | `Page<BestSellerProductDTO>` | Tìm best selling sản phẩm. |

### [src/main/java/com/example/workflow/repository/OrderRepository.java:28](../src/main/java/com/example/workflow/repository/OrderRepository.java#L28)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderRepository.findById`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L28)(`@Param("id")  Long id`) | `Optional<Order>` | Tìm theo ID. |
| [`OrderRepository.findByIdForUpdate`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L39)(`@Param("id")  Long id`) | `Optional<Order>` | Tìm theo ID cho update. |
| [`OrderRepository.findOrderIdsByStatusBefore`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L51)(`@Param("status")  OrderStatus status, @Param("cutoff")  LocalDateTime cutoff`) | `List<Long>` | Tìm đơn hàng ID theo trạng thái trước. |
| [`OrderRepository.findCustomAgreementTimeoutCandidates`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L59)(`@Param("now")  LocalDateTime now, @Param("statuses")  List<OrderStatus> statuses, Pageable pageable`) | `List<Long>` | Tìm custom thỏa thuận timeout candidates. |
| [`OrderRepository.findByStatusOrderByStartOrderTimeAscIdAsc`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L69)(`OrderStatus status, Pageable pageable`) | `Page<Order>` | Tìm theo trạng thái đơn hàng theo start đơn hàng thời gian tăng dần ID tăng dần. |
| [`OrderRepository.findOrderIdsByUserId`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L76)(`@Param("userId")  String userId, @Param("oldestFirstStatuses")  List<OrderStatus> oldestFirstStatuses, @Param("shippingStatus")  OrderStatus shippingStatus, @Param("deliveredStatus")  OrderStatus deliveredStatus, @Param("cancelledStatus")  OrderStatus cancelledStatus, Pageable pageable`) | `Page<Long>` | Tìm đơn hàng ID theo người dùng ID. |
| [`OrderRepository.findListDtoByUserId`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L99)(`@Param("userId")  String userId, @Param("oldestFirstStatuses")  List<OrderStatus> oldestFirstStatuses, @Param("shippingStatus")  OrderStatus shippingStatus, @Param("deliveredStatus")  OrderStatus deliveredStatus, @Param("cancelledStatus")  OrderStatus cancelledStatus, @Param("minPrice")  BigDecimal minPrice, @Param("maxPrice")  BigDecimal maxPrice, Pageable pageable`) | `Page<OrderListDTO>` | Tìm list dto theo người dùng ID. |
| [`OrderRepository.findListDtoByUserIdAndStatus`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L131)(`@Param("userId")  String userId, @Param("status")  OrderStatus status, @Param("deliveredStatus")  OrderStatus deliveredStatus, @Param("minPrice")  BigDecimal minPrice, @Param("maxPrice")  BigDecimal maxPrice, Pageable pageable`) | `Page<OrderListDTO>` | Tìm list dto theo người dùng ID và trạng thái. |
| [`OrderRepository.findFullOrdersByIds`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L154)(`@Param("orderIds")  List<Long> orderIds`) | `List<Order>` | Tìm full đơn hàng theo ID. |
| [`OrderRepository.findListDtoByStatusOldestFirst`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L165)(`@Param("status")  OrderStatus status, Pageable pageable`) | `Page<OrderListDTO>` | Tìm list dto theo trạng thái oldest first. |
| [`OrderRepository.findUnassignedListDtoByStatus`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L177)(`@Param("status")  OrderStatus status, Pageable pageable`) | `Page<OrderListDTO>` | Tìm unassigned list dto theo trạng thái. |
| [`OrderRepository.findAssignmentQueueByStatus`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L189)(`@Param("status")  OrderStatus status, Pageable pageable`) | `Page<OrderAssignmentQueueDTO>` | Tìm phân công queue theo trạng thái. |
| [`OrderRepository.findListDtoByWarehouseStaffIdAndStatusIn`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L201)(`@Param("staffId")  String staffId, @Param("statuses")  List<OrderStatus> statuses, @Param("oldestFirstStatuses")  List<OrderStatus> oldestFirstStatuses, @Param("assignedStatus")  OrderStatus assignedStatus, @Param("kcsStatus")  OrderStatus kcsStatus, @Param("shippingStatus")  OrderStatus shippingStatus, Pageable pageable`) | `Page<OrderListDTO>` | Tìm list dto theo warehouse nhân viên ID và trạng thái trong. |
| [`OrderRepository.calculateTotalRevenue`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L230)(`@Param("status")  OrderStatus status`) | `BigDecimal` | Tính total revenue. |
| [`OrderRepository.countOrdersByStatus`](../src/main/java/com/example/workflow/repository/OrderRepository.java#L233)(`@Param("status")  OrderStatus status`) | `Long` | Thực hiện đơn hàng theo trạng thái. |

### [src/main/java/com/example/workflow/repository/OrderStatusHistoryRepository.java:12](../src/main/java/com/example/workflow/repository/OrderStatusHistoryRepository.java#L12)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderStatusHistoryRepository.findByOrderIdOrderByUpdatetimeAsc`](../src/main/java/com/example/workflow/repository/OrderStatusHistoryRepository.java#L12)(`Long orderId`) | `List<OrderStatusHistory>` | Tìm theo đơn hàng ID đơn hàng theo updatetime tăng dần. |

### [src/main/java/com/example/workflow/repository/PasswordResetTokenRepository.java:10](../src/main/java/com/example/workflow/repository/PasswordResetTokenRepository.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PasswordResetTokenRepository.findByTokenHashAndUsedAtIsNull`](../src/main/java/com/example/workflow/repository/PasswordResetTokenRepository.java#L10)(`String tokenHash`) | `Optional<PasswordResetToken>` | Tìm theo token hash và used tại is null. |
| [`PasswordResetTokenRepository.findByUser_IdAndUsedAtIsNull`](../src/main/java/com/example/workflow/repository/PasswordResetTokenRepository.java#L12)(`String userId`) | `List<PasswordResetToken>` | Tìm theo người dùng ID và used tại is null. |

### [src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java:17](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentAttemptRepository.findByProviderAndRequestReferenceForUpdate`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L17)(`@Param("provider")  String provider, @Param("reference")  String reference`) | `Optional<PaymentAttempt>` | Tìm theo provider và yêu cầu tài liệu tham khảo cho update. |
| [`PaymentAttemptRepository.findByProviderAndProviderTransactionId`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L24)(`String provider, String providerTransactionId`) | `Optional<PaymentAttempt>` | Tìm theo provider và provider giao dịch ID. |
| [`PaymentAttemptRepository.findFirstByOrderIdAndStatusOrderByOpenedAtDesc`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L26)(`Long orderId, PaymentAttemptStatus status`) | `Optional<PaymentAttempt>` | Tìm first theo đơn hàng ID và trạng thái đơn hàng theo opened tại giảm dần. |
| [`PaymentAttemptRepository.findFirstByOrderIdOrderByOpenedAtDesc`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L31)(`Long orderId`) | `Optional<PaymentAttempt>` | Tìm first theo đơn hàng ID đơn hàng theo opened tại giảm dần. |
| [`PaymentAttemptRepository.findDueAttemptIds`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L33)(`@Param("status")  PaymentAttemptStatus status, @Param("now")  LocalDateTime now, Pageable pageable`) | `List<Long>` | Tìm due lần thử ID. |
| [`PaymentAttemptRepository.findByIdForUpdate`](../src/main/java/com/example/workflow/repository/PaymentAttemptRepository.java#L40)(`@Param("id")  Long id`) | `Optional<PaymentAttempt>` | Tìm theo ID cho update. |

### [src/main/java/com/example/workflow/repository/ProductRepository.java:17](../src/main/java/com/example/workflow/repository/ProductRepository.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductRepository.findAllByAvailabilityPriority`](../src/main/java/com/example/workflow/repository/ProductRepository.java#L17)(`@Param("acceptingStatus")  ProductAvailabilityStatus acceptingStatus, Pageable pageable`) | `Page<Product>` | Tìm tất cả theo availability priority. |
| [`ProductRepository.searchByAvailabilityPriority`](../src/main/java/com/example/workflow/repository/ProductRepository.java#L26)(`@Param("keyword")  String keyword, @Param("minPrice")  BigDecimal minPrice, @Param("maxPrice")  BigDecimal maxPrice, @Param("acceptingStatus")  ProductAvailabilityStatus acceptingStatus, Pageable pageable`) | `Page<Product>` | Tìm kiếm theo availability priority. |

### [src/main/java/com/example/workflow/repository/ProductReviewRepository.java:17](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewRepository.existsByOrderItem_Id`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L17)(`Long orderItemId`) | `boolean` | Thực hiện theo mặt hàng của đơn hàng ID. |
| [`ProductReviewRepository.findByIdAndUser_Id`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L19)(`Long id, String userId`) | `Optional<ProductReview>` | Tìm theo ID và người dùng ID. |
| [`ProductReviewRepository.findByProduct_IdAndStatusOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L22)(`Long productId, ProductReviewStatus status, Pageable pageable`) | `Page<ProductReview>` | Tìm theo sản phẩm ID và trạng thái đơn hàng theo đã tạo tại giảm dần. |
| [`ProductReviewRepository.findByProductVariant_IdAndStatusOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L25)(`Long variantId, ProductReviewStatus status, Pageable pageable`) | `Page<ProductReview>` | Tìm theo sản phẩm biến thể ID và trạng thái đơn hàng theo đã tạo tại giảm dần. |
| [`ProductReviewRepository.findByProductVariant_IdOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L28)(`Long variantId, Pageable pageable`) | `Page<ProductReview>` | Tìm theo sản phẩm biến thể ID đơn hàng theo đã tạo tại giảm dần. |
| [`ProductReviewRepository.findByOrderItem_IdIn`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L31)(`Collection<Long> orderItemIds`) | `List<ProductReview>` | Tìm theo mặt hàng của đơn hàng ID trong. |
| [`ProductReviewRepository.countByProduct_IdAndStatus`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L33)(`Long productId, ProductReviewStatus status`) | `long` | Thực hiện theo sản phẩm ID và trạng thái. |
| [`ProductReviewRepository.countByProduct_IdAndStatusAndRatingIsNotNull`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L35)(`Long productId, ProductReviewStatus status`) | `long` | Thực hiện theo sản phẩm ID và trạng thái và rating is không null. |
| [`ProductReviewRepository.countByProduct_IdAndStatusAndRating`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L37)(`Long productId, ProductReviewStatus status, Integer rating`) | `long` | Thực hiện theo sản phẩm ID và trạng thái và rating. |
| [`ProductReviewRepository.countByProductVariant_IdAndStatus`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L39)(`Long variantId, ProductReviewStatus status`) | `long` | Thực hiện theo sản phẩm biến thể ID và trạng thái. |
| [`ProductReviewRepository.countByProductVariant_IdAndStatusAndRatingIsNotNull`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L41)(`Long variantId, ProductReviewStatus status`) | `long` | Thực hiện theo sản phẩm biến thể ID và trạng thái và rating is không null. |
| [`ProductReviewRepository.countByProductVariant_IdAndStatusAndRating`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L43)(`Long variantId, ProductReviewStatus status, Integer rating`) | `long` | Thực hiện theo sản phẩm biến thể ID và trạng thái và rating. |
| [`ProductReviewRepository.averageRatingByProduct`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L45)(`@Param("productId")  Long productId, @Param("status")  ProductReviewStatus status`) | `double` | Thực hiện rating theo sản phẩm. |
| [`ProductReviewRepository.averageRatingByVariant`](../src/main/java/com/example/workflow/repository/ProductReviewRepository.java#L49)(`@Param("variantId")  Long variantId, @Param("status")  ProductReviewStatus status`) | `double` | Thực hiện rating theo biến thể. |

### [src/main/java/com/example/workflow/repository/ProductTypeRepository.java:10](../src/main/java/com/example/workflow/repository/ProductTypeRepository.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductTypeRepository.findAllByActiveTrueOrderByNameAsc`](../src/main/java/com/example/workflow/repository/ProductTypeRepository.java#L10)() | `List<ProductType>` | Tìm tất cả theo đang hoạt động true đơn hàng theo tên tăng dần. |
| [`ProductTypeRepository.findAllByOrderByNameAsc`](../src/main/java/com/example/workflow/repository/ProductTypeRepository.java#L11)() | `List<ProductType>` | Tìm tất cả theo đơn hàng theo tên tăng dần. |
| [`ProductTypeRepository.findByCodeIgnoreCase`](../src/main/java/com/example/workflow/repository/ProductTypeRepository.java#L12)(`String code`) | `Optional<ProductType>` | Tìm theo code ignore case. |
| [`ProductTypeRepository.existsByCodeIgnoreCase`](../src/main/java/com/example/workflow/repository/ProductTypeRepository.java#L13)(`String code`) | `boolean` | Thực hiện theo code ignore case. |

### [src/main/java/com/example/workflow/repository/ProductVariantRepository.java:12](../src/main/java/com/example/workflow/repository/ProductVariantRepository.java#L12)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductVariantRepository.findActiveById`](../src/main/java/com/example/workflow/repository/ProductVariantRepository.java#L12)(`@Param("variantId")  Long variantId`) | `Optional<ProductVariant>` | Tìm đang hoạt động theo ID. |

### [src/main/java/com/example/workflow/repository/ProductionCheckpointImageRepository.java:9](../src/main/java/com/example/workflow/repository/ProductionCheckpointImageRepository.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpointImageRepository.findByCheckpoint_IdOrderByDisplayOrderAsc`](../src/main/java/com/example/workflow/repository/ProductionCheckpointImageRepository.java#L9)(`Long checkpointId`) | `List<ProductionCheckpointImage>` | Tìm theo checkpoint ID đơn hàng theo display đơn hàng tăng dần. |

### [src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java:17](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpointRepository.findByOrderItem_IdOrderBySubmittedAtAsc`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L17)(`Long orderItemId`) | `List<ProductionCheckpoint>` | Tìm theo mặt hàng của đơn hàng ID đơn hàng theo đã gửi tại tăng dần. |
| [`ProductionCheckpointRepository.findProductionCheckpointById`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L20)(`Long id`) | `Optional<ProductionCheckpoint>` | Tìm checkpoint sản xuất theo ID. |
| [`ProductionCheckpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L23)(`Long orderItemId, ProductionCheckpointStage stage`) | `Optional<ProductionCheckpoint>` | Tìm top theo mặt hàng của đơn hàng ID và stage đơn hàng theo lần thử number giảm dần. |
| [`ProductionCheckpointRepository.findByIdForUpdate`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L29)(`@Param("checkpointId")  Long checkpointId`) | `Optional<ProductionCheckpoint>` | Tìm theo ID cho update. |
| [`ProductionCheckpointRepository.findOrderItemIdsWithoutPassedFinal`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L36)(`@Param("orderId")  Long orderId`) | `List<Long>` | Tìm mặt hàng của đơn hàng ID không có passed cuối. |
| [`ProductionCheckpointRepository.existsByOrderItem_IdAndStageAndAttemptNumber`](../src/main/java/com/example/workflow/repository/ProductionCheckpointRepository.java#L43)(`Long orderItemId, ProductionCheckpointStage stage, int attemptNumber`) | `boolean` | Thực hiện theo mặt hàng của đơn hàng ID và stage và lần thử number. |

### [src/main/java/com/example/workflow/repository/ProductionDecisionRepository.java:10](../src/main/java/com/example/workflow/repository/ProductionDecisionRepository.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionDecisionRepository.findByCheckpoint_Id`](../src/main/java/com/example/workflow/repository/ProductionDecisionRepository.java#L10)(`Long checkpointId`) | `Optional<ProductionDecision>` | Tìm theo checkpoint ID. |
| [`ProductionDecisionRepository.existsByCheckpoint_Id`](../src/main/java/com/example/workflow/repository/ProductionDecisionRepository.java#L13)(`Long checkpointId`) | `boolean` | Thực hiện theo checkpoint ID. |

### [src/main/java/com/example/workflow/repository/ReputationHistoryRepository.java:11](../src/main/java/com/example/workflow/repository/ReputationHistoryRepository.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ReputationHistoryRepository.findByUser_IdOrderByCreatedAtDesc`](../src/main/java/com/example/workflow/repository/ReputationHistoryRepository.java#L11)(`String userId, Pageable pageable`) | `Page<ReputationHistory>` | Tìm theo người dùng ID đơn hàng theo đã tạo tại giảm dần. |

### [src/main/java/com/example/workflow/repository/UserRepository.java:19](../src/main/java/com/example/workflow/repository/UserRepository.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserRepository.findByIdForUpdate`](../src/main/java/com/example/workflow/repository/UserRepository.java#L19)(`@Param("id")  String id`) | `Optional<User>` | Tìm theo ID cho update. |
| [`UserRepository.existsByUsername`](../src/main/java/com/example/workflow/repository/UserRepository.java#L23)(`String username`) | `boolean` | Thực hiện theo tên đăng nhập. |
| [`UserRepository.findByUsername`](../src/main/java/com/example/workflow/repository/UserRepository.java#L24)(`String username`) | `Optional<User>` | Tìm theo tên đăng nhập. |
| [`UserRepository.findByEmailIgnoreCaseAndIsDeleteFalse`](../src/main/java/com/example/workflow/repository/UserRepository.java#L25)(`String email`) | `Optional<User>` | Tìm theo email ignore case và is delete false. |
| [`UserRepository.findByUsernameIgnoreCaseAndIsDeleteFalse`](../src/main/java/com/example/workflow/repository/UserRepository.java#L26)(`String username`) | `Optional<User>` | Tìm theo tên đăng nhập ignore case và is delete false. |
| [`UserRepository.findAllCustom`](../src/main/java/com/example/workflow/repository/UserRepository.java#L27)(`Pageable pageable`) | `Page<UserListDTO>` | Tìm tất cả custom. |
| [`UserRepository.findAllCustomByRoles`](../src/main/java/com/example/workflow/repository/UserRepository.java#L30)(`@Param("roles")  List<Role> roles, Pageable pageable`) | `Page<UserListDTO>` | Tìm tất cả custom theo vai trò. |
| [`UserRepository.findByRoleAndIsDeleteFalse`](../src/main/java/com/example/workflow/repository/UserRepository.java#L34)(`Role role, Pageable pageable`) | `Page<User>` | Tìm theo vai trò và is delete false. |
| [`UserRepository.findByRoleAndIsDeleteFalse`](../src/main/java/com/example/workflow/repository/UserRepository.java#L36)(`Role role`) | `List<User>` | Tìm theo vai trò và is delete false. |
| [`UserRepository.findAvailableStaff`](../src/main/java/com/example/workflow/repository/UserRepository.java#L38)(`Pageable pageable`) | `Page<User>` | Tìm khả dụng nhân viên. |
| [`UserRepository.existsByEmail`](../src/main/java/com/example/workflow/repository/UserRepository.java#L44)(`String email`) | `boolean` | Thực hiện theo email. |
| [`UserRepository.existsByPhone`](../src/main/java/com/example/workflow/repository/UserRepository.java#L45)(`String phone`) | `boolean` | Thực hiện theo điện thoại. |
| [`UserRepository.existsByEmailAndIdNot`](../src/main/java/com/example/workflow/repository/UserRepository.java#L46)(`String email, String id`) | `boolean` | Thực hiện theo email và ID không. |
| [`UserRepository.existsByPhoneAndIdNot`](../src/main/java/com/example/workflow/repository/UserRepository.java#L47)(`String phone, String id`) | `boolean` | Thực hiện theo điện thoại và ID không. |
| [`UserRepository.findByRoleInAndIsDeleteFalseAndEmailIsNotNull`](../src/main/java/com/example/workflow/repository/UserRepository.java#L49)(`List<Role> roles`) | `List<User>` | Tìm theo vai trò trong và is delete false và email is không null. |

### [src/main/java/com/example/workflow/repository/UserVoucherRepository.java:15](../src/main/java/com/example/workflow/repository/UserVoucherRepository.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserVoucherRepository.findByUserIdAndIsUsedFalse`](../src/main/java/com/example/workflow/repository/UserVoucherRepository.java#L15)(`String userId`) | `List<UserVoucher>` | Tìm theo người dùng ID và is used false. |
| [`UserVoucherRepository.findByIdForUpdate`](../src/main/java/com/example/workflow/repository/UserVoucherRepository.java#L17)(`@Param("id")  Long id`) | `Optional<UserVoucher>` | Tìm theo ID cho update. |
| [`UserVoucherRepository.findAvailableWallet`](../src/main/java/com/example/workflow/repository/UserVoucherRepository.java#L21)(`@Param("userId")  String userId, @Param("now")  LocalDateTime now`) | `List<UserVoucher>` | Tìm khả dụng wallet. |
| [`UserVoucherRepository.existsAvailableByUserIdAndTemplateId`](../src/main/java/com/example/workflow/repository/UserVoucherRepository.java#L29)(`@Param("userId")  String userId, @Param("templateId")  Long templateId, @Param("now")  LocalDateTime now`) | `boolean` | Thực hiện khả dụng theo người dùng ID và template ID. |

### [src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java:16](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`VoucherTemplateRepository.findAvailableTemplates`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L16)(`@Param("now")  LocalDateTime now`) | `List<VoucherTemplate>` | Tìm khả dụng templates. |
| [`VoucherTemplateRepository.findAvailableTemplatesForManagement`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L19)(`@Param("now")  LocalDateTime now`) | `List<VoucherTemplate>` | Tìm khả dụng templates cho management. |
| [`VoucherTemplateRepository.findAvailableGuestTemplates`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L22)(`@Param("now")  LocalDateTime now`) | `List<VoucherTemplate>` | Tìm khả dụng khách templates. |
| [`VoucherTemplateRepository.findByCodeIgnoreCase`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L25)(`@Param("code")  String code`) | `Optional<VoucherTemplate>` | Tìm theo code ignore case. |
| [`VoucherTemplateRepository.decrementQuantity`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L28)(`@Param("id")  Long id, @Param("now")  LocalDateTime now`) | `int` | Thực hiện số lượng. |
| [`VoucherTemplateRepository.decrementGuestQuantity`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L32)(`@Param("id")  Long id, @Param("now")  LocalDateTime now`) | `int` | Thực hiện khách số lượng. |
| [`VoucherTemplateRepository.incrementGuestQuantity`](../src/main/java/com/example/workflow/repository/VoucherTemplateRepository.java#L36)(`@Param("id")  Long id`) | `int` | Thực hiện khách số lượng. |

### [src/main/java/com/example/workflow/repository/WishlistItemRepository.java:17](../src/main/java/com/example/workflow/repository/WishlistItemRepository.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WishlistItemRepository.findActiveByUserId`](../src/main/java/com/example/workflow/repository/WishlistItemRepository.java#L17)(`@Param("userId")  String userId, Pageable pageable`) | `Page<WishlistItem>` | Tìm đang hoạt động theo người dùng ID. |
| [`WishlistItemRepository.findByUser_IdAndProduct_Id`](../src/main/java/com/example/workflow/repository/WishlistItemRepository.java#L26)(`String userId, Long productId`) | `Optional<WishlistItem>` | Tìm theo người dùng ID và sản phẩm ID. |
| [`WishlistItemRepository.findExistingProductIdsInWishlist`](../src/main/java/com/example/workflow/repository/WishlistItemRepository.java#L28)(`@Param("userId")  String userId, @Param("productIds")  Collection<Long> productIds`) | `Set<Long>` | Tìm existing sản phẩm ID trong wishlist. |
| [`WishlistItemRepository.deleteByUser_IdAndProduct_Id`](../src/main/java/com/example/workflow/repository/WishlistItemRepository.java#L37)(`String userId, Long productId`) | `void` | Xóa theo người dùng ID và sản phẩm ID. |

### [src/main/java/com/example/workflow/service/AuthService.java:19](../src/main/java/com/example/workflow/service/AuthService.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`AuthService.login`](../src/main/java/com/example/workflow/service/AuthService.java#L19)(`LoginRequest request`) | `AuthResponse` | Thực hiện auth. |

### [src/main/java/com/example/workflow/service/CartService.java:36](../src/main/java/com/example/workflow/service/CartService.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CartService.CartOwner.user`](../src/main/java/com/example/workflow/service/CartService.java#L36)(`String userId`) | `CartOwner` | Thực hiện giỏ hàng chủ sở hữu. |
| [`CartService.CartOwner.guest`](../src/main/java/com/example/workflow/service/CartService.java#L40)(`String guestSessionId`) | `CartOwner` | Thực hiện giỏ hàng chủ sở hữu. |
| [`CartService.CartOwner.cacheKey`](../src/main/java/com/example/workflow/service/CartService.java#L44)() | `String` | Tạo khóa cache khóa. |
| [`CartService.resolveOwner`](../src/main/java/com/example/workflow/service/CartService.java#L55)(`String guestSessionId`) | `CartOwner` | Xác định chủ sở hữu. |
| [`CartService.addToCart`](../src/main/java/com/example/workflow/service/CartService.java#L60)(`CartOwner owner, Long variantId, int quantity`) | `void` | Thêm tới giỏ hàng. |
| [`CartService.updateQuantity`](../src/main/java/com/example/workflow/service/CartService.java#L66)(`CartOwner owner, Long variantId, int newQuantity`) | `void` | Cập nhật số lượng. |
| [`CartService.removeFromCart`](../src/main/java/com/example/workflow/service/CartService.java#L72)(`CartOwner owner, Long variantId`) | `void` | Xóa từ giỏ hàng. |
| [`CartService.getCart`](../src/main/java/com/example/workflow/service/CartService.java#L78)(`CartOwner owner`) | `CartResDTO` | Lấy giỏ hàng. |
| [`CartService.toActiveCartDto`](../src/main/java/com/example/workflow/service/CartService.java#L84)(`Cart cart`) | `CartResDTO` | Chuyển thành đang hoạt động giỏ hàng dto. |
| [`CartService.isActiveCartItem`](../src/main/java/com/example/workflow/service/CartService.java#L99)(`CartItem item`) | `boolean` | Kiểm tra điều kiện đang hoạt động giỏ hàng mặt hàng. |
| [`CartService.addVariantToCart`](../src/main/java/com/example/workflow/service/CartService.java#L107)(`Cart cart, Long variantId, int quantity`) | `void` | Thêm biến thể tới giỏ hàng. |
| [`CartService.updateCartQuantity`](../src/main/java/com/example/workflow/service/CartService.java#L125)(`Cart cart, Long variantId, int newQuantity`) | `void` | Cập nhật giỏ hàng số lượng. |
| [`CartService.removeVariantFromCart`](../src/main/java/com/example/workflow/service/CartService.java#L135)(`Cart cart, Long variantId`) | `void` | Xóa biến thể từ giỏ hàng. |
| [`CartService.findCartItemOrThrow`](../src/main/java/com/example/workflow/service/CartService.java#L140)(`Cart cart, Long variantId`) | `CartItem` | Tìm giỏ hàng mặt hàng hoặc ném lỗi. |
| [`CartService.findCartItem`](../src/main/java/com/example/workflow/service/CartService.java#L147)(`Cart cart, Long variantId`) | `CartItem` | Tìm giỏ hàng mặt hàng. |
| [`CartService.getActiveVariantOrThrow`](../src/main/java/com/example/workflow/service/CartService.java#L154)(`Long variantId`) | `ProductVariant` | Lấy đang hoạt động biến thể hoặc ném lỗi. |
| [`CartService.createCartItem`](../src/main/java/com/example/workflow/service/CartService.java#L158)(`Cart cart, ProductVariant variant, int quantity`) | `CartItem` | Tạo giỏ hàng mặt hàng. |
| [`CartService.getCartForRead`](../src/main/java/com/example/workflow/service/CartService.java#L166)(`CartOwner owner`) | `Cart` | Lấy giỏ hàng cho đọc. |
| [`CartService.getOrCreateCart`](../src/main/java/com/example/workflow/service/CartService.java#L175)(`CartOwner owner`) | `Cart` | Lấy hoặc create giỏ hàng. |
| [`CartService.getExistingCart`](../src/main/java/com/example/workflow/service/CartService.java#L183)(`CartOwner owner, ConstantErrorCode errorCode`) | `Cart` | Lấy existing giỏ hàng. |
| [`CartService.requireCartOwner`](../src/main/java/com/example/workflow/service/CartService.java#L191)(`CartOwner owner`) | `CartOwner` | Lấy và đảm bảo tồn tại giỏ hàng chủ sở hữu. |
| [`CartService.getCartOrThrow`](../src/main/java/com/example/workflow/service/CartService.java#L201)(`String userId, ConstantErrorCode errorCode`) | `Cart` | Lấy giỏ hàng hoặc ném lỗi. |
| [`CartService.createEmptyGuestCart`](../src/main/java/com/example/workflow/service/CartService.java#L206)(`String guestSessionId`) | `Cart` | Tạo empty khách giỏ hàng. |
| [`CartService.getOrCreateGuestCart`](../src/main/java/com/example/workflow/service/CartService.java#L213)(`String guestSessionId`) | `Cart` | Lấy hoặc create khách giỏ hàng. |
| [`CartService.getGuestCartOrThrow`](../src/main/java/com/example/workflow/service/CartService.java#L222)(`String guestSessionId, ConstantErrorCode errorCode`) | `Cart` | Lấy khách giỏ hàng hoặc ném lỗi. |
| [`CartService.getAuthenticatedCustomerId`](../src/main/java/com/example/workflow/service/CartService.java#L228)() | `Optional<String>` | Lấy authenticated khách hàng ID. |
| [`CartService.validateProductAcceptingOrders`](../src/main/java/com/example/workflow/service/CartService.java#L236)(`ProductVariant variant`) | `void` | Kiểm tra hợp lệ sản phẩm accepting đơn hàng. |

### [src/main/java/com/example/workflow/service/CatalogDurationCalculator.java:13](../src/main/java/com/example/workflow/service/CatalogDurationCalculator.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CatalogDurationCalculator.calculate`](../src/main/java/com/example/workflow/service/CatalogDurationCalculator.java#L13)(`Double madeDay, int quantity`) | `int` | Tính catalog thời lượng calculator. |

### [src/main/java/com/example/workflow/service/ChangeRequestService.java:59](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L59)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChangeRequestService.list`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L59)(`Long orderId`) | `List<ChangeRequestDTO>` | Liệt kê change yêu cầu. |
| [`ChangeRequestService.create`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L72)(`Long orderId, CreateChangeRequestRequest request, String idempotencyKey`) | `ChangeRequestDTO` | Tạo change yêu cầu. |
| [`ChangeRequestService.decide`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L94)(`Long orderId, Long changeRequestId, ChangeRequestDecisionRequest request, String idempotencyKey`) | `ChangeRequestDTO` | Ra quyết định change yêu cầu. |
| [`ChangeRequestService.createWithinTransaction`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L122)(`Long orderId, CreateChangeRequestRequest input, User owner`) | `ChangeRequestDTO` | Tạo trong giao dịch. |
| [`ChangeRequestService.decideWithinTransaction`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L161)(`Long orderId, Long changeRequestId, ChangeRequestDecisionRequest input, User staff`) | `ChangeRequestDTO` | Ra quyết định trong giao dịch. |
| [`ChangeRequestService.authorizeParticipant`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L221)(`Order order, User actor`) | `void` | Kiểm tra quyền truy cập participant. |
| [`ChangeRequestService.requireEligibleOrder`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L237)(`Order order`) | `void` | Lấy và đảm bảo tồn tại eligible đơn hàng. |
| [`ChangeRequestService.requireEligibleItem`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L244)(`Order order, Long itemId`) | `OrderItem` | Lấy và đảm bảo tồn tại eligible mặt hàng. |
| [`ChangeRequestService.requireItem`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L250)(`Order order, Long itemId`) | `OrderItem` | Lấy và đảm bảo tồn tại mặt hàng. |
| [`ChangeRequestService.requireEligibleItem`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L257)(`OrderItem item`) | `void` | Lấy và đảm bảo tồn tại eligible mặt hàng. |
| [`ChangeRequestService.proposedSnapshot`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L269)(`CreateChangeRequestRequest input`) | `String` | Thực hiện snapshot. |
| [`ChangeRequestService.applyProposal`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L277)(`String currentSnapshot, String proposedSnapshot`) | `String` | Áp dụng đề cương. |
| [`ChangeRequestService.toDto`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L302)(`ChangeRequest request, Order order, OrderItem item`) | `ChangeRequestDTO` | Chuyển thành dto. |
| [`ChangeRequestService.lockOrder`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L315)(`Long orderId`) | `Order` | Khóa đơn hàng. |
| [`ChangeRequestService.requireOrder`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L320)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`ChangeRequestService.requireActiveAssignment`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L325)(`Long orderId, String expectedStaffId`) | `OrderAssignment` | Lấy và đảm bảo tồn tại phân công đang hoạt động. |
| [`ChangeRequestService.requireOwner`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L334)(`Order order, String ownerId`) | `void` | Lấy và đảm bảo tồn tại chủ sở hữu. |
| [`ChangeRequestService.requireActiveActor`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L340)(`User actor`) | `void` | Lấy và đảm bảo tồn tại đang hoạt động actor. |
| [`ChangeRequestService.requireOrderVersion`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L346)(`Order order, Long expectedVersion`) | `void` | Lấy và đảm bảo tồn tại đơn hàng phiên bản. |
| [`ChangeRequestService.requireSpecVersion`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L352)(`OrderItem item, Integer expectedVersion`) | `void` | Lấy và đảm bảo tồn tại đặc tả phiên bản. |
| [`ChangeRequestService.requireCreateRequest`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L358)(`CreateChangeRequestRequest request`) | `void` | Lấy và đảm bảo tồn tại create yêu cầu. |
| [`ChangeRequestService.requireDecisionRequest`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L371)(`ChangeRequestDecisionRequest request`) | `void` | Lấy và đảm bảo tồn tại quyết định yêu cầu. |
| [`ChangeRequestService.normalizedDescription`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L382)(`String value`) | `String` | Thực hiện description. |
| [`ChangeRequestService.normalizedRequiredReason`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L389)(`String value`) | `String` | Thực hiện bắt buộc lý do. |
| [`ChangeRequestService.normalizedDecisionReason`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L398)(`String value, ChangeRequestDecision decision`) | `String` | Thực hiện quyết định lý do. |
| [`ChangeRequestService.validatedImageUrls`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L410)(`List<String> values`) | `List<String>` | Thực hiện ảnh URL. |
| [`ChangeRequestService.invalid`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L428)(`String detail`) | `AppException` | Thực hiện change yêu cầu. |
| [`ChangeRequestService.safeItems`](../src/main/java/com/example/workflow/service/ChangeRequestService.java#L432)(`Order order`) | `List<OrderItem>` | Thực hiện mặt hàng. |

### [src/main/java/com/example/workflow/service/CheckoutService.java:66](../src/main/java/com/example/workflow/service/CheckoutService.java#L66)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutService.checkoutCurrentUser`](../src/main/java/com/example/workflow/service/CheckoutService.java#L66)(`CheckoutRequest request, String idempotencyKey`) | `CheckoutResponseDTO` | Thực hiện người dùng hiện tại. |
| [`CheckoutService.checkoutGuest`](../src/main/java/com/example/workflow/service/CheckoutService.java#L70)(`String guestSessionId, GuestCheckoutRequest request, String idempotencyKey`) | `CheckoutResponseDTO` | Thực hiện khách. |
| [`CheckoutService.checkoutUser`](../src/main/java/com/example/workflow/service/CheckoutService.java#L95)(`String userId, CheckoutRequest request, String idempotencyKey`) | `CheckoutResponseDTO` | Thực hiện người dùng. |
| [`CheckoutService.createUserOrder`](../src/main/java/com/example/workflow/service/CheckoutService.java#L116)(`String userId, CheckoutRequest request, List<Long> variantIds, PaymentMethod paymentMethod`) | `CheckoutResponseDTO` | Tạo người dùng đơn hàng. |
| [`CheckoutService.createGuestOrder`](../src/main/java/com/example/workflow/service/CheckoutService.java#L151)(`String guestSessionId, GuestCheckoutRequest request, List<Long> variantIds`) | `CheckoutResponseDTO` | Tạo đơn hàng của khách. |
| [`CheckoutService.validateCheckoutItem`](../src/main/java/com/example/workflow/service/CheckoutService.java#L196)(`CartItem cartItem`) | `ProductVariant` | Kiểm tra hợp lệ checkout mặt hàng. |
| [`CheckoutService.selectItemsStrictly`](../src/main/java/com/example/workflow/service/CheckoutService.java#L218)(`Cart cart, List<Long> variantIds`) | `List<CartItem>` | Thực hiện mặt hàng strictly. |
| [`CheckoutService.removeCheckedOutItems`](../src/main/java/com/example/workflow/service/CheckoutService.java#L235)(`Cart cart, List<CartItem> selectedItems`) | `void` | Xóa checked out mặt hàng. |
| [`CheckoutService.applyTotals`](../src/main/java/com/example/workflow/service/CheckoutService.java#L241)(`Order order, BigDecimal totalPrice, BigDecimal discountAmount`) | `void` | Áp dụng totals. |
| [`CheckoutService.response`](../src/main/java/com/example/workflow/service/CheckoutService.java#L251)(`Order order, String lookupToken`) | `CheckoutResponseDTO` | Thực hiện checkout. |
| [`CheckoutService.maximumDurationDays`](../src/main/java/com/example/workflow/service/CheckoutService.java#L256)(`Order order`) | `int` | Thực hiện thời lượng days. |
| [`CheckoutService.normalizeVariantIds`](../src/main/java/com/example/workflow/service/CheckoutService.java#L264)(`List<Long> variantIds`) | `List<Long>` | Chuẩn hóa biến thể ID. |
| [`CheckoutService.parsePaymentMethod`](../src/main/java/com/example/workflow/service/CheckoutService.java#L278)(`String value`) | `PaymentMethod` | Phân tích thanh toán method. |
| [`CheckoutService.normalizeCode`](../src/main/java/com/example/workflow/service/CheckoutService.java#L289)(`String value`) | `String` | Chuẩn hóa code. |

### [src/main/java/com/example/workflow/service/CurrentUserService.java:26](../src/main/java/com/example/workflow/service/CurrentUserService.java#L26)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CurrentUserService.findCurrentUserId`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L26)() | `Optional<String>` | Tìm người dùng hiện tại ID. |
| [`CurrentUserService.requireCurrentUserId`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L30)() | `String` | Lấy và đảm bảo tồn tại người dùng hiện tại ID. |
| [`CurrentUserService.findCurrentUser`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L34)() | `Optional<User>` | Tìm người dùng hiện tại. |
| [`CurrentUserService.requireCurrentUser`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L38)() | `User` | Lấy và đảm bảo tồn tại người dùng hiện tại. |
| [`CurrentUserService.requireCurrentUser`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L42)(`Role role, ConstantErrorCode errorCode, Object... errorArgs`) | `User` | Lấy và đảm bảo tồn tại người dùng hiện tại. |
| [`CurrentUserService.requireCurrentUser`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L46)(`Set<Role> roles, ConstantErrorCode errorCode, Object... errorArgs`) | `User` | Lấy và đảm bảo tồn tại người dùng hiện tại. |
| [`CurrentUserService.requireUserId`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L54)(`Authentication authentication`) | `String` | Lấy và đảm bảo tồn tại người dùng ID. |
| [`CurrentUserService.hasAuthority`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L62)(`String authority`) | `boolean` | Kiểm tra có quyền. |
| [`CurrentUserService.isCurrentUserOwner`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L72)(`String ownerUserId`) | `boolean` | Kiểm tra điều kiện người dùng hiện tại chủ sở hữu. |
| [`CurrentUserService.requireUser`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L76)(`Authentication authentication`) | `User` | Lấy và đảm bảo tồn tại người dùng. |
| [`CurrentUserService.findByUsername`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L85)(`Authentication authentication`) | `User` | Tìm theo tên đăng nhập. |
| [`CurrentUserService.requireAuthentication`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L90)() | `Authentication` | Lấy và đảm bảo tồn tại authentication. |
| [`CurrentUserService.currentAuthentication`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L95)() | `Optional<Authentication>` | Thực hiện authentication. |
| [`CurrentUserService.requireAuthenticated`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L100)(`Authentication authentication`) | `Authentication` | Lấy và đảm bảo tồn tại authenticated. |
| [`CurrentUserService.isAuthenticated`](../src/main/java/com/example/workflow/service/CurrentUserService.java#L107)(`Authentication authentication`) | `boolean` | Kiểm tra điều kiện authenticated. |

### [src/main/java/com/example/workflow/service/CustomAgreementTimeoutService.java:36](../src/main/java/com/example/workflow/service/CustomAgreementTimeoutService.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomAgreementTimeoutService.cancelExpiredCustomOrders`](../src/main/java/com/example/workflow/service/CustomAgreementTimeoutService.java#L36)() | `void` | Hủy hết hạn custom đơn hàng. |
| [`CustomAgreementTimeoutService.cancelIfStillExpired`](../src/main/java/com/example/workflow/service/CustomAgreementTimeoutService.java#L46)(`Long orderId`) | `boolean` | Hủy if still hết hạn. |

### [src/main/java/com/example/workflow/service/CustomRequestService.java:68](../src/main/java/com/example/workflow/service/CustomRequestService.java#L68)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestService.create`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L68)(`CreateCustomRequest request, String idempotencyKey`) | `CustomRequestDTO` | Tạo custom yêu cầu. |
| [`CustomRequestService.list`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L90)(`CustomRequestStatus status, Pageable pageable`) | `Page<CustomRequestDTO>` | Liệt kê custom yêu cầu. |
| [`CustomRequestService.get`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L100)(`Long requestId`) | `CustomRequestDTO` | Lấy custom yêu cầu. |
| [`CustomRequestService.update`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L105)(`Long requestId, UpdateCustomRequest request`) | `CustomRequestDTO` | Cập nhật custom yêu cầu. |
| [`CustomRequestService.delete`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L121)(`Long requestId, Long expectedVersion, String idempotencyKey`) | `void` | Xóa custom yêu cầu. |
| [`CustomRequestService.submit`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L141)(`Long requestId, SubmitCustomRequest request, String idempotencyKey`) | `CustomRequestSubmissionDTO` | Gửi custom yêu cầu. |
| [`CustomRequestService.createDraft`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L164)(`String ownerId, CreateCustomRequest request`) | `CustomRequestDTO` | Tạo bản nháp. |
| [`CustomRequestService.submitDraft`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L175)(`Long requestId, String ownerId, SubmitCustomRequest request`) | `CustomRequestSubmissionDTO` | Gửi bản nháp. |
| [`CustomRequestService.resolveContact`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L199)(`User owner, SubmitCustomRequest request`) | `OrderContactSnapshot` | Xác định contact. |
| [`CustomRequestService.submissionResponse`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L213)(`Long draftId, Order order`) | `CustomRequestSubmissionDTO` | Thực hiện phản hồi. |
| [`CustomRequestService.validateForSubmission`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L217)(`CustomRequest draft`) | `void` | Kiểm tra hợp lệ cho submission. |
| [`CustomRequestService.applyProductType`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L227)(`CustomRequest draft, Long typeId, Map<String, Object> values`) | `void` | Áp dụng sản phẩm loại. |
| [`CustomRequestService.validateFieldValues`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L244)(`List<ProductTypeFieldDTO> fields, Map<String, Object> values`) | `void` | Kiểm tra hợp lệ trường values. |
| [`CustomRequestService.readFieldValues`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L266)(`String json`) | `Map<String, Object>` | Đọc trường values. |
| [`CustomRequestService.readProductTypeFields`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L272)(`String json`) | `List<ProductTypeFieldDTO>` | Đọc sản phẩm loại trường. |
| [`CustomRequestService.requireActiveOwnerId`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L277)() | `String` | Lấy và đảm bảo tồn tại đang hoạt động chủ sở hữu ID. |
| [`CustomRequestService.findOwned`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L283)(`Long requestId, String ownerId`) | `CustomRequest` | Tìm owned. |
| [`CustomRequestService.lockOwned`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L288)(`Long requestId, String ownerId`) | `CustomRequest` | Khóa owned. |
| [`CustomRequestService.ensureEditable`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L293)(`CustomRequest draft`) | `void` | Thực hiện editable. |
| [`CustomRequestService.ensureVersion`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L299)(`CustomRequest draft, Long expectedVersion`) | `void` | Thực hiện phiên bản. |
| [`CustomRequestService.boundedPage`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L305)(`Pageable pageable`) | `Pageable` | Thực hiện trang. |
| [`CustomRequestService.requireCreateRequest`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L309)(`CreateCustomRequest request`) | `void` | Lấy và đảm bảo tồn tại create yêu cầu. |
| [`CustomRequestService.normalizeDescription`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L315)(`String value`) | `String` | Chuẩn hóa description. |
| [`CustomRequestService.normalizeImageUrls`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L327)(`List<String> imageUrls`) | `List<String>` | Chuẩn hóa ảnh URL. |
| [`CustomRequestService.requirePositive`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L351)(`Integer value`) | `Integer` | Lấy và đảm bảo tồn tại positive. |
| [`CustomRequestService.firstPresent`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L358)(`String preferred, String fallback`) | `String` | Thực hiện present. |
| [`CustomRequestService.invalid`](../src/main/java/com/example/workflow/service/CustomRequestService.java#L362)(`String detail`) | `AppException` | Thực hiện custom yêu cầu. |

### [src/main/java/com/example/workflow/service/DashboardService.java:18](../src/main/java/com/example/workflow/service/DashboardService.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DashboardService.getDashboardStats`](../src/main/java/com/example/workflow/service/DashboardService.java#L18)() | `DashboardStatsDTO` | Lấy dashboard stats. |

### [src/main/java/com/example/workflow/service/EmailService.java:31](../src/main/java/com/example/workflow/service/EmailService.java#L31)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`EmailService.sendOrderConfirmationEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L31)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod`) | `void` | Gửi đơn hàng confirmation email. |
| [`EmailService.sendGuestOrderConfirmationEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L38)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod, String orderAccessUrl, Integer productionDurationDays`) | `void` | Gửi đơn hàng của khách confirmation email. |
| [`EmailService.sendCustomOrderConfirmationEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L56)(`String toEmail, String customerName, Long orderId, String customSpec, Integer quantity, String orderAccessUrl`) | `void` | Gửi custom đơn hàng confirmation email. |
| [`EmailService.sendOrderAcceptedEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L73)(`String toEmail, String customerName, Long orderId`) | `void` | Gửi đơn hàng đã chấp nhận email. |
| [`EmailService.sendOrderAcceptedEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L77)(`String toEmail, String customerName, Long orderId, String orderAccessUrl, String productSummary, BigDecimal finalPrice, Integer quantity, Integer productionDurationDays`) | `void` | Gửi đơn hàng đã chấp nhận email. |
| [`EmailService.sendProductionStartedEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L122)(`String toEmail, String customerName, Long orderId, String orderAccessUrl`) | `void` | Gửi sản xuất started email. |
| [`EmailService.sendInitialShapeCheckpointEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L151)(`String toEmail, String customerName, Long orderId, Long orderItemId, java.util.List<String> imageUrls, String note, String orderAccessUrl`) | `void` | Gửi tạo hình ban đầu checkpoint email. |
| [`EmailService.sendReadyToShipEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L197)(`String toEmail, String customerName, Long orderId, String orderAccessUrl`) | `void` | Gửi sẵn sàng tới ship email. |
| [`EmailService.sendOrderConfirmationEmailNow`](../src/main/java/com/example/workflow/service/EmailService.java#L226)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod`) | `void` | Gửi đơn hàng confirmation email now. |
| [`EmailService.sendOrderConfirmationEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L234)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod`) | `void` | Gửi đơn hàng confirmation email now hoặc ném lỗi. |
| [`EmailService.sendOrderConfirmationEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L238)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod, String orderAccessUrl, Integer productionDurationDays`) | `void` | Gửi đơn hàng confirmation email now hoặc ném lỗi. |
| [`EmailService.sendOrderConfirmationEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L253)(`String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod, String orderAccessUrl, Integer productionDurationDays, String customSpec, Integer quantity`) | `void` | Gửi đơn hàng confirmation email now hoặc ném lỗi. |
| [`EmailService.sendOrderCancellationEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L279)(`String toEmail, String customerName, Long orderId, String reason`) | `void` | Gửi đơn hàng cancellation email. |
| [`EmailService.sendOrderCancellationEmailNow`](../src/main/java/com/example/workflow/service/EmailService.java#L286)(`String toEmail, String customerName, Long orderId, String reason`) | `void` | Gửi đơn hàng cancellation email now. |
| [`EmailService.sendOrderCancellationEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L294)(`String toEmail, String customerName, Long orderId, String reason`) | `void` | Gửi đơn hàng cancellation email now hoặc ném lỗi. |
| [`EmailService.sendPasswordResetEmail`](../src/main/java/com/example/workflow/service/EmailService.java#L305)(`String toEmail, String customerName, String resetLink, int expiresInMinutes`) | `void` | Gửi mật khẩu reset email. |
| [`EmailService.sendPasswordResetEmailNow`](../src/main/java/com/example/workflow/service/EmailService.java#L312)(`String toEmail, String customerName, String resetLink, int expiresInMinutes`) | `void` | Gửi mật khẩu reset email now. |
| [`EmailService.sendPasswordResetEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L320)(`String toEmail, String customerName, String resetLink, int expiresInMinutes`) | `void` | Gửi mật khẩu reset email now hoặc ném lỗi. |
| [`EmailService.sendWorkflowEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L329)(`String toEmail, String subject, String htmlContent`) | `void` | Gửi workflow email now hoặc ném lỗi. |
| [`EmailService.sendGuestWorkflowEmailNowOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L334)(`String emailType, String toEmail, String customerName, Long orderId`) | `void` | Gửi khách workflow email now hoặc ném lỗi. |
| [`EmailService.sendHtmlEmailOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L364)(`String toEmail, String subject, String htmlContent`) | `void` | Gửi html email hoặc ném lỗi. |
| [`EmailService.sendHtmlEmailOrThrow`](../src/main/java/com/example/workflow/service/EmailService.java#L368)(`String[] toEmails, String subject, String htmlContent`) | `void` | Gửi html email hoặc ném lỗi. |
| [`EmailService.buildPasswordResetHtml`](../src/main/java/com/example/workflow/service/EmailService.java#L382)(`String customerName, String resetLink, int expiresInMinutes`) | `String` | Tạo mật khẩu reset html. |
| [`EmailService.escapeHtml`](../src/main/java/com/example/workflow/service/EmailService.java#L405)(`String value`) | `String` | Thực hiện html. |

### [src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java:18](../src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestPurchaseWorkflowStateService.markStarted`](../src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java#L18)(`Long orderId, String processInstanceId`) | `void` | Đánh dấu started. |
| [`GuestPurchaseWorkflowStateService.markStartFailed`](../src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java#L27)(`Long orderId, String errorMessage`) | `void` | Đánh dấu start thất bại. |
| [`GuestPurchaseWorkflowStateService.getGuestOrderForUpdate`](../src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java#L36)(`Long orderId`) | `Order` | Lấy đơn hàng của khách cho update. |
| [`GuestPurchaseWorkflowStateService.truncate`](../src/main/java/com/example/workflow/service/GuestPurchaseWorkflowStateService.java#L44)(`String value, int maxLength`) | `String` | Thực hiện khách purchase workflow trạng thái. |

### [src/main/java/com/example/workflow/service/KeycloakIdentityService.java:48](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L48)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`KeycloakIdentityService.authenticate`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L48)(`String username, String password`) | `String` | Xác thực keycloak identity. |
| [`KeycloakIdentityService.createUser`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L62)(`UserCreDTO user, Role role`) | `String` | Tạo người dùng. |
| [`KeycloakIdentityService.updateUser`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L99)(`String userId, UserCreDTO user, Role role`) | `void` | Cập nhật người dùng. |
| [`KeycloakIdentityService.updateUserProfile`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L105)(`String userId, String username, UserProfileUpdateDTO user`) | `void` | Cập nhật người dùng hồ sơ. |
| [`KeycloakIdentityService.resetPassword`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L110)(`String userId, String newPassword, boolean temporary`) | `void` | Thực hiện mật khẩu. |
| [`KeycloakIdentityService.updateProfile`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L120)(`String adminToken, String userId, String username, String firstname, String lastname, String email`) | `void` | Cập nhật hồ sơ. |
| [`KeycloakIdentityService.disableUser`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L136)(`String userId`) | `void` | Thực hiện người dùng. |
| [`KeycloakIdentityService.deleteUserById`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L141)(`String userId`) | `void` | Xóa người dùng theo ID. |
| [`KeycloakIdentityService.assignRealmRole`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L153)(`String adminToken, String userId, String roleName`) | `void` | Phân công realm vai trò. |
| [`KeycloakIdentityService.synchronizeRealmRole`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L168)(`String adminToken, String userId, Role desiredRole`) | `void` | Thực hiện realm vai trò. |
| [`KeycloakIdentityService.isApplicationRole`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L210)(`String roleName`) | `boolean` | Kiểm tra điều kiện application vai trò. |
| [`KeycloakIdentityService.getAdminToken`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L219)() | `String` | Lấy quản trị viên token. |
| [`KeycloakIdentityService.requestToken`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L231)(`String tokenRealm, String tokenClientId, String username, String password`) | `Map<String, Object>` | Thực hiện token. |
| [`KeycloakIdentityService.exchangeAdmin`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L247)(`String url, HttpMethod method, String token, Object body, Class<T> responseType`) | `ResponseEntity<T>` | Thực hiện quản trị viên. |
| [`KeycloakIdentityService.exchangeAdmin`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L257)(`String url, HttpMethod method, String token, Object body, ParameterizedTypeReference<T> responseType`) | `ResponseEntity<T>` | Thực hiện quản trị viên. |
| [`KeycloakIdentityService.adminEntity`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L267)(`String token, Object body`) | `HttpEntity<Object>` | Thực hiện entity. |
| [`KeycloakIdentityService.adminUrl`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L274)(`String path`) | `String` | Thực hiện URL. |
| [`KeycloakIdentityService.extractCreatedId`](../src/main/java/com/example/workflow/service/KeycloakIdentityService.java#L278)(`URI location`) | `String` | Thực hiện đã tạo ID. |

### [src/main/java/com/example/workflow/service/ManagerOrderReviewService.java:69](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L69)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerOrderReviewService.listPending`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L69)(`Pageable pageable`) | `Page<ManagerOrderReviewSummaryDTO>` | Liệt kê đang chờ. |
| [`ManagerOrderReviewService.getReviewDetail`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L82)(`Long orderId`) | `ManagerOrderReviewDetailDTO` | Lấy duyệt/đánh giá chi tiết. |
| [`ManagerOrderReviewService.review`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L97)(`Long orderId, ManagerReviewRequest request, String idempotencyKey`) | `ManagerReviewResultDTO` | Duyệt/đánh giá quản lý đơn hàng duyệt/đánh giá. |
| [`ManagerOrderReviewService.reviewWithinTransaction`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L131)(`Long orderId, ManagerReviewRequest request, User manager, String idempotencyKey`) | `ManagerReviewResultDTO` | Duyệt/đánh giá trong giao dịch. |
| [`ManagerOrderReviewService.approve`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L147)(`Order order, User manager, String requestedStaffId`) | `ManagerReviewResultDTO` | Thực hiện quản lý đơn hàng duyệt/đánh giá. |
| [`ManagerOrderReviewService.reject`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L200)(`Order order, User manager, String reason, String idempotencyKey`) | `ManagerReviewResultDTO` | Thực hiện quản lý đơn hàng duyệt/đánh giá. |
| [`ManagerOrderReviewService.result`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L232)(`Order order, ManagerReviewDecision decision, User manager, LocalDateTime reviewedAt, String rejectionReason, OrderAssignmentDTO assignment`) | `ManagerReviewResultDTO` | Thực hiện quản lý đơn hàng duyệt/đánh giá. |
| [`ManagerOrderReviewService.requireManager`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L254)() | `User` | Lấy và đảm bảo tồn tại quản lý. |
| [`ManagerOrderReviewService.ensurePendingReview`](../src/main/java/com/example/workflow/service/ManagerOrderReviewService.java#L265)(`Order order, Long expectedVersion`) | `void` | Thực hiện đang chờ duyệt. |

### [src/main/java/com/example/workflow/service/NotificationService.java:44](../src/main/java/com/example/workflow/service/NotificationService.java#L44)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`NotificationService.sendNotification`](../src/main/java/com/example/workflow/service/NotificationService.java#L44)(`String title, String content, Long orderId, String targetUserId, String destination`) | `void` | Gửi thông báo. |
| [`NotificationService.sendNotificationNow`](../src/main/java/com/example/workflow/service/NotificationService.java#L57)(`String title, String content, Long orderId, String targetUserId, String destination`) | `Notification` | Gửi thông báo now. |
| [`NotificationService..afterCommit`](../src/main/java/com/example/workflow/service/NotificationService.java#L74)() | `void` | Thực hiện commit. |
| [`NotificationService.publishRealtimeNotification`](../src/main/java/com/example/workflow/service/NotificationService.java#L82)(`String destination, Notification notification`) | `void` | Phát realtime thông báo. |
| [`NotificationService.getCurrentUserNotifications`](../src/main/java/com/example/workflow/service/NotificationService.java#L95)(`Pageable pageable`) | `Page<NotificationDTO>` | Lấy người dùng hiện tại thông báo. |
| [`NotificationService.getAdminNotifications`](../src/main/java/com/example/workflow/service/NotificationService.java#L101)(`Pageable pageable`) | `Page<NotificationDTO>` | Lấy quản trị viên thông báo. |
| [`NotificationService.getCurrentUserUnreadCount`](../src/main/java/com/example/workflow/service/NotificationService.java#L115)() | `long` | Lấy người dùng hiện tại unread số lượng. |
| [`NotificationService.getAdminUnreadCount`](../src/main/java/com/example/workflow/service/NotificationService.java#L119)() | `long` | Lấy quản trị viên unread số lượng. |
| [`NotificationService.markCurrentUserNotificationsAsRead`](../src/main/java/com/example/workflow/service/NotificationService.java#L124)() | `void` | Đánh dấu người dùng hiện tại thông báo as đọc. |
| [`NotificationService.markAdminNotificationsAsRead`](../src/main/java/com/example/workflow/service/NotificationService.java#L132)() | `void` | Đánh dấu quản trị viên thông báo as đọc. |
| [`NotificationService.requireAdminOrManager`](../src/main/java/com/example/workflow/service/NotificationService.java#L156)() | `User` | Lấy và đảm bảo tồn tại quản trị viên hoặc quản lý. |
| [`NotificationService.getReadMap`](../src/main/java/com/example/workflow/service/NotificationService.java#L164)(`String userId, List<Notification> notifications`) | `Map<Long, NotificationRead>` | Lấy đọc map. |
| [`NotificationService.isAdminNotificationRead`](../src/main/java/com/example/workflow/service/NotificationService.java#L177)(`Notification notification, Map<Long, NotificationRead> readByNotificationId`) | `boolean` | Kiểm tra điều kiện quản trị viên thông báo đọc. |

### [src/main/java/com/example/workflow/service/OrderAgreementService.java:63](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L63)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAgreementService.getForCurrentParticipant`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L63)(`Long orderId`) | `OrderAgreementDTO` | Lấy cho hiện tại participant. |
| [`OrderAgreementService.submit`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L72)(`Long orderId, SubmitOrderAgreementRequest request, String idempotencyKey`) | `OrderAgreementDTO` | Gửi đơn hàng thỏa thuận. |
| [`OrderAgreementService.decide`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L84)(`Long orderId, OrderAgreementDecisionRequest request, String idempotencyKey`) | `OrderAgreementDTO` | Ra quyết định đơn hàng thỏa thuận. |
| [`OrderAgreementService.submitWithinTransaction`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L96)(`Long orderId, SubmitOrderAgreementRequest request, String userId`) | `OrderAgreementDTO` | Gửi trong giao dịch. |
| [`OrderAgreementService.decideWithinTransaction`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L155)(`Long orderId, OrderAgreementDecisionRequest request, String staffId`) | `OrderAgreementDTO` | Ra quyết định trong giao dịch. |
| [`OrderAgreementService.customSnapshot`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L215)(`Order order, SubmitOrderAgreementRequest request, String userId`) | `SnapshotAndTotals` | Thực hiện snapshot. |
| [`OrderAgreementService.catalogSnapshot`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L242)(`Order order, SubmitOrderAgreementRequest request`) | `SnapshotAndTotals` | Thực hiện snapshot. |
| [`OrderAgreementService.applyOfficialAgreement`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L276)(`Order order, OrderAgreement agreement`) | `void` | Áp dụng official thỏa thuận. |
| [`OrderAgreementService.authorizeParticipant`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L312)(`Order order, String actorId`) | `void` | Kiểm tra quyền truy cập participant. |
| [`OrderAgreementService.requireOwner`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L319)(`Order order, String userId`) | `void` | Lấy và đảm bảo tồn tại chủ sở hữu. |
| [`OrderAgreementService.requireActiveAssignment`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L325)(`Order order, String expectedStaffId`) | `void` | Lấy và đảm bảo tồn tại phân công đang hoạt động. |
| [`OrderAgreementService.requireVersion`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L334)(`Order order, Long expectedVersion`) | `void` | Lấy và đảm bảo tồn tại phiên bản. |
| [`OrderAgreementService.requireOrder`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L340)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`OrderAgreementService.toDto`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L345)(`Order order, OrderAgreement agreement`) | `OrderAgreementDTO` | Chuyển thành dto. |
| [`OrderAgreementService.emptyDto`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L355)(`Order order`) | `OrderAgreementDTO` | Thực hiện dto. |
| [`OrderAgreementService.notifyOwner`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L360)(`Order order, String title, String content`) | `void` | Thông báo chủ sở hữu. |
| [`OrderAgreementService.assignedStaffId`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L365)(`Order order`) | `String` | Thực hiện nhân viên ID. |
| [`OrderAgreementService.safeItems`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L370)(`Order order`) | `List<OrderItem>` | Thực hiện mặt hàng. |
| [`OrderAgreementService.normalizedDescription`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L374)(`String value`) | `String` | Thực hiện description. |
| [`OrderAgreementService.validateImageUrls`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L381)(`List<String> values`) | `List<String>` | Kiểm tra hợp lệ ảnh URL. |
| [`OrderAgreementService.requireCustomPaymentMethod`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L396)(`Order order, PaymentMethod paymentMethod`) | `PaymentMethod` | Lấy và đảm bảo tồn tại custom thanh toán method. |
| [`OrderAgreementService.normalizedReason`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L407)(`String value`) | `String` | Thực hiện lý do. |
| [`OrderAgreementService.normalizedOptional`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L412)(`String value`) | `String` | Thực hiện tùy chọn. |
| [`OrderAgreementService.nonNegative`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L416)(`BigDecimal value, String message`) | `BigDecimal` | Thực hiện negative. |
| [`OrderAgreementService.floorZero`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L421)(`BigDecimal value`) | `BigDecimal` | Thực hiện zero. |
| [`OrderAgreementService.badRequest`](../src/main/java/com/example/workflow/service/OrderAgreementService.java#L425)(`String message`) | `AppException` | Thực hiện yêu cầu. |

### [src/main/java/com/example/workflow/service/OrderAssignmentService.java:64](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L64)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignmentService.listAvailableStaff`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L64)(`Pageable pageable`) | `Page<AvailableStaffDTO>` | Liệt kê khả dụng nhân viên. |
| [`OrderAssignmentService.listClaimable`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L71)(`Pageable pageable`) | `Page<OrderAssignmentQueueDTO>` | Liệt kê claimable. |
| [`OrderAssignmentService.listAssignable`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L81)(`Pageable pageable`) | `Page<OrderAssignmentQueueDTO>` | Liệt kê assignable. |
| [`OrderAssignmentService.assignmentQueue`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L91)(`Pageable pageable`) | `Page<OrderAssignmentQueueDTO>` | Thực hiện queue. |
| [`OrderAssignmentService.assignByManager`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L98)(`Long orderId, AssignOrderRequest request, String idempotencyKey`) | `OrderAssignmentResultDTO` | Phân công theo quản lý. |
| [`OrderAssignmentService.claim`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L126)(`Long orderId, ClaimOrderRequest request, String idempotencyKey`) | `OrderAssignmentResultDTO` | Nhận xử lý đơn hàng phân công. |
| [`OrderAssignmentService.assignApprovedOrderWithinTransaction`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L159)(`Long orderId, Long expectedVersion, String staffId, String managerId`) | `OrderAssignmentResultDTO` | Phân công approved đơn hàng trong giao dịch. |
| [`OrderAssignmentService.assignWithinTransaction`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L176)(`Long orderId, Long expectedVersion, String staffId, AssignmentSource source, String actorId, boolean managerReview`) | `OrderAssignmentResultDTO` | Phân công trong giao dịch. |
| [`OrderAssignmentService.assignmentPayload`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L265)(`Long orderId, Long orderVersion, String staffId`) | `Map<String, Object>` | Thực hiện payload. |
| [`OrderAssignmentService.requireAssignRequest`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L273)(`AssignOrderRequest request`) | `void` | Lấy và đảm bảo tồn tại assign yêu cầu. |
| [`OrderAssignmentService.ensureVersion`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L284)(`Order order, Long expectedVersion`) | `void` | Thực hiện phiên bản. |
| [`OrderAssignmentService.requireAvailableStaff`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L290)(`User staff`) | `void` | Lấy và đảm bảo tồn tại khả dụng nhân viên. |
| [`OrderAssignmentService.requireActiveManager`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L296)() | `User` | Lấy và đảm bảo tồn tại đang hoạt động quản lý. |
| [`OrderAssignmentService.statusAfterAssignment`](../src/main/java/com/example/workflow/service/OrderAssignmentService.java#L307)(`Order order`) | `OrderStatus` | Thực hiện sau phân công. |

### [src/main/java/com/example/workflow/service/OrderCancellationService.java:63](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L63)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderCancellationService.cancelByCurrentUser`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L63)(`Long orderId, CancelOrderRequest request, String idempotencyKey`) | `OrderCancellationResultDTO` | Hủy theo người dùng hiện tại. |
| [`OrderCancellationService.getGuestCancellationView`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L82)(`Long orderId, String rawToken`) | `GuestOrderCancellationViewDTO` | Lấy khách cancellation dữ liệu hiển thị. |
| [`OrderCancellationService.cancelByGuest`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L101)(`Long orderId, String rawToken, CancelOrderRequest request, String idempotencyKey`) | `OrderCancellationResultDTO` | Hủy theo khách. |
| [`OrderCancellationService.cancelByManagerReject`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L122)(`Long orderId, Long expectedVersion, String reason, String managerId, String decisionReference, String idempotencyKey`) | `OrderCancellationResultDTO` | Hủy theo quản lý reject. |
| [`OrderCancellationService.cancelBySystem`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L148)(`Long orderId, Long expectedVersion, CancellationSource source, String reason, String sourceReference, String idempotencyKey`) | `OrderCancellationResultDTO` | Used by authenticated timers/workers/payment handlers, never bound directly to a public request. |
| [`OrderCancellationService.executeDurably`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L173)(`String scope, String idempotencyKey, CancellationCommand command`) | `OrderCancellationResultDTO` | Thực thi durably. |
| [`OrderCancellationService.cancelWithinTransaction`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L190)(`CancellationCommand command`) | `OrderCancellationResultDTO` | Hủy trong giao dịch. |
| [`OrderCancellationService.cancellationPenalty`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L245)(`BigDecimal finalPrice`) | `int` | Thực hiện penalty. |
| [`OrderCancellationService.lockReputationOwner`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L256)(`Order order, CancellationSource source, int penalty`) | `User` | Khóa reputation chủ sở hữu. |
| [`OrderCancellationService.releaseActiveAssignment`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L270)(`Long orderId`) | `boolean` | Thực hiện phân công đang hoạt động. |
| [`OrderCancellationService.authorizeLockedOrder`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L278)(`Order order, CancellationCommand command`) | `void` | Kiểm tra quyền truy cập locked đơn hàng. |
| [`OrderCancellationService.existingCancellation`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L293)(`Order order`) | `OrderCancellationResultDTO` | Thực hiện cancellation. |
| [`OrderCancellationService.requireOrder`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L300)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`OrderCancellationService.requireRequest`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L305)(`CancelOrderRequest request`) | `void` | Lấy và đảm bảo tồn tại yêu cầu. |
| [`OrderCancellationService.normalizedReason`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L311)(`String reason`) | `String` | Thực hiện lý do. |
| [`OrderCancellationService.requireReference`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L322)(`String reference`) | `String` | Lấy và đảm bảo tồn tại tài liệu tham khảo. |
| [`OrderCancellationService.requestReference`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L333)(`String prefix, Long orderId, String idempotencyKey`) | `String` | Thực hiện tài liệu tham khảo. |
| [`OrderCancellationService.assignedStaffId`](../src/main/java/com/example/workflow/service/OrderCancellationService.java#L338)(`Order order`) | `String` | Thực hiện nhân viên ID. |

### [src/main/java/com/example/workflow/service/OrderChatService.java:42](../src/main/java/com/example/workflow/service/OrderChatService.java#L42)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderChatService.ensureThreadForAssignedOrder`](../src/main/java/com/example/workflow/service/OrderChatService.java#L42)(`Long orderId`) | `OrderChatThreadDTO` | Called by the assignment event and safe to retry. Guest orders intentionally have no thread. |
| [`OrderChatService.getThread`](../src/main/java/com/example/workflow/service/OrderChatService.java#L70)(`Long orderId`) | `OrderChatThreadDTO` | Lấy luồng chat. |
| [`OrderChatService.getMessages`](../src/main/java/com/example/workflow/service/OrderChatService.java#L78)(`Long orderId`) | `List<OrderChatMessageDTO>` | Lấy tin nhắn. |
| [`OrderChatService.send`](../src/main/java/com/example/workflow/service/OrderChatService.java#L88)(`SendOrderChatMessageRequest request, Authentication authentication`) | `OrderChatMessageDTO` | Gửi đơn hàng chat. |
| [`OrderChatService.authorizeSubscription`](../src/main/java/com/example/workflow/service/OrderChatService.java#L112)(`Long orderId, Authentication authentication`) | `void` | Kiểm tra quyền truy cập subscription. |
| [`OrderChatService.authorize`](../src/main/java/com/example/workflow/service/OrderChatService.java#L117)(`OrderChatThread thread, String actorId`) | `void` | Kiểm tra quyền truy cập đơn hàng chat. |
| [`OrderChatService.requireThread`](../src/main/java/com/example/workflow/service/OrderChatService.java#L123)(`Long orderId`) | `OrderChatThread` | Lấy và đảm bảo tồn tại luồng chat. |
| [`OrderChatService.requireOrder`](../src/main/java/com/example/workflow/service/OrderChatService.java#L128)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`OrderChatService.assignedStaffId`](../src/main/java/com/example/workflow/service/OrderChatService.java#L133)(`Order order`) | `String` | Thực hiện nhân viên ID. |
| [`OrderChatService.assignedStaff`](../src/main/java/com/example/workflow/service/OrderChatService.java#L138)(`Order order`) | `User` | Thực hiện nhân viên. |
| [`OrderChatService.displayName`](../src/main/java/com/example/workflow/service/OrderChatService.java#L142)(`User user`) | `String` | Thực hiện tên. |
| [`OrderChatService.backfillParticipantNames`](../src/main/java/com/example/workflow/service/OrderChatService.java#L149)(`OrderChatThread thread, Order order`) | `void` | Thực hiện participant names. |
| [`OrderChatService.toDto`](../src/main/java/com/example/workflow/service/OrderChatService.java#L164)(`OrderChatThread thread`) | `OrderChatThreadDTO` | Chuyển thành dto. |
| [`OrderChatService.toMessageDto`](../src/main/java/com/example/workflow/service/OrderChatService.java#L169)(`OrderChatThread thread, ChatMessage message`) | `OrderChatMessageDTO` | Chuyển thành tin nhắn dto. |
| [`OrderChatService.logSendLatency`](../src/main/java/com/example/workflow/service/OrderChatService.java#L181)(`Long orderId, long startedAt, long authorizedAt, long persistedAt, long publishedAt`) | `void` | Thực hiện send latency. |
| [`OrderChatService.elapsedMillis`](../src/main/java/com/example/workflow/service/OrderChatService.java#L195)(`long start, long end`) | `long` | Thực hiện millis. |

### [src/main/java/com/example/workflow/service/OrderDeliveryService.java:76](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L76)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderDeliveryService.handover`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L76)(`Long orderId, ShippingHandoverRequest request, String idempotencyKey`) | `ShippingHandoverResponse` | Thực hiện đơn hàng giao hàng. |
| [`OrderDeliveryService.getUserReceiptView`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L106)(`Long orderId`) | `ReceiptViewDTO` | Lấy người dùng biên nhận dữ liệu hiển thị. |
| [`OrderDeliveryService.getGuestReceiptView`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L115)(`Long orderId, String rawToken`) | `ReceiptViewDTO` | Lấy khách biên nhận dữ liệu hiển thị. |
| [`OrderDeliveryService.confirmByUser`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L124)(`Long orderId, ReceiptConfirmRequest request, String idempotencyKey`) | `ReceiptConfirmResponse` | Thực hiện theo người dùng. |
| [`OrderDeliveryService.confirmByGuest`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L143)(`Long orderId, String rawToken, ReceiptConfirmRequest request, String idempotencyKey`) | `ReceiptConfirmResponse` | Thực hiện theo khách. |
| [`OrderDeliveryService.executeReceipt`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L166)(`String scope, String idempotencyKey, Long orderId, ReceiptConfirmRequest request, String ownerId, boolean guest, String rawToken`) | `ReceiptConfirmResponse` | Thực thi biên nhận. |
| [`OrderDeliveryService.handoverWithinTransaction`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L198)(`Long orderId, ShippingHandoverRequest request, User staff`) | `ShippingHandoverResponse` | Thực hiện trong giao dịch. |
| [`OrderDeliveryService.confirmWithinTransaction`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L229)(`Long orderId, ReceiptConfirmRequest request, String ownerId, boolean guest, String rawToken`) | `ReceiptConfirmResponse` | Thực hiện trong giao dịch. |
| [`OrderDeliveryService.validateCompleteHandoverItems`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L293)(`Order order, List<ShippingHandoverItemRequest> requestedItems`) | `Map<Long, Integer>` | Kiểm tra hợp lệ hoàn tất handover mặt hàng. |
| [`OrderDeliveryService.validateCompleteReceiptItems`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L318)(`Order order, List<ItemCheckRequest> requestedItems`) | `Map<Long, Integer>` | Kiểm tra hợp lệ hoàn tất biên nhận mặt hàng. |
| [`OrderDeliveryService.buildMismatches`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L341)(`Order order, Map<Long, Integer> received`) | `List<ReceiptMismatchDTO>` | Tạo mismatches. |
| [`OrderDeliveryService.receiptSnapshot`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L357)(`Order order, Map<Long, Integer> received, boolean matched, ReceiptConfirmRequest request`) | `String` | Thực hiện snapshot. |
| [`OrderDeliveryService.effectiveExportedQuantity`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L378)(`Order order, OrderItem item`) | `int` | Thực hiện exported số lượng. |
| [`OrderDeliveryService.toReceiptView`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L384)(`Order order`) | `ReceiptViewDTO` | Chuyển thành biên nhận dữ liệu hiển thị. |
| [`OrderDeliveryService.toHandoverResponse`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L407)(`Order order`) | `ShippingHandoverResponse` | Chuyển thành handover phản hồi. |
| [`OrderDeliveryService.expectedDeliveryAt`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L418)(`Order order`) | `LocalDateTime` | Thực hiện giao hàng tại. |
| [`OrderDeliveryService.itemMap`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L422)(`Order order`) | `Map<Long, OrderItem>` | Thực hiện map. |
| [`OrderDeliveryService.requireHistoricalStaff`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L431)(`Order order, String staffId`) | `void` | Lấy và đảm bảo tồn tại historical nhân viên. |
| [`OrderDeliveryService.requireHandoverRequest`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L443)(`ShippingHandoverRequest request`) | `void` | Lấy và đảm bảo tồn tại handover yêu cầu. |
| [`OrderDeliveryService.requireReceiptRequest`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L449)(`ReceiptConfirmRequest request`) | `void` | Lấy và đảm bảo tồn tại biên nhận yêu cầu. |
| [`OrderDeliveryService.normalizeOptionalNote`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L456)(`String note`) | `String` | Chuẩn hóa tùy chọn note. |
| [`OrderDeliveryService.requireActive`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L463)(`User actor, ConstantErrorCode errorCode`) | `void` | Lấy và đảm bảo tồn tại đang hoạt động. |
| [`OrderDeliveryService.requireOwner`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L469)(`Order order, String ownerId`) | `void` | Lấy và đảm bảo tồn tại chủ sở hữu. |
| [`OrderDeliveryService.requireOrderVersion`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L475)(`Order order, Long expectedVersion`) | `void` | Lấy và đảm bảo tồn tại đơn hàng phiên bản. |
| [`OrderDeliveryService.lockOrder`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L481)(`Long orderId`) | `Order` | Khóa đơn hàng. |
| [`OrderDeliveryService.requireOrder`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L486)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`OrderDeliveryService.safeItems`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L491)(`Order order`) | `List<OrderItem>` | Thực hiện mặt hàng. |
| [`OrderDeliveryService.invalidHandover`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L495)(`String detail`) | `AppException` | Thực hiện handover. |
| [`OrderDeliveryService.invalidReceipt`](../src/main/java/com/example/workflow/service/OrderDeliveryService.java#L499)(`String detail`) | `AppException` | Thực hiện biên nhận. |

### [src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java:63](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L63)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLifecycleEventHandler.handleChangeRequestSubmitted`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L63)(`ChangeRequestSubmittedEvent event`) | `void` | Xử lý change yêu cầu đã gửi. |
| [`OrderLifecycleEventHandler.handleChangeRequestDecided`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L67)(`ChangeRequestDecidedEvent event`) | `void` | Xử lý change yêu cầu decided. |
| [`OrderLifecycleEventHandler.handleOrderCreated`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L71)(`OrderCreatedEvent event`) | `void` | Xử lý đơn hàng đã tạo. |
| [`OrderLifecycleEventHandler.handleOrderAccepted`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L119)(`OrderAcceptedEvent event`) | `void` | Xử lý đơn hàng đã chấp nhận. |
| [`OrderLifecycleEventHandler.handleGuestOrderCreated`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L145)(`GuestOrderCreatedEvent event`) | `void` | Xử lý đơn hàng của khách đã tạo. |
| [`OrderLifecycleEventHandler.handleOrderAssigned`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L176)(`OrderAssignedEvent event`) | `void` | Xử lý đơn hàng được phân công. |
| [`OrderLifecycleEventHandler.handleOrderReviewed`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L193)(`OrderReviewedEvent event`) | `void` | Xử lý đơn hàng đã duyệt. |
| [`OrderLifecycleEventHandler.handleOrderAgreementSubmitted`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L215)(`OrderAgreementSubmittedEvent event`) | `void` | Xử lý đơn hàng thỏa thuận đã gửi. |
| [`OrderLifecycleEventHandler.handleOrderAgreementReviewed`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L236)(`OrderAgreementReviewedEvent event`) | `void` | Xử lý đơn hàng thỏa thuận đã duyệt. |
| [`OrderLifecycleEventHandler.handleOrderDelivered`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L251)(`OrderDeliveredEvent event`) | `void` | Xử lý đơn hàng delivered. |
| [`OrderLifecycleEventHandler.handleOrderShipping`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L271)(`OrderShippingEvent event`) | `void` | Xử lý đơn hàng vận chuyển. |
| [`OrderLifecycleEventHandler.handleOrderCancelled`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L281)(`OrderCancelledEvent event`) | `void` | Xử lý đơn hàng đã hủy. |
| [`OrderLifecycleEventHandler.handlePaymentConfirmed`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L325)(`PaymentConfirmedEvent event`) | `void` | Xử lý thanh toán confirmed. |
| [`OrderLifecycleEventHandler.handlePaymentOpened`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L342)(`PaymentOpenedEvent event`) | `void` | Xử lý thanh toán opened. |
| [`OrderLifecycleEventHandler.handleProductionStarted`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L351)(`ProductionStartedEvent event`) | `void` | Xử lý sản xuất started. |
| [`OrderLifecycleEventHandler.handleProductionCheckpointSubmitted`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L381)(`ProductionCheckpointSubmittedEvent event`) | `void` | Xử lý checkpoint sản xuất đã gửi. |
| [`OrderLifecycleEventHandler.handleProductionCheckpointReviewed`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L424)(`ProductionCheckpointReviewedEvent event`) | `void` | Xử lý checkpoint sản xuất đã duyệt. |
| [`OrderLifecycleEventHandler.handleOrderReadyToShip`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L445)(`OrderReadyToShipEvent event`) | `void` | Xử lý đơn hàng sẵn sàng tới ship. |
| [`OrderLifecycleEventHandler.requireOrder`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L471)(`Long orderId, String eventType`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng. |
| [`OrderLifecycleEventHandler.startLifecycle`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L479)(`Order order`) | `OrderWorkflowService.StartResult` | Bắt đầu lifecycle. |
| [`OrderLifecycleEventHandler.resolvePaymentMethod`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L492)(`Order order`) | `String` | Xác định thanh toán method. |
| [`OrderLifecycleEventHandler.requireWorkflowAdvance`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L498)(`Long orderId, boolean advanced, String eventType`) | `void` | Lấy và đảm bảo tồn tại workflow advance. |
| [`OrderLifecycleEventHandler.firstItem`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L506)(`Order order`) | `OrderItem` | Thực hiện mặt hàng. |
| [`OrderLifecycleEventHandler.customSpec`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L510)(`OrderItem item`) | `String` | Thực hiện đặc tả. |
| [`OrderLifecycleEventHandler.authenticatedOrderUrl`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L526)(`Long orderId`) | `String` | Thực hiện đơn hàng URL. |
| [`OrderLifecycleEventHandler.signedGuestOrderUrl`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L535)(`Order order`) | `String` | Thực hiện đơn hàng của khách URL. |
| [`OrderLifecycleEventHandler.catalogSummary`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L546)(`Order order`) | `String` | Thực hiện tóm tắt. |
| [`OrderLifecycleEventHandler.itemName`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L559)(`OrderItem item`) | `String` | Thực hiện tên. |
| [`OrderLifecycleEventHandler.totalQuantity`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L577)(`Order order`) | `Integer` | Thực hiện số lượng. |
| [`OrderLifecycleEventHandler.maximumDurationDays`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L583)(`Order order`) | `Integer` | Thực hiện thời lượng days. |
| [`OrderLifecycleEventHandler.paymentLabel`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L593)(`Order order`) | `String` | Thực hiện label. |
| [`OrderLifecycleEventHandler.assignedStaffId`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L607)(`Order order`) | `String` | Thực hiện nhân viên ID. |
| [`OrderLifecycleEventHandler.cancellationMessage`](../src/main/java/com/example/workflow/service/OrderLifecycleEventHandler.java#L614)(`Long orderId, String reason`) | `String` | Thực hiện tin nhắn. |

### [src/main/java/com/example/workflow/service/OrderLookupService.java:18](../src/main/java/com/example/workflow/service/OrderLookupService.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLookupService.require`](../src/main/java/com/example/workflow/service/OrderLookupService.java#L18)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại đơn hàng lookup. |
| [`OrderLookupService.requireForUpdate`](../src/main/java/com/example/workflow/service/OrderLookupService.java#L24)(`Long orderId`) | `Order` | Lấy và đảm bảo tồn tại cho update. |

### [src/main/java/com/example/workflow/service/OrderLookupTokenService.java:38](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L38)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLookupTokenService.issueFor`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L38)(`Order order`) | `String` | Issues a public token once and stores only its SHA-256 hash on the order. |
| [`OrderLookupTokenService.issueFor`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L51)(`Order order, Set<Scope> scopes, Duration validity`) | `String` | Caller must supply approved TTL policy; no silent business default. Reissue replaces the hash. |
| [`OrderLookupTokenService.revoke`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L60)(`Order order`) | `void` | Thu hồi đơn hàng lookup token. |
| [`OrderLookupTokenService.authorize`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L66)(`Order order, String token, Scope scope`) | `void` | Kiểm tra quyền truy cập đơn hàng lookup token. |
| [`OrderLookupTokenService.issueSignedReadToken`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L81)(`Order order, Duration validity`) | `String` | Creates a stateless, expiring READ-only token for milestone emails. It does not rotate or invalidate the guest token issued at checkout. |
| [`OrderLookupTokenService.matches`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L91)(`Order order, String rawToken`) | `boolean` | Thực hiện đơn hàng lookup token. |
| [`OrderLookupTokenService.maskEmail`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L102)(`String email`) | `String` | Che một phần email. |
| [`OrderLookupTokenService.hash`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L116)(`String rawToken`) | `String` | Thực hiện đơn hàng lookup token. |
| [`OrderLookupTokenService.matchesSignedReadToken`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L125)(`Order order, String token`) | `boolean` | Thực hiện token đọc có chữ ký. |
| [`OrderLookupTokenService.sign`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L147)(`Long orderId, long expiresAt`) | `String` | Thực hiện đơn hàng lookup token. |
| [`OrderLookupTokenService.requireSigningSecret`](../src/main/java/com/example/workflow/service/OrderLookupTokenService.java#L159)() | `void` | Lấy và đảm bảo tồn tại signing secret. |

### [src/main/java/com/example/workflow/service/OrderReorderService.java:48](../src/main/java/com/example/workflow/service/OrderReorderService.java#L48)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderReorderService.reorder`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L48)(`Long orderId, String idempotencyKey`) | `ReorderResponseDTO` | Thực hiện đơn hàng reorder. |
| [`OrderReorderService.reorderWithinTransaction`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L63)(`Long orderId, String ownerId`) | `ReorderResponseDTO` | Thực hiện trong giao dịch. |
| [`OrderReorderService.reorderCatalog`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L77)(`Order source, String ownerId`) | `ReorderResponseDTO` | Thực hiện catalog. |
| [`OrderReorderService.reorderCustom`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L102)(`Order source, String ownerId`) | `ReorderResponseDTO` | Thực hiện custom. |
| [`OrderReorderService.customSnapshot`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L120)(`Order source, OrderItem item`) | `CustomSnapshot` | Thực hiện snapshot. |
| [`OrderReorderService.validateCatalogItem`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L150)(`OrderItem item, ProductVariant variant`) | `void` | Kiểm tra hợp lệ catalog mặt hàng. |
| [`OrderReorderService.resolveOrderType`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L162)(`Order order`) | `OrderType` | Xác định đơn hàng loại. |
| [`OrderReorderService.text`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L168)(`JsonNode node`) | `String` | Thực hiện đơn hàng reorder. |
| [`OrderReorderService.firstPresent`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L172)(`String first, String second`) | `String` | Thực hiện present. |
| [`OrderReorderService.safeItems`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L176)(`Order order`) | `List<OrderItem>` | Thực hiện mặt hàng. |
| [`OrderReorderService.invalid`](../src/main/java/com/example/workflow/service/OrderReorderService.java#L180)(`String detail`) | `AppException` | Thực hiện đơn hàng reorder. |

### [src/main/java/com/example/workflow/service/OrderService.java:37](../src/main/java/com/example/workflow/service/OrderService.java#L37)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderService.assertCurrentUserCanViewOrder`](../src/main/java/com/example/workflow/service/OrderService.java#L37)(`Order order`) | `void` | Kiểm tra điều kiện và báo lỗi nếu không đạt người dùng hiện tại can dữ liệu hiển thị đơn hàng. |
| [`OrderService.getOrderById`](../src/main/java/com/example/workflow/service/OrderService.java#L45)(`Long id`) | `OrderDTO` | Lấy đơn hàng theo ID. |
| [`OrderService.getOrderHistory`](../src/main/java/com/example/workflow/service/OrderService.java#L52)(`Long orderId`) | `List<OrderStatusHistoryDTO>` | Lấy đơn hàng lịch sử. |
| [`OrderService.getMyOrders`](../src/main/java/com/example/workflow/service/OrderService.java#L60)(`BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `Page<OrderListDTO>` | Lấy của người dùng đơn hàng. |
| [`OrderService.getMyCancelledOrders`](../src/main/java/com/example/workflow/service/OrderService.java#L87)(`BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `Page<OrderListDTO>` | Lấy của người dùng đã hủy đơn hàng. |
| [`OrderService.getMyAssignedStaffOrders`](../src/main/java/com/example/workflow/service/OrderService.java#L104)(`Pageable pageable`) | `Page<OrderListDTO>` | Lấy của người dùng nhân viên được phân công đơn hàng. |
| [`OrderService.getGuestOrder`](../src/main/java/com/example/workflow/service/OrderService.java#L136)(`Long orderId, String token`) | `OrderDTO` | Lấy đơn hàng của khách. |
| [`OrderService.getGuestOrderHistory`](../src/main/java/com/example/workflow/service/OrderService.java#L143)(`Long orderId, String token`) | `List<OrderStatusHistoryDTO>` | Lấy đơn hàng của khách lịch sử. |

### [src/main/java/com/example/workflow/service/OrderStatusHistoryService.java:17](../src/main/java/com/example/workflow/service/OrderStatusHistoryService.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderStatusHistoryService.record`](../src/main/java/com/example/workflow/service/OrderStatusHistoryService.java#L17)(`Order order, OrderStatus oldStatus, OrderStatus newStatus, String actorId`) | `void` | Ghi nhận trạng thái đơn hàng lịch sử. |

### [src/main/java/com/example/workflow/service/OrderWorkflowService.java:45](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L45)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderWorkflowService.startAfterOrderCreated`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L45)(`Long orderId, boolean guestOrder, String orderType, String paymentMethod, List<Long> orderItemIds`) | `StartResult` | Bắt đầu sau đơn hàng đã tạo. |
| [`OrderWorkflowService.completeManagerReview`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L86)(`Long orderId, boolean approved, String managerId, LocalDateTime confirmationDueAt`) | `boolean` | Completes the manager task when an order already has an active process. Orders created before workflow cutover legitimately have no task and keep the database/outbox path as their source of truth. |
| [`OrderWorkflowService.advanceAfterAssignment`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L110)(`Long orderId, String staffId`) | `boolean` | Advances the currently deployed guest flow and also supports the message catch event used by the target workflow planned for the full BPMN cutover. |
| [`OrderWorkflowService.correlateOrderCancelled`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L146)(`Long orderId`) | `boolean` | Completes an explicit cancellation catch event after the database/outbox commit. A process without that subscription is left untouched for reconciliation; it is never deleted as a substitute for the audited Order cancellation. |
| [`OrderWorkflowService.correlateAgreementSubmitted`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L165)(`Long orderId, Integer agreementVersion, String paymentMethod`) | `boolean` | Hook for the WF03 interrupting wait-for-form scope; DB remains authoritative during staged cutover. |
| [`OrderWorkflowService.completeAgreementReview`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L179)(`Long orderId, Integer agreementVersion, String decision, String staffId`) | `boolean` | Completes the target WF03 staff task when that process version is deployed. |
| [`OrderWorkflowService.correlatePaymentOpened`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L190)(`Long orderId, Long attemptId, java.time.LocalDateTime dueAt`) | `boolean` | Đồng bộ với workflow BPMN thanh toán opened. |
| [`OrderWorkflowService.correlatePaymentSuccess`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L204)(`Long orderId`) | `boolean` | Đồng bộ với workflow BPMN thanh toán thành công. |
| [`OrderWorkflowService.completeProductionStart`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L214)(`Long orderId, String staffId, java.time.LocalDateTime startedAt`) | `boolean` | Hoàn tất sản xuất start. |
| [`OrderWorkflowService.completeProductionCheckpointSubmission`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L224)(`Long orderId, Long orderItemId, Long checkpointId, ProductionCheckpointStage stage, int attemptNumber`) | `boolean` | Hoàn tất checkpoint sản xuất submission. |
| [`OrderWorkflowService.completeProductionCheckpointReview`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L244)(`Long orderId, Long orderItemId, Long checkpointId, ProductionDecisionType decision`) | `boolean` | Hoàn tất checkpoint sản xuất duyệt/đánh giá. |
| [`OrderWorkflowService.completeProduction`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L260)(`Long orderId`) | `boolean` | Hoàn tất sản xuất. |
| [`OrderWorkflowService.correlateShipping`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L270)(`Long orderId, String staffId`) | `boolean` | Đồng bộ với workflow BPMN vận chuyển. |
| [`OrderWorkflowService.correlateDelivered`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L278)(`Long orderId`) | `boolean` | Đồng bộ với workflow BPMN delivered. |
| [`OrderWorkflowService.hasActiveProcess`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L282)(`Long orderId`) | `boolean` | Kiểm tra có đang hoạt động quy trình. |
| [`OrderWorkflowService.activeProcess`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L286)(`Long orderId`) | `ProcessInstance` | Thực hiện quy trình. |
| [`OrderWorkflowService.correlateMessage`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L297)(`Long orderId, String messageName, Map<String, Object> variables`) | `boolean` | Đồng bộ với workflow BPMN tin nhắn. |
| [`OrderWorkflowService.toDate`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L311)(`LocalDateTime value`) | `Date` | Chuyển thành ngày. |
| [`OrderWorkflowService.singleTask`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L318)(`Long orderId, String taskDefinitionKey`) | `Task` | Thực hiện tác vụ. |
| [`OrderWorkflowService.singleItemTask`](../src/main/java/com/example/workflow/service/OrderWorkflowService.java#L331)(`Long orderId, Long orderItemId, String taskDefinitionKey`) | `Task` | Thực hiện mặt hàng tác vụ. |

### [src/main/java/com/example/workflow/service/PasswordService.java:44](../src/main/java/com/example/workflow/service/PasswordService.java#L44)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PasswordService.requestPasswordReset`](../src/main/java/com/example/workflow/service/PasswordService.java#L44)(`ForgotPasswordRequest request`) | `void` | Thực hiện mật khẩu reset. |
| [`PasswordService.resetPassword`](../src/main/java/com/example/workflow/service/PasswordService.java#L80)(`ResetPasswordRequest request`) | `void` | Thực hiện mật khẩu. |
| [`PasswordService.changeCurrentUserPassword`](../src/main/java/com/example/workflow/service/PasswordService.java#L97)(`ChangePasswordRequest request`) | `void` | Thực hiện người dùng hiện tại mật khẩu. |
| [`PasswordService.findActiveUser`](../src/main/java/com/example/workflow/service/PasswordService.java#L109)(`String identifier`) | `Optional<User>` | Tìm đang hoạt động người dùng. |
| [`PasswordService.validatePasswordConfirmation`](../src/main/java/com/example/workflow/service/PasswordService.java#L117)(`String newPassword, String confirmPassword`) | `void` | Kiểm tra hợp lệ mật khẩu confirmation. |
| [`PasswordService.generateToken`](../src/main/java/com/example/workflow/service/PasswordService.java#L123)() | `String` | Thực hiện token. |
| [`PasswordService.hashToken`](../src/main/java/com/example/workflow/service/PasswordService.java#L129)(`String token`) | `String` | Thực hiện token. |
| [`PasswordService.buildResetLink`](../src/main/java/com/example/workflow/service/PasswordService.java#L141)(`String token`) | `String` | Tạo reset link. |
| [`PasswordService.normalizeIdentifier`](../src/main/java/com/example/workflow/service/PasswordService.java#L149)(`String identifier`) | `String` | Chuẩn hóa identifier. |

### [src/main/java/com/example/workflow/service/PaymentService.java:74](../src/main/java/com/example/workflow/service/PaymentService.java#L74)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentService.openPayment`](../src/main/java/com/example/workflow/service/PaymentService.java#L74)(`Long orderId, String idempotencyKey`) | `PaymentAttemptDTO` | Mở thanh toán. |
| [`PaymentService.getPaymentStatus`](../src/main/java/com/example/workflow/service/PaymentService.java#L93)(`Long orderId`) | `PaymentAttemptDTO` | Lấy thanh toán trạng thái. |
| [`PaymentService.openWithinTransaction`](../src/main/java/com/example/workflow/service/PaymentService.java#L107)(`Long orderId, String ownerId`) | `PaymentAttemptDTO` | Mở trong giao dịch. |
| [`PaymentService.toDto`](../src/main/java/com/example/workflow/service/PaymentService.java#L162)(`Order order, PaymentAttempt attempt`) | `PaymentAttemptDTO` | Chuyển thành dto. |
| [`PaymentService.buildQrUrl`](../src/main/java/com/example/workflow/service/PaymentService.java#L183)(`PaymentAttempt attempt`) | `String` | Tạo qr URL. |
| [`PaymentService.officialAmount`](../src/main/java/com/example/workflow/service/PaymentService.java#L202)(`Order order`) | `BigDecimal` | Thực hiện số tiền. |
| [`PaymentService.requireOwner`](../src/main/java/com/example/workflow/service/PaymentService.java#L214)(`Order order, String ownerId`) | `void` | Lấy và đảm bảo tồn tại chủ sở hữu. |
| [`PaymentService.requireOnlinePayment`](../src/main/java/com/example/workflow/service/PaymentService.java#L220)(`Order order`) | `void` | Lấy và đảm bảo tồn tại online thanh toán. |
| [`PaymentService.newPaymentCode`](../src/main/java/com/example/workflow/service/PaymentService.java#L234)() | `String` | Tạo thanh toán code. |
| [`PaymentService.transferContent`](../src/main/java/com/example/workflow/service/PaymentService.java#L243)(`PaymentAttempt attempt`) | `String` | Thực hiện content. |
| [`PaymentService.normalizedProvider`](../src/main/java/com/example/workflow/service/PaymentService.java#L249)() | `String` | Thực hiện provider. |
| [`PaymentService.blankToNull`](../src/main/java/com/example/workflow/service/PaymentService.java#L253)(`String value`) | `String` | Thực hiện tới null. |

### [src/main/java/com/example/workflow/service/PaymentTimeoutService.java:44](../src/main/java/com/example/workflow/service/PaymentTimeoutService.java#L44)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentTimeoutService.expireDuePayments`](../src/main/java/com/example/workflow/service/PaymentTimeoutService.java#L44)() | `void` | Xử lý hết hạn due thanh toán. |
| [`PaymentTimeoutService.expireIfStillDue`](../src/main/java/com/example/workflow/service/PaymentTimeoutService.java#L60)(`Long attemptId`) | `boolean` | Xử lý hết hạn if still due. |
| [`PaymentTimeoutService.expireWithinTransaction`](../src/main/java/com/example/workflow/service/PaymentTimeoutService.java#L66)(`Long attemptId, LocalDateTime now`) | `boolean` | Xử lý hết hạn trong giao dịch. |

### [src/main/java/com/example/workflow/service/PaymentWebhookService.java:71](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L71)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentWebhookService.process`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L71)(`byte[] rawBody, HttpHeaders headers`) | `WebhookResult` | Xử lý thanh toán webhook. |
| [`PaymentWebhookService.verifySignature`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L149)(`byte[] rawBody, String receivedSignature, String rawTimestamp`) | `void` | Thực hiện chữ ký. |
| [`PaymentWebhookService.parse`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L185)(`byte[] rawBody`) | `SePayPayload` | Phân tích thanh toán webhook. |
| [`PaymentWebhookService.validateRequiredFields`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L193)(`SePayPayload payload`) | `void` | Kiểm tra hợp lệ bắt buộc trường. |
| [`PaymentWebhookService.validationFailure`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L204)(`SePayPayload payload, PaymentAttempt attempt`) | `String` | Thực hiện lỗi. |
| [`PaymentWebhookService.expireAndCancel`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L216)(`Order order, PaymentAttempt attempt`) | `void` | Xử lý hết hạn và cancel. |
| [`PaymentWebhookService.markReconciliation`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L230)(`PaymentAttempt attempt, byte[] rawBody`) | `void` | Đánh dấu reconciliation. |
| [`PaymentWebhookService.safePayload`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L236)(`byte[] rawBody`) | `String` | Thực hiện payload. |
| [`PaymentWebhookService.normalizedProvider`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L241)() | `String` | Thực hiện provider. |
| [`PaymentWebhookService.unauthorized`](../src/main/java/com/example/workflow/service/PaymentWebhookService.java#L245)() | `AppException` | Thực hiện thanh toán webhook. |

### [src/main/java/com/example/workflow/service/ProductReviewService.java:45](../src/main/java/com/example/workflow/service/ProductReviewService.java#L45)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewService.createForOrderItem`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L45)(`Long orderItemId, ProductReviewRequest request`) | `ProductReviewDTO` | Tạo cho mặt hàng của đơn hàng. |
| [`ProductReviewService.updateMyReview`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L73)(`Long reviewId, ProductReviewRequest request`) | `ProductReviewDTO` | Cập nhật của người dùng duyệt/đánh giá. |
| [`ProductReviewService.hideReview`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L85)(`Long reviewId, String reason`) | `ProductReviewDTO` | Thực hiện duyệt/đánh giá. |
| [`ProductReviewService.restoreReview`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L96)(`Long reviewId`) | `ProductReviewDTO` | Khôi phục duyệt/đánh giá. |
| [`ProductReviewService.getProductReviews`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L107)(`Long productId, Pageable pageable`) | `Page<ProductReviewDTO>` | Lấy sản phẩm duyệt/đánh giá. |
| [`ProductReviewService.getVisibleVariantReviews`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L115)(`Long variantId, Pageable pageable`) | `Page<ProductReviewDTO>` | Lấy được hiển thị biến thể duyệt/đánh giá. |
| [`ProductReviewService.getManageableVariantReviews`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L123)(`Long variantId, Pageable pageable`) | `Page<ProductReviewDTO>` | Lấy có thể quản lý biến thể duyệt/đánh giá. |
| [`ProductReviewService.getProductSummary`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L130)(`Long productId`) | `ProductReviewSummaryDTO` | Lấy sản phẩm tóm tắt. |
| [`ProductReviewService.getVariantSummary`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L136)(`Long variantId`) | `ProductReviewSummaryDTO` | Lấy biến thể tóm tắt. |
| [`ProductReviewService.validateReviewTarget`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L142)(`Order order, OrderItem orderItem, User currentUser`) | `void` | Kiểm tra hợp lệ duyệt/đánh giá đích. |
| [`ProductReviewService.validateReviewContent`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L155)(`ProductReviewRequest request`) | `void` | Kiểm tra hợp lệ duyệt/đánh giá content. |
| [`ProductReviewService.applyRequest`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L162)(`ProductReview review, ProductReviewRequest request`) | `void` | Áp dụng yêu cầu. |
| [`ProductReviewService.buildSummary`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L169)(`Long productId, Long variantId`) | `ProductReviewSummaryDTO` | Tạo tóm tắt. |
| [`ProductReviewService.normalizeImageUrls`](../src/main/java/com/example/workflow/service/ProductReviewService.java#L213)(`List<String> imageUrls`) | `List<String>` | Chuẩn hóa ảnh URL. |

### [src/main/java/com/example/workflow/service/ProductService.java:50](../src/main/java/com/example/workflow/service/ProductService.java#L50)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductService.getAllProducts`](../src/main/java/com/example/workflow/service/ProductService.java#L50)(`String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `Page<ProductDTO>` | Lấy tất cả sản phẩm. |
| [`ProductService.getBestSellingProducts`](../src/main/java/com/example/workflow/service/ProductService.java#L59)(`String period, Pageable pageable`) | `Page<BestSellerProductDTO>` | Lấy best selling sản phẩm. |
| [`ProductService.getProductById`](../src/main/java/com/example/workflow/service/ProductService.java#L71)(`Long id`) | `ProductDTO` | Lấy sản phẩm theo ID. |
| [`ProductService.createProduct`](../src/main/java/com/example/workflow/service/ProductService.java#L77)(`ProductDTO dto`) | `ProductDTO` | Tạo sản phẩm. |
| [`ProductService.updateProduct`](../src/main/java/com/example/workflow/service/ProductService.java#L90)(`Long id, ProductDTO dto`) | `void` | Cập nhật sản phẩm. |
| [`ProductService.addVariant`](../src/main/java/com/example/workflow/service/ProductService.java#L134)(`Long productId, ProductVariantDTO dto`) | `ProductDTO` | Thêm biến thể. |
| [`ProductService.deleteProduct`](../src/main/java/com/example/workflow/service/ProductService.java#L151)(`Long id`) | `void` | Xóa sản phẩm. |
| [`ProductService.prepareProductForCreate`](../src/main/java/com/example/workflow/service/ProductService.java#L163)(`Product product`) | `void` | Thực hiện sản phẩm cho create. |
| [`ProductService.applyCustomCatalogReferences`](../src/main/java/com/example/workflow/service/ProductService.java#L174)(`ProductDTO dto, Product product, boolean creating`) | `void` | Áp dụng custom catalog references. |
| [`ProductService.validateMadeDay`](../src/main/java/com/example/workflow/service/ProductService.java#L185)(`Double madeDay`) | `void` | Kiểm tra hợp lệ made day. |
| [`ProductService.applyVariantOwnership`](../src/main/java/com/example/workflow/service/ProductService.java#L192)(`Product product, ProductVariant variant`) | `void` | Áp dụng biến thể ownership. |
| [`ProductService.createVariant`](../src/main/java/com/example/workflow/service/ProductService.java#L197)(`Product product, ProductVariantDTO dto`) | `ProductVariant` | Tạo biến thể. |
| [`ProductService.requireActiveProduct`](../src/main/java/com/example/workflow/service/ProductService.java#L203)(`Long productId`) | `Product` | Lấy và đảm bảo tồn tại đang hoạt động sản phẩm. |
| [`ProductService.requireActiveVariant`](../src/main/java/com/example/workflow/service/ProductService.java#L213)(`Long variantId`) | `ProductVariant` | Lấy và đảm bảo tồn tại đang hoạt động biến thể. |
| [`ProductService.productsCacheKey`](../src/main/java/com/example/workflow/service/ProductService.java#L219)(`String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable`) | `String` | Thực hiện cache khóa. |
| [`ProductService.normalizeSearchKeyword`](../src/main/java/com/example/workflow/service/ProductService.java#L228)(`String keyword`) | `String` | Chuẩn hóa search keyword. |
| [`ProductService.normalizePrice`](../src/main/java/com/example/workflow/service/ProductService.java#L235)(`BigDecimal price`) | `BigDecimal` | Chuẩn hóa giá. |
| [`ProductService.resolveBestSellerRange`](../src/main/java/com/example/workflow/service/ProductService.java#L239)(`String period`) | `BestSellerRange` | Xác định best seller range. |

### [src/main/java/com/example/workflow/service/ProductTypeService.java:34](../src/main/java/com/example/workflow/service/ProductTypeService.java#L34)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductTypeService.listActive`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L34)() | `List<ProductTypeDTO>` | Liệt kê đang hoạt động. |
| [`ProductTypeService.listAll`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L39)() | `List<ProductTypeDTO>` | Liệt kê tất cả. |
| [`ProductTypeService.getActive`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L44)(`Long id`) | `ProductTypeDTO` | Lấy đang hoạt động. |
| [`ProductTypeService.create`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L51)(`SaveProductTypeRequest request`) | `ProductTypeDTO` | Tạo sản phẩm loại. |
| [`ProductTypeService.update`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L65)(`Long id, SaveProductTypeRequest request`) | `ProductTypeDTO` | Cập nhật sản phẩm loại. |
| [`ProductTypeService.validate`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L79)(`SaveProductTypeRequest request, boolean updating`) | `void` | Kiểm tra hợp lệ sản phẩm loại. |
| [`ProductTypeService.apply`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L103)(`ProductType type, SaveProductTypeRequest request`) | `void` | Áp dụng sản phẩm loại. |
| [`ProductTypeService.normalizeField`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L111)(`ProductTypeFieldDTO field`) | `ProductTypeFieldDTO` | Chuẩn hóa trường. |
| [`ProductTypeService.readFields`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L118)(`String json`) | `List<ProductTypeFieldDTO>` | Đọc trường. |
| [`ProductTypeService.writeFields`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L126)(`List<ProductTypeFieldDTO> fields`) | `String` | Thực hiện trường. |
| [`ProductTypeService.toDto`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L136)(`ProductType type`) | `ProductTypeDTO` | Chuyển thành dto. |
| [`ProductTypeService.invalid`](../src/main/java/com/example/workflow/service/ProductTypeService.java#L141)(`String detail`) | `AppException` | Thực hiện sản phẩm loại. |

### [src/main/java/com/example/workflow/service/ProductionCalendarService.java:28](../src/main/java/com/example/workflow/service/ProductionCalendarService.java#L28)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCalendarService.calculateCompletionDate`](../src/main/java/com/example/workflow/service/ProductionCalendarService.java#L28)(`LocalDate startDate, int productionDays`) | `LocalDate` | The start date is not counted. Sundays and configured holidays are skipped. |
| [`ProductionCalendarService.isProductionDay`](../src/main/java/com/example/workflow/service/ProductionCalendarService.java#L41)(`LocalDate date`) | `boolean` | Kiểm tra điều kiện sản xuất day. |
| [`ProductionCalendarService.parseHolidays`](../src/main/java/com/example/workflow/service/ProductionCalendarService.java#L45)(`String configuredHolidays`) | `Set<LocalDate>` | Phân tích holidays. |

### [src/main/java/com/example/workflow/service/ProductionCheckpointService.java:69](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L69)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpointService.getProgress`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L69)(`Long orderId`) | `List<OrderItemProductionDTO>` | Lấy danh sách tiến độ checkpoint theo từng mặt hàng của đơn hàng đã xác thực quyền truy cập. |
| [`ProductionCheckpointService.getGuestProgress`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L78)(`Long orderId, String token`) | `List<OrderItemProductionDTO>` | Lấy tiến độ checkpoint công khai cho khách bằng token đọc đã ký; chỉ trả dữ liệu khách được phép xem. |
| [`ProductionCheckpointService.progressView`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L86)(`Order order, boolean ownerView`) | `List<OrderItemProductionDTO>` | Tạo dữ liệu tiến độ theo từng OrderItem và ẩn checkpoint FINAL_PRODUCT ở góc nhìn khách hàng. |
| [`ProductionCheckpointService.submit`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L108)(`Long orderId, Long orderItemId, SubmitProductionCheckpointRequest request, String idempotencyKey`) | `ProductionCheckpointDTO` | Nhận checkpoint INITIAL_SHAPE hoặc FINAL_PRODUCT; kiểm tra quyền, áp dụng idempotency và trả ProductionCheckpointDTO. |
| [`ProductionCheckpointService.review`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L137)(`Long orderId, Long checkpointId, ReviewProductionCheckpointRequest request, String idempotencyKey`) | `ReviewProductionCheckpointResultDTO` | Duyệt checkpoint FINAL_PRODUCT bằng PASS hoặc REWORK_REQUIRED; áp dụng idempotency và trả kết quả cập nhật tiến độ đơn. |
| [`ProductionCheckpointService.submitWithinTransaction`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L173)(`Long orderId, Long orderItemId, SubmitProductionCheckpointRequest request, User staff`) | `ProductionCheckpointDTO` | Lưu checkpoint trong giao dịch đã mở sau khi kiểm tra version đơn, assignment, OrderItem và ảnh. |
| [`ProductionCheckpointService.reviewWithinTransaction`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L214)(`Long orderId, Long checkpointId, ReviewProductionCheckpointRequest request, User manager`) | `ReviewProductionCheckpointResultDTO` | Cập nhật quyết định KCS trong giao dịch; khi đạt điều kiện thì cập nhật trạng thái item/đơn và giải phóng assignment. |
| [`ProductionCheckpointService.newInitialCheckpoint`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L306)(`OrderItem item, User staff, List<String> imageUrls, String note`) | `ProductionCheckpoint` | Tạo checkpoint tạo hình ban đầu; trạng thái được đánh dấu hoàn tất ngay, không qua bước KCS. |
| [`ProductionCheckpointService.newFinalCheckpoint`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L325)(`OrderItem item, User staff, List<String> imageUrls, String note`) | `ProductionCheckpoint` | Tạo checkpoint sản phẩm cuối cho lần gửi hiện tại để quản lý KCS duyệt. |
| [`ProductionCheckpointService.baseCheckpoint`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L367)(`OrderItem item, User staff, ProductionCheckpointStage stage, int attempt, String note, List<String> imageUrls`) | `ProductionCheckpoint` | Khởi tạo checkpoint chung gồm mặt hàng, người gửi, stage, số lần thử, ghi chú và ảnh. |
| [`ProductionCheckpointService.lockProductionOrder`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L391)(`Long orderId`) | `Order` | Khóa sản xuất đơn hàng. |
| [`ProductionCheckpointService.requireActiveAssignment`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L400)(`Long orderId`) | `OrderAssignment` | Tìm và xác nhận đơn hàng vẫn có assignment sản xuất đang hoạt động. |
| [`ProductionCheckpointService.requireItem`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L407)(`Order order, Long orderItemId`) | `OrderItem` | Tìm OrderItem thuộc đúng đơn hàng; từ chối ID item không thuộc đơn. |
| [`ProductionCheckpointService.requireReviewableCheckpoint`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L414)(`Order order, ProductionCheckpoint checkpoint, ReviewProductionCheckpointRequest request`) | `void` | Xác nhận checkpoint FINAL_PRODUCT thuộc đơn và đang ở trạng thái có thể review. |
| [`ProductionCheckpointService.authorizeProgressRead`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L437)(`Order order, User actor`) | `void` | Kiểm tra actor được phép xem tiến độ sản xuất của đơn hàng. |
| [`ProductionCheckpointService.requireSubmitRequest`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L449)(`SubmitProductionCheckpointRequest request`) | `void` | Kiểm tra stage, version, ghi chú và danh sách ảnh của request submit. |
| [`ProductionCheckpointService.requireReviewRequest`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L461)(`ReviewProductionCheckpointRequest request`) | `void` | Kiểm tra version, quyết định và lý do của request KCS review. |
| [`ProductionCheckpointService.reviewReason`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L476)(`ReviewProductionCheckpointRequest request`) | `String` | Chuẩn hóa lý do review và áp dụng điều kiện bắt buộc theo quyết định. |
| [`ProductionCheckpointService.validatedImageUrls`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L484)(`List<String> values`) | `List<String>` | Kiểm tra số lượng, định dạng và URL HTTPS của ảnh checkpoint. |
| [`ProductionCheckpointService.canonicalSubmit`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L507)(`Long orderId, Long orderItemId, SubmitProductionCheckpointRequest request`) | `Map<String, Object>` | Tạo payload ổn định để so sánh request idempotency của lần submit. |
| [`ProductionCheckpointService.requireOrderVersion`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L522)(`Order order, Long expectedVersion`) | `void` | So sánh version request với version mới nhất của đơn; báo conflict nếu đã cũ. |
| [`ProductionCheckpointService.isInitialComplete`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L528)(`ProductionCheckpointStatus status`) | `boolean` | Kiểm tra stage tạo hình ban đầu đã được hoàn tất. |
| [`ProductionCheckpointService.isPendingReview`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L532)(`ProductionCheckpointStatus status`) | `boolean` | Kiểm tra checkpoint đang chờ quản lý KCS duyệt. |
| [`ProductionCheckpointService.isPassed`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L536)(`ProductionCheckpointStatus status`) | `boolean` | Kiểm tra checkpoint đã được KCS thông qua. |
| [`ProductionCheckpointService.assignedStaffId`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L540)(`Order order`) | `String` | Lấy ID nhân viên đang được phân công sản xuất cho đơn. |
| [`ProductionCheckpointService.safeItems`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L545)(`Order order`) | `List<OrderItem>` | Trả danh sách OrderItem của đơn; dùng danh sách rỗng nếu collection chưa được khởi tạo. |
| [`ProductionCheckpointService.normalizedOptional`](../src/main/java/com/example/workflow/service/ProductionCheckpointService.java#L549)(`String value`) | `String` | Trim chuỗi tùy chọn và đổi chuỗi rỗng thành null. |

### [src/main/java/com/example/workflow/service/ProductionStartService.java:52](../src/main/java/com/example/workflow/service/ProductionStartService.java#L52)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionStartService.start`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L52)(`Long orderId, String idempotencyKey`) | `StartProductionResultDTO` | Bắt đầu sản xuất start. |
| [`ProductionStartService.startWithinTransaction`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L75)(`Long orderId, String staffId`) | `StartProductionResultDTO` | Bắt đầu trong giao dịch. |
| [`ProductionStartService.paymentAllowsStart`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L117)(`Order order`) | `boolean` | Thực hiện allows start. |
| [`ProductionStartService.requireProductionDurations`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L130)(`Order order`) | `void` | Lấy và đảm bảo tồn tại sản xuất durations. |
| [`ProductionStartService.applyDeadlines`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L141)(`Order order`) | `void` | Áp dụng deadlines. |
| [`ProductionStartService.result`](../src/main/java/com/example/workflow/service/ProductionStartService.java#L154)(`Order order`) | `StartProductionResultDTO` | Thực hiện sản xuất start. |

### [src/main/java/com/example/workflow/service/ReputationService.java:31](../src/main/java/com/example/workflow/service/ReputationService.java#L31)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ReputationService.changeReputation`](../src/main/java/com/example/workflow/service/ReputationService.java#L31)(`User user, int delta, String reason, String referenceType, String referenceId`) | `User` | Thực hiện reputation. |
| [`ReputationService.getMyHistory`](../src/main/java/com/example/workflow/service/ReputationService.java#L58)(`Pageable pageable`) | `Page<ReputationHistoryDTO>` | Lấy của người dùng lịch sử. |

### [src/main/java/com/example/workflow/service/UserService.java:39](../src/main/java/com/example/workflow/service/UserService.java#L39)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserService.startUserRegistrationProcess`](../src/main/java/com/example/workflow/service/UserService.java#L39)(`UserCreDTO dto`) | `void` | Bắt đầu người dùng registration quy trình. |
| [`UserService.validateRegistrationRequest`](../src/main/java/com/example/workflow/service/UserService.java#L56)(`UserCreDTO dto`) | `void` | Kiểm tra hợp lệ registration yêu cầu. |
| [`UserService.resolveRegistrationRole`](../src/main/java/com/example/workflow/service/UserService.java#L71)(`UserCreDTO dto`) | `Role` | Xác định registration vai trò. |
| [`UserService.canCurrentUserAssignRole`](../src/main/java/com/example/workflow/service/UserService.java#L79)() | `boolean` | Kiểm tra khả năng người dùng hiện tại assign vai trò. |
| [`UserService.normalizeRegistrationData`](../src/main/java/com/example/workflow/service/UserService.java#L83)(`UserCreDTO dto`) | `void` | Chuẩn hóa registration data. |
| [`UserService.trimToNull`](../src/main/java/com/example/workflow/service/UserService.java#L94)(`String value`) | `String` | Thực hiện tới null. |
| [`UserService.parseRole`](../src/main/java/com/example/workflow/service/UserService.java#L98)(`String role`) | `Role` | Phân tích vai trò. |
| [`UserService.extractSubFromToken`](../src/main/java/com/example/workflow/service/UserService.java#L106)(`String tokenOrId`) | `String` | Thực hiện sub từ token. |
| [`UserService.getAllUsers`](../src/main/java/com/example/workflow/service/UserService.java#L122)(`List<Role> roles, Pageable pageable`) | `Page<UserListDTO>` | Lấy tất cả users. |
| [`UserService.getUserById`](../src/main/java/com/example/workflow/service/UserService.java#L131)(`String id`) | `UserResDTO` | Lấy người dùng theo ID. |
| [`UserService.getMyProfile`](../src/main/java/com/example/workflow/service/UserService.java#L137)() | `UserResDTO` | Lấy của người dùng hồ sơ. |
| [`UserService.updateMyProfile`](../src/main/java/com/example/workflow/service/UserService.java#L142)(`UserProfileUpdateDTO request`) | `UserResDTO` | Cập nhật của người dùng hồ sơ. |
| [`UserService.updateUser`](../src/main/java/com/example/workflow/service/UserService.java#L153)(`String id, UserCreDTO request`) | `UserResDTO` | Cập nhật người dùng. |
| [`UserService.resolveUpdatedRole`](../src/main/java/com/example/workflow/service/UserService.java#L164)(`User user, UserCreDTO request`) | `Role` | Xác định updated vai trò. |
| [`UserService.normalizeProfileData`](../src/main/java/com/example/workflow/service/UserService.java#L176)(`UserProfileUpdateDTO request`) | `void` | Chuẩn hóa hồ sơ data. |
| [`UserService.validateProfileUniqueness`](../src/main/java/com/example/workflow/service/UserService.java#L186)(`User user, UserProfileUpdateDTO request`) | `void` | Kiểm tra hợp lệ hồ sơ uniqueness. |
| [`UserService.deleteUser`](../src/main/java/com/example/workflow/service/UserService.java#L196)(`String id`) | `void` | Xóa người dùng. |
| [`UserService.requireUser`](../src/main/java/com/example/workflow/service/UserService.java#L204)(`String id`) | `User` | Lấy và đảm bảo tồn tại người dùng. |
| [`UserService.requireUser`](../src/main/java/com/example/workflow/service/UserService.java#L209)(`String id, ConstantErrorCode errorCode, Object... args`) | `User` | Lấy và đảm bảo tồn tại người dùng. |
| [`UserService.requireActiveUser`](../src/main/java/com/example/workflow/service/UserService.java#L215)(`String id`) | `User` | Lấy và đảm bảo tồn tại đang hoạt động người dùng. |
| [`UserService.requireActiveStaff`](../src/main/java/com/example/workflow/service/UserService.java#L224)(`String staffId`) | `User` | Lấy và đảm bảo tồn tại đang hoạt động nhân viên. |
| [`UserService.requireStaff`](../src/main/java/com/example/workflow/service/UserService.java#L233)(`String staffId`) | `User` | Lấy và đảm bảo tồn tại nhân viên. |
| [`UserService.fullName`](../src/main/java/com/example/workflow/service/UserService.java#L242)(`User user`) | `String` | Thực hiện tên. |
| [`UserService.displayName`](../src/main/java/com/example/workflow/service/UserService.java#L246)(`User user`) | `String` | Thực hiện tên. |

### [src/main/java/com/example/workflow/service/VoucherService.java:54](../src/main/java/com/example/workflow/service/VoucherService.java#L54)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`VoucherService.getActiveTemplates`](../src/main/java/com/example/workflow/service/VoucherService.java#L54)() | `List<VoucherTemplateDTO>` | Lấy đang hoạt động templates. |
| [`VoucherService.getActiveTemplatesForManagement`](../src/main/java/com/example/workflow/service/VoucherService.java#L63)() | `List<VoucherTemplateDTO>` | Lấy đang hoạt động templates cho management. |
| [`VoucherService.getGuestVoucherOptions`](../src/main/java/com/example/workflow/service/VoucherService.java#L72)(`BigDecimal subtotal`) | `List<VoucherCartOptionDTO>` | Lấy khách voucher options. |
| [`VoucherService.getGuestVoucherOptions`](../src/main/java/com/example/workflow/service/VoucherService.java#L77)(`BigDecimal subtotal, String guestSessionId`) | `List<VoucherCartOptionDTO>` | Lấy khách voucher options. |
| [`VoucherService.getMyWallet`](../src/main/java/com/example/workflow/service/VoucherService.java#L87)() | `List<UserVoucherDTO>` | Lấy của người dùng wallet. |
| [`VoucherService.getCartVoucherOptions`](../src/main/java/com/example/workflow/service/VoucherService.java#L101)(`BigDecimal subtotal`) | `CartVoucherOptionsDTO` | Lấy giỏ hàng voucher options. |
| [`VoucherService.redeemVoucher`](../src/main/java/com/example/workflow/service/VoucherService.java#L146)(`Long templateId`) | `UserVoucherDTO` | Đổi voucher voucher. |
| [`VoucherService.useVoucherForCheckout`](../src/main/java/com/example/workflow/service/VoucherService.java#L182)(`Long userVoucherId, String userId, BigDecimal totalPrice`) | `UserVoucher` | Sử dụng voucher cho checkout. |
| [`VoucherService.previewVoucherForCheckout`](../src/main/java/com/example/workflow/service/VoucherService.java#L197)(`Long userVoucherId, String userId, BigDecimal totalPrice`) | `UserVoucher` | Validate an agreement voucher without consuming it; confirmation performs the locked consume. |
| [`VoucherService.applyGuestVoucherForCheckout`](../src/main/java/com/example/workflow/service/VoucherService.java#L208)(`String voucherCode, BigDecimal totalPrice, String guestSessionId, String email, String phone`) | `AppliedGuestVoucher` | Áp dụng khách voucher cho checkout. |
| [`VoucherService.recordGuestVoucherUsage`](../src/main/java/com/example/workflow/service/VoucherService.java#L242)(`AppliedGuestVoucher appliedVoucher, Order order`) | `void` | Ghi nhận khách voucher usage. |
| [`VoucherService.restoreGuestVoucher`](../src/main/java/com/example/workflow/service/VoucherService.java#L263)(`VoucherTemplate template`) | `boolean` | Khôi phục khách voucher. |
| [`VoucherService.restoreAfterCancellation`](../src/main/java/com/example/workflow/service/VoucherService.java#L271)(`Order order`) | `boolean` | Khôi phục sau cancellation. |
| [`VoucherService.calculateDiscountAmount`](../src/main/java/com/example/workflow/service/VoucherService.java#L294)(`UserVoucher voucher, BigDecimal totalPrice`) | `BigDecimal` | Tính giảm giá số tiền. |
| [`VoucherService.createNewVoucherCampaign`](../src/main/java/com/example/workflow/service/VoucherService.java#L301)(`CreateVoucherTemplateRequest request`) | `VoucherTemplateDTO` | Tạo new voucher campaign. |
| [`VoucherService.getTemplateOrThrow`](../src/main/java/com/example/workflow/service/VoucherService.java#L308)(`Long templateId`) | `VoucherTemplate` | Lấy template hoặc ném lỗi. |
| [`VoucherService.validateTemplateForRedeem`](../src/main/java/com/example/workflow/service/VoucherService.java#L313)(`VoucherTemplate template, LocalDateTime now`) | `void` | Kiểm tra hợp lệ template cho redeem. |
| [`VoucherService.validateRemainingPoints`](../src/main/java/com/example/workflow/service/VoucherService.java#L325)(`int remainingPoints`) | `void` | Kiểm tra hợp lệ remaining points. |
| [`VoucherService.validateTemplateNotAlreadyOwned`](../src/main/java/com/example/workflow/service/VoucherService.java#L335)(`String userId, Long templateId, LocalDateTime now`) | `void` | Kiểm tra hợp lệ template không already owned. |
| [`VoucherService.createUserVoucher`](../src/main/java/com/example/workflow/service/VoucherService.java#L345)(`User user, VoucherTemplate template, LocalDateTime redeemTime`) | `UserVoucher` | Tạo người dùng voucher. |
| [`VoucherService.buildCartOption`](../src/main/java/com/example/workflow/service/VoucherService.java#L355)(`Long userVoucherId, VoucherTemplate template, String source, BigDecimal subtotal`) | `VoucherCartOptionDTO` | Tạo giỏ hàng option. |
| [`VoucherService.markBestOption`](../src/main/java/com/example/workflow/service/VoucherService.java#L375)(`List<VoucherCartOptionDTO> options`) | `VoucherCartOptionDTO` | Đánh dấu best option. |
| [`VoucherService.cartOptionComparator`](../src/main/java/com/example/workflow/service/VoucherService.java#L388)() | `Comparator<VoucherCartOptionDTO>` | Thực hiện option comparator. |
| [`VoucherService.validateVoucherForCheckout`](../src/main/java/com/example/workflow/service/VoucherService.java#L396)(`UserVoucher voucher, String userId, BigDecimal totalPrice`) | `void` | Kiểm tra hợp lệ voucher cho checkout. |
| [`VoucherService.buildGuestCartOption`](../src/main/java/com/example/workflow/service/VoucherService.java#L411)(`VoucherTemplate template, BigDecimal subtotal, String guestSessionId`) | `VoucherCartOptionDTO` | Tạo khách giỏ hàng option. |
| [`VoucherService.validateGuestVoucherForCheckout`](../src/main/java/com/example/workflow/service/VoucherService.java#L423)(`VoucherTemplate template, BigDecimal totalPrice, LocalDateTime now`) | `void` | Kiểm tra hợp lệ khách voucher cho checkout. |
| [`VoucherService.validateGuestVoucherNotAlreadyUsed`](../src/main/java/com/example/workflow/service/VoucherService.java#L438)(`Long voucherTemplateId, String guestSessionId, String emailHash, String phoneHash`) | `void` | Kiểm tra hợp lệ khách voucher không already used. |
| [`VoucherService.guestVoucherAlreadyUsed`](../src/main/java/com/example/workflow/service/VoucherService.java#L453)() | `AppException` | Thực hiện voucher already used. |
| [`VoucherService.normalizeEmail`](../src/main/java/com/example/workflow/service/VoucherService.java#L461)(`String email`) | `String` | Chuẩn hóa email. |
| [`VoucherService.normalizePhone`](../src/main/java/com/example/workflow/service/VoucherService.java#L465)(`String phone`) | `String` | Chuẩn hóa điện thoại. |
| [`VoucherService.requireGuestIdentity`](../src/main/java/com/example/workflow/service/VoucherService.java#L476)(`String value, String fieldName`) | `String` | Lấy và đảm bảo tồn tại khách identity. |
| [`VoucherService.sha256`](../src/main/java/com/example/workflow/service/VoucherService.java#L483)(`String value`) | `String` | Thực hiện 256. |
| [`VoucherService.calculateDiscountAmount`](../src/main/java/com/example/workflow/service/VoucherService.java#L492)(`VoucherTemplate template, BigDecimal totalPrice`) | `BigDecimal` | Tính giảm giá số tiền. |
| [`VoucherService.nonNegative`](../src/main/java/com/example/workflow/service/VoucherService.java#L506)(`BigDecimal value`) | `BigDecimal` | Thực hiện negative. |
| [`VoucherService.floorZero`](../src/main/java/com/example/workflow/service/VoucherService.java#L510)(`BigDecimal value`) | `BigDecimal` | Thực hiện zero. |
| [`VoucherService.AppliedGuestVoucher.none`](../src/main/java/com/example/workflow/service/VoucherService.java#L521)() | `AppliedGuestVoucher` | Thực hiện applied khách voucher. |
| [`VoucherService.AppliedGuestVoucher.applied`](../src/main/java/com/example/workflow/service/VoucherService.java#L525)() | `boolean` | Thực hiện applied khách voucher. |

### [src/main/java/com/example/workflow/service/WishlistService.java:34](../src/main/java/com/example/workflow/service/WishlistService.java#L34)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WishlistService.getMyWishlist`](../src/main/java/com/example/workflow/service/WishlistService.java#L34)(`Pageable pageable`) | `Page<ProductDTO>` | Lấy của người dùng wishlist. |
| [`WishlistService.addToWishlist`](../src/main/java/com/example/workflow/service/WishlistService.java#L47)(`Long productId`) | `ProductDTO` | Thêm tới wishlist. |
| [`WishlistService.removeFromWishlist`](../src/main/java/com/example/workflow/service/WishlistService.java#L60)(`Long productId`) | `void` | Xóa từ wishlist. |
| [`WishlistService.isInMyWishlist`](../src/main/java/com/example/workflow/service/WishlistService.java#L67)(`Long productId`) | `boolean` | Kiểm tra điều kiện trong của người dùng wishlist. |
| [`WishlistService.getMyWishlistStatus`](../src/main/java/com/example/workflow/service/WishlistService.java#L77)(`Collection<Long> productIds`) | `Map<Long, Boolean>` | Lấy của người dùng wishlist trạng thái. |
| [`WishlistService.createWishlistItem`](../src/main/java/com/example/workflow/service/WishlistService.java#L99)(`Product product`) | `ProductDTO` | Tạo wishlist mặt hàng. |
| [`WishlistService.normalizeProductIds`](../src/main/java/com/example/workflow/service/WishlistService.java#L108)(`Collection<Long> productIds`) | `List<Long>` | Chuẩn hóa sản phẩm ID. |
| [`WishlistService.cacheKeyForProductIds`](../src/main/java/com/example/workflow/service/WishlistService.java#L122)(`Collection<Long> productIds`) | `String` | Tạo khóa cache khóa cho sản phẩm ID. |

### [src/main/java/com/example/workflow/service/assembler/OrderDetailsAssembler.java:23](../src/main/java/com/example/workflow/service/assembler/OrderDetailsAssembler.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderDetailsAssembler.toDto`](../src/main/java/com/example/workflow/service/assembler/OrderDetailsAssembler.java#L23)(`Order order`) | `OrderDTO` | Chuyển thành dto. |
| [`OrderDetailsAssembler.enrichReviewStatuses`](../src/main/java/com/example/workflow/service/assembler/OrderDetailsAssembler.java#L29)(`OrderDTO dto`) | `void` | Thực hiện duyệt/đánh giá statuses. |

### [src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java:24](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L24)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ApplicationCacheService.evictCartChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L24)(`String cartCacheKey`) | `void` | Xóa khỏi cache giỏ hàng changed. |
| [`ApplicationCacheService.evictUserCartChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L30)(`String userId`) | `void` | Xóa khỏi cache người dùng giỏ hàng changed. |
| [`ApplicationCacheService.evictGuestCheckoutCart`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L36)(`String guestSessionId`) | `void` | Xóa khỏi cache khách checkout giỏ hàng. |
| [`ApplicationCacheService.evictUserCheckoutCart`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L42)(`String userId`) | `void` | Xóa khỏi cache người dùng checkout giỏ hàng. |
| [`ApplicationCacheService.evictOrderCreated`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L46)(`Order order`) | `void` | Xóa khỏi cache đơn hàng đã tạo. |
| [`ApplicationCacheService.evictWarehouseClaimed`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L58)(`Order order, String staffId`) | `void` | Xóa khỏi cache warehouse claimed. |
| [`ApplicationCacheService.evictStaffAssigned`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L64)(`Order order, OrderStatus oldStatus, String previousStaffId, String assignedStaffId`) | `void` | Xóa khỏi cache nhân viên được phân công. |
| [`ApplicationCacheService.evictManagerReviewed`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L74)(`Order order, boolean approved, String assignedStaffId`) | `void` | Xóa khỏi cache quản lý đã duyệt. |
| [`ApplicationCacheService.evictOrderDelivered`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L85)(`Order order, String userId`) | `void` | Xóa khỏi cache đơn hàng delivered. |
| [`ApplicationCacheService.evictOrderCancelled`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L90)(`Order order, OrderStatus oldStatus`) | `void` | Xóa khỏi cache đơn hàng đã hủy. |
| [`ApplicationCacheService.evictChangeRequestChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L107)(`Order order, String staffId`) | `void` | Xóa khỏi cache change yêu cầu changed. |
| [`ApplicationCacheService.evictShippingHandover`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L112)(`Order order, String staffId`) | `void` | Xóa khỏi cache vận chuyển handover. |
| [`ApplicationCacheService.evictProductCreated`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L117)() | `void` | Xóa khỏi cache sản phẩm đã tạo. |
| [`ApplicationCacheService.evictCatalogTaxonomyChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L122)() | `void` | Xóa khỏi cache catalog taxonomy changed. |
| [`ApplicationCacheService.evictProductUpdated`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L127)(`Long productId`) | `void` | Xóa khỏi cache sản phẩm updated. |
| [`ApplicationCacheService.evictProductVariantAdded`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L138)(`Long productId`) | `void` | Xóa khỏi cache sản phẩm biến thể added. |
| [`ApplicationCacheService.evictProductDeleted`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L144)(`Long productId`) | `void` | Xóa khỏi cache sản phẩm deleted. |
| [`ApplicationCacheService.evictWishlistChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L150)(`String userId, Long productId`) | `void` | Xóa khỏi cache wishlist changed. |
| [`ApplicationCacheService.evictVoucherRedeemed`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L158)(`String userId`) | `void` | Xóa khỏi cache voucher redeemed. |
| [`ApplicationCacheService.evictVoucherCampaignChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L166)() | `void` | Xóa khỏi cache voucher campaign changed. |
| [`ApplicationCacheService.evictReputationChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L171)(`String userId`) | `void` | Xóa khỏi cache reputation changed. |
| [`ApplicationCacheService.evictProductReviewChanged`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L180)() | `void` | Xóa khỏi cache sản phẩm duyệt/đánh giá changed. |
| [`ApplicationCacheService.evictProductReviewChangedByUser`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L185)(`String userId`) | `void` | Xóa khỏi cache sản phẩm duyệt/đánh giá changed theo người dùng. |
| [`ApplicationCacheService.evictUserRegistered`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L190)() | `void` | Xóa khỏi cache người dùng registered. |
| [`ApplicationCacheService.evictMyProfileUpdated`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L194)() | `void` | Xóa khỏi cache của người dùng hồ sơ updated. |
| [`ApplicationCacheService.evictUserUpdated`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L199)(`String userId`) | `void` | Xóa khỏi cache người dùng updated. |
| [`ApplicationCacheService.evictUserDeleted`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L204)(`String userId`) | `void` | Xóa khỏi cache người dùng deleted. |
| [`ApplicationCacheService.evictManagerPendingOrdersAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L209)(`OrderStatus status`) | `void` | Xóa khỏi cache quản lý đang chờ đơn hàng sau commit. |
| [`ApplicationCacheService.evictUserOrdersAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L219)(`String userId`) | `void` | Xóa khỏi cache người dùng đơn hàng sau commit. |
| [`ApplicationCacheService.evictUserCancelledOrdersAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L226)(`String userId`) | `void` | Xóa khỏi cache người dùng đã hủy đơn hàng sau commit. |
| [`ApplicationCacheService.evictStaffAssignedOrdersAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L233)(`String staffId`) | `void` | Xóa khỏi cache nhân viên được phân công đơn hàng sau commit. |
| [`ApplicationCacheService.evictUserStateAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L240)(`String userId`) | `void` | Xóa khỏi cache người dùng trạng thái sau commit. |
| [`ApplicationCacheService.evictUserVoucherWalletAfterCommit`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L248)(`String userId`) | `void` | Xóa khỏi cache người dùng voucher wallet sau commit. |
| [`ApplicationCacheService.publishDeferredCacheEvictions`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L254)(`String reason, CacheEvictionEntry... entries`) | `void` | Phát deferred cache evictions. |
| [`ApplicationCacheService.publishDeferredCacheEvictions`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L267)(`String reason, Collection<CacheEvictionEntry> entries`) | `void` | Phát deferred cache evictions. |
| [`ApplicationCacheService.wishlistEntries`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L277)() | `List<CacheEvictionEntry>` | Thực hiện entries. |
| [`ApplicationCacheService.orderUserId`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L285)(`Order order`) | `String` | Thực hiện người dùng ID. |
| [`ApplicationCacheService.orderStaffId`](../src/main/java/com/example/workflow/service/cache/ApplicationCacheService.java#L289)(`Order order`) | `String` | Thực hiện nhân viên ID. |

### [src/main/java/com/example/workflow/service/consistency/DurableRequestExecutor.java:30](../src/main/java/com/example/workflow/service/consistency/DurableRequestExecutor.java#L30)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DurableRequestExecutor.execute`](../src/main/java/com/example/workflow/service/consistency/DurableRequestExecutor.java#L30)(`String scope, String key, String canonicalPayload, Supplier<String> operation`) | `String` | Thực thi durable yêu cầu executor. |
| [`DurableRequestExecutor.hash`](../src/main/java/com/example/workflow/service/consistency/DurableRequestExecutor.java#L57)(`String value`) | `String` | Thực hiện durable yêu cầu executor. |

### [src/main/java/com/example/workflow/service/consistency/GuestOrderAccessGuard.java:20](../src/main/java/com/example/workflow/service/consistency/GuestOrderAccessGuard.java#L20)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestOrderAccessGuard.authorize`](../src/main/java/com/example/workflow/service/consistency/GuestOrderAccessGuard.java#L20)(`Order order, String rawToken, OrderLookupTokenService.Scope scope, long limit, Duration window`) | `void` | Budget/window are supplied explicitly by each endpoint policy. Never key by the raw token. |

### [src/main/java/com/example/workflow/service/consistency/OrderTransitionPolicy.java:22](../src/main/java/com/example/workflow/service/consistency/OrderTransitionPolicy.java#L22)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderTransitionPolicy.allows`](../src/main/java/com/example/workflow/service/consistency/OrderTransitionPolicy.java#L22)(`OrderStatus from, OrderStatus to, CancellationSource cancellation`) | `boolean` | Thực hiện đơn hàng transition policy. |

### [src/main/java/com/example/workflow/service/consistency/OrderTransitionService.java:29](../src/main/java/com/example/workflow/service/consistency/OrderTransitionService.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderTransitionService.apply`](../src/main/java/com/example/workflow/service/consistency/OrderTransitionService.java#L29)(`long orderId, long expectedVersion, OrderStatus next, String actor, String reason, String reference, CancellationSource cancellation`) | `Order` | Called inside an authorized workflow transaction together with its domain side effects. |

### [src/main/java/com/example/workflow/service/consistency/OutboxDispatcher.java:27](../src/main/java/com/example/workflow/service/consistency/OutboxDispatcher.java#L27)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OutboxDispatcher.poll`](../src/main/java/com/example/workflow/service/consistency/OutboxDispatcher.java#L27)() | `void` | Thăm dò và xử lý outbox dispatcher. |
| [`OutboxDispatcher.dispatchOne`](../src/main/java/com/example/workflow/service/consistency/OutboxDispatcher.java#L32)() | `boolean` | Phân phối one. |

### [src/main/java/com/example/workflow/service/consistency/OutboxRecoveryService.java:18](../src/main/java/com/example/workflow/service/consistency/OutboxRecoveryService.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OutboxRecoveryService.requeue`](../src/main/java/com/example/workflow/service/consistency/OutboxRecoveryService.java#L18)(`String eventId`) | `boolean` | Đưa lại vào hàng đợi outbox recovery. |
| [`OutboxRecoveryService.summary`](../src/main/java/com/example/workflow/service/consistency/OutboxRecoveryService.java#L27)() | `WorkflowOperationsSummaryDTO` | Thực hiện outbox recovery. |
| [`OutboxRecoveryService.incidents`](../src/main/java/com/example/workflow/service/consistency/OutboxRecoveryService.java#L50)(`int requestedLimit`) | `List<WorkflowOutboxIncidentDTO>` | Thực hiện outbox recovery. |

### [src/main/java/com/example/workflow/service/consistency/OutboxStore.java:18](../src/main/java/com/example/workflow/service/consistency/OutboxStore.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OutboxStore.append`](../src/main/java/com/example/workflow/service/consistency/OutboxStore.java#L18)(`String type, Object payload`) | `String` | Thực hiện outbox store. |

### [src/main/java/com/example/workflow/service/factory/CatalogOrderFactory.java:34](../src/main/java/com/example/workflow/service/factory/CatalogOrderFactory.java#L34)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CatalogOrderFactory.create`](../src/main/java/com/example/workflow/service/factory/CatalogOrderFactory.java#L34)(`PaymentMethod paymentMethod, OrderContactSnapshot contact, User user, String guestSessionId, List<CartItem> cartItems`) | `Order` | Tạo catalog đơn hàng. |

### [src/main/java/com/example/workflow/service/factory/CustomOrderFactory.java:30](../src/main/java/com/example/workflow/service/factory/CustomOrderFactory.java#L30)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomOrderFactory.create`](../src/main/java/com/example/workflow/service/factory/CustomOrderFactory.java#L30)(`User owner, CustomRequest draft, OrderContactSnapshot contact`) | `Order` | Tạo custom đơn hàng. |
| [`CustomOrderFactory.snapshot`](../src/main/java/com/example/workflow/service/factory/CustomOrderFactory.java#L60)(`CustomRequest draft`) | `String` | Thực hiện custom đơn hàng. |
| [`CustomOrderFactory.readJson`](../src/main/java/com/example/workflow/service/factory/CustomOrderFactory.java#L74)(`String json, Object fallback`) | `Object` | Đọc json. |

### [src/main/java/com/example/workflow/service/factory/UserFactory.java:11](../src/main/java/com/example/workflow/service/factory/UserFactory.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserFactory.create`](../src/main/java/com/example/workflow/service/factory/UserFactory.java#L11)(`UserCreDTO request, String id, Role role`) | `User` | Tạo người dùng. |

### [src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java:39](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L39)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutConcurrencyService.acquireCheckoutLocks`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L39)(`String userId, Collection<Long> variantIds`) | `CheckoutLocks` | Lấy khóa checkout locks. |
| [`CheckoutConcurrencyService.checkoutLockKeys`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L67)(`String userId, Collection<Long> variantIds`) | `List<String>` | Thực hiện lock khóa. |
| [`CheckoutConcurrencyService.tryAcquireUntilDeadline`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L81)(`String key, String token, Duration leaseTtl, long deadlineNanos`) | `boolean` | Thử acquire until hạn hoàn thành. |
| [`CheckoutConcurrencyService.sleepBeforeRetry`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L93)(`long deadlineNanos`) | `void` | Thực hiện trước retry. |
| [`CheckoutConcurrencyService.releaseQuietly`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L104)(`List<String> keys, String token`) | `void` | Thực hiện quietly. |
| [`CheckoutConcurrencyService.CheckoutLocks.noop`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L119)() | `CheckoutLocks` | Thực hiện checkout locks. |
| [`CheckoutConcurrencyService.CheckoutLocks.close`](../src/main/java/com/example/workflow/service/redis/CheckoutConcurrencyService.java#L123)() | `void` | Đóng checkout locks. |

### [src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java:40](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutIdempotencyService.begin`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L40)(`String userId, String idempotencyKey`) | `CheckoutIdempotencyState` | Thực hiện checkout idempotency. |
| [`CheckoutIdempotencyService.complete`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L81)(`CheckoutIdempotencyState state, Map<String, String> response`) | `void` | Hoàn tất checkout idempotency. |
| [`CheckoutIdempotencyService.fail`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L100)(`CheckoutIdempotencyState state`) | `void` | Ghi nhận thất bại checkout idempotency. |
| [`CheckoutIdempotencyService.findStoredResponse`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L106)(`String responseKey`) | `Map<String, String>` | Tìm stored phản hồi. |
| [`CheckoutIdempotencyService.releaseInProgress`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L118)(`CheckoutIdempotencyState state`) | `void` | Thực hiện trong tiến độ. |
| [`CheckoutIdempotencyService.key`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L126)(`String userId, String idempotencyKey, String suffix`) | `String` | Thực hiện checkout idempotency. |
| [`CheckoutIdempotencyService.ttl`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L130)(`long ttlMs`) | `Duration` | Thực hiện checkout idempotency. |
| [`CheckoutIdempotencyService.idempotencyUnavailable`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L134)(`Exception ex`) | `AppException` | Thực hiện unavailable. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.disabled`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L166)() | `CheckoutIdempotencyState` | Thực hiện checkout idempotency trạng thái. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.replay`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L170)(`Map<String, String> response`) | `CheckoutIdempotencyState` | Phát lại checkout idempotency trạng thái. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.active`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L174)(`String responseKey, String inProgressKey, String token`) | `CheckoutIdempotencyState` | Thực hiện checkout idempotency trạng thái. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.isActive`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L178)() | `boolean` | Kiểm tra điều kiện đang hoạt động. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.isReplay`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L182)() | `boolean` | Kiểm tra điều kiện replay. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.response`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L186)() | `Map<String, String>` | Thực hiện checkout idempotency trạng thái. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.responseKey`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L190)() | `String` | Thực hiện khóa. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.inProgressKey`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L194)() | `String` | Thực hiện tiến độ khóa. |
| [`CheckoutIdempotencyService.CheckoutIdempotencyState.token`](../src/main/java/com/example/workflow/service/redis/CheckoutIdempotencyService.java#L198)() | `String` | Thực hiện checkout idempotency trạng thái. |

### [src/main/java/com/example/workflow/service/redis/DeferredCacheEvictionPublisher.java:14](../src/main/java/com/example/workflow/service/redis/DeferredCacheEvictionPublisher.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DeferredCacheEvictionPublisher.publishEventually`](../src/main/java/com/example/workflow/service/redis/DeferredCacheEvictionPublisher.java#L14)(`String reason, List<CacheEvictionEntry> entries`) | `void` | Phát eventually. |

### [src/main/java/com/example/workflow/service/redis/DomainEventPublisher.java:14](../src/main/java/com/example/workflow/service/redis/DomainEventPublisher.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DomainEventPublisher.publishAfterCommit`](../src/main/java/com/example/workflow/service/redis/DomainEventPublisher.java#L14)(`String type, Object payload`) | `void` | Persist in the caller's transaction; the dispatcher publishes after commit. |
| [`DomainEventPublisher.publish`](../src/main/java/com/example/workflow/service/redis/DomainEventPublisher.java#L15)(`String type, Object payload`) | `void` | Phát domain sự kiện publisher. |

### [src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java:36](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`LoginRateLimitService.assertAllowed`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L36)(`String username`) | `void` | Kiểm tra điều kiện và báo lỗi nếu không đạt được phép. |
| [`LoginRateLimitService.recordFailure`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L53)(`String username`) | `void` | Ghi nhận lỗi. |
| [`LoginRateLimitService.clearFailures`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L71)(`String username`) | `void` | Xóa/làm trống failures. |
| [`LoginRateLimitService.lockDuration`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L83)(`long failureCount`) | `Duration` | Khóa thời lượng. |
| [`LoginRateLimitService.failureCountKey`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L89)(`String username`) | `String` | Thực hiện số lượng khóa. |
| [`LoginRateLimitService.lockKey`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L93)(`String username`) | `String` | Khóa khóa. |
| [`LoginRateLimitService.credentialKey`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L97)(`String username`) | `String` | Thực hiện khóa. |
| [`LoginRateLimitService.normalizedThreshold`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L102)() | `long` | Thực hiện threshold. |
| [`LoginRateLimitService.normalizedBaseLockMinutes`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L106)() | `long` | Thực hiện cơ sở lock minutes. |
| [`LoginRateLimitService.normalizedLockIncrementMinutes`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L110)() | `long` | Thực hiện lock increment minutes. |
| [`LoginRateLimitService.remainingTtlSeconds`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L114)(`String key`) | `long` | Thực hiện ttl seconds. |
| [`LoginRateLimitService.valueOrOne`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L119)(`Long value`) | `long` | Thực hiện hoặc one. |
| [`LoginRateLimitService.tooManyLoginAttempts`](../src/main/java/com/example/workflow/service/redis/LoginRateLimitService.java#L123)(`Duration retryAfter`) | `AppException` | Thực hiện many login các lần thử. |

### [src/main/java/com/example/workflow/service/redis/OptionalCacheService.java:28](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L28)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OptionalCacheService.clear`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L28)(`String cacheName`) | `boolean` | Xóa/làm trống tùy chọn cache. |
| [`OptionalCacheService.evict`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L41)(`String cacheName, Object key`) | `boolean` | Xóa khỏi cache tùy chọn cache. |
| [`OptionalCacheService.evictByPrefix`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L54)(`String cacheName, String keyPrefix`) | `void` | Xóa khỏi cache theo tiền tố. |
| [`OptionalCacheService.clearAfterCommit`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L73)(`String cacheName`) | `void` | Xóa/làm trống sau commit. |
| [`OptionalCacheService.evictAfterCommit`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L77)(`String cacheName, Object key`) | `void` | Xóa khỏi cache sau commit. |
| [`OptionalCacheService.evictByPrefixAfterCommit`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L81)(`String cacheName, String keyPrefix`) | `void` | Xóa khỏi cache theo tiền tố sau commit. |
| [`OptionalCacheService.apply`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L85)(`CacheEvictionRequestedEvent event`) | `void` | Áp dụng tùy chọn cache. |
| [`OptionalCacheService.applyOrThrow`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L95)(`CacheEvictionRequestedEvent event`) | `void` | Durable consumer must see failures so the event remains eligible for retry. |
| [`OptionalCacheService.apply`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L114)(`CacheEvictionEntry entry`) | `void` | Áp dụng tùy chọn cache. |
| [`OptionalCacheService.deleteKeys`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L129)(`String cacheName, String keyPrefix, Collection<String> keysToDelete`) | `void` | Xóa khóa. |
| [`OptionalCacheService.runAfterCommit`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L140)(`Runnable task`) | `void` | Chạy sau commit. |
| [`OptionalCacheService..afterCommit`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L146)() | `void` | Thực hiện commit. |
| [`OptionalCacheService.clearAllAvailableCaches`](../src/main/java/com/example/workflow/service/redis/OptionalCacheService.java#L156)() | `CacheClearResult` | Xóa/làm trống tất cả khả dụng caches. |

### [src/main/java/com/example/workflow/service/redis/RateLimitService.java:61](../src/main/java/com/example/workflow/service/redis/RateLimitService.java#L61)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RateLimitService.check`](../src/main/java/com/example/workflow/service/redis/RateLimitService.java#L61)(`RateLimitRule rule, String identityKey`) | `RateLimitDecision` | Kiểm tra rate limit. |
| [`RateLimitService.checkFixedWindow`](../src/main/java/com/example/workflow/service/redis/RateLimitService.java#L83)(`RateLimitRule rule, String identityKey`) | `RateLimitDecision` | Kiểm tra fixed window. |
| [`RateLimitService.checkSlidingWindow`](../src/main/java/com/example/workflow/service/redis/RateLimitService.java#L107)(`RateLimitRule rule, String identityKey`) | `RateLimitDecision` | Kiểm tra sliding window. |
| [`RateLimitService.key`](../src/main/java/com/example/workflow/service/redis/RateLimitService.java#L140)(`RateLimitRule rule, String identityKey, String bucket`) | `String` | Thực hiện rate limit. |

### [src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java:23](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisEventIdempotencyService.isCompleted`](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L23)(`String eventType, Object businessKey`) | `boolean` | Kiểm tra điều kiện đã hoàn tất. |
| [`RedisEventIdempotencyService.markCompleted`](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L37)(`String eventType, Object businessKey`) | `void` | Đánh dấu đã hoàn tất. |
| [`RedisEventIdempotencyService.tryAcquire`](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L54)(`String eventType, Object businessKey`) | `boolean` | Thử acquire. |
| [`RedisEventIdempotencyService.key`](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L72)(`String eventType, Object businessKey`) | `String` | Thực hiện redis sự kiện idempotency. |
| [`RedisEventIdempotencyService.normalize`](../src/main/java/com/example/workflow/service/redis/RedisEventIdempotencyService.java#L76)(`String value`) | `String` | Chuẩn hóa redis sự kiện idempotency. |

### [src/main/java/com/example/workflow/service/redis/RedisLockService.java:26](../src/main/java/com/example/workflow/service/redis/RedisLockService.java#L26)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisLockService.tryLock`](../src/main/java/com/example/workflow/service/redis/RedisLockService.java#L26)(`String key, String token, Duration ttl`) | `boolean` | Thử lock. |
| [`RedisLockService.unlockIfOwner`](../src/main/java/com/example/workflow/service/redis/RedisLockService.java#L38)(`String key, String token`) | `void` | Mở khóa if chủ sở hữu. |

### [src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java:80](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L80)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisStreamEventConsumer.poll`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L80)() | `void` | Thăm dò và xử lý redis stream sự kiện consumer. |
| [`RedisStreamEventConsumer.ensureGroup`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L99)() | `void` | Thực hiện group. |
| [`RedisStreamEventConsumer.isGroupAlreadyExists`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L125)(`RuntimeException e`) | `boolean` | Kiểm tra điều kiện group already tồn tại. |
| [`RedisStreamEventConsumer.read`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L130)(`ReadOffset offset`) | `List<MapRecord<String, Object, Object>>` | Đọc redis stream sự kiện consumer. |
| [`RedisStreamEventConsumer.handleRecords`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L139)(`List<MapRecord<String, Object, Object>> records`) | `void` | Xử lý bản ghi. |
| [`RedisStreamEventConsumer.toContext`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L165)(`MapRecord<String, Object, Object> record`) | `RedisStreamEventContext` | Chuyển thành context. |
| [`RedisStreamEventConsumer.acknowledge`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L181)(`MapRecord<String, Object, Object> record`) | `void` | Thực hiện redis stream sự kiện consumer. |
| [`RedisStreamEventConsumer.handleRecord`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L185)(`String type, String payload`) | `void` | Xử lý bản ghi. |
| [`RedisStreamEventConsumer.handleNotificationRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L215)(`String payload`) | `void` | Xử lý thông báo requested. |
| [`RedisStreamEventConsumer.handleOrderConfirmationEmailRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L226)(`String payload`) | `void` | Xử lý đơn hàng confirmation email requested. |
| [`RedisStreamEventConsumer.handleOrderCancellationEmailRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L241)(`String payload`) | `void` | Xử lý đơn hàng cancellation email requested. |
| [`RedisStreamEventConsumer.handlePasswordResetEmailRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L251)(`String payload`) | `void` | Xử lý mật khẩu reset email requested. |
| [`RedisStreamEventConsumer.handleOrderCreated`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L261)(`String payload`) | `void` | Xử lý đơn hàng đã tạo. |
| [`RedisStreamEventConsumer.handleOrderReviewed`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L265)(`String payload`) | `void` | Xử lý đơn hàng đã duyệt. |
| [`RedisStreamEventConsumer.handleOrderAccepted`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L269)(`String payload`) | `void` | Xử lý đơn hàng đã chấp nhận. |
| [`RedisStreamEventConsumer.handleOrderAssigned`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L273)(`String payload`) | `void` | Xử lý đơn hàng được phân công. |
| [`RedisStreamEventConsumer.handleOrderAgreementSubmitted`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L277)(`String payload`) | `void` | Xử lý đơn hàng thỏa thuận đã gửi. |
| [`RedisStreamEventConsumer.handleOrderAgreementReviewed`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L283)(`String payload`) | `void` | Xử lý đơn hàng thỏa thuận đã duyệt. |
| [`RedisStreamEventConsumer.handleWorkflowEmailRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L289)(`String payload`) | `void` | Xử lý workflow email requested. |
| [`RedisStreamEventConsumer.handleGuestOrderCreated`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L316)(`String payload`) | `void` | Xử lý đơn hàng của khách đã tạo. |
| [`RedisStreamEventConsumer.handleOrderDelivered`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L320)(`String payload`) | `void` | Xử lý đơn hàng delivered. |
| [`RedisStreamEventConsumer.handleOrderCancelled`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L324)(`String payload`) | `void` | Xử lý đơn hàng đã hủy. |
| [`RedisStreamEventConsumer.handlePaymentConfirmed`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L328)(`String payload`) | `void` | Xử lý thanh toán confirmed. |
| [`RedisStreamEventConsumer.handlePaymentOpened`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L332)(`String payload`) | `void` | Xử lý thanh toán opened. |
| [`RedisStreamEventConsumer.handleProductionStarted`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L336)(`String payload`) | `void` | Xử lý sản xuất started. |
| [`RedisStreamEventConsumer.handleProductionCheckpointSubmitted`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L340)(`String payload`) | `void` | Xử lý checkpoint sản xuất đã gửi. |
| [`RedisStreamEventConsumer.handleProductionCheckpointReviewed`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L346)(`String payload`) | `void` | Xử lý checkpoint sản xuất đã duyệt. |
| [`RedisStreamEventConsumer.handleOrderReadyToShip`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L352)(`String payload`) | `void` | Xử lý đơn hàng sẵn sàng tới ship. |
| [`RedisStreamEventConsumer.handleOrderShipping`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L356)(`String payload`) | `void` | Xử lý đơn hàng vận chuyển. |
| [`RedisStreamEventConsumer.handleChangeRequestSubmitted`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L360)(`String payload`) | `void` | Xử lý change yêu cầu đã gửi. |
| [`RedisStreamEventConsumer.handleChangeRequestDecided`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L364)(`String payload`) | `void` | Xử lý change yêu cầu decided. |
| [`RedisStreamEventConsumer.handleCacheEvictionRequested`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L368)(`String payload`) | `void` | Xử lý cache eviction requested. |
| [`RedisStreamEventConsumer.readPayload`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L373)(`String payload, Class<T> payloadType`) | `T` | Đọc payload. |
| [`RedisStreamEventConsumer.asString`](../src/main/java/com/example/workflow/service/redis/RedisStreamEventConsumer.java#L381)(`Object value`) | `String` | Thực hiện string. |

### [src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java:28](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L28)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisStreamOperationsService.dlqSize`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L28)() | `long` | Thực hiện kích thước. |
| [`RedisStreamOperationsService.incidents`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L33)(`int requestedLimit`) | `List<WorkflowDlqIncidentDTO>` | Thực hiện redis stream thao tác. |
| [`RedisStreamOperationsService.replay`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L42)(`String recordId`) | `boolean` | Phát lại redis stream thao tác. |
| [`RedisStreamOperationsService.toIncident`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L59)(`MapRecord<String, Object, Object> record`) | `WorkflowDlqIncidentDTO` | Chuyển thành sự cố. |
| [`RedisStreamOperationsService.copyRequired`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L68)(`Map<Object, Object> source, Map<String, String> target, String key`) | `void` | Thực hiện bắt buộc. |
| [`RedisStreamOperationsService.value`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L74)(`Map<Object, Object> values, String key`) | `String` | Thực hiện redis stream thao tác. |
| [`RedisStreamOperationsService.parseInt`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L79)(`String value`) | `int` | Phân tích int. |
| [`RedisStreamOperationsService.parseInstant`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L87)(`String value`) | `Instant` | Phân tích instant. |
| [`RedisStreamOperationsService.limit`](../src/main/java/com/example/workflow/service/redis/RedisStreamOperationsService.java#L95)(`String value, int maxLength`) | `String` | Thực hiện redis stream thao tác. |

### [src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java:46](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L46)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisStreamRetryTemplate.execute`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L46)(`RedisStreamEventContext context, Runnable handler`) | `RetryDecision` | Thực thi redis stream retry template. |
| [`RedisStreamRetryTemplate.retryEnabledFor`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L91)(`String eventType`) | `boolean` | Thử lại enabled cho. |
| [`RedisStreamRetryTemplate.enabledTypes`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L98)() | `Set<String>` | Thực hiện types. |
| [`RedisStreamRetryTemplate.readMetadata`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L108)(`RedisStreamEventContext context`) | `RetryMetadata` | Đọc metadata. |
| [`RedisStreamRetryTemplate.writeMetadata`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L124)(`RedisStreamEventContext context, RetryMetadata metadata`) | `void` | Thực hiện metadata. |
| [`RedisStreamRetryTemplate.clearMetadata`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L138)(`RedisStreamEventContext context`) | `void` | Xóa/làm trống metadata. |
| [`RedisStreamRetryTemplate.publishToDlq`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L146)(`RedisStreamEventContext context, int attempts, RuntimeException error`) | `void` | Phát tới DLQ. |
| [`RedisStreamRetryTemplate.resolveBackoffMs`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L180)(`int attempts`) | `long` | Xác định backoff ms. |
| [`RedisStreamRetryTemplate.parseBackoffMs`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L194)(`String raw`) | `long` | Phân tích backoff ms. |
| [`RedisStreamRetryTemplate.metadataKey`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L203)(`RedisStreamEventContext context`) | `String` | Thực hiện khóa. |
| [`RedisStreamRetryTemplate.normalize`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L210)(`String value`) | `String` | Chuẩn hóa redis stream retry template. |
| [`RedisStreamRetryTemplate.nullToEmpty`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L214)(`String value`) | `String` | Thực hiện tới empty. |
| [`RedisStreamRetryTemplate.RetryMetadata.empty`](../src/main/java/com/example/workflow/service/redis/RedisStreamRetryTemplate.java#L224)() | `RetryMetadata` | Thực hiện retry metadata. |

### [src/main/java/com/example/workflow/util/GuestSessionUtils.java:15](../src/main/java/com/example/workflow/util/GuestSessionUtils.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestSessionUtils.normalize`](../src/main/java/com/example/workflow/util/GuestSessionUtils.java#L15)(`String guestSessionId`) | `String` | Chuẩn hóa phiên khách. |
| [`GuestSessionUtils.invalid`](../src/main/java/com/example/workflow/util/GuestSessionUtils.java#L26)() | `AppException` | Thực hiện phiên khách. |

### [src/main/java/com/example/workflow/util/JsonUtils.java:10](../src/main/java/com/example/workflow/util/JsonUtils.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`JsonUtils.write`](../src/main/java/com/example/workflow/util/JsonUtils.java#L10)(`ObjectMapper objectMapper, Object value, String context`) | `String` | Thực hiện json. |
| [`JsonUtils.read`](../src/main/java/com/example/workflow/util/JsonUtils.java#L18)(`ObjectMapper objectMapper, String value, Class<T> type, String context`) | `T` | Đọc json. |

### [src/main/java/com/example/workflow/util/MoneyUtils.java:14](../src/main/java/com/example/workflow/util/MoneyUtils.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`MoneyUtils.normalize`](../src/main/java/com/example/workflow/util/MoneyUtils.java#L14)(`BigDecimal value`) | `BigDecimal` | Chuẩn hóa money. |
| [`MoneyUtils.nonNegative`](../src/main/java/com/example/workflow/util/MoneyUtils.java#L18)(`BigDecimal value`) | `BigDecimal` | Thực hiện negative. |
| [`MoneyUtils.subtractFloorZero`](../src/main/java/com/example/workflow/util/MoneyUtils.java#L23)(`BigDecimal minuend, BigDecimal subtrahend`) | `BigDecimal` | Thực hiện floor zero. |

### [src/main/java/com/example/workflow/util/PageableUtils.java:10](../src/main/java/com/example/workflow/util/PageableUtils.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PageableUtils.normalize`](../src/main/java/com/example/workflow/util/PageableUtils.java#L10)(`Pageable pageable, int defaultSize, int maxSize`) | `Pageable` | Chuẩn hóa pageable. |

### [src/main/java/com/example/workflow/util/TextNormalizer.java:11](../src/main/java/com/example/workflow/util/TextNormalizer.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`TextNormalizer.optional`](../src/main/java/com/example/workflow/util/TextNormalizer.java#L11)(`String value`) | `String` | Thực hiện text normalizer. |
| [`TextNormalizer.email`](../src/main/java/com/example/workflow/util/TextNormalizer.java#L15)(`String value`) | `String` | Thực hiện text normalizer. |
| [`TextNormalizer.fullName`](../src/main/java/com/example/workflow/util/TextNormalizer.java#L20)(`String lastName, String firstName`) | `String` | Thực hiện tên. |

### [src/main/java/com/example/workflow/util/ThrowableUtils.java:8](../src/main/java/com/example/workflow/util/ThrowableUtils.java#L8)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ThrowableUtils.rootMessage`](../src/main/java/com/example/workflow/util/ThrowableUtils.java#L8)(`Throwable throwable`) | `String` | Thực hiện tin nhắn. |
| [`ThrowableUtils.typedRootMessage`](../src/main/java/com/example/workflow/util/ThrowableUtils.java#L14)(`Throwable throwable`) | `String` | Thực hiện root tin nhắn. |
| [`ThrowableUtils.rootCause`](../src/main/java/com/example/workflow/util/ThrowableUtils.java#L20)(`Throwable throwable`) | `Throwable` | Thực hiện cause. |

### [src/main/java/com/example/workflow/util/UserDisplayNameUtils.java:9](../src/main/java/com/example/workflow/util/UserDisplayNameUtils.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserDisplayNameUtils.fullName`](../src/main/java/com/example/workflow/util/UserDisplayNameUtils.java#L9)(`User user`) | `String` | Thực hiện tên. |
| [`UserDisplayNameUtils.displayName`](../src/main/java/com/example/workflow/util/UserDisplayNameUtils.java#L13)(`User user`) | `String` | Thực hiện tên. |

### [src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java:15](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskContext.getVariable`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L15)(`String name`) | `Object` | Lấy variable. |
| [`WorkflowTaskContext.getString`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L19)(`String name`) | `String` | Lấy string. |
| [`WorkflowTaskContext.requireString`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L24)(`String name`) | `String` | Lấy và đảm bảo tồn tại string. |
| [`WorkflowTaskContext.getLong`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L32)(`String name`) | `Long` | Lấy long. |
| [`WorkflowTaskContext.requireLong`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L47)(`String name`) | `Long` | Lấy và đảm bảo tồn tại long. |
| [`WorkflowTaskContext.isTrue`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L55)(`String name`) | `boolean` | Kiểm tra điều kiện true. |
| [`WorkflowTaskContext.setVariable`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L60)(`String name, Object value`) | `void` | Đặt variable. |
| [`WorkflowTaskContext.processInstanceId`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L64)() | `String` | Xử lý instance ID. |
| [`WorkflowTaskContext.businessKey`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L68)() | `String` | Thực hiện khóa. |
| [`WorkflowTaskContext.activityId`](../src/main/java/com/example/workflow/workflow/WorkflowTaskContext.java#L72)() | `String` | Thực hiện ID. |

### [src/main/java/com/example/workflow/workflow/WorkflowTaskHandler.java:6](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandler.java#L6)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskHandler.taskType`](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandler.java#L6)() | `String` | Thực hiện loại. |
| [`WorkflowTaskHandler.taskTypes`](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandler.java#L8)() | `List<String>` | Thực hiện types. |
| [`WorkflowTaskHandler.handle`](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandler.java#L12)(`WorkflowTaskContext context`) | `void` | Xử lý workflow tác vụ. |

### [src/main/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistry.java:36](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistry.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskHandlerRegistry.getRequired`](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistry.java#L36)(`String taskType`) | `WorkflowTaskHandler` | Lấy bắt buộc. |
| [`WorkflowTaskHandlerRegistry.normalize`](../src/main/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistry.java#L47)(`String taskType`) | `String` | Chuẩn hóa workflow tác vụ handler registry. |

### [src/main/java/com/example/workflow/workflow/handler/CancelExpiredOrderWorkflowTaskHandler.java:21](../src/main/java/com/example/workflow/workflow/handler/CancelExpiredOrderWorkflowTaskHandler.java#L21)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CancelExpiredOrderWorkflowTaskHandler.taskType`](../src/main/java/com/example/workflow/workflow/handler/CancelExpiredOrderWorkflowTaskHandler.java#L21)() | `String` | Thực hiện loại. |
| [`CancelExpiredOrderWorkflowTaskHandler.taskTypes`](../src/main/java/com/example/workflow/workflow/handler/CancelExpiredOrderWorkflowTaskHandler.java#L26)() | `List<String>` | Thực hiện types. |
| [`CancelExpiredOrderWorkflowTaskHandler.handle`](../src/main/java/com/example/workflow/workflow/handler/CancelExpiredOrderWorkflowTaskHandler.java#L31)(`WorkflowTaskContext context`) | `void` | Xử lý cancel hết hạn đơn hàng workflow tác vụ. |

### [src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java:25](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L25)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLifecycleBpmnTest.guestCatalogUsesTheSameReviewAssignmentAndProductionLifecycle`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L25)() | `void` | Kiểm tra kịch bản khách catalog sử dụng the cùng duyệt/đánh giá phân công và sản xuất lifecycle. |
| [`OrderLifecycleBpmnTest.customCodAgreementRunsThroughKcsReworkAndDelivery`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L42)() | `void` | Kiểm tra kịch bản custom cod thỏa thuận runs through kcs làm lại và giao hàng. |
| [`OrderLifecycleBpmnTest.productionCreatesOneParallelPathPerOrderItemNotPerQuantity`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L89)() | `void` | Kiểm tra kịch bản sản xuất creates one parallel path per mặt hàng của đơn hàng không per số lượng. |
| [`OrderLifecycleBpmnTest.onlinePaymentWaitsForOpenedAttemptAndSuccessBeforeProduction`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L108)() | `void` | Kiểm tra kịch bản online thanh toán waits cho opened lần thử và thành công trước sản xuất. |
| [`OrderLifecycleBpmnTest.cancellationMessageInterruptsTheWholeLifecycle`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L138)() | `void` | Kiểm tra kịch bản cancellation tin nhắn interrupts the whole lifecycle. |
| [`OrderLifecycleBpmnTest.assignmentRetryIsIdempotentWhenReviewedEventAlreadyAdvancedIt`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L153)() | `void` | Kiểm tra kịch bản phân công retry is idempotent khi đã duyệt sự kiện already advanced it. |
| [`OrderLifecycleBpmnTest.timeoutThatLosesTheDatabaseRaceStillWaitsForTheDurableAgreementEvent`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L172)() | `void` | Kiểm tra kịch bản timeout that loses the database race still waits cho the durable thỏa thuận sự kiện. |
| [`OrderLifecycleBpmnTest.processEngine`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L200)() | `ProcessEngine` | Kiểm tra kịch bản quy trình engine. |
| [`OrderLifecycleBpmnTest.processEngine`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L204)(`JavaDelegate workflowTaskDelegate`) | `ProcessEngine` | Kiểm tra kịch bản quy trình engine. |
| [`OrderLifecycleBpmnTest.start`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L219)(`ProcessEngine engine, Long orderId, boolean guest, String orderType, String paymentMethod, List<Long> itemIds`) | `ProcessInstance` | Kiểm tra kịch bản start. |
| [`OrderLifecycleBpmnTest.workflow`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L241)(`ProcessEngine engine`) | `OrderWorkflowService` | Kiểm tra kịch bản workflow. |
| [`OrderLifecycleBpmnTest.complete`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L245)(`ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey, Map<String, Object> variables`) | `void` | Kiểm tra kịch bản hoàn tất. |
| [`OrderLifecycleBpmnTest.singleTask`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L255)(`ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey`) | `Task` | Kiểm tra kịch bản single tác vụ. |
| [`OrderLifecycleBpmnTest.tasks`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L262)(`ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey`) | `List<Task>` | Kiểm tra kịch bản tác vụ. |
| [`OrderLifecycleBpmnTest.taskKeys`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L269)(`ProcessEngine engine, ProcessInstance instance`) | `List<String>` | Kiểm tra kịch bản tác vụ khóa. |
| [`OrderLifecycleBpmnTest.subscription`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L278)(`ProcessEngine engine, Long orderId, String messageName`) | `Execution` | Kiểm tra kịch bản subscription. |
| [`OrderLifecycleBpmnTest.correlate`](../src/test/java/com/example/workflow/bpmn/OrderLifecycleBpmnTest.java#L285)(`ProcessEngine engine, Long orderId, String messageName, Map<String, Object> variables`) | `void` | Kiểm tra kịch bản correlate. |

### [src/test/java/com/example/workflow/bpmn/WorkflowTaskDelegateBpmnTest.java:19](../src/test/java/com/example/workflow/bpmn/WorkflowTaskDelegateBpmnTest.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskDelegateBpmnTest.serviceTaskInvokesSharedDelegateAndWritesProcessVariable`](../src/test/java/com/example/workflow/bpmn/WorkflowTaskDelegateBpmnTest.java#L19)() | `void` | Kiểm tra kịch bản service tác vụ invokes shared delegate và writes quy trình variable. |
| [`WorkflowTaskDelegateBpmnTest..taskType`](../src/test/java/com/example/workflow/bpmn/WorkflowTaskDelegateBpmnTest.java#L22)() | `String` | Kiểm tra kịch bản tác vụ loại. |
| [`WorkflowTaskDelegateBpmnTest..handle`](../src/test/java/com/example/workflow/bpmn/WorkflowTaskDelegateBpmnTest.java#L27)(`WorkflowTaskContext context`) | `void` | Kiểm tra kịch bản handle. |

### [src/test/java/com/example/workflow/config/SecurityConfigTest.java:18](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`SecurityConfigTest.mapsKeycloakRealmRolesToExistingAuthorities`](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L18)() | `void` | Kiểm tra kịch bản maps keycloak realm vai trò tới existing quyền. |
| [`SecurityConfigTest.jwtWithoutRealmRolesHasNoAuthorities`](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L40)() | `void` | Kiểm tra kịch bản jwt không có realm vai trò has no quyền. |
| [`SecurityConfigTest.jwtRoleConverterIgnoresBlankAndNonStringRoles`](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L54)() | `void` | Kiểm tra kịch bản jwt vai trò converter ignores blank và non string vai trò. |
| [`SecurityConfigTest.corsConfigurationParsesConfiguredOrigins`](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L71)() | `void` | Kiểm tra kịch bản cors configuration parses configured origins. |
| [`SecurityConfigTest.jwtBuilder`](../src/test/java/com/example/workflow/config/SecurityConfigTest.java#L91)() | `Jwt.Builder` | Kiểm tra kịch bản jwt builder. |

### [src/test/java/com/example/workflow/config/WebSocketAuthInterceptorTest.java:33](../src/test/java/com/example/workflow/config/WebSocketAuthInterceptorTest.java#L33)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WebSocketAuthInterceptorTest.orderTopicSubscriptionDelegatesMembershipCheckUsingAuthenticatedPrincipal`](../src/test/java/com/example/workflow/config/WebSocketAuthInterceptorTest.java#L33)() | `void` | Kiểm tra kịch bản đơn hàng topic subscription delegates membership check using authenticated principal. |
| [`WebSocketAuthInterceptorTest.malformedOrderChatTopicIsRejected`](../src/test/java/com/example/workflow/config/WebSocketAuthInterceptorTest.java#L50)() | `void` | Kiểm tra kịch bản malformed đơn hàng chat topic is rejected. |

### [src/test/java/com/example/workflow/controller/AuthControllerTest.java:27](../src/test/java/com/example/workflow/controller/AuthControllerTest.java#L27)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`AuthControllerTest.loginChecksCredentialWindowAndClearsFailuresAfterSuccess`](../src/test/java/com/example/workflow/controller/AuthControllerTest.java#L27)() | `void` | Kiểm tra kịch bản login checks credential window và clears failures sau thành công. |
| [`AuthControllerTest.loginRecordsFailureOnlyForInvalidCredentials`](../src/test/java/com/example/workflow/controller/AuthControllerTest.java#L42)() | `void` | Kiểm tra kịch bản login bản ghi lỗi chỉ cho invalid credentials. |
| [`AuthControllerTest.loginDoesNotAuthenticateWhenCredentialIsAlreadyBlocked`](../src/test/java/com/example/workflow/controller/AuthControllerTest.java#L58)() | `void` | Kiểm tra kịch bản login does không authenticate khi credential is already blocked. |
| [`AuthControllerTest.loginRequest`](../src/test/java/com/example/workflow/controller/AuthControllerTest.java#L73)(`String username, String password`) | `LoginRequest` | Kiểm tra kịch bản login yêu cầu. |

### [src/test/java/com/example/workflow/controller/CustomRequestControllerContractTest.java:9](../src/test/java/com/example/workflow/controller/CustomRequestControllerContractTest.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestControllerContractTest.allCustomRequestEndpointsRequireUserAuthority`](../src/test/java/com/example/workflow/controller/CustomRequestControllerContractTest.java#L9)() | `void` | Kiểm tra kịch bản tất cả custom yêu cầu endpoints require người dùng quyền. |

### [src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java:17](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerOrderControllerContractTest.reviewAndAssignmentWritesRequireManagerAndIdempotencyKey`](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L17)() | `void` | Kiểm tra kịch bản duyệt/đánh giá và phân công writes require quản lý và idempotency khóa. |
| [`ManagerOrderControllerContractTest.assignmentQueueReadIsManagerOnly`](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L30)() | `void` | Kiểm tra kịch bản phân công queue đọc is quản lý chỉ. |
| [`ManagerOrderControllerContractTest.productionCheckpointWritesRequireCorrectRoleAndIdempotencyKey`](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L39)() | `void` | Kiểm tra kịch bản checkpoint sản xuất writes require correct vai trò và idempotency khóa. |
| [`ManagerOrderControllerContractTest.assertManagerWriteContract`](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L60)(`Method method`) | `void` | Kiểm tra kịch bản assert quản lý ghi contract. |
| [`ManagerOrderControllerContractTest.assertWriteContract`](../src/test/java/com/example/workflow/controller/ManagerOrderControllerContractTest.java#L71)(`Method method, String authority, int headerParameterIndex`) | `void` | Kiểm tra kịch bản assert ghi contract. |

### [src/test/java/com/example/workflow/delegate/WorkflowTaskDelegateTest.java:17](../src/test/java/com/example/workflow/delegate/WorkflowTaskDelegateTest.java#L17)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskDelegateTest.delegatesToHandlerSelectedByTaskTypeVariable`](../src/test/java/com/example/workflow/delegate/WorkflowTaskDelegateTest.java#L17)() | `void` | Kiểm tra kịch bản delegates tới handler selected theo tác vụ loại variable. |

### [src/test/java/com/example/workflow/dto/ManagerReviewRequestValidationTest.java:12](../src/test/java/com/example/workflow/dto/ManagerReviewRequestValidationTest.java#L12)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerReviewRequestValidationTest.rejectionRequiresReason`](../src/test/java/com/example/workflow/dto/ManagerReviewRequestValidationTest.java#L12)() | `void` | Kiểm tra kịch bản rejection requires lý do. |
| [`ManagerReviewRequestValidationTest.approvalMayIncludeStaff`](../src/test/java/com/example/workflow/dto/ManagerReviewRequestValidationTest.java#L19)() | `void` | Kiểm tra kịch bản approval may include nhân viên. |
| [`ManagerReviewRequestValidationTest.rejectionCannotIncludeStaff`](../src/test/java/com/example/workflow/dto/ManagerReviewRequestValidationTest.java#L26)() | `void` | Kiểm tra kịch bản rejection không thể include nhân viên. |

### [src/test/java/com/example/workflow/entity/OrderContactSnapshotTest.java:9](../src/test/java/com/example/workflow/entity/OrderContactSnapshotTest.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderContactSnapshotTest.compatibilityAccessorsUseEmbeddedSnapshot`](../src/test/java/com/example/workflow/entity/OrderContactSnapshotTest.java#L9)() | `void` | Kiểm tra kịch bản compatibility accessors use embedded snapshot. |
| [`OrderContactSnapshotTest.userOrderCanExistWithoutGuestIdentityOrLookupToken`](../src/test/java/com/example/workflow/entity/OrderContactSnapshotTest.java#L28)() | `void` | Kiểm tra kịch bản người dùng đơn hàng can exist không có khách identity hoặc lookup token. |

### [src/test/java/com/example/workflow/entity/ProductionDomainModelTest.java:13](../src/test/java/com/example/workflow/entity/ProductionDomainModelTest.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionDomainModelTest.defaultsAreSafeForOrdersThatDoNotRequireProduction`](../src/test/java/com/example/workflow/entity/ProductionDomainModelTest.java#L13)() | `void` | Kiểm tra kịch bản defaults are an toàn cho đơn hàng that do không require sản xuất. |
| [`ProductionDomainModelTest.relationshipHelpersKeepBothSidesConsistent`](../src/test/java/com/example/workflow/entity/ProductionDomainModelTest.java#L23)() | `void` | Kiểm tra kịch bản relationship helpers keep both sides consistent. |
| [`ProductionDomainModelTest.checkpointAttemptNumberMustBePositive`](../src/test/java/com/example/workflow/entity/ProductionDomainModelTest.java#L47)() | `void` | Kiểm tra kịch bản checkpoint lần thử number must be positive. |

### [src/test/java/com/example/workflow/entity/converter/StringListJsonConverterTest.java:12](../src/test/java/com/example/workflow/entity/converter/StringListJsonConverterTest.java#L12)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`StringListJsonConverterTest.roundTripsImageUrlsInOrder`](../src/test/java/com/example/workflow/entity/converter/StringListJsonConverterTest.java#L12)() | `void` | Kiểm tra kịch bản round trips ảnh URL trong đơn hàng. |
| [`StringListJsonConverterTest.readsNullAndLegacyRawAttachmentSafely`](../src/test/java/com/example/workflow/entity/converter/StringListJsonConverterTest.java#L20)() | `void` | Kiểm tra kịch bản reads null và legacy raw attachment safely. |

### [src/test/java/com/example/workflow/mapper/CustomOrderPriceMappingTest.java:10](../src/test/java/com/example/workflow/mapper/CustomOrderPriceMappingTest.java#L10)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomOrderPriceMappingTest.unknownCustomPriceRemainsNullAndFreeConfirmedPriceRemainsZero`](../src/test/java/com/example/workflow/mapper/CustomOrderPriceMappingTest.java#L10)() | `void` | Kiểm tra kịch bản unknown custom giá remains null và free confirmed giá remains zero. |

### [src/test/java/com/example/workflow/mapper/CustomRequestMapperTest.java:16](../src/test/java/com/example/workflow/mapper/CustomRequestMapperTest.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestMapperTest.mapsEditableDraftWithoutExposingOwnerId`](../src/test/java/com/example/workflow/mapper/CustomRequestMapperTest.java#L16)() | `void` | Kiểm tra kịch bản maps editable bản nháp không có exposing chủ sở hữu ID. |
| [`CustomRequestMapperTest.mapsSubmittedDraftAsReadOnly`](../src/test/java/com/example/workflow/mapper/CustomRequestMapperTest.java#L32)() | `void` | Kiểm tra kịch bản maps đã gửi bản nháp as đọc chỉ. |
| [`CustomRequestMapperTest.request`](../src/test/java/com/example/workflow/mapper/CustomRequestMapperTest.java#L46)(`CustomRequestStatus status`) | `CustomRequest` | Kiểm tra kịch bản yêu cầu. |

### [src/test/java/com/example/workflow/mapper/OrderItemMapperSnapshotTest.java:15](../src/test/java/com/example/workflow/mapper/OrderItemMapperSnapshotTest.java#L15)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderItemMapperSnapshotTest.customItemUsesSnapshotWithoutCatalogVariant`](../src/test/java/com/example/workflow/mapper/OrderItemMapperSnapshotTest.java#L15)() | `void` | Kiểm tra kịch bản custom mặt hàng sử dụng snapshot không có catalog biến thể. |
| [`OrderItemMapperSnapshotTest.customItemReadsLegacySnapshotWithoutExposingParsingToApiClients`](../src/test/java/com/example/workflow/mapper/OrderItemMapperSnapshotTest.java#L40)() | `void` | Kiểm tra kịch bản custom mặt hàng reads legacy snapshot không có exposing parsing tới api clients. |
| [`OrderItemMapperSnapshotTest.catalogItemPrefersImmutableSnapshotAndFallsBackForLegacyRows`](../src/test/java/com/example/workflow/mapper/OrderItemMapperSnapshotTest.java#L52)() | `void` | Kiểm tra kịch bản catalog mặt hàng prefers immutable snapshot và falls back cho legacy rows. |

### [src/test/java/com/example/workflow/mapper/ProductionMapperTest.java:23](../src/test/java/com/example/workflow/mapper/ProductionMapperTest.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionMapperTest.mapsCompleteItemProductionTimeline`](../src/test/java/com/example/workflow/mapper/ProductionMapperTest.java#L23)() | `void` | Kiểm tra kịch bản maps hoàn tất mặt hàng sản xuất tiến độ thời gian. |
| [`ProductionMapperTest.user`](../src/test/java/com/example/workflow/mapper/ProductionMapperTest.java#L83)(`String id, String firstName, String lastName, String username`) | `User` | Kiểm tra kịch bản người dùng. |

### [src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java:29](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L29)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ConsistencyIntegrationTest.dataSource`](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L29)() | `DataSource` | Kiểm tra kịch bản data nguồn. |
| [`ConsistencyIntegrationTest.setup`](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L33)() | `void` | Kiểm tra kịch bản setup. |
| [`ConsistencyIntegrationTest.requestResultAndEventRollbackTogetherThenRetryWorks`](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L48)() | `void` | Kiểm tra kịch bản yêu cầu kết quả và sự kiện rollback together then retry works. |
| [`ConsistencyIntegrationTest.concurrentSameKeyRunsBusinessOperationOnlyOnce`](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L66)() | `void` | Kiểm tra kịch bản concurrent cùng khóa runs business thao tác chỉ once. |
| [`ConsistencyIntegrationTest.redisFailureRemainsRetryableAndKeepsSameEventIdAndPayload`](../src/test/java/com/example/workflow/migration/ConsistencyIntegrationTest.java#L87)() | `void` | Kiểm tra kịch bản redis lỗi remains retryable và giữ lại cùng sự kiện ID và payload. |

### [src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java:19](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DomainExpandMigrationTest.legacyDatabase`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L19)() | `Connection` | Kiểm tra kịch bản legacy database. |
| [`DomainExpandMigrationTest.openConnection`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L36)() | `Connection` | Kiểm tra kịch bản open connection. |
| [`DomainExpandMigrationTest.legacyStatusType`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L40)() | `String` | Kiểm tra kịch bản legacy trạng thái loại. |
| [`DomainExpandMigrationTest.legacyCheckpointStatusType`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L42)() | `String` | Kiểm tra kịch bản legacy checkpoint trạng thái loại. |
| [`DomainExpandMigrationTest.legacyProductionDecisionType`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L46)() | `String` | Kiểm tra kịch bản legacy sản xuất quyết định loại. |
| [`DomainExpandMigrationTest.migrate`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L50)(`Connection connection`) | `void` | Kiểm tra kịch bản migrate. |
| [`DomainExpandMigrationTest.applyChangelog`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L54)(`Connection connection, String changelog`) | `void` | Kiểm tra kịch bản apply changelog. |
| [`DomainExpandMigrationTest.migratesLegacyRowsWithoutGuessingPaymentOrDurationAndIsRepeatable`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L60)() | `void` | Kiểm tra kịch bản migrates legacy rows không có guessing thanh toán hoặc thời lượng và is repeatable. |
| [`DomainExpandMigrationTest.onlyOneActiveOrderCanOccupyStaffAndReleasedAssignmentKeepsHistory`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L96)() | `void` | Kiểm tra kịch bản chỉ one đang hoạt động đơn hàng can occupy nhân viên và đã giải phóng phân công giữ lại lịch sử. |
| [`DomainExpandMigrationTest.paymentProviderReferenceCannotBeConsumedTwice`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L115)() | `void` | Kiểm tra kịch bản thanh toán provider tài liệu tham khảo không thể be consumed twice. |
| [`DomainExpandMigrationTest.productionCheckpointAttemptsImagesAndDecisionAreImmutableByConstraint`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L127)() | `void` | Kiểm tra kịch bản checkpoint sản xuất các lần thử ảnh và quyết định are immutable theo constraint. |
| [`DomainExpandMigrationTest.normalizesLegacyProductionStatusesBeforeNewEnumsAreRead`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L175)() | `void` | Kiểm tra kịch bản normalizes legacy sản xuất statuses trước new enums are đọc. |
| [`DomainExpandMigrationTest.expandsLegacyChangeRequestsAndNormalizesTheirStatuses`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L243)() | `void` | Kiểm tra kịch bản expands legacy change yêu cầu và normalizes their statuses. |
| [`DomainExpandMigrationTest.addsDeliveryContractWithoutMarkingLegacyShippingAsNewHandover`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L289)() | `void` | Kiểm tra kịch bản adds giao hàng contract không có marking legacy vận chuyển as new handover. |
| [`DomainExpandMigrationTest.customContentMigrationBackfillsLegacyNullAttachmentsWithoutRenamingColumns`](../src/test/java/com/example/workflow/migration/DomainExpandMigrationTest.java#L314)() | `void` | Kiểm tra kịch bản custom content migration backfills legacy null attachments không có renaming columns. |

### [src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java:18](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`IntegrationCutoverMigrationTest.removesRetiredInventorySchemaAddsOperationsIndexAndIsRepeatable`](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L18)() | `void` | Kiểm tra kịch bản removes retired inventory schema adds thao tác index và is repeatable. |
| [`IntegrationCutoverMigrationTest.apply`](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L54)(`java.sql.Connection connection`) | `void` | Kiểm tra kịch bản apply. |
| [`IntegrationCutoverMigrationTest.tableExists`](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L64)(`java.sql.Connection connection, String table`) | `boolean` | Kiểm tra kịch bản table tồn tại. |
| [`IntegrationCutoverMigrationTest.columnExists`](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L70)(`java.sql.Connection connection, String table, String column`) | `boolean` | Kiểm tra kịch bản column tồn tại. |
| [`IntegrationCutoverMigrationTest.indexExists`](../src/test/java/com/example/workflow/migration/IntegrationCutoverMigrationTest.java#L76)(`java.sql.Connection connection, String index`) | `boolean` | Kiểm tra kịch bản index tồn tại. |

### [src/test/java/com/example/workflow/migration/LegacyOrderStateMapperTest.java:9](../src/test/java/com/example/workflow/migration/LegacyOrderStateMapperTest.java#L9)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`LegacyOrderStateMapperTest.pendingLegacyPaymentDoesNotImplyManagerApproval`](../src/test/java/com/example/workflow/migration/LegacyOrderStateMapperTest.java#L9)() | `void` | Kiểm tra kịch bản đang chờ legacy thanh toán does không imply quản lý approval. |
| [`LegacyOrderStateMapperTest.shippingDoesNotProveOnlinePaymentWasReceived`](../src/test/java/com/example/workflow/migration/LegacyOrderStateMapperTest.java#L17)() | `void` | Kiểm tra kịch bản vận chuyển does không prove online thanh toán was received. |
| [`LegacyOrderStateMapperTest.warehouseAssignmentCannotBeBlindlyRenamedToProduction`](../src/test/java/com/example/workflow/migration/LegacyOrderStateMapperTest.java#L24)() | `void` | Kiểm tra kịch bản warehouse phân công không thể be blindly renamed tới sản xuất. |

### [src/test/java/com/example/workflow/migration/MySqlConsistencyIntegrationTest.java:13](../src/test/java/com/example/workflow/migration/MySqlConsistencyIntegrationTest.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`MySqlConsistencyIntegrationTest.dataSource`](../src/test/java/com/example/workflow/migration/MySqlConsistencyIntegrationTest.java#L13)() | `DataSource` | Kiểm tra kịch bản data nguồn. |

### [src/test/java/com/example/workflow/migration/MySqlDomainExpandMigrationTest.java:18](../src/test/java/com/example/workflow/migration/MySqlDomainExpandMigrationTest.java#L18)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`MySqlDomainExpandMigrationTest.openConnection`](../src/test/java/com/example/workflow/migration/MySqlDomainExpandMigrationTest.java#L18)() | `Connection` | Kiểm tra kịch bản open connection. |
| [`MySqlDomainExpandMigrationTest.legacyStatusType`](../src/test/java/com/example/workflow/migration/MySqlDomainExpandMigrationTest.java#L23)() | `String` | Kiểm tra kịch bản legacy trạng thái loại. |

### [src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java:32](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L32)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestRepositoryTest.listsMultipleOwnedDraftsByMostRecentlyUpdated`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L32)() | `void` | Kiểm tra kịch bản lists multiple owned drafts theo most recently updated. |
| [`CustomRequestRepositoryTest.ownerQueriesAndSubmitLockDoNotReturnAnotherUsersDraft`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L65)() | `void` | Kiểm tra kịch bản chủ sở hữu queries và submit lock do không return another users bản nháp. |
| [`CustomRequestRepositoryTest.oneOrderCannotBeLinkedToTwoCustomRequests`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L82)() | `void` | Kiểm tra kịch bản one đơn hàng không thể be linked tới two custom yêu cầu. |
| [`CustomRequestRepositoryTest.lifecycleCallbacksPopulateTimestampsAndVersion`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L97)() | `void` | Kiểm tra kịch bản lifecycle callbacks populate timestamps và phiên bản. |
| [`CustomRequestRepositoryTest.draft`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L109)(`String ownerId, String description, LocalDateTime updatedAt`) | `CustomRequest` | Kiểm tra kịch bản bản nháp. |
| [`CustomRequestRepositoryTest.persistUser`](../src/test/java/com/example/workflow/repository/CustomRequestRepositoryTest.java#L119)(`String id, String username, String phone`) | `void` | Kiểm tra kịch bản persist người dùng. |

### [src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java:84](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L84)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestSubmissionAtomicityTest.Config.objectMapper`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L84)() | `ObjectMapper` | Kiểm tra kịch bản object mapper. |
| [`CustomRequestSubmissionAtomicityTest.Config.javaMailSender`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L90)() | `JavaMailSender` | Kiểm tra kịch bản java mail sender. |
| [`CustomRequestSubmissionAtomicityTest.Config.templateEngine`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L95)() | `TemplateEngine` | Kiểm tra kịch bản template engine. |
| [`CustomRequestSubmissionAtomicityTest.Config.messagingTemplate`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L96)() | `SimpMessagingTemplate` | Kiểm tra kịch bản messaging template. |
| [`CustomRequestSubmissionAtomicityTest.setUpSchemaAndData`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L110)() | `void` | Kiểm tra kịch bản set lên schema và data. |
| [`CustomRequestSubmissionAtomicityTest.resetEventPublisher`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L143)() | `void` | Kiểm tra kịch bản reset sự kiện publisher. |
| [`CustomRequestSubmissionAtomicityTest.retryWithSameKeyReturnsSameOrderAndDoesNotDuplicateOutbox`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L148)() | `void` | Kiểm tra kịch bản retry với cùng khóa trả về cùng đơn hàng và does không duplicate outbox. |
| [`CustomRequestSubmissionAtomicityTest.concurrentSubmissionsWithDifferentKeysCreateOnlyOneOrder`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L180)() | `void` | Kiểm tra kịch bản concurrent submissions với different khóa create chỉ one đơn hàng. |
| [`CustomRequestSubmissionAtomicityTest.eventWriteFailureRollsBackOrderDraftAndRequestRecord`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L214)() | `void` | Kiểm tra kịch bản sự kiện ghi lỗi rolls back đơn hàng bản nháp và yêu cầu bản ghi. |
| [`CustomRequestSubmissionAtomicityTest.persistDraft`](../src/test/java/com/example/workflow/repository/CustomRequestSubmissionAtomicityTest.java#L236)() | `CustomRequest` | Kiểm tra kịch bản persist bản nháp. |

### [src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java:100](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L100)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignmentAtomicityTest.Config.mapper`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L100)() | `ObjectMapper` | Kiểm tra kịch bản mapper. |
| [`OrderAssignmentAtomicityTest.Config.assignmentMapper`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L104)() | `OrderAssignmentMapper` | Kiểm tra kịch bản phân công mapper. |
| [`OrderAssignmentAtomicityTest.Config.managerReviewMapper`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L108)() | `ManagerReviewMapper` | Kiểm tra kịch bản quản lý duyệt/đánh giá mapper. |
| [`OrderAssignmentAtomicityTest.Config.productionMapper`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L112)() | `ProductionMapper` | Kiểm tra kịch bản sản xuất mapper. |
| [`OrderAssignmentAtomicityTest.schema`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L137)() | `void` | Kiểm tra kịch bản schema. |
| [`OrderAssignmentAtomicityTest.repeatedManagerAssignmentCreatesOneAssignmentAndOneTransition`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L151)() | `void` | Kiểm tra kịch bản repeated quản lý phân công creates one phân công và one transition. |
| [`OrderAssignmentAtomicityTest.activeStaffCannotBeAssignedToASecondOrder`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L177)() | `void` | Kiểm tra kịch bản đang hoạt động nhân viên không thể be được phân công tới a second đơn hàng. |
| [`OrderAssignmentAtomicityTest.guestCatalogClaimPublishesAcceptedMilestoneOnce`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L200)() | `void` | Kiểm tra kịch bản khách catalog claim publishes đã chấp nhận milestone once. |
| [`OrderAssignmentAtomicityTest.managerReviewWithStaffCommitsDecisionAssignmentAndDeadlineAtomically`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L223)() | `void` | Kiểm tra kịch bản quản lý duyệt/đánh giá với nhân viên commits quyết định phân công và hạn hoàn thành atomically. |
| [`OrderAssignmentAtomicityTest.concurrentManagersCanCreateOnlyOneAssignmentForAnOrder`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L245)() | `void` | Kiểm tra kịch bản concurrent managers can create chỉ one phân công cho an đơn hàng. |
| [`OrderAssignmentAtomicityTest.concurrentOrdersCanOccupyAStaffOnlyOnce`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L271)() | `void` | Kiểm tra kịch bản concurrent đơn hàng can occupy a nhân viên chỉ once. |
| [`OrderAssignmentAtomicityTest.concurrentFinalKcsReviewsTransitionAndReleaseExactlyOnce`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L297)() | `void` | Kiểm tra kịch bản concurrent cuối kcs duyệt/đánh giá transition và release exactly once. |
| [`OrderAssignmentAtomicityTest.attemptProductionReview`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L343)(`Long orderId, Long checkpointId, String key, CountDownLatch ready, CountDownLatch start`) | `boolean` | Kiểm tra kịch bản lần thử sản xuất duyệt/đánh giá. |
| [`OrderAssignmentAtomicityTest.attemptManagerAssignment`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L367)(`Long orderId, String staffId, String key, CountDownLatch ready, CountDownLatch start`) | `boolean` | Kiểm tra kịch bản lần thử quản lý phân công. |
| [`OrderAssignmentAtomicityTest.saveProductionFixture`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L386)() | `ProductionFixture` | Kiểm tra kịch bản save sản xuất fixture. |
| [`OrderAssignmentAtomicityTest.productionItem`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L427)(`Order order, String name`) | `OrderItem` | Kiểm tra kịch bản sản xuất mặt hàng. |
| [`OrderAssignmentAtomicityTest.pendingFinal`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L439)(`OrderItem item, User staff`) | `ProductionCheckpoint` | Kiểm tra kịch bản đang chờ cuối. |
| [`OrderAssignmentAtomicityTest.saveFixture`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L451)(`boolean guest, OrderType type`) | `Fixture` | Kiểm tra kịch bản save fixture. |
| [`OrderAssignmentAtomicityTest.saveFixture`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L455)(`boolean guest, OrderType type, OrderStatus status`) | `Fixture` | Kiểm tra kịch bản save fixture. |
| [`OrderAssignmentAtomicityTest.saveOrder`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L465)(`User owner, OrderType type`) | `Long` | Kiểm tra kịch bản save đơn hàng. |
| [`OrderAssignmentAtomicityTest.saveOrderInCurrentTransaction`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L471)(`User owner, OrderType type, OrderStatus orderStatus`) | `Long` | Kiểm tra kịch bản save đơn hàng trong hiện tại giao dịch. |
| [`OrderAssignmentAtomicityTest.saveUser`](../src/test/java/com/example/workflow/repository/OrderAssignmentAtomicityTest.java#L480)(`String id, Role role, boolean active`) | `User` | Kiểm tra kịch bản save người dùng. |

### [src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java:67](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L67)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderCancellationAtomicityTest.Config.mapper`](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L67)() | `ObjectMapper` | Kiểm tra kịch bản mapper. |
| [`OrderCancellationAtomicityTest.schema`](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L86)() | `void` | Kiểm tra kịch bản schema. |
| [`OrderCancellationAtomicityTest.repeatedRequestReturnsStoredResultWithoutRepeatingBusinessEffects`](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L99)() | `void` | Kiểm tra kịch bản repeated yêu cầu trả về stored kết quả không có repeating business effects. |
| [`OrderCancellationAtomicityTest.sameKeyWithDifferentReasonConflictsWithoutSecondTransition`](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L129)() | `void` | Kiểm tra kịch bản cùng khóa với different lý do conflicts không có second transition. |
| [`OrderCancellationAtomicityTest.saveOrder`](../src/test/java/com/example/workflow/repository/OrderCancellationAtomicityTest.java#L144)(`Double finalPrice, int reputation`) | `Saved` | Kiểm tra kịch bản save đơn hàng. |

### [src/test/java/com/example/workflow/repository/ProductRepositoryAvailabilityTest.java:22](../src/test/java/com/example/workflow/repository/ProductRepositoryAvailabilityTest.java#L22)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductRepositoryAvailabilityTest.productDefaultsToAcceptingOrders`](../src/test/java/com/example/workflow/repository/ProductRepositoryAvailabilityTest.java#L22)() | `void` | Kiểm tra kịch bản sản phẩm defaults tới accepting đơn hàng. |
| [`ProductRepositoryAvailabilityTest.searchPrioritizesProductsAcceptingOrdersInsteadOfLegacyStock`](../src/test/java/com/example/workflow/repository/ProductRepositoryAvailabilityTest.java#L30)() | `void` | Kiểm tra kịch bản search prioritizes sản phẩm accepting đơn hàng instead of legacy stock. |
| [`ProductRepositoryAvailabilityTest.product`](../src/test/java/com/example/workflow/repository/ProductRepositoryAvailabilityTest.java#L51)(`String name, ProductAvailabilityStatus status`) | `Product` | Kiểm tra kịch bản sản phẩm. |

### [src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java:46](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L46)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionRepositoryTest.persistsAssignmentCheckpointImagesAndDecisionAsOneTimeline`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L46)() | `void` | Kiểm tra kịch bản persists phân công checkpoint ảnh và quyết định as one tiến độ thời gian. |
| [`ProductionRepositoryTest.enforcesOneCurrentAssignmentPerOrderItem`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L99)() | `void` | Kiểm tra kịch bản enforces one hiện tại phân công per mặt hàng của đơn hàng. |
| [`ProductionRepositoryTest.enforcesUniqueCheckpointAttemptForItemAndStage`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L110)() | `void` | Kiểm tra kịch bản enforces unique checkpoint lần thử cho mặt hàng và stage. |
| [`ProductionRepositoryTest.assignment`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L120)(`OrderItem item, User staff, User manager`) | `OrderItemAssignment` | Kiểm tra kịch bản phân công. |
| [`ProductionRepositoryTest.checkpoint`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L128)(`OrderItem item, User staff, int attempt`) | `ProductionCheckpoint` | Kiểm tra kịch bản checkpoint. |
| [`ProductionRepositoryTest.persistHandmadeItem`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L137)() | `OrderItem` | Kiểm tra kịch bản persist handmade mặt hàng. |
| [`ProductionRepositoryTest.persistUser`](../src/test/java/com/example/workflow/repository/ProductionRepositoryTest.java#L167)(`String id, String username, String phone, Role role`) | `User` | Kiểm tra kịch bản persist người dùng. |

### [src/test/java/com/example/workflow/repository/TransitionAtomicityTest.java:33](../src/test/java/com/example/workflow/repository/TransitionAtomicityTest.java#L33)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`TransitionAtomicityTest.Config.mapper`](../src/test/java/com/example/workflow/repository/TransitionAtomicityTest.java#L33)() | `ObjectMapper` | Kiểm tra kịch bản mapper. |
| [`TransitionAtomicityTest.schema`](../src/test/java/com/example/workflow/repository/TransitionAtomicityTest.java#L42)() | `void` | Kiểm tra kịch bản schema. |
| [`TransitionAtomicityTest.transitionAuditVersionResultAndCacheEventShareJpaTransaction`](../src/test/java/com/example/workflow/repository/TransitionAtomicityTest.java#L50)() | `void` | Kiểm tra kịch bản transition audit phiên bản kết quả và cache sự kiện share jpa giao dịch. |

### [src/test/java/com/example/workflow/service/CartServiceTest.java:55](../src/test/java/com/example/workflow/service/CartServiceTest.java#L55)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CartServiceTest.addToCartAddsNewItemWhenVariantIsMissing`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L55)() | `void` | Kiểm tra kịch bản add tới giỏ hàng adds new mặt hàng khi biến thể is missing. |
| [`CartServiceTest.addToCartIncreasesQuantityWhenVariantAlreadyExists`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L73)() | `void` | Kiểm tra kịch bản add tới giỏ hàng increases số lượng khi biến thể already tồn tại. |
| [`CartServiceTest.addToCartDoesNotUseLegacyVariantStockForMadeToOrderProduct`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L89)() | `void` | Kiểm tra kịch bản add tới giỏ hàng does không use legacy biến thể stock cho made tới đơn hàng sản phẩm. |
| [`CartServiceTest.addToCartRejectsProductThatIsNotAcceptingOrders`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L104)() | `void` | Kiểm tra kịch bản add tới giỏ hàng từ chối sản phẩm that is không accepting đơn hàng. |
| [`CartServiceTest.getCartByUserIdFiltersDeletedItemsAndRecalculatesTotal`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L123)() | `void` | Kiểm tra kịch bản get giỏ hàng theo người dùng ID filters deleted mặt hàng và recalculates total. |
| [`CartServiceTest.getCartByUserIdUsesProductImageWhenVariantImageIsBlank`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L159)() | `void` | Kiểm tra kịch bản get giỏ hàng theo người dùng ID sử dụng sản phẩm ảnh khi biến thể ảnh is blank. |
| [`CartServiceTest.getCartByUserIdThrowsWhenCartDoesNotExist`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L184)() | `void` | Kiểm tra kịch bản get giỏ hàng theo người dùng ID ném lỗi khi giỏ hàng does không exist. |
| [`CartServiceTest.updateQuantityChangesExistingItem`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L193)() | `void` | Kiểm tra kịch bản update số lượng changes existing mặt hàng. |
| [`CartServiceTest.updateQuantityRemovesItemWhenQuantityIsZero`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L205)() | `void` | Kiểm tra kịch bản update số lượng removes mặt hàng khi số lượng is zero. |
| [`CartServiceTest.updateQuantityThrowsWhenVariantIsNotInCart`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L217)() | `void` | Kiểm tra kịch bản update số lượng ném lỗi khi biến thể is không trong giỏ hàng. |
| [`CartServiceTest.updateQuantityThrowsWhenCartDoesNotExist`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L229)() | `void` | Kiểm tra kịch bản update số lượng ném lỗi khi giỏ hàng does không exist. |
| [`CartServiceTest.removeFromCartRemovesMatchingVariant`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L240)() | `void` | Kiểm tra kịch bản remove từ giỏ hàng removes matching biến thể. |
| [`CartServiceTest.removeFromCartThrowsWhenCartDoesNotExist`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L253)() | `void` | Kiểm tra kịch bản remove từ giỏ hàng ném lỗi khi giỏ hàng does không exist. |
| [`CartServiceTest.user`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L264)(`Long id`) | `User` | Kiểm tra kịch bản người dùng. |
| [`CartServiceTest.cartWithItems`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L271)(`Long id, User user, CartItem... items`) | `Cart` | Kiểm tra kịch bản giỏ hàng với mặt hàng. |
| [`CartServiceTest.product`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L279)(`boolean deleted, String imageUrl`) | `Product` | Kiểm tra kịch bản sản phẩm. |
| [`CartServiceTest.variant`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L286)(`Long id, String name, double price, int quantity, boolean deleted, Product product`) | `ProductVariant` | Kiểm tra kịch bản biến thể. |
| [`CartServiceTest.cartItem`](../src/test/java/com/example/workflow/service/CartServiceTest.java#L296)(`ProductVariant variant, int quantity`) | `CartItem` | Kiểm tra kịch bản giỏ hàng mặt hàng. |

### [src/test/java/com/example/workflow/service/CatalogDurationCalculatorTest.java:14](../src/test/java/com/example/workflow/service/CatalogDurationCalculatorTest.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CatalogDurationCalculatorTest.calculatesApprovedCatalogExamples`](../src/test/java/com/example/workflow/service/CatalogDurationCalculatorTest.java#L14)(`int quantity, int expectedDays`) | `void` | Kiểm tra kịch bản calculates approved catalog examples. |
| [`CatalogDurationCalculatorTest.roundsMakeDaysAndQualityAllowanceSeparately`](../src/test/java/com/example/workflow/service/CatalogDurationCalculatorTest.java#L20)() | `void` | Kiểm tra kịch bản rounds make days và quality allowance separately. |
| [`CatalogDurationCalculatorTest.rejectsMissingOrInvalidMadeDay`](../src/test/java/com/example/workflow/service/CatalogDurationCalculatorTest.java#L25)() | `void` | Kiểm tra kịch bản từ chối missing hoặc invalid made day. |

### [src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java:78](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L78)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChangeRequestAtomicityTest.Config.objectMapper`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L78)() | `ObjectMapper` | Kiểm tra kịch bản object mapper. |
| [`ChangeRequestAtomicityTest.schema`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L97)() | `void` | Kiểm tra kịch bản schema. |
| [`ChangeRequestAtomicityTest.repeatedDecisionWithSameKeyReturnsStoredResultAndPublishesOnce`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L111)() | `void` | Kiểm tra kịch bản repeated quyết định với cùng khóa trả về stored kết quả và publishes once. |
| [`ChangeRequestAtomicityTest.concurrentOppositeDecisionsAllowExactlyOneWinner`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L140)() | `void` | Kiểm tra kịch bản concurrent opposite decisions allow exactly one winner. |
| [`ChangeRequestAtomicityTest.attemptDecision`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L174)(`Fixture fixture, ChangeRequestDecision decision, String reason, String key, CountDownLatch ready, CountDownLatch start`) | `boolean` | Kiểm tra kịch bản lần thử quyết định. |
| [`ChangeRequestAtomicityTest.saveFixture`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L201)() | `Fixture` | Kiểm tra kịch bản save fixture. |
| [`ChangeRequestAtomicityTest.saveUser`](../src/test/java/com/example/workflow/service/ChangeRequestAtomicityTest.java#L255)(`String id, Role role`) | `User` | Kiểm tra kịch bản save người dùng. |

### [src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java:62](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L62)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ChangeRequestServiceTest.setUp`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L62)() | `void` | Kiểm tra kịch bản set lên. |
| [`ChangeRequestServiceTest.ownerCreatesPendingRequestWithoutMutatingOfficialSpec`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L79)() | `void` | Kiểm tra kịch bản chủ sở hữu creates đang chờ yêu cầu không có mutating official đặc tả. |
| [`ChangeRequestServiceTest.assignedStaffAcceptsByCreatingNextSpecVersionOnly`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L96)() | `void` | Kiểm tra kịch bản nhân viên được phân công accepts theo creating next đặc tả phiên bản chỉ. |
| [`ChangeRequestServiceTest.rejectionRequiresReasonAndDoesNotMutateSpec`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L119)() | `void` | Kiểm tra kịch bản rejection requires lý do và does không mutate đặc tả. |
| [`ChangeRequestServiceTest.staleSpecCannotBeAccepted`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L133)() | `void` | Kiểm tra kịch bản stale đặc tả không thể be đã chấp nhận. |
| [`ChangeRequestServiceTest.staleOrImplementedRequestCanStillBeRejectedWithReason`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L147)() | `void` | Kiểm tra kịch bản stale hoặc implemented yêu cầu can still be rejected với lý do. |
| [`ChangeRequestServiceTest.completedItemCannotReceiveOrApplyChange`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L164)() | `void` | Kiểm tra kịch bản đã hoàn tất mặt hàng không thể receive hoặc apply change. |
| [`ChangeRequestServiceTest.immutableFieldsInPayloadAreRejectedInsteadOfSilentlyIgnored`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L176)() | `void` | Kiểm tra kịch bản immutable trường trong payload are rejected instead of silently ignored. |
| [`ChangeRequestServiceTest.inactiveOwnerCannotCreateChangeRequest`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L194)() | `void` | Kiểm tra kịch bản inactive chủ sở hữu không thể create change yêu cầu. |
| [`ChangeRequestServiceTest.stubLockedOrderAndAssignment`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L208)() | `void` | Kiểm tra kịch bản stub locked đơn hàng và phân công. |
| [`ChangeRequestServiceTest.pendingRequest`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L213)() | `ChangeRequest` | Kiểm tra kịch bản đang chờ yêu cầu. |
| [`ChangeRequestServiceTest.order`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L231)(`OrderItem orderItem, User user`) | `Order` | Kiểm tra kịch bản đơn hàng. |
| [`ChangeRequestServiceTest.item`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L243)() | `OrderItem` | Kiểm tra kịch bản mặt hàng. |
| [`ChangeRequestServiceTest.assignment`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L256)() | `OrderAssignment` | Kiểm tra kịch bản phân công. |
| [`ChangeRequestServiceTest.user`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L263)(`String id`) | `User` | Kiểm tra kịch bản người dùng. |
| [`ChangeRequestServiceTest.read`](../src/test/java/com/example/workflow/service/ChangeRequestServiceTest.java#L270)(`String json, String field`) | `String` | Kiểm tra kịch bản đọc. |

### [src/test/java/com/example/workflow/service/CheckoutServiceTest.java:65](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L65)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutServiceTest.setUp`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L65)() | `void` | Kiểm tra kịch bản set lên. |
| [`CheckoutServiceTest.userCheckoutCreatesPendingOrderWithServerSnapshotsAndKeepsUnselectedItem`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L84)() | `void` | Kiểm tra kịch bản người dùng checkout creates đang chờ đơn hàng với server snapshots và giữ lại unselected mặt hàng. |
| [`CheckoutServiceTest.guestCheckoutUsesCodPublishesTokenizedMailEventAndDoesNotCreateNotification`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L124)() | `void` | Kiểm tra kịch bản khách checkout sử dụng cod publishes tokenized mail sự kiện và does không create thông báo. |
| [`CheckoutServiceTest.user`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L154)(`String id, int reputation`) | `User` | Kiểm tra kịch bản người dùng. |
| [`CheckoutServiceTest.cart`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L166)(`User user, String guestSessionId, CartItem... items`) | `Cart` | Kiểm tra kịch bản giỏ hàng. |
| [`CheckoutServiceTest.cartItem`](../src/test/java/com/example/workflow/service/CheckoutServiceTest.java#L176)(`Long variantId, int quantity, double price, double madeDay`) | `CartItem` | Kiểm tra kịch bản giỏ hàng mặt hàng. |

### [src/test/java/com/example/workflow/service/CurrentUserServiceTest.java:35](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L35)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CurrentUserServiceTest.clearSecurityContext`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L35)() | `void` | Kiểm tra kịch bản clear security context. |
| [`CurrentUserServiceTest.requireCurrentUserIdUsesJwtSubjectWithoutQueryingDatabase`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L40)() | `void` | Kiểm tra kịch bản require người dùng hiện tại ID sử dụng jwt subject không có querying database. |
| [`CurrentUserServiceTest.requireCurrentUserLoadsUserByJwtSubject`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L49)() | `void` | Kiểm tra kịch bản require người dùng hiện tại loads người dùng theo jwt subject. |
| [`CurrentUserServiceTest.requireCurrentUserFallsBackToUsernameWhenJwtSubjectIsNotInLocalDatabase`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L60)() | `void` | Kiểm tra kịch bản require người dùng hiện tại falls back tới tên đăng nhập khi jwt subject is không trong local database. |
| [`CurrentUserServiceTest.nonJwtAuthenticationResolvesIdentityFromUsername`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L71)() | `void` | Kiểm tra kịch bản non jwt authentication resolves identity từ tên đăng nhập. |
| [`CurrentUserServiceTest.anonymousAuthenticationIsNotTreatedAsCurrentUser`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L86)() | `void` | Kiểm tra kịch bản anonymous authentication is không treated as người dùng hiện tại. |
| [`CurrentUserServiceTest.authenticate`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L102)(`org.springframework.security.core.Authentication authentication`) | `void` | Kiểm tra kịch bản authenticate. |
| [`CurrentUserServiceTest.jwtAuthentication`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L106)(`String subject, String username, String authority`) | `JwtAuthenticationToken` | Kiểm tra kịch bản jwt authentication. |
| [`CurrentUserServiceTest.user`](../src/test/java/com/example/workflow/service/CurrentUserServiceTest.java#L121)(`String id, String username`) | `User` | Kiểm tra kịch bản người dùng. |

### [src/test/java/com/example/workflow/service/CustomAgreementTimeoutServiceTest.java:25](../src/test/java/com/example/workflow/service/CustomAgreementTimeoutServiceTest.java#L25)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomAgreementTimeoutServiceTest.expiredUnsubmittedCustomOrderUsesSharedCancellationKernel`](../src/test/java/com/example/workflow/service/CustomAgreementTimeoutServiceTest.java#L25)() | `void` | Kiểm tra kịch bản hết hạn unsubmitted custom đơn hàng sử dụng shared cancellation kernel. |
| [`CustomAgreementTimeoutServiceTest.firstValidSubmissionMakesTimeoutNoOpEvenWhenDueHasPassed`](../src/test/java/com/example/workflow/service/CustomAgreementTimeoutServiceTest.java#L43)() | `void` | Kiểm tra kịch bản first valid submission makes timeout no op even khi due has passed. |

### [src/test/java/com/example/workflow/service/CustomRequestServiceTest.java:70](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L70)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CustomRequestServiceTest.setUp`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L70)() | `void` | Kiểm tra kịch bản set lên. |
| [`CustomRequestServiceTest.createDraftUsesJwtOwnerAndDoesNotEmitOrderSideEffects`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L91)() | `void` | Kiểm tra kịch bản create bản nháp sử dụng jwt chủ sở hữu và does không emit đơn hàng side effects. |
| [`CustomRequestServiceTest.updateRejectsStaleVersionWithoutChangingDraft`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L117)() | `void` | Kiểm tra kịch bản update từ chối stale phiên bản không có changing bản nháp. |
| [`CustomRequestServiceTest.submitCreatesOneUnpricedCustomOrderAndImmutableSnapshot`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L133)() | `void` | Kiểm tra kịch bản submit creates one unpriced custom đơn hàng và immutable snapshot. |
| [`CustomRequestServiceTest.submittedDraftReturnsExistingOrderWithoutDuplicatingEvents`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L180)() | `void` | Kiểm tra kịch bản đã gửi bản nháp trả về existing đơn hàng không có duplicating sự kiện. |
| [`CustomRequestServiceTest.deleteLocksOwnedDraftAndUsesExpectedVersion`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L207)() | `void` | Kiểm tra kịch bản delete locks owned bản nháp và sử dụng expected phiên bản. |
| [`CustomRequestServiceTest.ownershipMismatchIsReportedAsNotFound`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L220)() | `void` | Kiểm tra kịch bản ownership mismatch is reported as không found. |
| [`CustomRequestServiceTest.acceptsExactlyFourThousandWordsAndRejectsFourThousandAndOne`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L232)() | `void` | Kiểm tra kịch bản accepts exactly four thousand words và từ chối four thousand và one. |
| [`CustomRequestServiceTest.rejectsRelativeOrNonHttpImageUrls`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L247)() | `void` | Kiểm tra kịch bản từ chối relative hoặc non http ảnh URL. |
| [`CustomRequestServiceTest.softDeletedUserIsRejectedBeforeDraftAccess`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L259)() | `void` | Kiểm tra kịch bản soft deleted người dùng is rejected trước bản nháp access. |
| [`CustomRequestServiceTest.draft`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L273)() | `CustomRequest` | Kiểm tra kịch bản bản nháp. |
| [`CustomRequestServiceTest.owner`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L292)() | `User` | Kiểm tra kịch bản chủ sở hữu. |
| [`CustomRequestServiceTest.stubDurableExecution`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L303)() | `void` | Kiểm tra kịch bản stub durable execution. |
| [`CustomRequestServiceTest.stubDraftSave`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L310)() | `void` | Kiểm tra kịch bản stub bản nháp save. |
| [`CustomRequestServiceTest.stubOrderSave`](../src/test/java/com/example/workflow/service/CustomRequestServiceTest.java#L323)() | `void` | Kiểm tra kịch bản stub đơn hàng save. |

### [src/test/java/com/example/workflow/service/DashboardServiceTest.java:23](../src/test/java/com/example/workflow/service/DashboardServiceTest.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`DashboardServiceTest.getDashboardStatsAggregatesOrderMetricsAndDefaultsNullRevenueToZero`](../src/test/java/com/example/workflow/service/DashboardServiceTest.java#L23)() | `void` | Kiểm tra kịch bản get dashboard stats aggregates đơn hàng metrics và defaults null revenue tới zero. |

### [src/test/java/com/example/workflow/service/GuestPurchaseWorkflowStateServiceTest.java:21](../src/test/java/com/example/workflow/service/GuestPurchaseWorkflowStateServiceTest.java#L21)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`GuestPurchaseWorkflowStateServiceTest.marksGuestOrderStarted`](../src/test/java/com/example/workflow/service/GuestPurchaseWorkflowStateServiceTest.java#L21)() | `void` | Kiểm tra kịch bản marks đơn hàng của khách started. |
| [`GuestPurchaseWorkflowStateServiceTest.marksGuestOrderStartFailureWithBoundedDiagnostic`](../src/test/java/com/example/workflow/service/GuestPurchaseWorkflowStateServiceTest.java#L34)() | `void` | Kiểm tra kịch bản marks đơn hàng của khách start lỗi với bounded diagnostic. |
| [`GuestPurchaseWorkflowStateServiceTest.guestOrder`](../src/test/java/com/example/workflow/service/GuestPurchaseWorkflowStateServiceTest.java#L47)(`Long id`) | `Order` | Kiểm tra kịch bản đơn hàng của khách. |

### [src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java:23](../src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java#L23)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`KeycloakIdentityServiceTest.setUp`](../src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java#L23)() | `void` | Kiểm tra kịch bản set lên. |
| [`KeycloakIdentityServiceTest.updateUserSynchronizesProfileAndApplicationRealmRoleByKeycloakId`](../src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java#L37)() | `void` | Kiểm tra kịch bản update người dùng synchronizes hồ sơ và application realm vai trò theo keycloak ID. |
| [`KeycloakIdentityServiceTest.updateUserProfileDoesNotModifyRealmRoles`](../src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java#L85)() | `void` | Kiểm tra kịch bản update người dùng hồ sơ does không modify realm vai trò. |
| [`KeycloakIdentityServiceTest.resetPasswordUsesKeycloakAdminResetPasswordEndpoint`](../src/test/java/com/example/workflow/service/KeycloakIdentityServiceTest.java#L113)() | `void` | Kiểm tra kịch bản reset mật khẩu sử dụng keycloak quản trị viên reset mật khẩu endpoint. |

### [src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java:59](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L59)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ManagerOrderReviewServiceTest.executeDurableOperation`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L59)() | `void` | Kiểm tra kịch bản execute durable thao tác. |
| [`ManagerOrderReviewServiceTest.approveCustomWithoutStaffCreatesOneFixedConfirmationDeadline`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L66)() | `void` | Kiểm tra kịch bản approve custom không có nhân viên creates one fixed confirmation hạn hoàn thành. |
| [`ManagerOrderReviewServiceTest.rejectUsesSharedCancellationKernelAndRecordsAuthenticatedManager`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L95)() | `void` | Kiểm tra kịch bản reject sử dụng shared cancellation kernel và bản ghi authenticated quản lý. |
| [`ManagerOrderReviewServiceTest.inactiveManagerCannotReview`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L135)() | `void` | Kiểm tra kịch bản inactive quản lý không thể duyệt/đánh giá. |
| [`ManagerOrderReviewServiceTest.order`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L154)(`Long id, OrderType type`) | `Order` | Kiểm tra kịch bản đơn hàng. |
| [`ManagerOrderReviewServiceTest.user`](../src/test/java/com/example/workflow/service/ManagerOrderReviewServiceTest.java#L163)(`String id, Role role`) | `User` | Kiểm tra kịch bản người dùng. |

### [src/test/java/com/example/workflow/service/OrderAccessAndTransitionTest.java:13](../src/test/java/com/example/workflow/service/OrderAccessAndTransitionTest.java#L13)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAccessAndTransitionTest.tokenScopeExpiryRevocationAndRotationAreEnforced`](../src/test/java/com/example/workflow/service/OrderAccessAndTransitionTest.java#L13)() | `void` | Kiểm tra kịch bản token scope expiry revocation và rotation are enforced. |
| [`OrderAccessAndTransitionTest.cancelledOrdersCannotReviveAndCustomersCannotCancelAcceptedOrders`](../src/test/java/com/example/workflow/service/OrderAccessAndTransitionTest.java#L29)() | `void` | Kiểm tra kịch bản đã hủy đơn hàng không thể revive và customers không thể cancel đã chấp nhận đơn hàng. |

### [src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java:53](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L53)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAgreementServiceTest.setUp`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L53)() | `void` | Kiểm tra kịch bản set lên. |
| [`OrderAgreementServiceTest.validCustomSubmissionStopsTimeoutAndCreatesNewVersion`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L62)() | `void` | Kiểm tra kịch bản valid custom submission stops timeout và creates new phiên bản. |
| [`OrderAgreementServiceTest.catalogClientCannotOverrideOfficialPriceOrDuration`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L98)() | `void` | Kiểm tra kịch bản catalog client không thể override official giá hoặc thời lượng. |
| [`OrderAgreementServiceTest.requestChangesKeepsFirstSubmissionTimestampAndDoesNotRestartTimer`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L117)() | `void` | Kiểm tra kịch bản yêu cầu changes giữ lại first submission timestamp và does không restart timer. |
| [`OrderAgreementServiceTest.customOrder`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L153)(`OrderStatus status`) | `Order` | Kiểm tra kịch bản custom đơn hàng. |
| [`OrderAgreementServiceTest.activeAssignment`](../src/test/java/com/example/workflow/service/OrderAgreementServiceTest.java#L171)() | `OrderAssignment` | Kiểm tra kịch bản phân công đang hoạt động. |

### [src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java:62](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L62)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderAssignmentServiceTest.managerAssignmentMovesUserOrderToDiscussingAndCreatesActiveAssignment`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L62)() | `void` | Kiểm tra kịch bản quản lý phân công moves người dùng đơn hàng tới discussing và creates phân công đang hoạt động. |
| [`OrderAssignmentServiceTest.guestCatalogBecomesAcceptedWhenStaffIsAssigned`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L80)() | `void` | Kiểm tra kịch bản khách catalog becomes đã chấp nhận khi nhân viên is được phân công. |
| [`OrderAssignmentServiceTest.busyStaffCannotReceiveAnotherOrder`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L96)() | `void` | Kiểm tra kịch bản busy nhân viên không thể receive another đơn hàng. |
| [`OrderAssignmentServiceTest.staleOrderVersionFailsBeforeLockingStaff`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L113)() | `void` | Kiểm tra kịch bản stale đơn hàng phiên bản fails trước locking nhân viên. |
| [`OrderAssignmentServiceTest.legacyWarehouseAssignmentCannotBeOverwritten`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L126)() | `void` | Kiểm tra kịch bản legacy warehouse phân công không thể be overwritten. |
| [`OrderAssignmentServiceTest.inactiveManagerCannotListAssignableStaff`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L141)() | `void` | Kiểm tra kịch bản inactive quản lý không thể list assignable nhân viên. |
| [`OrderAssignmentServiceTest.staffClaimableQueueUsesPendingAssignmentAndReturnsOrderVersion`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L156)() | `void` | Kiểm tra kịch bản nhân viên claimable queue sử dụng đang chờ phân công và trả về đơn hàng phiên bản. |
| [`OrderAssignmentServiceTest.managerAssignableQueueUsesSamePendingAssignmentContract`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L179)() | `void` | Kiểm tra kịch bản quản lý assignable queue sử dụng cùng đang chờ phân công contract. |
| [`OrderAssignmentServiceTest.prepareAvailable`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L202)(`Order order, User staff`) | `void` | Kiểm tra kịch bản prepare khả dụng. |
| [`OrderAssignmentServiceTest.transitionTo`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L211)(`Order order, OrderStatus status`) | `void` | Kiểm tra kịch bản transition tới. |
| [`OrderAssignmentServiceTest.order`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L220)(`Long id, OrderStatus status, OrderType type, User owner`) | `Order` | Kiểm tra kịch bản đơn hàng. |
| [`OrderAssignmentServiceTest.user`](../src/test/java/com/example/workflow/service/OrderAssignmentServiceTest.java#L230)(`String id, Role role, boolean active`) | `User` | Kiểm tra kịch bản người dùng. |

### [src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java:57](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L57)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderCancellationServiceTest.appliesEveryCancellationPenaltyBoundary`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L57)(`double finalPrice, int expected`) | `void` | Kiểm tra kịch bản applies every cancellation penalty boundary. |
| [`OrderCancellationServiceTest.customWithoutOfficialPriceHasNoPenalty`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L63)() | `void` | Kiểm tra kịch bản custom không có official giá has no penalty. |
| [`OrderCancellationServiceTest.cancellationCommitsPenaltyVoucherAssignmentHistoryAndOutboxOnce`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L68)() | `void` | Kiểm tra kịch bản cancellation commits penalty voucher phân công lịch sử và outbox once. |
| [`OrderCancellationServiceTest.insufficientReputationLeavesEveryCancellationEffectUntouched`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L92)() | `void` | Kiểm tra kịch bản insufficient reputation leaves every cancellation effect untouched. |
| [`OrderCancellationServiceTest.alreadyCancelledOrderNeverRepeatsSideEffects`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L107)() | `void` | Kiểm tra kịch bản already đã hủy đơn hàng never repeats side effects. |
| [`OrderCancellationServiceTest.userCannotCancelAcceptedOrder`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L125)() | `void` | Kiểm tra kịch bản người dùng không thể cancel đã chấp nhận đơn hàng. |
| [`OrderCancellationServiceTest.guestCancellationUsesScopedTokenAndNeverTouchesReputation`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L137)() | `void` | Kiểm tra kịch bản khách cancellation sử dụng scoped token và never touches reputation. |
| [`OrderCancellationServiceTest.staleVersionFailsBeforeAnyFinancialEffect`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L154)() | `void` | Kiểm tra kịch bản stale phiên bản fails trước any financial effect. |
| [`OrderCancellationServiceTest.anotherUserCannotCancelOwnersOrder`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L167)() | `void` | Kiểm tra kịch bản another người dùng không thể cancel owners đơn hàng. |
| [`OrderCancellationServiceTest.command`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L177)(`CancellationSource source, String actor`) | `OrderCancellationService.CancellationCommand` | Kiểm tra kịch bản command. |
| [`OrderCancellationServiceTest.cancellableOrder`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L181)(`User owner, Double finalPrice`) | `Order` | Kiểm tra kịch bản cancellable đơn hàng. |
| [`OrderCancellationServiceTest.user`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L191)(`String id, int reputation`) | `User` | Kiểm tra kịch bản người dùng. |
| [`OrderCancellationServiceTest.markCancelled`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L198)(`Order order`) | `Order` | Kiểm tra kịch bản mark đã hủy. |
| [`OrderCancellationServiceTest.markCancelled`](../src/test/java/com/example/workflow/service/OrderCancellationServiceTest.java#L202)(`Order order, CancellationSource source`) | `Order` | Kiểm tra kịch bản mark đã hủy. |

### [src/test/java/com/example/workflow/service/OrderChatServiceTest.java:40](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderChatServiceTest.setUp`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L40)() | `void` | Kiểm tra kịch bản set lên. |
| [`OrderChatServiceTest.assignmentRetryReturnsTheSameThread`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L46)() | `void` | Kiểm tra kịch bản phân công retry trả về the cùng luồng chat. |
| [`OrderChatServiceTest.managerOrOtherUserCannotSubscribeToOrderChat`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L59)() | `void` | Kiểm tra kịch bản quản lý hoặc other người dùng không thể subscribe tới đơn hàng chat. |
| [`OrderChatServiceTest.guestOrderNeverGetsChatThread`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L68)() | `void` | Kiểm tra kịch bản đơn hàng của khách never gets chat luồng chat. |
| [`OrderChatServiceTest.sendUsesJwtParticipantAndDoesNotReloadOrderOrUser`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L78)() | `void` | Kiểm tra kịch bản send sử dụng jwt participant và does không reload đơn hàng hoặc người dùng. |
| [`OrderChatServiceTest.assignedOrder`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L107)() | `Order` | Kiểm tra kịch bản được phân công đơn hàng. |
| [`OrderChatServiceTest.thread`](../src/test/java/com/example/workflow/service/OrderChatServiceTest.java#L119)() | `OrderChatThread` | Kiểm tra kịch bản luồng chat. |

### [src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java:73](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L73)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderDeliveryAtomicityTest.Config.objectMapper`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L73)() | `ObjectMapper` | Kiểm tra kịch bản object mapper. |
| [`OrderDeliveryAtomicityTest.schema`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L93)() | `void` | Kiểm tra kịch bản schema. |
| [`OrderDeliveryAtomicityTest.repeatedReceiptWithSameKeyReturnsStoredResultAndAwardsOnce`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L105)() | `void` | Kiểm tra kịch bản repeated biên nhận với cùng khóa trả về stored kết quả và awards once. |
| [`OrderDeliveryAtomicityTest.concurrentReceiptRequestsAllowOneDeliveryTransition`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L128)() | `void` | Kiểm tra kịch bản concurrent biên nhận yêu cầu allow one giao hàng transition. |
| [`OrderDeliveryAtomicityTest.attempt`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L158)(`Fixture fixture, String key, CountDownLatch ready, CountDownLatch start`) | `boolean` | Kiểm tra kịch bản lần thử. |
| [`OrderDeliveryAtomicityTest.saveFixture`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L170)() | `Fixture` | Kiểm tra kịch bản save fixture. |
| [`OrderDeliveryAtomicityTest.receipt`](../src/test/java/com/example/workflow/service/OrderDeliveryAtomicityTest.java#L209)(`Long orderVersion, Long itemId`) | `ReceiptConfirmRequest` | Kiểm tra kịch bản biên nhận. |

### [src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java:54](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L54)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderDeliveryServiceTest.setUp`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L54)() | `void` | Kiểm tra kịch bản set lên. |
| [`OrderDeliveryServiceTest.historicalAssignedStaffCanHandoverEveryItemAtomically`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L62)() | `void` | Kiểm tra kịch bản historical nhân viên được phân công can handover every mặt hàng atomically. |
| [`OrderDeliveryServiceTest.handoverRejectsIncompleteItemListWithoutChangingOrder`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L92)() | `void` | Kiểm tra kịch bản handover từ chối incomplete mặt hàng list không có changing đơn hàng. |
| [`OrderDeliveryServiceTest.unacceptedMismatchReturnsComparisonWithoutPersistingReceipt`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L113)() | `void` | Kiểm tra kịch bản unaccepted mismatch trả về comparison không có persisting biên nhận. |
| [`OrderDeliveryServiceTest.matchedUserReceiptDeliversAndAwardsReputationOnceInTransaction`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L135)() | `void` | Kiểm tra kịch bản matched người dùng biên nhận delivers và awards reputation once trong giao dịch. |
| [`OrderDeliveryServiceTest.guestReceiptNeverAwardsReputation`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L161)() | `void` | Kiểm tra kịch bản khách biên nhận never awards reputation. |
| [`OrderDeliveryServiceTest.legacyShippingRowMayUseOrderedQuantityFallback`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L183)() | `void` | Kiểm tra kịch bản legacy vận chuyển row may use ordered số lượng fallback. |
| [`OrderDeliveryServiceTest.order`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L198)(`OrderStatus status, User owner, OrderItem... items`) | `Order` | Kiểm tra kịch bản đơn hàng. |
| [`OrderDeliveryServiceTest.item`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L210)(`Long id, int quantity`) | `OrderItem` | Kiểm tra kịch bản mặt hàng. |
| [`OrderDeliveryServiceTest.user`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L218)(`String id`) | `User` | Kiểm tra kịch bản người dùng. |
| [`OrderDeliveryServiceTest.receipt`](../src/test/java/com/example/workflow/service/OrderDeliveryServiceTest.java#L226)(`Long version, Long itemId, int quantity, boolean acceptMismatch`) | `ReceiptConfirmRequest` | Kiểm tra kịch bản biên nhận. |

### [src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java:60](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L60)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLifecycleEventHandlerTest.setUp`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L60)() | `void` | Kiểm tra kịch bản set lên. |
| [`OrderLifecycleEventHandlerTest.userCatalogOrderCreatedRoutesSideEffectsFromParentEvent`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L77)() | `void` | Kiểm tra kịch bản người dùng catalog đơn hàng đã tạo routes side effects từ parent sự kiện. |
| [`OrderLifecycleEventHandlerTest.customOrderCreatedUsesItemSnapshot`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L99)() | `void` | Kiểm tra kịch bản custom đơn hàng đã tạo sử dụng mặt hàng snapshot. |
| [`OrderLifecycleEventHandlerTest.customOrderCreatedReadsLegacySpecSnapshot`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L117)() | `void` | Kiểm tra kịch bản custom đơn hàng đã tạo reads legacy đặc tả snapshot. |
| [`OrderLifecycleEventHandlerTest.orderAcceptedEmailsOwnerAndOnlyNotifiesSystemUser`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L135)() | `void` | Kiểm tra kịch bản đơn hàng đã chấp nhận emails chủ sở hữu và chỉ notifies system người dùng. |
| [`OrderLifecycleEventHandlerTest.guestCatalogOrderStartsMandatoryAssignmentWorkflow`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L153)() | `void` | Kiểm tra kịch bản khách catalog đơn hàng starts mandatory phân công workflow. |
| [`OrderLifecycleEventHandlerTest.orderAssignedAdvancesWorkflowForCurrentAssignment`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L183)() | `void` | Kiểm tra kịch bản đơn hàng được phân công advances workflow cho hiện tại phân công. |
| [`OrderLifecycleEventHandlerTest.guestAcceptedEmailContainsSignedLinkAndCatalogSnapshot`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L200)() | `void` | Kiểm tra kịch bản khách đã chấp nhận email contains đã ký link và catalog snapshot. |
| [`OrderLifecycleEventHandlerTest.orderCancelledRoutesOwnerStaffAdminAndRelevantCache`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L224)() | `void` | Kiểm tra kịch bản đơn hàng đã hủy routes chủ sở hữu nhân viên quản trị viên và relevant cache. |
| [`OrderLifecycleEventHandlerTest.paymentConfirmedCorrelatesWorkflowAndNotifiesAssignedStaffOnly`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L262)() | `void` | Kiểm tra kịch bản thanh toán confirmed correlates workflow và notifies nhân viên được phân công chỉ. |
| [`OrderLifecycleEventHandlerTest.initialShapeForUserCompletesItemTaskThenEmailsAndNotifiesOwner`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L281)() | `void` | Kiểm tra kịch bản tạo hình ban đầu cho người dùng completes mặt hàng tác vụ then emails và notifies chủ sở hữu. |
| [`OrderLifecycleEventHandlerTest.initialShapeForGuestEmailsSignedLinkWithoutCreatingAccountNotification`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L308)() | `void` | Kiểm tra kịch bản tạo hình ban đầu cho khách emails đã ký link không có creating account thông báo. |
| [`OrderLifecycleEventHandlerTest.reworkReviewCompletesKcsTaskAndNotifiesAssignedStaff`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L330)() | `void` | Kiểm tra kịch bản làm lại duyệt/đánh giá completes kcs tác vụ và notifies nhân viên được phân công. |
| [`OrderLifecycleEventHandlerTest.readyToShipEmailsAndNotifiesUserWithoutAdvancingToShipping`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L350)() | `void` | Kiểm tra kịch bản sẵn sàng tới ship emails và notifies người dùng không có advancing tới vận chuyển. |
| [`OrderLifecycleEventHandlerTest.userOrder`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L369)() | `Order` | Kiểm tra kịch bản người dùng đơn hàng. |
| [`OrderLifecycleEventHandlerTest.guestOrder`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L379)() | `Order` | Kiểm tra kịch bản đơn hàng của khách. |
| [`OrderLifecycleEventHandlerTest.staff`](../src/test/java/com/example/workflow/service/OrderLifecycleEventHandlerTest.java#L388)(`String id`) | `User` | Kiểm tra kịch bản nhân viên. |

### [src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java:14](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderLookupTokenServiceTest.signedReadTokenIsOrderBoundReadOnlyAndDoesNotRotateCheckoutToken`](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L14)() | `void` | Kiểm tra kịch bản token đọc có chữ ký is đơn hàng bound đọc chỉ và does không rotate checkout token. |
| [`OrderLookupTokenServiceTest.order`](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L45)(`Long id`) | `com.example.workflow.entity.Order` | Kiểm tra kịch bản đơn hàng. |
| [`OrderLookupTokenServiceTest.issuesUniqueTokenAndPersistsOnlyHash`](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L51)() | `void` | Kiểm tra kịch bản issues unique token và persists chỉ hash. |
| [`OrderLookupTokenServiceTest.revokeInvalidatesScopedTokenWithoutKeepingRevocationTimestamp`](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L65)() | `void` | Kiểm tra kịch bản revoke invalidates scoped token không có keeping revocation timestamp. |
| [`OrderLookupTokenServiceTest.masksEmailWithoutExposingLocalPart`](../src/test/java/com/example/workflow/service/OrderLookupTokenServiceTest.java#L82)() | `void` | Kiểm tra kịch bản masks email không có exposing local part. |

### [src/test/java/com/example/workflow/service/OrderReorderServiceTest.java:40](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderReorderServiceTest.setUp`](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L40)() | `void` | Kiểm tra kịch bản set lên. |
| [`OrderReorderServiceTest.catalogReorderAddsOnlyVariantsStillAcceptingOrders`](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L48)() | `void` | Kiểm tra kịch bản catalog reorder adds chỉ biến thể still accepting đơn hàng. |
| [`OrderReorderServiceTest.customReorderCreatesEditableDraftFromSnapshotWithoutCopyingWorkflowState`](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L64)() | `void` | Kiểm tra kịch bản custom reorder creates editable bản nháp từ snapshot không có copying workflow trạng thái. |
| [`OrderReorderServiceTest.sourceOrder`](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L95)(`OrderType type, OrderItem... items`) | `Order` | Kiểm tra kịch bản nguồn đơn hàng. |
| [`OrderReorderServiceTest.catalogItem`](../src/test/java/com/example/workflow/service/OrderReorderServiceTest.java#L108)(`Long itemId, Long variantId, ProductAvailabilityStatus status`) | `OrderItem` | Kiểm tra kịch bản catalog mặt hàng. |

### [src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java:33](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L33)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OrderWorkflowServiceTest.startsExactlyOneLifecycleWithOrderBusinessKey`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L33)() | `void` | Kiểm tra kịch bản starts exactly one lifecycle với đơn hàng business khóa. |
| [`OrderWorkflowServiceTest.replayedOrderCreatedReusesTheActiveLifecycle`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L55)() | `void` | Kiểm tra kịch bản replayed đơn hàng đã tạo reuses the đang hoạt động lifecycle. |
| [`OrderWorkflowServiceTest.completesExistingManagerReviewTaskWithDecisionVariables`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L71)() | `void` | Kiểm tra kịch bản completes existing quản lý duyệt/đánh giá tác vụ với quyết định variables. |
| [`OrderWorkflowServiceTest.completesDeployedGuestAssignmentTaskBeforeTryingMessageCorrelation`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L85)() | `void` | Kiểm tra kịch bản completes deployed khách phân công tác vụ trước trying tin nhắn correlation. |
| [`OrderWorkflowServiceTest.completesLegacyGuestAssignmentTaskDuringDeploymentTransition`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L96)() | `void` | Kiểm tra kịch bản completes legacy khách phân công tác vụ during deployment transition. |
| [`OrderWorkflowServiceTest.correlatesFutureOrderAssignedMessageWhenNoAssignmentTaskExists`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L107)() | `void` | Kiểm tra kịch bản correlates future đơn hàng được phân công tin nhắn khi no phân công tác vụ tồn tại. |
| [`OrderWorkflowServiceTest.correlatesPaymentOpenedWithAttemptAndAbsoluteDueAt`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L125)() | `void` | Kiểm tra kịch bản correlates thanh toán opened với lần thử và absolute due tại. |
| [`OrderWorkflowServiceTest.completesOnlyTheProductionStartTaskForTheOrder`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L144)() | `void` | Kiểm tra kịch bản completes chỉ the sản xuất start tác vụ cho the đơn hàng. |
| [`OrderWorkflowServiceTest.completesInitialShapeTaskForTheExactOrderItem`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L159)() | `void` | Kiểm tra kịch bản completes tạo hình ban đầu tác vụ cho the exact mặt hàng của đơn hàng. |
| [`OrderWorkflowServiceTest.completesFinalKcsTaskWithReworkBranchForTheExactOrderItem`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L176)() | `void` | Kiểm tra kịch bản completes cuối kcs tác vụ với làm lại branch cho the exact mặt hàng của đơn hàng. |
| [`OrderWorkflowServiceTest.correlatesReadyToShipOnlyAfterProductionCompletion`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L193)() | `void` | Kiểm tra kịch bản correlates sẵn sàng tới ship chỉ sau sản xuất hoàn tất. |
| [`OrderWorkflowServiceTest.stubTaskQuery`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L208)(`Long orderId, String definitionKey, List<Task> tasks`) | `void` | Kiểm tra kịch bản stub tác vụ query. |
| [`OrderWorkflowServiceTest.stubItemTaskQuery`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L216)(`Long orderId, Long orderItemId, String definitionKey, List<Task> tasks`) | `void` | Kiểm tra kịch bản stub mặt hàng tác vụ query. |
| [`OrderWorkflowServiceTest.stubAssignmentTaskQueries`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L227)(`Long orderId, List<Task> lifecycleTasks, List<Task> guestTasks, List<Task> legacyTasks`) | `void` | Kiểm tra kịch bản stub phân công tác vụ queries. |
| [`OrderWorkflowServiceTest.taskQuery`](../src/test/java/com/example/workflow/service/OrderWorkflowServiceTest.java#L239)(`Long orderId, String definitionKey, List<Task> tasks`) | `TaskQuery` | Kiểm tra kịch bản tác vụ query. |

### [src/test/java/com/example/workflow/service/PasswordServiceTest.java:61](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L61)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PasswordServiceTest.setUp`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L61)() | `void` | Kiểm tra kịch bản set lên. |
| [`PasswordServiceTest.requestPasswordResetCreatesSingleUseTokenAndSendsEmail`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L66)() | `void` | Kiểm tra kịch bản yêu cầu mật khẩu reset creates single use token và sends email. |
| [`PasswordServiceTest.requestPasswordResetDoesNotRevealMissingAccounts`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L98)() | `void` | Kiểm tra kịch bản yêu cầu mật khẩu reset does không reveal missing accounts. |
| [`PasswordServiceTest.resetPasswordUpdatesKeycloakAndConsumesToken`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L109)() | `void` | Kiểm tra kịch bản reset mật khẩu updates keycloak và consumes token. |
| [`PasswordServiceTest.resetPasswordRejectsExpiredTokenAndConsumesIt`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L122)() | `void` | Kiểm tra kịch bản reset mật khẩu từ chối hết hạn token và consumes it. |
| [`PasswordServiceTest.resetPasswordRejectsPasswordConfirmationMismatchBeforeTokenLookup`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L140)() | `void` | Kiểm tra kịch bản reset mật khẩu từ chối mật khẩu confirmation mismatch trước token lookup. |
| [`PasswordServiceTest.changeCurrentUserPasswordAuthenticatesOldPasswordBeforeResetting`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L152)() | `void` | Kiểm tra kịch bản change người dùng hiện tại mật khẩu authenticates old mật khẩu trước resetting. |
| [`PasswordServiceTest.changeCurrentUserPasswordRejectsUnchangedPassword`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L165)() | `void` | Kiểm tra kịch bản change người dùng hiện tại mật khẩu từ chối unchanged mật khẩu. |
| [`PasswordServiceTest.user`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L178)(`String id, String username, String email`) | `User` | Kiểm tra kịch bản người dùng. |
| [`PasswordServiceTest.token`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L188)(`User user, String tokenHash, LocalDateTime expiresAt`) | `PasswordResetToken` | Kiểm tra kịch bản token. |
| [`PasswordServiceTest.hashToken`](../src/test/java/com/example/workflow/service/PasswordServiceTest.java#L196)(`String token`) | `String` | Kiểm tra kịch bản hash token. |

### [src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java:75](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L75)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`PaymentPhase9ServiceTest.setUp`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L75)() | `void` | Kiểm tra kịch bản set lên. |
| [`PaymentPhase9ServiceTest.validWebhookMarksPaymentPaidButKeepsOrderAccepted`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L95)() | `void` | Kiểm tra kịch bản valid webhook marks thanh toán paid but giữ lại đơn hàng đã chấp nhận. |
| [`PaymentPhase9ServiceTest.duplicateWebhookIsAcknowledgedWithoutPublishingAgain`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L121)() | `void` | Kiểm tra kịch bản duplicate webhook is acknowledged không có publishing again. |
| [`PaymentPhase9ServiceTest.invalidOrExpiredSignatureNeverReadsPaymentData`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L140)() | `void` | Kiểm tra kịch bản invalid hoặc hết hạn chữ ký never reads thanh toán data. |
| [`PaymentPhase9ServiceTest.lateSuccessNeverRevivesCancelledOrderAndRequiresReconciliation`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L159)() | `void` | Kiểm tra kịch bản late thành công never revives đã hủy đơn hàng và requires reconciliation. |
| [`PaymentPhase9ServiceTest.timeoutCannotCancelAfterPaymentWasCommitted`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L185)() | `void` | Kiểm tra kịch bản timeout không thể cancel sau thanh toán was committed. |
| [`PaymentPhase9ServiceTest.codStartCreatesDeadlinesAndRepeatedStartKeepsOriginalValues`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L203)() | `void` | Kiểm tra kịch bản cod start creates deadlines và repeated start giữ lại original values. |
| [`PaymentPhase9ServiceTest.assignedStaffCannotStartOnlineOrderBeforePaid`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L250)() | `void` | Kiểm tra kịch bản nhân viên được phân công không thể start online đơn hàng trước paid. |
| [`PaymentPhase9ServiceTest.cancelledOrderCannotBeStartedEvenIfAStaleAssignmentIsObserved`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L278)() | `void` | Kiểm tra kịch bản đã hủy đơn hàng không thể be started even if a stale phân công is observed. |
| [`PaymentPhase9ServiceTest.openPaymentUsesServerAmountAndReturnsExistingAttemptWithoutExtendingDueAt`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L307)() | `void` | Kiểm tra kịch bản open thanh toán sử dụng server số tiền và trả về existing lần thử không có extending due tại. |
| [`PaymentPhase9ServiceTest.codOrderNeverCreatesAnOnlinePaymentAttempt`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L350)() | `void` | Kiểm tra kịch bản cod đơn hàng never creates an online thanh toán lần thử. |
| [`PaymentPhase9ServiceTest.pendingAttempt`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L376)(`LocalDateTime dueAt`) | `PaymentAttempt` | Kiểm tra kịch bản đang chờ lần thử. |
| [`PaymentPhase9ServiceTest.acceptedOnlineOrder`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L389)(`PaymentStatus paymentStatus`) | `Order` | Kiểm tra kịch bản đã chấp nhận online đơn hàng. |
| [`PaymentPhase9ServiceTest.acceptedCodOrder`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L396)() | `Order` | Kiểm tra kịch bản đã chấp nhận cod đơn hàng. |
| [`PaymentPhase9ServiceTest.baseAcceptedOrder`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L404)() | `Order` | Kiểm tra kịch bản cơ sở đã chấp nhận đơn hàng. |
| [`PaymentPhase9ServiceTest.orderItem`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L414)(`long id, int duration`) | `OrderItem` | Kiểm tra kịch bản mặt hàng của đơn hàng. |
| [`PaymentPhase9ServiceTest.activeAssignment`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L421)(`String staffId`) | `OrderAssignment` | Kiểm tra kịch bản phân công đang hoạt động. |
| [`PaymentPhase9ServiceTest.webhookBody`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L430)(`long transactionId, String code, String amount`) | `byte[]` | Kiểm tra kịch bản webhook nội dung. |
| [`PaymentPhase9ServiceTest.signedHeaders`](../src/test/java/com/example/workflow/service/PaymentPhase9ServiceTest.java#L443)(`byte[] body, long timestamp`) | `HttpHeaders` | Kiểm tra kịch bản đã ký headers. |

### [src/test/java/com/example/workflow/service/ProductReviewServiceTest.java:55](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L55)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductReviewServiceTest.setUp`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L55)() | `void` | Kiểm tra kịch bản set lên. |
| [`ProductReviewServiceTest.createForOrderItemCreatesReviewForDeliveredOwner`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L69)() | `void` | Kiểm tra kịch bản create cho mặt hàng của đơn hàng creates duyệt/đánh giá cho delivered chủ sở hữu. |
| [`ProductReviewServiceTest.createForOrderItemRejectsOtherUserOrderItem`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L107)() | `void` | Kiểm tra kịch bản create cho mặt hàng của đơn hàng từ chối other người dùng mặt hàng của đơn hàng. |
| [`ProductReviewServiceTest.createForOrderItemRejectsEmptyReviewContent`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L124)() | `void` | Kiểm tra kịch bản create cho mặt hàng của đơn hàng từ chối empty duyệt/đánh giá content. |
| [`ProductReviewServiceTest.createForOrderItemRejectsDuplicateReview`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L142)() | `void` | Kiểm tra kịch bản create cho mặt hàng của đơn hàng từ chối duplicate duyệt/đánh giá. |
| [`ProductReviewServiceTest.user`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L160)(`String id`) | `User` | Kiểm tra kịch bản người dùng. |
| [`ProductReviewServiceTest.deliveredOrder`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L170)(`Long id, User user`) | `Order` | Kiểm tra kịch bản delivered đơn hàng. |
| [`ProductReviewServiceTest.orderItem`](../src/test/java/com/example/workflow/service/ProductReviewServiceTest.java#L178)(`Long id, Order order`) | `OrderItem` | Kiểm tra kịch bản mặt hàng của đơn hàng. |

### [src/test/java/com/example/workflow/service/ProductServiceTest.java:40](../src/test/java/com/example/workflow/service/ProductServiceTest.java#L40)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductServiceTest.managerCannotSetMissingNonFiniteOrShortDuration`](../src/test/java/com/example/workflow/service/ProductServiceTest.java#L40)() | `void` | Kiểm tra kịch bản quản lý không thể set missing non finite hoặc short thời lượng. |
| [`ProductServiceTest.fullUpdateFromLegacyClientKeepsExistingAvailabilityWhenFieldIsMissing`](../src/test/java/com/example/workflow/service/ProductServiceTest.java#L53)() | `void` | Kiểm tra kịch bản full update từ legacy client giữ lại existing availability khi trường is missing. |

### [src/test/java/com/example/workflow/service/ProductionCalendarServiceTest.java:11](../src/test/java/com/example/workflow/service/ProductionCalendarServiceTest.java#L11)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCalendarServiceTest.doesNotCountStartDateAndSkipsSunday`](../src/test/java/com/example/workflow/service/ProductionCalendarServiceTest.java#L11)() | `void` | Kiểm tra kịch bản does không số lượng start ngày và skips sunday. |
| [`ProductionCalendarServiceTest.alsoSkipsConfiguredHoliday`](../src/test/java/com/example/workflow/service/ProductionCalendarServiceTest.java#L20)() | `void` | Kiểm tra kịch bản also skips configured holiday. |

### [src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java:69](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L69)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ProductionCheckpointServiceTest.setUp`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L69)() | `void` | Kiểm tra kịch bản set lên. |
| [`ProductionCheckpointServiceTest.initialShapeIsImmediatelyPassedWithoutManagerReview`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L93)() | `void` | Kiểm tra kịch bản tạo hình ban đầu is immediately passed không có quản lý duyệt/đánh giá. |
| [`ProductionCheckpointServiceTest.finalProductRequiresInitialShape`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L123)() | `void` | Kiểm tra kịch bản sản phẩm cuối requires tạo hình ban đầu. |
| [`ProductionCheckpointServiceTest.reworkKeepsOldAttemptAndNextSubmissionUsesNextAttemptNumber`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L147)() | `void` | Kiểm tra kịch bản làm lại giữ lại old lần thử và next submission sử dụng next lần thử number. |
| [`ProductionCheckpointServiceTest.passingOneOfManyItemsKeepsOrderCreatingAndStaffBusy`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L201)() | `void` | Kiểm tra kịch bản passing one of many mặt hàng giữ lại đơn hàng creating và nhân viên busy. |
| [`ProductionCheckpointServiceTest.passingLastItemTransitionsOnceAndReleasesExactAssignment`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L232)() | `void` | Kiểm tra kịch bản passing last mặt hàng transitions once và releases exact phân công. |
| [`ProductionCheckpointServiceTest.staffFromAnotherAssignmentCannotSubmitCheckpoint`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L275)() | `void` | Kiểm tra kịch bản nhân viên từ another phân công không thể submit checkpoint. |
| [`ProductionCheckpointServiceTest.guestProgressUsesSignedTokenAndHidesFinalProductAttempts`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L295)() | `void` | Kiểm tra kịch bản tiến độ của khách sử dụng đã ký token và hides sản phẩm cuối các lần thử. |
| [`ProductionCheckpointServiceTest.stubOrderAndAssignment`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L321)(`Order order, String staffId`) | `OrderAssignment` | Kiểm tra kịch bản stub đơn hàng và phân công. |
| [`ProductionCheckpointServiceTest.producingOrder`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L328)(`OrderItem... items`) | `Order` | Kiểm tra kịch bản producing đơn hàng. |
| [`ProductionCheckpointServiceTest.item`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L339)(`Long id`) | `OrderItem` | Kiểm tra kịch bản mặt hàng. |
| [`ProductionCheckpointServiceTest.checkpoint`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L349)(`OrderItem item, User staff, Long id, ProductionCheckpointStage stage, int attempt, ProductionCheckpointStatus status`) | `ProductionCheckpoint` | Kiểm tra kịch bản checkpoint. |
| [`ProductionCheckpointServiceTest.activeAssignment`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L369)(`String staffId`) | `OrderAssignment` | Kiểm tra kịch bản phân công đang hoạt động. |
| [`ProductionCheckpointServiceTest.user`](../src/test/java/com/example/workflow/service/ProductionCheckpointServiceTest.java#L377)(`String id`) | `User` | Kiểm tra kịch bản người dùng. |

### [src/test/java/com/example/workflow/service/UserServiceTest.java:59](../src/test/java/com/example/workflow/service/UserServiceTest.java#L59)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`UserServiceTest.setUpMapperDefaults`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L59)() | `void` | Kiểm tra kịch bản set lên mapper defaults. |
| [`UserServiceTest.registrationCreatesUserWithDefaultUserRole`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L65)() | `void` | Kiểm tra kịch bản registration creates người dùng với default người dùng vai trò. |
| [`UserServiceTest.registrationAllowsAdminToAssignNonUserRole`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L84)() | `void` | Kiểm tra kịch bản registration allows quản trị viên tới assign non người dùng vai trò. |
| [`UserServiceTest.registrationRejectsBlankPasswordBeforeCreatingUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L98)() | `void` | Kiểm tra kịch bản registration từ chối blank mật khẩu trước creating người dùng. |
| [`UserServiceTest.registrationRejectsDuplicateUsernameBeforeCreatingUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L110)() | `void` | Kiểm tra kịch bản registration từ chối duplicate tên đăng nhập trước creating người dùng. |
| [`UserServiceTest.registrationRejectsDuplicateEmailBeforeCreatingUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L122)() | `void` | Kiểm tra kịch bản registration từ chối duplicate email trước creating người dùng. |
| [`UserServiceTest.registrationNormalizesBlankEmailToNull`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L135)() | `void` | Kiểm tra kịch bản registration normalizes blank email tới null. |
| [`UserServiceTest.registrationRejectsDuplicatePhoneBeforeCreatingUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L149)() | `void` | Kiểm tra kịch bản registration từ chối duplicate điện thoại trước creating người dùng. |
| [`UserServiceTest.registrationRejectsInvalidRoleWhenCurrentUserCanAssignRole`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L161)() | `void` | Kiểm tra kịch bản registration từ chối invalid vai trò khi người dùng hiện tại can assign vai trò. |
| [`UserServiceTest.getUserByIdReturnsMappedResponse`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L174)() | `void` | Kiểm tra kịch bản get người dùng theo ID trả về mapped phản hồi. |
| [`UserServiceTest.updateUserSyncsKeycloakAndPersistsMappedEntity`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L189)() | `void` | Kiểm tra kịch bản update người dùng syncs keycloak và persists mapped entity. |
| [`UserServiceTest.updateUserAllowsAdminToSynchronizeRoleWithKeycloak`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L207)() | `void` | Kiểm tra kịch bản update người dùng allows quản trị viên tới synchronize vai trò với keycloak. |
| [`UserServiceTest.updateUserRejectsRoleChangeFromUnprivilegedUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L225)() | `void` | Kiểm tra kịch bản update người dùng từ chối vai trò change từ unprivileged người dùng. |
| [`UserServiceTest.updateMyProfileUsesJwtSubjectAndPreservesRole`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L243)() | `void` | Kiểm tra kịch bản update của người dùng hồ sơ sử dụng jwt subject và preserves vai trò. |
| [`UserServiceTest.deleteUserDisablesKeycloakUserAndSoftDeletesEntity`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L267)() | `void` | Kiểm tra kịch bản delete người dùng disables keycloak người dùng và soft deletes entity. |
| [`UserServiceTest.getUserByIdThrowsWhenUserDoesNotExist`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L279)() | `void` | Kiểm tra kịch bản get người dùng theo ID ném lỗi khi người dùng does không exist. |
| [`UserServiceTest.appExceptionFormatsParameterizedMessage`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L290)() | `void` | Kiểm tra kịch bản app exception formats parameterized tin nhắn. |
| [`UserServiceTest.validUser`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L302)() | `UserCreDTO` | Kiểm tra kịch bản valid người dùng. |
| [`UserServiceTest.user`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L315)(`String id, String username`) | `User` | Kiểm tra kịch bản người dùng. |
| [`UserServiceTest.mockAuthenticatedRole`](../src/test/java/com/example/workflow/service/UserServiceTest.java#L323)(`String authority`) | `void` | Kiểm tra kịch bản mock authenticated vai trò. |

### [src/test/java/com/example/workflow/service/VoucherServiceTest.java:54](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L54)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`VoucherServiceTest.applyGuestVoucherDecrementsQuantityAtomicallyAndReturnsDiscount`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L54)() | `void` | Kiểm tra kịch bản apply khách voucher decrements số lượng atomically và trả về giảm giá. |
| [`VoucherServiceTest.applyGuestVoucherFailsWhenAtomicDecrementLosesRace`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L72)() | `void` | Kiểm tra kịch bản apply khách voucher fails khi atomic decrement loses race. |
| [`VoucherServiceTest.guestVoucherOptionsMarkVoucherUsedByCurrentSessionAsUnavailable`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L85)() | `void` | Kiểm tra kịch bản khách voucher options mark voucher used theo hiện tại session as unavailable. |
| [`VoucherServiceTest.applyGuestVoucherRejectsExpiredCampaign`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L103)() | `void` | Kiểm tra kịch bản apply khách voucher từ chối hết hạn campaign. |
| [`VoucherServiceTest.applyGuestVoucherRejectsOrderBelowCampaignMinimum`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L118)() | `void` | Kiểm tra kịch bản apply khách voucher từ chối đơn hàng below campaign minimum. |
| [`VoucherServiceTest.applyGuestVoucherRejectsReusedEmailBeforeDecrementingQuantity`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L132)() | `void` | Kiểm tra kịch bản apply khách voucher từ chối reused email trước decrementing số lượng. |
| [`VoucherServiceTest.applyGuestVoucherRejectsReusedPhoneBeforeDecrementingQuantity`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L150)() | `void` | Kiểm tra kịch bản apply khách voucher từ chối reused điện thoại trước decrementing số lượng. |
| [`VoucherServiceTest.userVoucherCheckoutRemainsIndependentFromGuestUsage`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L167)() | `void` | Kiểm tra kịch bản người dùng voucher checkout remains independent từ khách usage. |
| [`VoucherServiceTest.recordGuestVoucherUsagePersistsOnlyHashesAndOrderReference`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L189)() | `void` | Kiểm tra kịch bản bản ghi khách voucher usage persists chỉ hashes và đơn hàng tài liệu tham khảo. |
| [`VoucherServiceTest.recordGuestVoucherUsageTranslatesConcurrentUniqueConstraintViolation`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L211)() | `void` | Kiểm tra kịch bản bản ghi khách voucher usage translates concurrent unique constraint violation. |
| [`VoucherServiceTest.redeemVoucherRejectsGuestVoucher`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L223)() | `void` | Kiểm tra kịch bản redeem voucher từ chối khách voucher. |
| [`VoucherServiceTest.guestVoucher`](../src/test/java/com/example/workflow/service/VoucherServiceTest.java#L239)() | `VoucherTemplate` | Kiểm tra kịch bản khách voucher. |

### [src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java:26](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L26)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`ApplicationCacheServiceTest.userCheckoutOnlyEvictsCartImmediately`](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L26)() | `void` | Kiểm tra kịch bản người dùng checkout chỉ evicts giỏ hàng immediately. |
| [`ApplicationCacheServiceTest.orderCreatedEvictsOnlyAffectedUserReadModels`](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L34)() | `void` | Kiểm tra kịch bản đơn hàng đã tạo evicts chỉ affected người dùng đọc models. |
| [`ApplicationCacheServiceTest.orderCancelledEvictsOperationalCachesButNotCatalogOrAnalytics`](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L52)() | `void` | Kiểm tra kịch bản đơn hàng đã hủy evicts operational caches but không catalog hoặc analytics. |
| [`ApplicationCacheServiceTest.changeRequestEvictsOwnerAndAssignedStaffOrderListsOnly`](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L80)() | `void` | Kiểm tra kịch bản change yêu cầu evicts chủ sở hữu và nhân viên được phân công đơn hàng lists chỉ. |
| [`ApplicationCacheServiceTest.order`](../src/test/java/com/example/workflow/service/cache/ApplicationCacheServiceTest.java#L99)(`String userId, String staffId`) | `Order` | Kiểm tra kịch bản đơn hàng. |

### [src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java:16](../src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java#L16)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`OutboxRecoveryServiceTest.setUp`](../src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java#L16)() | `void` | Kiểm tra kịch bản set lên. |
| [`OutboxRecoveryServiceTest.summaryAndIncidentsNeverExposePayload`](../src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java#L38)() | `void` | Kiểm tra kịch bản tóm tắt và sự cố never expose payload. |
| [`OutboxRecoveryServiceTest.requeueOnlyResetsAFailedPendingEvent`](../src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java#L58)() | `void` | Kiểm tra kịch bản requeue chỉ resets a thất bại đang chờ sự kiện. |
| [`OutboxRecoveryServiceTest.insert`](../src/test/java/com/example/workflow/service/consistency/OutboxRecoveryServiceTest.java#L74)(`String eventId, String eventType, int attempts, LocalDateTime publishedAt, String lastError`) | `void` | Kiểm tra kịch bản insert. |

### [src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java:36](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L36)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutConcurrencyServiceTest.setUp`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L36)() | `void` | Kiểm tra kịch bản set lên. |
| [`CheckoutConcurrencyServiceTest.acquiresCartThenSortedVariantLocksAndReleasesInReverseOrder`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L44)() | `void` | Kiểm tra kịch bản acquires giỏ hàng then sorted biến thể locks và releases trong reverse đơn hàng. |
| [`CheckoutConcurrencyServiceTest.releasesAcquiredLocksWhenNextLockCannotBeAcquired`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L63)() | `void` | Kiểm tra kịch bản releases acquired locks khi next lock không thể be acquired. |
| [`CheckoutConcurrencyServiceTest.concurrentSpamCheckoutRequestsForSameUserAllowOnlyOneActiveOrderCreation`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L75)() | `void` | Kiểm tra kịch bản concurrent spam checkout yêu cầu cho cùng người dùng allow chỉ one đang hoạt động đơn hàng creation. |
| [`CheckoutConcurrencyServiceTest.concurrentCheckoutRequestsForSameVariantAllowOnlyOneActiveReservationPath`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L85)() | `void` | Kiểm tra kịch bản concurrent checkout yêu cầu cho cùng biến thể allow chỉ one đang hoạt động reservation path. |
| [`CheckoutConcurrencyServiceTest.useInMemoryRedisLocks`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L95)() | `ConcurrentHashMap<String, String>` | Kiểm tra kịch bản use trong memory redis locks. |
| [`CheckoutConcurrencyServiceTest.runConcurrentLockAttempts`](../src/test/java/com/example/workflow/service/redis/CheckoutConcurrencyServiceTest.java#L111)(`IntFunction<String> userIdFactory, List<Long> variantIds`) | `long` | Kiểm tra kịch bản run concurrent lock các lần thử. |

### [src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java:39](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L39)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CheckoutIdempotencyServiceTest.setUp`](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L39)() | `void` | Kiểm tra kịch bản set lên. |
| [`CheckoutIdempotencyServiceTest.replaysStoredResponseWithoutStartingNewProcessing`](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L46)() | `void` | Kiểm tra kịch bản replays stored phản hồi không có starting new processing. |
| [`CheckoutIdempotencyServiceTest.createsInProgressMarkerWhenNoStoredResponseExists`](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L57)() | `void` | Kiểm tra kịch bản creates trong tiến độ marker khi no stored phản hồi tồn tại. |
| [`CheckoutIdempotencyServiceTest.rejectsDuplicateRequestWhileOriginalIsStillProcessing`](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L67)() | `void` | Kiểm tra kịch bản từ chối duplicate yêu cầu while original is still processing. |
| [`CheckoutIdempotencyServiceTest.storesCompletedResponseAndReleasesInProgressMarker`](../src/test/java/com/example/workflow/service/redis/CheckoutIdempotencyServiceTest.java#L77)() | `void` | Kiểm tra kịch bản stores đã hoàn tất phản hồi và releases trong tiến độ marker. |

### [src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java:30](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L30)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`LoginRateLimitServiceTest.setUp`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L30)() | `void` | Kiểm tra kịch bản set lên. |
| [`LoginRateLimitServiceTest.assertAllowedAllowsCredentialWhenNoLockExists`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L38)() | `void` | Kiểm tra kịch bản assert được phép allows credential khi no lock tồn tại. |
| [`LoginRateLimitServiceTest.assertAllowedRejectsCredentialWhileLockKeyHasTtl`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L47)() | `void` | Kiểm tra kịch bản assert được phép từ chối credential while lock khóa has ttl. |
| [`LoginRateLimitServiceTest.recordFailureDoesNotLockBeforeFifthConsecutiveFailure`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L57)() | `void` | Kiểm tra kịch bản bản ghi lỗi does không lock trước fifth consecutive lỗi. |
| [`LoginRateLimitServiceTest.recordFailureLocksThreeMinutesAtFifthConsecutiveFailure`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L66)() | `void` | Kiểm tra kịch bản bản ghi lỗi locks three minutes tại fifth consecutive lỗi. |
| [`LoginRateLimitServiceTest.recordFailureLocksFourMinutesAtSixthConsecutiveFailure`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L78)() | `void` | Kiểm tra kịch bản bản ghi lỗi locks four minutes tại sixth consecutive lỗi. |
| [`LoginRateLimitServiceTest.recordFailureLocksFiveMinutesAtSeventhConsecutiveFailure`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L90)() | `void` | Kiểm tra kịch bản bản ghi lỗi locks five minutes tại seventh consecutive lỗi. |
| [`LoginRateLimitServiceTest.clearFailuresDeletesConsecutiveFailureCountAndLockKeys`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L102)() | `void` | Kiểm tra kịch bản clear failures deletes consecutive lỗi số lượng và lock khóa. |
| [`LoginRateLimitServiceTest.skipsBlankUsernameBecauseValidationWillRejectTheRequest`](../src/test/java/com/example/workflow/service/redis/LoginRateLimitServiceTest.java#L110)() | `void` | Kiểm tra kịch bản skips blank tên đăng nhập because validation will reject the yêu cầu. |

### [src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java:22](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L22)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisEventIdempotencyServiceTest.setUp`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L22)() | `void` | Kiểm tra kịch bản set lên. |
| [`RedisEventIdempotencyServiceTest.acquiresEventKeyWhenItDoesNotExist`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L28)() | `void` | Kiểm tra kịch bản acquires sự kiện khóa khi it does không exist. |
| [`RedisEventIdempotencyServiceTest.rejectsDuplicateEventKey`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L41)() | `void` | Kiểm tra kịch bản từ chối duplicate sự kiện khóa. |
| [`RedisEventIdempotencyServiceTest.allowsProcessingWhenRedisIdempotencyIsUnavailable`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L54)() | `void` | Kiểm tra kịch bản allows processing khi redis idempotency is unavailable. |
| [`RedisEventIdempotencyServiceTest.detectsCompletedEventKey`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L67)() | `void` | Kiểm tra kịch bản detects đã hoàn tất sự kiện khóa. |
| [`RedisEventIdempotencyServiceTest.marksEventCompletedWithTtl`](../src/test/java/com/example/workflow/service/redis/RedisEventIdempotencyServiceTest.java#L76)() | `void` | Kiểm tra kịch bản marks sự kiện đã hoàn tất với ttl. |

### [src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java:50](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L50)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisStreamEventConsumerTest.guestOrderCreatedEventSendsGuestConfirmationEmail`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L50)() | `void` | Kiểm tra kịch bản đơn hàng của khách đã tạo sự kiện sends khách confirmation email. |
| [`RedisStreamEventConsumerTest.tokenizedGuestOrderCreatedEventIncludesSecureOrderLinkAndDuration`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L57)() | `void` | Kiểm tra kịch bản tokenized đơn hàng của khách đã tạo sự kiện includes secure đơn hàng link và thời lượng. |
| [`RedisStreamEventConsumerTest.guestOrderCreatedEventDelegatesExactlyOnce`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L69)() | `void` | Kiểm tra kịch bản đơn hàng của khách đã tạo sự kiện delegates exactly once. |
| [`RedisStreamEventConsumerTest.orderAcceptedEventDelegatesToLifecycleHandler`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L76)() | `void` | Kiểm tra kịch bản đơn hàng đã chấp nhận sự kiện delegates tới lifecycle handler. |
| [`RedisStreamEventConsumerTest.workflowEmailEventSendsEmailThroughConsumer`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L89)() | `void` | Kiểm tra kịch bản workflow email sự kiện sends email through consumer. |
| [`RedisStreamEventConsumerTest.workflowEmailEventResolvesGuestContactFromOrder`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L112)() | `void` | Kiểm tra kịch bản workflow email sự kiện resolves khách contact từ đơn hàng. |
| [`RedisStreamEventConsumerTest.productionCheckpointSubmittedDelegatesToLifecycleHandler`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L133)() | `void` | Kiểm tra kịch bản checkpoint sản xuất đã gửi delegates tới lifecycle handler. |
| [`RedisStreamEventConsumerTest.productionCheckpointReviewedDelegatesToLifecycleHandler`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L149)() | `void` | Kiểm tra kịch bản checkpoint sản xuất đã duyệt delegates tới lifecycle handler. |
| [`RedisStreamEventConsumerTest.readyToShipDelegatesToLifecycleHandler`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L165)() | `void` | Kiểm tra kịch bản sẵn sàng tới ship delegates tới lifecycle handler. |
| [`RedisStreamEventConsumerTest.payload`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L180)(`Long orderId`) | `String` | Kiểm tra kịch bản payload. |
| [`RedisStreamEventConsumerTest.guestOrder`](../src/test/java/com/example/workflow/service/redis/RedisStreamEventConsumerTest.java#L184)() | `Order` | Kiểm tra kịch bản đơn hàng của khách. |

### [src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java:44](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L44)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`RedisStreamRetryTemplateTest.setUp`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L44)() | `void` | Kiểm tra kịch bản set lên. |
| [`RedisStreamRetryTemplateTest.nonRetryEventRethrowsFailureWithoutWritingMetadata`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L55)() | `void` | Kiểm tra kịch bản non retry sự kiện rethrows lỗi không có writing metadata. |
| [`RedisStreamRetryTemplateTest.successfulRetryEnabledEventClearsMetadataAndRequestsAck`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L75)() | `void` | Kiểm tra kịch bản thành công retry enabled sự kiện clears metadata và yêu cầu ack. |
| [`RedisStreamRetryTemplateTest.failedRetryEnabledEventStoresRetryMetadataAndDoesNotAck`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L84)() | `void` | Kiểm tra kịch bản thất bại retry enabled sự kiện stores retry metadata và does không ack. |
| [`RedisStreamRetryTemplateTest.eventIsSkippedWhenNextRetryTimeIsInFuture`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L100)() | `void` | Kiểm tra kịch bản sự kiện is skipped khi next retry thời gian is trong future. |
| [`RedisStreamRetryTemplateTest.eventMovesToDlqWhenMaxAttemptsIsReached`](../src/test/java/com/example/workflow/service/redis/RedisStreamRetryTemplateTest.java#L112)() | `void` | Kiểm tra kịch bản sự kiện moves tới DLQ khi max các lần thử is reached. |

### [src/test/java/com/example/workflow/workflow/CancelExpiredOrderWorkflowTaskHandlerTest.java:19](../src/test/java/com/example/workflow/workflow/CancelExpiredOrderWorkflowTaskHandlerTest.java#L19)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`CancelExpiredOrderWorkflowTaskHandlerTest.customTimeoutDelegatesToSharedCancellationKernel`](../src/test/java/com/example/workflow/workflow/CancelExpiredOrderWorkflowTaskHandlerTest.java#L19)() | `void` | Kiểm tra kịch bản custom timeout delegates tới shared cancellation kernel. |
| [`CancelExpiredOrderWorkflowTaskHandlerTest.paymentTimeoutUsesThePersistedAttemptId`](../src/test/java/com/example/workflow/workflow/CancelExpiredOrderWorkflowTaskHandlerTest.java#L32)() | `void` | Kiểm tra kịch bản thanh toán timeout sử dụng the persisted lần thử ID. |

### [src/test/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistryTest.java:14](../src/test/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistryTest.java#L14)

| Hàm/phương thức | Giá trị trả về | Mục đích |
|---|---|---|
| [`WorkflowTaskHandlerRegistryTest.resolvesHandlerWithNormalizedTaskType`](../src/test/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistryTest.java#L14)() | `void` | Kiểm tra kịch bản resolves handler với đã chuẩn hóa tác vụ loại. |
| [`WorkflowTaskHandlerRegistryTest.unknownTaskTypeHasClearError`](../src/test/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistryTest.java#L23)() | `void` | Kiểm tra kịch bản unknown tác vụ loại has clear lỗi. |
| [`WorkflowTaskHandlerRegistryTest.duplicateTaskTypesFailAtStartup`](../src/test/java/com/example/workflow/workflow/WorkflowTaskHandlerRegistryTest.java#L35)() | `void` | Kiểm tra kịch bản duplicate tác vụ types fail tại startup. |

## Tệp source không có hàm khai báo tường minh

Các tệp dưới đây hiện không có named function/method được khai báo trực tiếp; DTO, entity, enum và cấu hình có thể vẫn chứa dữ liệu/annotation hoặc method được sinh tự động khi build.

- [src/main/java/com/example/workflow/cache/CacheNames.java](../src/main/java/com/example/workflow/cache/CacheNames.java)
- [src/main/java/com/example/workflow/dto/AssignOrderRequest.java](../src/main/java/com/example/workflow/dto/AssignOrderRequest.java)
- [src/main/java/com/example/workflow/dto/AuthResponse.java](../src/main/java/com/example/workflow/dto/AuthResponse.java)
- [src/main/java/com/example/workflow/dto/AvailableStaffDTO.java](../src/main/java/com/example/workflow/dto/AvailableStaffDTO.java)
- [src/main/java/com/example/workflow/dto/BestSellerProductDTO.java](../src/main/java/com/example/workflow/dto/BestSellerProductDTO.java)
- [src/main/java/com/example/workflow/dto/CancelOrderRequest.java](../src/main/java/com/example/workflow/dto/CancelOrderRequest.java)
- [src/main/java/com/example/workflow/dto/CartItemDTO.java](../src/main/java/com/example/workflow/dto/CartItemDTO.java)
- [src/main/java/com/example/workflow/dto/CartResDTO.java](../src/main/java/com/example/workflow/dto/CartResDTO.java)
- [src/main/java/com/example/workflow/dto/CartVoucherOptionsDTO.java](../src/main/java/com/example/workflow/dto/CartVoucherOptionsDTO.java)
- [src/main/java/com/example/workflow/dto/ChangePasswordRequest.java](../src/main/java/com/example/workflow/dto/ChangePasswordRequest.java)
- [src/main/java/com/example/workflow/dto/ChangeRequestDTO.java](../src/main/java/com/example/workflow/dto/ChangeRequestDTO.java)
- [src/main/java/com/example/workflow/dto/CheckoutRequest.java](../src/main/java/com/example/workflow/dto/CheckoutRequest.java)
- [src/main/java/com/example/workflow/dto/CheckoutResponseDTO.java](../src/main/java/com/example/workflow/dto/CheckoutResponseDTO.java)
- [src/main/java/com/example/workflow/dto/ClaimOrderRequest.java](../src/main/java/com/example/workflow/dto/ClaimOrderRequest.java)
- [src/main/java/com/example/workflow/dto/CreateCustomRequest.java](../src/main/java/com/example/workflow/dto/CreateCustomRequest.java)
- [src/main/java/com/example/workflow/dto/CreateVoucherTemplateRequest.java](../src/main/java/com/example/workflow/dto/CreateVoucherTemplateRequest.java)
- [src/main/java/com/example/workflow/dto/CustomRequestDTO.java](../src/main/java/com/example/workflow/dto/CustomRequestDTO.java)
- [src/main/java/com/example/workflow/dto/CustomRequestSubmissionDTO.java](../src/main/java/com/example/workflow/dto/CustomRequestSubmissionDTO.java)
- [src/main/java/com/example/workflow/dto/DashboardStatsDTO.java](../src/main/java/com/example/workflow/dto/DashboardStatsDTO.java)
- [src/main/java/com/example/workflow/dto/ForgotPasswordRequest.java](../src/main/java/com/example/workflow/dto/ForgotPasswordRequest.java)
- [src/main/java/com/example/workflow/dto/GuestCheckoutRequest.java](../src/main/java/com/example/workflow/dto/GuestCheckoutRequest.java)
- [src/main/java/com/example/workflow/dto/GuestOrderCancellationViewDTO.java](../src/main/java/com/example/workflow/dto/GuestOrderCancellationViewDTO.java)
- [src/main/java/com/example/workflow/dto/ItemCheckRequest.java](../src/main/java/com/example/workflow/dto/ItemCheckRequest.java)
- [src/main/java/com/example/workflow/dto/LoginRequest.java](../src/main/java/com/example/workflow/dto/LoginRequest.java)
- [src/main/java/com/example/workflow/dto/ManagerOrderReviewDetailDTO.java](../src/main/java/com/example/workflow/dto/ManagerOrderReviewDetailDTO.java)
- [src/main/java/com/example/workflow/dto/ManagerOrderReviewSummaryDTO.java](../src/main/java/com/example/workflow/dto/ManagerOrderReviewSummaryDTO.java)
- [src/main/java/com/example/workflow/dto/ManagerReviewResultDTO.java](../src/main/java/com/example/workflow/dto/ManagerReviewResultDTO.java)
- [src/main/java/com/example/workflow/dto/NotificationDTO.java](../src/main/java/com/example/workflow/dto/NotificationDTO.java)
- [src/main/java/com/example/workflow/dto/OrderAgreementDTO.java](../src/main/java/com/example/workflow/dto/OrderAgreementDTO.java)
- [src/main/java/com/example/workflow/dto/OrderAgreementDecisionRequest.java](../src/main/java/com/example/workflow/dto/OrderAgreementDecisionRequest.java)
- [src/main/java/com/example/workflow/dto/OrderAssignmentDTO.java](../src/main/java/com/example/workflow/dto/OrderAssignmentDTO.java)
- [src/main/java/com/example/workflow/dto/OrderAssignmentQueueDTO.java](../src/main/java/com/example/workflow/dto/OrderAssignmentQueueDTO.java)
- [src/main/java/com/example/workflow/dto/OrderAssignmentResultDTO.java](../src/main/java/com/example/workflow/dto/OrderAssignmentResultDTO.java)
- [src/main/java/com/example/workflow/dto/OrderCancellationResultDTO.java](../src/main/java/com/example/workflow/dto/OrderCancellationResultDTO.java)
- [src/main/java/com/example/workflow/dto/OrderChatMessageDTO.java](../src/main/java/com/example/workflow/dto/OrderChatMessageDTO.java)
- [src/main/java/com/example/workflow/dto/OrderChatThreadDTO.java](../src/main/java/com/example/workflow/dto/OrderChatThreadDTO.java)
- [src/main/java/com/example/workflow/dto/OrderItemAssignmentDTO.java](../src/main/java/com/example/workflow/dto/OrderItemAssignmentDTO.java)
- [src/main/java/com/example/workflow/dto/OrderItemDTO.java](../src/main/java/com/example/workflow/dto/OrderItemDTO.java)
- [src/main/java/com/example/workflow/dto/OrderItemProductionDTO.java](../src/main/java/com/example/workflow/dto/OrderItemProductionDTO.java)
- [src/main/java/com/example/workflow/dto/OrderListDTO.java](../src/main/java/com/example/workflow/dto/OrderListDTO.java)
- [src/main/java/com/example/workflow/dto/OrderStatusHistoryDTO.java](../src/main/java/com/example/workflow/dto/OrderStatusHistoryDTO.java)
- [src/main/java/com/example/workflow/dto/PaymentAttemptDTO.java](../src/main/java/com/example/workflow/dto/PaymentAttemptDTO.java)
- [src/main/java/com/example/workflow/dto/ProductDTO.java](../src/main/java/com/example/workflow/dto/ProductDTO.java)
- [src/main/java/com/example/workflow/dto/ProductReviewDTO.java](../src/main/java/com/example/workflow/dto/ProductReviewDTO.java)
- [src/main/java/com/example/workflow/dto/ProductReviewRequest.java](../src/main/java/com/example/workflow/dto/ProductReviewRequest.java)
- [src/main/java/com/example/workflow/dto/ProductReviewSummaryDTO.java](../src/main/java/com/example/workflow/dto/ProductReviewSummaryDTO.java)
- [src/main/java/com/example/workflow/dto/ProductTypeDTO.java](../src/main/java/com/example/workflow/dto/ProductTypeDTO.java)
- [src/main/java/com/example/workflow/dto/ProductTypeFieldDTO.java](../src/main/java/com/example/workflow/dto/ProductTypeFieldDTO.java)
- [src/main/java/com/example/workflow/dto/ProductVariantDTO.java](../src/main/java/com/example/workflow/dto/ProductVariantDTO.java)
- [src/main/java/com/example/workflow/dto/ProductionCheckpointDTO.java](../src/main/java/com/example/workflow/dto/ProductionCheckpointDTO.java)
- [src/main/java/com/example/workflow/dto/ProductionCheckpointImageDTO.java](../src/main/java/com/example/workflow/dto/ProductionCheckpointImageDTO.java)
- [src/main/java/com/example/workflow/dto/ProductionDecisionDTO.java](../src/main/java/com/example/workflow/dto/ProductionDecisionDTO.java)
- [src/main/java/com/example/workflow/dto/ReceiptConfirmRequest.java](../src/main/java/com/example/workflow/dto/ReceiptConfirmRequest.java)
- [src/main/java/com/example/workflow/dto/ReceiptConfirmResponse.java](../src/main/java/com/example/workflow/dto/ReceiptConfirmResponse.java)
- [src/main/java/com/example/workflow/dto/ReceiptItemDTO.java](../src/main/java/com/example/workflow/dto/ReceiptItemDTO.java)
- [src/main/java/com/example/workflow/dto/ReceiptMismatchDTO.java](../src/main/java/com/example/workflow/dto/ReceiptMismatchDTO.java)
- [src/main/java/com/example/workflow/dto/ReceiptViewDTO.java](../src/main/java/com/example/workflow/dto/ReceiptViewDTO.java)
- [src/main/java/com/example/workflow/dto/ReorderItemDTO.java](../src/main/java/com/example/workflow/dto/ReorderItemDTO.java)
- [src/main/java/com/example/workflow/dto/ReorderResponseDTO.java](../src/main/java/com/example/workflow/dto/ReorderResponseDTO.java)
- [src/main/java/com/example/workflow/dto/ReputationHistoryDTO.java](../src/main/java/com/example/workflow/dto/ReputationHistoryDTO.java)
- [src/main/java/com/example/workflow/dto/ResetPasswordRequest.java](../src/main/java/com/example/workflow/dto/ResetPasswordRequest.java)
- [src/main/java/com/example/workflow/dto/ReviewProductionCheckpointRequest.java](../src/main/java/com/example/workflow/dto/ReviewProductionCheckpointRequest.java)
- [src/main/java/com/example/workflow/dto/ReviewProductionCheckpointResultDTO.java](../src/main/java/com/example/workflow/dto/ReviewProductionCheckpointResultDTO.java)
- [src/main/java/com/example/workflow/dto/SaveProductTypeRequest.java](../src/main/java/com/example/workflow/dto/SaveProductTypeRequest.java)
- [src/main/java/com/example/workflow/dto/SendOrderChatMessageRequest.java](../src/main/java/com/example/workflow/dto/SendOrderChatMessageRequest.java)
- [src/main/java/com/example/workflow/dto/ShippingHandoverItemDTO.java](../src/main/java/com/example/workflow/dto/ShippingHandoverItemDTO.java)
- [src/main/java/com/example/workflow/dto/ShippingHandoverItemRequest.java](../src/main/java/com/example/workflow/dto/ShippingHandoverItemRequest.java)
- [src/main/java/com/example/workflow/dto/ShippingHandoverRequest.java](../src/main/java/com/example/workflow/dto/ShippingHandoverRequest.java)
- [src/main/java/com/example/workflow/dto/ShippingHandoverResponse.java](../src/main/java/com/example/workflow/dto/ShippingHandoverResponse.java)
- [src/main/java/com/example/workflow/dto/StartProductionResultDTO.java](../src/main/java/com/example/workflow/dto/StartProductionResultDTO.java)
- [src/main/java/com/example/workflow/dto/SubmitCustomRequest.java](../src/main/java/com/example/workflow/dto/SubmitCustomRequest.java)
- [src/main/java/com/example/workflow/dto/SubmitOrderAgreementRequest.java](../src/main/java/com/example/workflow/dto/SubmitOrderAgreementRequest.java)
- [src/main/java/com/example/workflow/dto/SubmitProductionCheckpointRequest.java](../src/main/java/com/example/workflow/dto/SubmitProductionCheckpointRequest.java)
- [src/main/java/com/example/workflow/dto/UpdateCustomRequest.java](../src/main/java/com/example/workflow/dto/UpdateCustomRequest.java)
- [src/main/java/com/example/workflow/dto/UserCreDTO.java](../src/main/java/com/example/workflow/dto/UserCreDTO.java)
- [src/main/java/com/example/workflow/dto/UserListDTO.java](../src/main/java/com/example/workflow/dto/UserListDTO.java)
- [src/main/java/com/example/workflow/dto/UserProfileUpdateDTO.java](../src/main/java/com/example/workflow/dto/UserProfileUpdateDTO.java)
- [src/main/java/com/example/workflow/dto/UserResDTO.java](../src/main/java/com/example/workflow/dto/UserResDTO.java)
- [src/main/java/com/example/workflow/dto/UserVoucherDTO.java](../src/main/java/com/example/workflow/dto/UserVoucherDTO.java)
- [src/main/java/com/example/workflow/dto/VoucherCartOptionDTO.java](../src/main/java/com/example/workflow/dto/VoucherCartOptionDTO.java)
- [src/main/java/com/example/workflow/dto/VoucherTemplateDTO.java](../src/main/java/com/example/workflow/dto/VoucherTemplateDTO.java)
- [src/main/java/com/example/workflow/dto/WorkflowDlqIncidentDTO.java](../src/main/java/com/example/workflow/dto/WorkflowDlqIncidentDTO.java)
- [src/main/java/com/example/workflow/dto/WorkflowOperationsSummaryDTO.java](../src/main/java/com/example/workflow/dto/WorkflowOperationsSummaryDTO.java)
- [src/main/java/com/example/workflow/dto/WorkflowOutboxIncidentDTO.java](../src/main/java/com/example/workflow/dto/WorkflowOutboxIncidentDTO.java)
- [src/main/java/com/example/workflow/entity/Cart.java](../src/main/java/com/example/workflow/entity/Cart.java)
- [src/main/java/com/example/workflow/entity/CartItem.java](../src/main/java/com/example/workflow/entity/CartItem.java)
- [src/main/java/com/example/workflow/entity/ChangeRequest.java](../src/main/java/com/example/workflow/entity/ChangeRequest.java)
- [src/main/java/com/example/workflow/entity/ChatMessage.java](../src/main/java/com/example/workflow/entity/ChatMessage.java)
- [src/main/java/com/example/workflow/entity/GuestVoucherUsage.java](../src/main/java/com/example/workflow/entity/GuestVoucherUsage.java)
- [src/main/java/com/example/workflow/entity/Notification.java](../src/main/java/com/example/workflow/entity/Notification.java)
- [src/main/java/com/example/workflow/entity/NotificationRead.java](../src/main/java/com/example/workflow/entity/NotificationRead.java)
- [src/main/java/com/example/workflow/entity/OrderAgreement.java](../src/main/java/com/example/workflow/entity/OrderAgreement.java)
- [src/main/java/com/example/workflow/entity/OrderChatThread.java](../src/main/java/com/example/workflow/entity/OrderChatThread.java)
- [src/main/java/com/example/workflow/entity/OrderContactSnapshot.java](../src/main/java/com/example/workflow/entity/OrderContactSnapshot.java)
- [src/main/java/com/example/workflow/entity/OrderReceipt.java](../src/main/java/com/example/workflow/entity/OrderReceipt.java)
- [src/main/java/com/example/workflow/entity/OrderStatusHistory.java](../src/main/java/com/example/workflow/entity/OrderStatusHistory.java)
- [src/main/java/com/example/workflow/entity/PasswordResetToken.java](../src/main/java/com/example/workflow/entity/PasswordResetToken.java)
- [src/main/java/com/example/workflow/entity/PaymentAttempt.java](../src/main/java/com/example/workflow/entity/PaymentAttempt.java)
- [src/main/java/com/example/workflow/entity/Product.java](../src/main/java/com/example/workflow/entity/Product.java)
- [src/main/java/com/example/workflow/entity/ProductType.java](../src/main/java/com/example/workflow/entity/ProductType.java)
- [src/main/java/com/example/workflow/entity/ProductVariant.java](../src/main/java/com/example/workflow/entity/ProductVariant.java)
- [src/main/java/com/example/workflow/entity/User.java](../src/main/java/com/example/workflow/entity/User.java)
- [src/main/java/com/example/workflow/entity/UserVoucher.java](../src/main/java/com/example/workflow/entity/UserVoucher.java)
- [src/main/java/com/example/workflow/entity/VoucherTemplate.java](../src/main/java/com/example/workflow/entity/VoucherTemplate.java)
- [src/main/java/com/example/workflow/event/EventTypes.java](../src/main/java/com/example/workflow/event/EventTypes.java)
- [src/main/java/com/example/workflow/event/payload/CacheEvictionRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/CacheEvictionRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/ChangeRequestDecidedEvent.java](../src/main/java/com/example/workflow/event/payload/ChangeRequestDecidedEvent.java)
- [src/main/java/com/example/workflow/event/payload/ChangeRequestSubmittedEvent.java](../src/main/java/com/example/workflow/event/payload/ChangeRequestSubmittedEvent.java)
- [src/main/java/com/example/workflow/event/payload/GuestOrderCreatedEvent.java](../src/main/java/com/example/workflow/event/payload/GuestOrderCreatedEvent.java)
- [src/main/java/com/example/workflow/event/payload/NotificationRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/NotificationRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderAcceptedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderAcceptedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderAgreementReviewedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderAgreementReviewedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderAgreementSubmittedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderAgreementSubmittedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderAssignedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderAssignedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderCancellationEmailRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderCancellationEmailRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderCancelledEvent.java](../src/main/java/com/example/workflow/event/payload/OrderCancelledEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderConfirmationEmailRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderConfirmationEmailRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderCreatedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderCreatedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderDeliveredEvent.java](../src/main/java/com/example/workflow/event/payload/OrderDeliveredEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderReadyToShipEvent.java](../src/main/java/com/example/workflow/event/payload/OrderReadyToShipEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderReviewedEvent.java](../src/main/java/com/example/workflow/event/payload/OrderReviewedEvent.java)
- [src/main/java/com/example/workflow/event/payload/OrderShippingEvent.java](../src/main/java/com/example/workflow/event/payload/OrderShippingEvent.java)
- [src/main/java/com/example/workflow/event/payload/PasswordResetEmailRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/PasswordResetEmailRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/PaymentConfirmedEvent.java](../src/main/java/com/example/workflow/event/payload/PaymentConfirmedEvent.java)
- [src/main/java/com/example/workflow/event/payload/PaymentOpenedEvent.java](../src/main/java/com/example/workflow/event/payload/PaymentOpenedEvent.java)
- [src/main/java/com/example/workflow/event/payload/ProductionCheckpointReviewedEvent.java](../src/main/java/com/example/workflow/event/payload/ProductionCheckpointReviewedEvent.java)
- [src/main/java/com/example/workflow/event/payload/ProductionCheckpointSubmittedEvent.java](../src/main/java/com/example/workflow/event/payload/ProductionCheckpointSubmittedEvent.java)
- [src/main/java/com/example/workflow/event/payload/ProductionStartedEvent.java](../src/main/java/com/example/workflow/event/payload/ProductionStartedEvent.java)
- [src/main/java/com/example/workflow/event/payload/WorkflowEmailRequestedEvent.java](../src/main/java/com/example/workflow/event/payload/WorkflowEmailRequestedEvent.java)
- [src/main/java/com/example/workflow/event/payload/WorkflowEventPayload.java](../src/main/java/com/example/workflow/event/payload/WorkflowEventPayload.java)
- [src/main/java/com/example/workflow/exception/AppException.java](../src/main/java/com/example/workflow/exception/AppException.java)
- [src/main/java/com/example/workflow/exception/ErrorResponse.java](../src/main/java/com/example/workflow/exception/ErrorResponse.java)
- [src/main/java/com/example/workflow/mapper/CentralMapperConfig.java](../src/main/java/com/example/workflow/mapper/CentralMapperConfig.java)
- [src/main/java/com/example/workflow/nume/AssignmentReleaseReason.java](../src/main/java/com/example/workflow/nume/AssignmentReleaseReason.java)
- [src/main/java/com/example/workflow/nume/AssignmentSource.java](../src/main/java/com/example/workflow/nume/AssignmentSource.java)
- [src/main/java/com/example/workflow/nume/AssignmentStatus.java](../src/main/java/com/example/workflow/nume/AssignmentStatus.java)
- [src/main/java/com/example/workflow/nume/CancellationSource.java](../src/main/java/com/example/workflow/nume/CancellationSource.java)
- [src/main/java/com/example/workflow/nume/ChangeRequestDecision.java](../src/main/java/com/example/workflow/nume/ChangeRequestDecision.java)
- [src/main/java/com/example/workflow/nume/ChangeRequestStatus.java](../src/main/java/com/example/workflow/nume/ChangeRequestStatus.java)
- [src/main/java/com/example/workflow/nume/CustomRequestStatus.java](../src/main/java/com/example/workflow/nume/CustomRequestStatus.java)
- [src/main/java/com/example/workflow/nume/GuestWorkflowStatus.java](../src/main/java/com/example/workflow/nume/GuestWorkflowStatus.java)
- [src/main/java/com/example/workflow/nume/ManagerReviewDecision.java](../src/main/java/com/example/workflow/nume/ManagerReviewDecision.java)
- [src/main/java/com/example/workflow/nume/OrderAgreementDecision.java](../src/main/java/com/example/workflow/nume/OrderAgreementDecision.java)
- [src/main/java/com/example/workflow/nume/OrderAgreementStatus.java](../src/main/java/com/example/workflow/nume/OrderAgreementStatus.java)
- [src/main/java/com/example/workflow/nume/OrderItemProductionStatus.java](../src/main/java/com/example/workflow/nume/OrderItemProductionStatus.java)
- [src/main/java/com/example/workflow/nume/OrderItemSourceType.java](../src/main/java/com/example/workflow/nume/OrderItemSourceType.java)
- [src/main/java/com/example/workflow/nume/OrderProductionStatus.java](../src/main/java/com/example/workflow/nume/OrderProductionStatus.java)
- [src/main/java/com/example/workflow/nume/OrderStatus.java](../src/main/java/com/example/workflow/nume/OrderStatus.java)
- [src/main/java/com/example/workflow/nume/OrderType.java](../src/main/java/com/example/workflow/nume/OrderType.java)
- [src/main/java/com/example/workflow/nume/PaymentAttemptStatus.java](../src/main/java/com/example/workflow/nume/PaymentAttemptStatus.java)
- [src/main/java/com/example/workflow/nume/PaymentMethod.java](../src/main/java/com/example/workflow/nume/PaymentMethod.java)
- [src/main/java/com/example/workflow/nume/PaymentStatus.java](../src/main/java/com/example/workflow/nume/PaymentStatus.java)
- [src/main/java/com/example/workflow/nume/ProductAvailabilityStatus.java](../src/main/java/com/example/workflow/nume/ProductAvailabilityStatus.java)
- [src/main/java/com/example/workflow/nume/ProductReviewStatus.java](../src/main/java/com/example/workflow/nume/ProductReviewStatus.java)
- [src/main/java/com/example/workflow/nume/ProductTypeFieldKind.java](../src/main/java/com/example/workflow/nume/ProductTypeFieldKind.java)
- [src/main/java/com/example/workflow/nume/ProductionCheckpointStage.java](../src/main/java/com/example/workflow/nume/ProductionCheckpointStage.java)
- [src/main/java/com/example/workflow/nume/ProductionCheckpointStatus.java](../src/main/java/com/example/workflow/nume/ProductionCheckpointStatus.java)
- [src/main/java/com/example/workflow/nume/ProductionDecisionType.java](../src/main/java/com/example/workflow/nume/ProductionDecisionType.java)
- [src/main/java/com/example/workflow/nume/Role.java](../src/main/java/com/example/workflow/nume/Role.java)
- [src/main/java/com/example/workflow/ratelimit/RateLimitAlgorithm.java](../src/main/java/com/example/workflow/ratelimit/RateLimitAlgorithm.java)
- [src/main/java/com/example/workflow/ratelimit/RateLimitIdentity.java](../src/main/java/com/example/workflow/ratelimit/RateLimitIdentity.java)
- [src/main/java/com/example/workflow/repository/CartItemRepository.java](../src/main/java/com/example/workflow/repository/CartItemRepository.java)
- [src/main/java/com/example/workflow/repository/OrderReceiptRepository.java](../src/main/java/com/example/workflow/repository/OrderReceiptRepository.java)
- [src/main/java/com/example/workflow/service/redis/RedisStreamEventContext.java](../src/main/java/com/example/workflow/service/redis/RedisStreamEventContext.java)
