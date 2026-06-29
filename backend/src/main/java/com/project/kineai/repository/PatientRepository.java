    package com.project.kineai.repository;

    import com.project.kineai.model.entity.Patient;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;

    public interface PatientRepository extends JpaRepository<Patient, UUID> {
        Optional<Patient> findByUserId(UUID userId);
        List<Patient> findByKineId(UUID kineId);
        // PatientRepository.java
        long count(); // déjà fourni par JpaRepository, rien à ajouter
        List<Patient> findAll(); // déjà fourni aussi

        Optional<Patient> findByUser_Email(String email);
    }
