/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.motollantas.MotoLlantasVirtual.ServiceImpl;

import com.motollantas.MotoLlantasVirtual.DTO.AdminDateDTO;
import com.motollantas.MotoLlantasVirtual.DTO.ClientDateDTO;
import com.motollantas.MotoLlantasVirtual.Service.RepairOrderService;
import com.motollantas.MotoLlantasVirtual.Service.ServiceTypeService;
import com.motollantas.MotoLlantasVirtual.Service.UserService;
import com.motollantas.MotoLlantasVirtual.dao.RepairOrderDao;
import com.motollantas.MotoLlantasVirtual.dao.UserDao;
import com.motollantas.MotoLlantasVirtual.domain.Employee;
import com.motollantas.MotoLlantasVirtual.domain.OrderPriority;
import com.motollantas.MotoLlantasVirtual.domain.OrderStatus;
import com.motollantas.MotoLlantasVirtual.domain.RepairOrder;
import com.motollantas.MotoLlantasVirtual.domain.ServiceType;
import com.motollantas.MotoLlantasVirtual.domain.User;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 *
 * @author esteb
 */
@Service
public class RepairOrderServiceImpl implements RepairOrderService {

    @Autowired
    RepairOrderDao repair;

    @Autowired
    UserDao user;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    ServiceTypeService serviceTypeService;

    @Autowired
    UserService userService;

    @Override
    public void createDateClient(ClientDateDTO clientDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User client = user.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario No encontrado"));
        ServiceType serviceType = serviceTypeService.findById(clientDto.getServiceTypeId());

        RepairOrder orden = new RepairOrder();
        orden.setUser(client);
        orden.setFullName(client.getFullName());
        orden.setIdentification(client.getIdentification());
        orden.setModelName(clientDto.getModelName());
        orden.setLicensePlate(clientDto.getLicensePlate());
        orden.setAppointmentDate(clientDto.getAppointmentDate());
        orden.setServiceType(serviceType);
        orden.setBrand(clientDto.getBrand());
        orden.setYear(clientDto.getYear());

        repair.save(orden);
    }

    @Override
    public boolean existsByAppointmentDate(LocalDateTime dateTime) {
        return repair.existsByAppointmentDate(dateTime);
    }

    @Override
    public boolean hasOverlappingAppointment(LocalDateTime newStart, ServiceType serviceType) {
        LocalDateTime newEnd = newStart.plus(serviceType.getDuration());

        LocalDateTime startOfDay = newStart.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = newStart.toLocalDate().atTime(LocalTime.MAX);

        List<RepairOrder> appointments = repair.findAppointmentsInDay(startOfDay, endOfDay);

        for (RepairOrder r : appointments) {
            LocalDateTime existingStart = r.getAppointmentDate();
            LocalDateTime existingEnd = existingStart.plus(r.getServiceType().getDuration());

            if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<ClientDateDTO> getAppointmentbyUser(Long userId) {
        List<RepairOrder> orders = repair.findByUserId(userId);
        return orders.stream().map(order -> {
            ClientDateDTO dto = modelMapper.map(order, ClientDateDTO.class);
            if (order.getServiceType() != null) {
                dto.setServiceTypeName(order.getServiceType().getServiceName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<RepairOrder> findById(Long id) {
        return repair.findById(id);
    }

    @Override
    public void updateDateClient(ClientDateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User client = user.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario No encontrado"));
        RepairOrder orden = repair.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        ServiceType serviceType = serviceTypeService.findById(dto.getServiceTypeId());

        orden.setUser(client);
        orden.setFullName(client.getFullName());
        orden.setIdentification(client.getIdentification());
        orden.setAppointmentDate(dto.getAppointmentDate());
        orden.setServiceType(serviceType);
        orden.setBrand(dto.getBrand());
        orden.setModelName(dto.getModelName());
        orden.setYear(dto.getYear());
        orden.setLicensePlate(dto.getLicensePlate());

        repair.save(orden);
    }

    @Override
    public void deleteById(Long id) {
        repair.deleteById(id);
    }

    @Override
    public List<RepairOrder> findByStatus(OrderStatus status) {
        return repair.findByOrderStatus(status);
    }

    @Override
    public void createFromAdmin(AdminDateDTO dto, ServiceType serviceType) {
        RepairOrder order = new RepairOrder();

        // Buscar usuario por identificación
        Optional<User> userOpt = userService.findByIdentification(dto.getIdentification());

        // Si existe, asociarlo a la orden
        userOpt.ifPresent(order::setUser);

        order.setFullName(dto.getFullName());
        order.setIdentification(dto.getIdentification());
        order.setBrand(dto.getBrand());
        order.setModelName(dto.getModelName());
        order.setYear(dto.getYear());
        order.setLicensePlate(dto.getLicensePlate());
        order.setAppointmentDate(dto.getAppointmentDate());
        order.setServiceType(serviceType);
        order.setOrderStatus(OrderStatus.NUEVO);
        order.setPriority(OrderPriority.BAJA);

        repair.save(order);
    }

    @Override
    public void updateFromAdmin(RepairOrder updatedOrder) {
        RepairOrder existingOrder = repair.findById(updatedOrder.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        existingOrder.setFullName(updatedOrder.getFullName());
        existingOrder.setIdentification(updatedOrder.getIdentification());
        existingOrder.setBrand(updatedOrder.getBrand());
        existingOrder.setModelName(updatedOrder.getModelName());
        existingOrder.setYear(updatedOrder.getYear());
        existingOrder.setDisplacement(updatedOrder.getDisplacement());
        existingOrder.setKilometraje(updatedOrder.getKilometraje());
        existingOrder.setLicensePlate(updatedOrder.getLicensePlate());
        existingOrder.setColor(updatedOrder.getColor());
        existingOrder.setAppointmentDate(updatedOrder.getAppointmentDate());
        existingOrder.setOrderStatus(updatedOrder.getOrderStatus());
        existingOrder.setServiceType(updatedOrder.getServiceType());
        existingOrder.setMechanic(updatedOrder.getMechanic());
        existingOrder.setPriority(updatedOrder.getPriority());
        existingOrder.setProblemDescription(updatedOrder.getProblemDescription());

        repair.save(existingOrder);
    }

    @Override
    public List<RepairOrder> findByStatusASC(OrderStatus status) {
        return repair.findByOrderStatusOrderByAppointmentDateAsc(status);
    }

    @Override
    public List<RepairOrder> findByMechanicAndOrderStatusOrderByAppointmentDateAsc(Employee mechanic, OrderStatus status) {
        return repair.findByMechanicAndOrderStatusOrderByAppointmentDateAsc(mechanic, status);
    }

    @Override
    public void updateFromAdminOrMechanic(RepairOrder updatedOrder, Employee currentEmployee) {
        RepairOrder existingOrder = repair.findById(updatedOrder.getId())
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        boolean isAdmin = currentEmployee.getRoles().stream()
                .anyMatch(role -> role.equalsIgnoreCase("ADMIN"));

        boolean isMechanic = currentEmployee.getRoles().stream()
                .anyMatch(role -> role.equalsIgnoreCase("MECANICO"));

        if (isAdmin) {
            existingOrder.setAppointmentDate(updatedOrder.getAppointmentDate());
            existingOrder.setFullName(updatedOrder.getFullName());
            existingOrder.setIdentification(updatedOrder.getIdentification());
            existingOrder.setBrand(updatedOrder.getBrand());
            existingOrder.setModelName(updatedOrder.getModelName());
            existingOrder.setYear(updatedOrder.getYear());
            existingOrder.setDisplacement(updatedOrder.getDisplacement());
            existingOrder.setKilometraje(updatedOrder.getKilometraje());
            existingOrder.setLicensePlate(updatedOrder.getLicensePlate());
            existingOrder.setColor(updatedOrder.getColor());
            existingOrder.setServiceType(updatedOrder.getServiceType());
            existingOrder.setPriority(updatedOrder.getPriority());
            existingOrder.setOrderStatus(updatedOrder.getOrderStatus());
            existingOrder.setMechanic(updatedOrder.getMechanic());
            existingOrder.setProblemDescription(updatedOrder.getProblemDescription());
        } else if (isMechanic) {
            existingOrder.setOrderStatus(updatedOrder.getOrderStatus());
            existingOrder.setBrand(updatedOrder.getBrand());
            existingOrder.setModelName(updatedOrder.getModelName());
            existingOrder.setYear(updatedOrder.getYear());
            existingOrder.setDisplacement(updatedOrder.getDisplacement());
            existingOrder.setKilometraje(updatedOrder.getKilometraje());
            existingOrder.setLicensePlate(updatedOrder.getLicensePlate());
            existingOrder.setColor(updatedOrder.getColor());
            existingOrder.setServiceType(updatedOrder.getServiceType());
            existingOrder.setPriority(updatedOrder.getPriority());
            existingOrder.setOrderStatus(updatedOrder.getOrderStatus());
            existingOrder.setProblemDescription(updatedOrder.getProblemDescription());
        }

        repair.save(existingOrder);
    }

    @Override
    public List<RepairOrder> findAll() {
        return repair.findAll();
    }

}
