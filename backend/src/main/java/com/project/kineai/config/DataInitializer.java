package com.project.kineai.config;

import com.project.kineai.model.entity.Exercise;
import com.project.kineai.model.entity.User;
import com.project.kineai.model.enums.BodyZone;
import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Role;
import com.project.kineai.repository.ExerciseRepository;
import com.project.kineai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExerciseRepository exerciseRepository;

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists();
        initExercises();
    }

    private void createAdminIfNotExists() {
        // Vérifier si l'admin existe déjà
        if (userRepository.existsByEmail("admin@kineai.com")) {
            log.info("Admin déjà existant — aucune création");
            return;
        }

        // Créer le compte admin
        User admin = User.builder()
                .email("admin@kineai.com")
                .password(passwordEncoder.encode("Admin@2025"))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("✅ Compte admin créé — admin@kineai.com");
    }

    private void initExercises() {
        if (exerciseRepository.count() > 0) {
            log.info("Exercices déjà présents — initialisation ignorée");
            return;
        }

        List<Exercise> exercises = List.of(

                // ═══════════ GENOU ═══════════
                Exercise.builder()
                        .name("Flexion genou — initiation")
                        .description("Pliez doucement le genou vers"
                                + " l'arrière, sans forcer, jusqu'à"
                                + " ressentir une légère tension.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(110)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                Exercise.builder()
                        .name("Flexion genou — renforcement")
                        .description("Pliez le genou à 90° en"
                                + " contrôlant la descente et la montée.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(90)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                Exercise.builder()
                        .name("Extension genou complète")
                        .description("Étendez complètement la jambe"
                                + " puis revenez à la position de"
                                + " départ avec contrôle musculaire.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(170)
                        .toleranceDegree(10)
                        .recommendedDuration(30)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                // ✅ NOUVEAU — 2ème exercice par niveau, GENOU
                Exercise.builder()
                        .name("Extension genou — initiation")
                        .description("Étendez doucement la jambe"
                                + " devant vous, sans forcer, puis"
                                + " repliez lentement.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(140)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                Exercise.builder()
                        .name("Squat partiel contrôlé")
                        .description("Fléchissez les genoux en"
                                + " gardant le dos droit, comme"
                                + " pour vous asseoir, puis remontez.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(110)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                Exercise.builder()
                        .name("Fente arrière genou")
                        .description("Reculez une jambe en fléchissant"
                                + " le genou avant à angle contrôlé,"
                                + " puis revenez à la position initiale.")
                        .bodyZone(BodyZone.GENOU)
                        .targetAngle(150)
                        .toleranceDegree(10)
                        .recommendedDuration(30)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_HIP,LEFT_KNEE,LEFT_ANKLE")
                        .build(),

                // ═══════════ EPAULE ═══════════
                Exercise.builder()
                        .name("Élévation épaule — légère")
                        .description("Levez doucement le bras devant"
                                + " vous jusqu'à hauteur d'épaule,"
                                + " sans douleur.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(60)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Élévation épaule — 90°")
                        .description("Élevez le bras latéralement"
                                + " jusqu'à 90°, maintenez puis"
                                + " redescendez lentement.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(90)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(10)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Rotation épaule complète")
                        .description("Effectuez une élévation complète"
                                + " du bras au-dessus de la tête avec"
                                + " contrôle de la trajectoire.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(150)
                        .toleranceDegree(12)
                        .recommendedDuration(30)
                        .repsTarget(14)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                // ✅ NOUVEAU — 2ème exercice par niveau, EPAULE
                Exercise.builder()
                        .name("Rotation épaule — légère")
                        .description("Effectuez de petits cercles"
                                + " avec le bras tendu, dans un"
                                + " mouvement doux et contrôlé.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(45)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Élévation latérale épaule")
                        .description("Levez le bras sur le côté"
                                + " jusqu'à l'horizontale, maintenez"
                                + " puis redescendez avec contrôle.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(100)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(10)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Élévation complète résistance")
                        .description("Levez le bras complètement"
                                + " au-dessus de la tête avec un"
                                + " mouvement lent et puissant.")
                        .bodyZone(BodyZone.EPAULE)
                        .targetAngle(165)
                        .toleranceDegree(12)
                        .recommendedDuration(30)
                        .repsTarget(14)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),


                // ═══════════ DOS ═══════════
                Exercise.builder()
                        .name("Redressement dos — doux")
                        .description("Redressez progressivement le"
                                + " haut du dos en gardant les"
                                + " abdominaux contractés.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(100)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Extension dos — intermédiaire")
                        .description("Redressez le dos jusqu'à la"
                                + " position verticale, maintenez"
                                + " 2 secondes.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(160)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(10)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Gainage dorsal dynamique")
                        .description("Alternez redressement complet"
                                + " et flexion contrôlée du dos avec"
                                + " rythme soutenu.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(170)
                        .toleranceDegree(10)
                        .recommendedDuration(35)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                // ✅ NOUVEAU — 2ème exercice par niveau, DOS
                Exercise.builder()
                        .name("Inclinaison latérale dos")
                        .description("Inclinez doucement le buste"
                                + " sur le côté en gardant les"
                                + " hanches stables.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(110)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Rotation tronc contrôlée")
                        .description("Tournez le haut du corps"
                                + " progressivement en gardant le"
                                + " bassin fixe, puis revenez.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(150)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(10)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Extension dorsale complète")
                        .description("Redressez complètement le dos"
                                + " avec un mouvement ample et"
                                + " maîtrisé sur toute l'amplitude.")
                        .bodyZone(BodyZone.DOS)
                        .targetAngle(175)
                        .toleranceDegree(10)
                        .recommendedDuration(30)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                // ═══════════ HANCHE ═══════════
                Exercise.builder()
                        .name("Mobilisation hanche — douce")
                        .description("Levez doucement le genou vers"
                                + " l'avant en gardant le dos droit,"
                                + " sans déséquilibre.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(100)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Flexion hanche — contrôlée")
                        .description("Levez le genou à hauteur de la"
                                + " hanche, maintenez l'équilibre"
                                + " puis redescendez lentement.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(90)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Fente avant contrôlée")
                        .description("Réalisez une fente avant en"
                                + " gardant l'alignement du genou et"
                                + " de la hanche.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(120)
                        .toleranceDegree(12)
                        .recommendedDuration(30)
                        .repsTarget(14)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                // ✅ NOUVEAU — 2ème exercice par niveau, HANCHE
                Exercise.builder()
                        .name("Abduction hanche légère")
                        .description("Écartez doucement la jambe"
                                + " sur le côté en gardant le"
                                + " buste stable.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(110)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(8)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Extension hanche contrôlée")
                        .description("Tendez la jambe vers l'arrière"
                                + " en gardant l'équilibre, puis"
                                + " ramenez-la lentement.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(100)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                Exercise.builder()
                        .name("Fente latérale hanche")
                        .description("Réalisez une fente sur le côté"
                                + " en gardant l'alignement du genou"
                                + " et de la hanche.")
                        .bodyZone(BodyZone.HANCHE)
                        .targetAngle(130)
                        .toleranceDegree(12)
                        .recommendedDuration(30)
                        .repsTarget(14)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_HIP,LEFT_KNEE")
                        .build(),

                // ═══════════ COUDE ═══════════
                Exercise.builder()
                        .name("Flexion coude — légère")
                        .description("Pliez doucement le coude vers"
                                + " l'épaule, sans à-coups, en"
                                + " contrôlant le mouvement.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(90)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(10)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Flexion-extension coude")
                        .description("Alternez flexion complète et"
                                + " extension du coude avec un"
                                + " rythme régulier.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(60)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Extension coude résistance")
                        .description("Étendez complètement le bras"
                                + " avec un mouvement lent et"
                                + " un contrôle musculaire maximal.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(170)
                        .toleranceDegree(10)
                        .recommendedDuration(30)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build() ,


                // ✅ NOUVEAU — 2ème exercice par niveau, COUDE
                Exercise.builder()
                        .name("Extension coude légère")
                        .description("Étendez doucement le bras"
                                + " depuis le coude plié, sans"
                                + " forcer, puis repliez.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(120)
                        .toleranceDegree(20)
                        .recommendedDuration(20)
                        .repsTarget(10)
                        .difficultyLevel(Level.DEBUTANT)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Pronation-supination coude")
                        .description("Tournez l'avant-bras alternant"
                                + " paume vers le haut et vers le"
                                + " bas, coude fixe.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(75)
                        .toleranceDegree(15)
                        .recommendedDuration(25)
                        .repsTarget(12)
                        .difficultyLevel(Level.INTERMEDIAIRE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build(),

                Exercise.builder()
                        .name("Flexion-extension résistance")
                        .description("Alternez flexion et extension"
                                + " complètes du coude avec un"
                                + " contrôle musculaire maximal.")
                        .bodyZone(BodyZone.COUDE)
                        .targetAngle(165)
                        .toleranceDegree(10)
                        .recommendedDuration(30)
                        .repsTarget(15)
                        .difficultyLevel(Level.AVANCE)
                        .mediapipeJoints("LEFT_SHOULDER,LEFT_ELBOW,LEFT_WRIST")
                        .build()
        );

        exerciseRepository.saveAll(exercises);
        log.info("{} exercices initialisés avec succès"
                + " (5 zones × 3 niveaux)", exercises.size());
    }
}
