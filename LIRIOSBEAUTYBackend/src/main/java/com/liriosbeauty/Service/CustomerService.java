package com.liriosbeauty.Service;

import com.liriosbeauty.Entity.Customer;
import com.liriosbeauty.Repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAll() {
        return customerRepository.findAll();
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
}