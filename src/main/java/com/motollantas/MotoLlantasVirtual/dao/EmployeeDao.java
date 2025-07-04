/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.motollantas.MotoLlantasVirtual.dao;

import com.motollantas.MotoLlantasVirtual.domain.Employee;
import com.motollantas.MotoLlantasVirtual.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDao extends JpaRepository<Employee, Long> {

    List<Employee> findByIdentificationContainingIgnoreCase(String cedula);

    List<Employee> findByRolesContaining(String rol);

    List<Employee> findByActiveTrue();

    List<Employee> findByActiveFalse();
    
    Optional<Employee> findByUser(User user);
}
