package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.CustomerDTO;
import com.liriosbeauty.Entity.Customer;
import com.liriosbeauty.Repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<CustomerDTO> getAll() {
        return customerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Customer save(Customer customer) {
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw new RuntimeException("Bu nömrə artıq qeydiyyatdadır");
        }
        return customerRepository.save(customer);
    }

    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    private CustomerDTO toDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(c.getId());
        dto.setFullName(c.getFullName());
        dto.setPhone(c.getPhone());
        dto.setRegisteredAt(c.getCreatedAt());
        return dto;
    }
}