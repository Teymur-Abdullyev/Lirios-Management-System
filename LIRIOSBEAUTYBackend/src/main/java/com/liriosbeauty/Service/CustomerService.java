package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.CustomerDTO;
import com.liriosbeauty.Entity.Customer;
import com.liriosbeauty.Repository.CustomerRepository;
import com.liriosbeauty.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public List<CustomerDTO> getAll() {
        return customerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Customer save(Customer customer) {
        String normalizedPhone = normalizePhone(customer.getPhone());
        customer.setPhone(normalizedPhone);

        if (customerRepository.existsByPhoneNormalized(normalizedPhone)) {
            throw new RuntimeException("Bu nömrə artıq qeydiyyatdadır");
        }
        return customerRepository.save(customer);
    }

    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhoneNormalized(phone);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        boolean hasPlusPrefix = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        return hasPlusPrefix ? "+" + digits : digits;
    }

    private CustomerDTO toDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(c.getId());
        dto.setFullName(c.getFullName());
        dto.setPhone(c.getPhone());
        dto.setRegisteredAt(c.getCreatedAt());
        dto.setCurrentDebt(orderRepository.getDebtByCustomerId(c.getId()));
        return dto;
    }
}