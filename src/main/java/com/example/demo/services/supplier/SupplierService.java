package com.example.demo.services.supplier;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.supplier.CreateSupplierDto;
import com.example.demo.dtos.supplier.UpdateSupplierDto;
import com.example.demo.models.Supplier;
import com.example.demo.repositories.SupplierRepository;
import com.example.demo.services.notification.NotificationService;

@Service
public class SupplierService implements ISupplierService {
    @Autowired
    private final SupplierRepository supplierRepository;
    private final NotificationService notificationService;

    public SupplierService(SupplierRepository supplierRepository, NotificationService notificationService) {
        this.supplierRepository = supplierRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Supplier createSupplier(CreateSupplierDto createDto) {
        Supplier supplier = new Supplier();
        supplier.setName(createDto.getName());
        supplier.setAddress(createDto.getAddress());
        supplier.setContact(createDto.getContact());
        supplier.setPhone(createDto.getPhone());
        Supplier saved = supplierRepository.save(supplier);
        try {
            notificationService.logAction("Supplier Onboarded", "Supplier '" + saved.getName() + "' was added to the network (ID: " + saved.getId() + ").");
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public Supplier updateSupplier(UpdateSupplierDto updateDto, Long id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow();
        supplier.setName(updateDto.getName());
        supplier.setAddress(updateDto.getAddress());
        supplier.setContact(updateDto.getContact());
        supplier.setPhone(updateDto.getPhone());
        Supplier saved = supplierRepository.save(supplier);
        try {
            notificationService.logAction("Supplier Updated", "Supplier '" + saved.getName() + "' profile was updated (ID: " + saved.getId() + ").");
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id).orElseThrow();
    }
}
