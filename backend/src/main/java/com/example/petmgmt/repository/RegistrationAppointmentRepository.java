package com.example.petmgmt.repository;

import com.example.petmgmt.domain.entity.RegistrationAppointment;
import com.example.petmgmt.storage.FileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class RegistrationAppointmentRepository {

    private static final String FILE_NAME = "registration_appointments";

    @Autowired
    private FileStorage fileStorage;

    public RegistrationAppointment save(RegistrationAppointment appointment) {
        List<RegistrationAppointment> appointments = loadAll();

        if (appointment.getId() == null) {
            appointment.setId(fileStorage.nextId(FILE_NAME));
            appointment.setCreatedAt(LocalDateTime.now());
            appointment.setUpdatedAt(LocalDateTime.now());
            appointments.add(appointment);
        } else {
            appointment.setUpdatedAt(LocalDateTime.now());
            appointments = appointments.stream()
                    .map(item -> item.getId().equals(appointment.getId()) ? appointment : item)
                    .collect(Collectors.toList());
        }

        fileStorage.save(FILE_NAME, appointments);
        return appointment;
    }

    public Optional<RegistrationAppointment> findById(Long id) {
        return loadAll().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    public List<RegistrationAppointment> findAll() {
        return new ArrayList<>(loadAll());
    }

    public List<RegistrationAppointment> findAll(Predicate<RegistrationAppointment> predicate) {
        return loadAll().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private List<RegistrationAppointment> loadAll() {
        return fileStorage.load(FILE_NAME, new TypeReference<>() {});
    }
}
