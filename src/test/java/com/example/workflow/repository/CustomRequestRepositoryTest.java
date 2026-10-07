package com.example.workflow.repository;

import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.User;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class CustomRequestRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomRequestRepository repository;

    @Test
    void listsMultipleOwnedDraftsByMostRecentlyUpdated() {
        persistUser("owner-1", "owner.one", "0900000101");
        CustomRequest older = repository.save(draft(
                "owner-1",
                "{\"shape\":\"round\"}",
                LocalDateTime.of(2026, 10, 7, 8, 0)
        ));
        CustomRequest newer = repository.save(draft(
                "owner-1",
                "{\"shape\":\"square\"}",
                LocalDateTime.of(2026, 10, 7, 9, 0)
        ));
        CustomRequest submitted = draft(
                "owner-1",
                "{\"shape\":\"triangle\"}",
                LocalDateTime.of(2026, 10, 7, 10, 0)
        );
        submitted.markSubmitted(88L, LocalDateTime.of(2026, 10, 7, 10, 5));
        repository.saveAndFlush(submitted);
        entityManager.clear();

        var result = repository.findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc(
                "owner-1",
                CustomRequestStatus.DRAFT,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(CustomRequest::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void ownerQueriesAndSubmitLockDoNotReturnAnotherUsersDraft() {
        persistUser("owner-2", "owner.two", "0900000102");
        persistUser("owner-3", "owner.three", "0900000103");
        CustomRequest saved = repository.saveAndFlush(draft(
                "owner-2",
                "{\"color\":\"blue\"}",
                LocalDateTime.of(2026, 10, 7, 8, 0)
        ));
        entityManager.clear();

        assertThat(repository.findByIdAndOwnerId(saved.getId(), "owner-3")).isEmpty();
        assertThat(repository.findOwnedByIdForUpdate(saved.getId(), "owner-3")).isEmpty();
        assertThat(repository.findOwnedByIdForUpdate(saved.getId(), "owner-2")).isPresent();
        assertThat(repository.existsByIdAndOwnerId(saved.getId(), "owner-2")).isTrue();
    }

    @Test
    void oneOrderCannotBeLinkedToTwoCustomRequests() {
        persistUser("owner-4", "owner.four", "0900000104");
        LocalDateTime submissionTime = LocalDateTime.of(2026, 10, 7, 11, 0);
        CustomRequest first = draft("owner-4", "{\"size\":\"M\"}", submissionTime.minusHours(1));
        first.markSubmitted(101L, submissionTime);
        repository.saveAndFlush(first);

        CustomRequest second = draft("owner-4", "{\"size\":\"L\"}", submissionTime.minusMinutes(30));
        second.markSubmitted(101L, submissionTime.plusMinutes(1));

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lifecycleCallbacksPopulateTimestampsAndVersion() {
        persistUser("owner-5", "owner.five", "0900000105");

        CustomRequest saved = repository.saveAndFlush(draft("owner-5", "{\"note\":\"sample\"}", null));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.isEditable()).isTrue();
    }

    private CustomRequest draft(String ownerId, String spec, LocalDateTime updatedAt) {
        CustomRequest request = new CustomRequest();
        request.setOwnerId(ownerId);
        request.setSpec(spec);
        request.setQuantity(1);
        request.setUpdatedAt(updatedAt);
        request.setCreatedAt(updatedAt);
        return request;
    }

    private void persistUser(String id, String username, String phone) {
        User user = new User();
        user.setId(id);
        user.setFirstname("First");
        user.setLastname("Last");
        user.setUsername(username);
        user.setGender("OTHER");
        user.setPhone(phone);
        user.setRole(Role.USER);
        user.setIsActive(true);
        entityManager.persist(user);
    }
}
