package com.liriosbeauty.Service;

import com.liriosbeauty.DTO.EmployeeDTO;
import com.liriosbeauty.Entity.Employee;
import com.liriosbeauty.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<EmployeeDTO> getAll() {
        return employeeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<EmployeeDTO> getActive() {
        return employeeRepository.findByActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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

    private EmployeeDTO toDTO(Employee e) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(e.getId());
        dto.setFullName(e.getFullName());
        dto.setPhone(e.getPhone());
        dto.setBaseSalary(e.getBaseSalary());
        dto.setActive(e.isActive());
        dto.setHiredAt(e.getHiredAt());
        return dto;
    }
}