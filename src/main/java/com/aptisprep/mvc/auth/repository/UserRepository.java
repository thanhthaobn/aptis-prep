package com.aptisprep.mvc.auth.repository;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

    @Query(
            value =
                    "select u.* from users u where u.role = 'STUDENT' "
                            + "and (:status is not null or u.status <> 'INACTIVE') "
                            + "and (:status is null or u.status = :status) "
                            + "and (:pattern = '%' or lower(u.full_name) like :pattern escape '\\' "
                            + "or lower(u.email) like :pattern escape '\\') "
                            + "order by u.created_at desc, u.id desc",
            countQuery =
                    "select count(*) from users u where u.role = 'STUDENT' "
                            + "and (:status is not null or u.status <> 'INACTIVE') "
                            + "and (:status is null or u.status = :status) "
                            + "and (:pattern = '%' or lower(u.full_name) like :pattern escape '\\' "
                            + "or lower(u.email) like :pattern escape '\\')",
            nativeQuery = true)
    Page<User> searchStudents(
            @Param("pattern") String pattern, @Param("status") String status, Pageable pageable);
}

