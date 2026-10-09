package com.example.workflow.repository;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderItemAssignment;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.ProductionCheckpointImage;
import com.example.workflow.entity.ProductionDecision;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.nume.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class ProductionRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderItemAssignmentRepository assignmentRepository;

    @Autowired
    private ProductionCheckpointRepository checkpointRepository;

    @Test
    void persistsAssignmentCheckpointImagesAndDecisionAsOneTimeline() {
        OrderItem item = persistHandmadeItem();
        User staff = persistUser("staff-1", "staff.one", "0900000001", Role.STAFF);
        User manager = persistUser("manager-1", "manager.one", "0900000002", Role.MANAGER);

        OrderItemAssignment assignment = new OrderItemAssignment();
        assignment.setOrderItem(item);
        assignment.setAssignedStaff(staff);
        assignment.setAssignedBy(manager);
        assignmentRepository.saveAndFlush(assignment);

        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setOrderItem(item);
        checkpoint.setStage(ProductionCheckpointStage.INITIAL_SHAPE);
        checkpoint.setAttemptNumber(1);
        checkpoint.setNote("Initial checkpoint");
        checkpoint.setSubmittedBy(staff);

        ProductionCheckpointImage image = new ProductionCheckpointImage();
        image.setImageUrl("https://cdn.example/initial.jpg");
        image.setPublicId("initial-1");
        image.setDisplayOrder(0);
        checkpoint.addImage(image);

        ProductionDecision decision = new ProductionDecision();
        decision.setDecisionType(ProductionDecisionType.APPROVED);
        decision.setReason("Approved");
        decision.setDecidedBy(manager);
        checkpoint.attachDecision(decision);
        checkpointRepository.saveAndFlush(checkpoint);

        entityManager.clear();

        assertThat(assignmentRepository.findByOrderItem_Id(item.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getAssignedStaff().getId()).isEqualTo("staff-1");
                    assertThat(saved.getAssignedBy().getId()).isEqualTo("manager-1");
                    assertThat(saved.getAssignedAt()).isNotNull();
                    assertThat(saved.getVersion()).isNotNull();
                });
        assertThat(checkpointRepository.findByOrderItem_IdOrderBySubmittedAtAsc(item.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getImages()).singleElement()
                            .extracting(ProductionCheckpointImage::getImageUrl)
                            .isEqualTo("https://cdn.example/initial.jpg");
                    assertThat(saved.getDecision().getDecisionType()).isEqualTo(ProductionDecisionType.APPROVED);
                    assertThat(saved.getSubmittedAt()).isNotNull();
                });
    }

    @Test
    void enforcesOneCurrentAssignmentPerOrderItem() {
        OrderItem item = persistHandmadeItem();
        User staff = persistUser("staff-2", "staff.two", "0900000003", Role.STAFF);
        User manager = persistUser("manager-2", "manager.two", "0900000004", Role.MANAGER);
        assignmentRepository.saveAndFlush(assignment(item, staff, manager));

        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(assignment(item, staff, manager)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesUniqueCheckpointAttemptForItemAndStage() {
        OrderItem item = persistHandmadeItem();
        User staff = persistUser("staff-3", "staff.three", "0900000005", Role.STAFF);
        checkpointRepository.saveAndFlush(checkpoint(item, staff, 1));

        assertThatThrownBy(() -> checkpointRepository.saveAndFlush(checkpoint(item, staff, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private OrderItemAssignment assignment(OrderItem item, User staff, User manager) {
        OrderItemAssignment assignment = new OrderItemAssignment();
        assignment.setOrderItem(item);
        assignment.setAssignedStaff(staff);
        assignment.setAssignedBy(manager);
        return assignment;
    }

    private ProductionCheckpoint checkpoint(OrderItem item, User staff, int attempt) {
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setOrderItem(item);
        checkpoint.setStage(ProductionCheckpointStage.INITIAL_SHAPE);
        checkpoint.setAttemptNumber(attempt);
        checkpoint.setSubmittedBy(staff);
        return checkpoint;
    }

    private OrderItem persistHandmadeItem() {
        Product product = new Product();
        product.setProductName("Handmade product");
        product.setPrice(100.0);
        product.setHandmade(true);
        entityManager.persist(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Default");
        variant.setPrice(100.0);
        variant.setQuantity(5);
        entityManager.persist(variant);

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setProductionStatus(OrderProductionStatus.WAITING_PRODUCTION);
        order.setStartOrderTime(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        entityManager.persist(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductVariant(variant);
        item.setPrice(100.0);
        item.setQuantity(1);
        item.setHandmade(true);
        item.setProductionStatus(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }

    private User persistUser(String id, String username, String phone, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstname("First");
        user.setLastname("Last");
        user.setUsername(username);
        user.setGender("OTHER");
        user.setPhone(phone);
        user.setRole(role);
        user.setIsActive(true);
        entityManager.persist(user);
        return user;
    }
}
