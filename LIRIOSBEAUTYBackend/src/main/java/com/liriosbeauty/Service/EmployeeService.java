package com.liriosbeauty.Service;

import com.liriosbeauty.Entity.Employee;
import com.liriosbeauty.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> getActive() {
        return employeeRepository.findByActiveTrue();
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee updated) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İşçi tapılmadı"));
        employee.setFullName(updated.getFullName());
        employee.setPhone(updated.getPhone());
        employee.setBaseSalary(updated.getBaseSalary());
        return employeeRepository.save(employee);
    }

    public void deactivate(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İşçi tapılmadı"));
        employee.setActive(false);
        employeeRepository.save(employee);
    }
}