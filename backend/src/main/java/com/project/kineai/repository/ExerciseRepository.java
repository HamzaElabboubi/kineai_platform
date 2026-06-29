package com.project.kineai.repository;

import com.project.kineai.model.entity.Exercise;
import com.project.kineai.model.enums.BodyZone;
import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Pathology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByBodyZoneAndDifficultyLevel(BodyZone bodyZone, Level level);
    List<Exercise> findByBodyZone(BodyZone bodyZone);
}
