package com.liriosbeauty.Repository;

import com.liriosbeauty.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

        @Query(value = "SELECT * FROM customers c " +
            "WHERE regexp_replace(c.phone, '\\D', '', 'g') = regexp_replace(:phone, '\\D', '', 'g') " +
            "LIMIT 1", nativeQuery = true)
        Optional<Customer> findByPhoneNormalized(@Param("phone") String phone);

    boolean existsByPhone(String phone);

        @Query(value = "SELECT EXISTS(SELECT 1 FROM customers c " +
            "WHERE regexp_replace(c.phone, '\\D', '', 'g') = regexp_replace(:phone, '\\D', '', 'g'))",
            nativeQuery = true)
        boolean existsByPhoneNormalized(@Param("phone") String phone);
}