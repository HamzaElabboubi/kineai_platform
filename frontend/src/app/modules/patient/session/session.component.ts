import {
  Component, inject, OnInit, OnDestroy,
  signal, ViewChild, ElementRef, NgZone,
  ChangeDetectionStrategy, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, retry } from 'rxjs/operators';
import { ExerciseService }
  from '../../../core/services/exercise.service';
import { SessionService }
  from '../../../core/services/session.service';
import { PatientService }
  from '../../../core/services/patient.service';
import { AuthService }
  from '../../../core/services/auth.service';
import {
  ExerciseResponse,
  SessionResponse
} from '../../../core/models/session.model';
import { PatientSidebarComponent }
  from '../../../shared/components/patient-sidebar/patient-sidebar.component';
import { RehabPlanService }
  from '../../../core/services/rehab-plan.service';

type SessionPhase =
  'SELECT'       |
  'INSTRUCTIONS' |
  'CALIBRATION'  |
  'SESSION'      |
  'COMPLETED'    |
  'ERROR';

@Component({
  selector: 'app-session',
  standalone: true,
  imports: [CommonModule, PatientSidebarComponent],
  templateUrl: './session.component.html',
  styleUrl: './session.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SessionComponent
  implements OnInit, OnDestroy {

  @ViewChild('videoEl')
  videoRef!: ElementRef<HTMLVideoElement>;

  @ViewChild('canvasEl')
  canvasRef!: ElementRef<HTMLCanvasElement>;

  private exerciseService = inject(ExerciseService);
  private sessionService  = inject(SessionService);
  private authService     = inject(AuthService);
  private router          = inject(Router);
  private ngZone          = inject(NgZone);
  private rehabPlanService = inject(RehabPlanService);
  private patientService  = inject(PatientService);

  // ✅ Subject de destruction — désabonnement global
  private destroy$ = new Subject<void>();

  // ══════════════════════════════════════════
  // SIGNAUX ÉTAT
  // ══════════════════════════════════════════
  activePlanId     = signal<string | null>(null);
  phase            = signal<SessionPhase>('SELECT');
  exercises        = signal<ExerciseResponse[]>([]);
  selectedEx       = signal<ExerciseResponse | null>(null);
  currentSession   = signal<SessionResponse | null>(null);
  completedSession = signal<SessionResponse | null>(null);
  currentStep      = signal<number>(0);

  // ── Métriques temps réel ───────────────────
  isConformant   = signal<boolean>(false);
  conformityPct  = signal<number>(0);
  repsCompleted  = signal<number>(0);
  calibCountdown = signal<number>(2);
  sessionTime    = signal<number>(0);

  // ✅ Message visuel STABLE — ne change que sur
  // événement réel, jamais à chaque frame
  feedback       = signal<string>('');
  errorMsg       = signal<string>('');
  voiceEnabled   = signal<boolean>(true);

  // ✅ Signal réactif pour le niveau patient
  patientLevelSignal = signal<string>('Inconnu');

  // ── Animations ────────────────────────────
  badgeUnlocked  = signal<string | null>(null);
  xpGained       = signal<number>(0);

  // ✅ États de chargement scripts
  loadingScripts = signal<boolean>(false);

  // ══════════════════════════════════════════
  // ÉTAPES INSTRUCTIONS
  // ══════════════════════════════════════════
  readonly steps = [
    {
      icon: '📏',
      title: 'Positionnez-vous correctement',
      desc:
        'Placez-vous à environ 1,5 mètre de la'
        + ' caméra. Votre corps entier doit être'
        + ' visible à l\'écran.'
    },
    {
      icon: '💡',
      title: 'Vérifiez l\'éclairage',
      desc:
        'Assurez-vous d\'être bien éclairé de face.'
        + ' Évitez les contre-jours derrière vous.'
    },
    {
      icon: '👕',
      title: 'Portez une tenue adaptée',
      desc:
        'Des vêtements ajustés permettent à la'
        + ' caméra de mieux détecter vos'
        + ' articulations.'
    },
    {
      icon: '📝',
      title: 'Votre exercice',
      desc:
        'Lisez attentivement les paramètres de'
        + ' l\'exercice ci-dessous avant de commencer.'
    }
  ];

  // ══════════════════════════════════════════
  // MAPPING LANDMARKS MEDIAPIPE
  // ══════════════════════════════════════════
  private readonly LANDMARK_INDEX:
    Record<string, number> = {
    'NOSE': 0,
    'LEFT_SHOULDER': 11,  'RIGHT_SHOULDER': 12,
    'LEFT_ELBOW': 13,     'RIGHT_ELBOW': 14,
    'LEFT_WRIST': 15,     'RIGHT_WRIST': 16,
    'LEFT_HIP': 23,       'RIGHT_HIP': 24,
    'LEFT_KNEE': 25,      'RIGHT_KNEE': 26,
    'LEFT_ANKLE': 27,     'RIGHT_ANKLE': 28,
    'LEFT_EAR': 7,        'RIGHT_EAR': 8
  };

  private readonly JOINT_NAMES_FR:
    Record<number, string> = {
    11: 'épaule gauche',   12: 'épaule droite',
    13: 'coude gauche',    14: 'coude droit',
    15: 'poignet gauche',  16: 'poignet droit',
    23: 'hanche gauche',   24: 'hanche droite',
    25: 'genou gauche',    26: 'genou droit',
    27: 'cheville gauche', 28: 'cheville droite'
  };

  // ══════════════════════════════════════════
  // PRIVÉ
  // ══════════════════════════════════════════
  private pose: any = null;
  private camera: any = null;
  private metricsInterval:
    ReturnType<typeof setInterval> | null = null;
  private timerInterval:
    ReturnType<typeof setInterval> | null = null;
  private scriptLoaded = false;
  private calibStable = 0;
  private allScores: number[] = [];
  private frameAngles: Record<string, number> = {};
  private autoCompleting = false;

  // ── Algorithme répétitions ─────────────────
  private repPhase:
    'NEUTRAL'    |
    'GOING_DOWN' |
    'AT_BOTTOM'  |
    'GOING_UP'   = 'NEUTRAL';
  private angleHistory: number[] = [];
  private lastAngle = 0;
  private readonly HISTORY_SIZE = 5;

  // ── Détection vitesse ─────────────────────
  private lastFrameTime = 0;

  // ── Maintien ──────────────────────────────
  private holdStartTime = 0;
  private holdAnnounced = false;

  // ── Speech ────────────────────────────────
  // ✅ Guard speechSynthesis multi-navigateur
  private readonly speech =
    typeof window !== 'undefined'
    && 'speechSynthesis' in window
      ? window.speechSynthesis : null;
  private lastSpokenMsg = '';
  private lastSpeakTime = 0;
  private lastVisibilityWarning = 0;

  // ✅ Métriques précédentes — évite envois inutiles
  private lastSentConformityPct = -1;
  private lastSentReps = -1;

  protected readonly Math = Math;

  // ══════════════════════════════════════════
  // ✅ beforeunload — interrompt la session si
  // l'onglet est fermé en pleine séance
  // ══════════════════════════════════════════
  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    const session = this.currentSession();
    if (session && (
      this.phase() === 'SESSION' ||
      this.phase() === 'CALIBRATION'
    )) {
      this.sessionService
        .interrupt(session.id)
        .subscribe();
      event.preventDefault();
      event.returnValue =
        'Une séance est en cours.'
        + ' Êtes-vous sûr de vouloir quitter ?';
    }
  }

  // ══════════════════════════════════════════
  // GETTERS
  // ══════════════════════════════════════════
  get patientName(): string {
    return this.authService.getFullName() || 'Patient';
  }

  get patientLevel(): string {
    return this.patientLevelSignal();
  }

  get progressPct(): number {
    const target = this.selectedEx()?.repsTarget ?? 10;
    return Math.min(100,
      Math.round(
        (this.repsCompleted() / target) * 100));
  }

  // ✅ feedbackLevel basé uniquement sur isConformant
  // — plus de lecture du texte feedback qui change
  get feedbackLevel(): 'good' | 'far' {
    return this.isConformant() ? 'good' : 'far';
  }

  // ══════════════════════════════════════════
  // LIFECYCLE
  // ══════════════════════════════════════════
  ngOnInit(): void {
    this.loadExercises();
    this.loadActivePlanId();
    this.loadCurrentLevel();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopAll();
  }

  // ══════════════════════════════════════════
  // EXERCICES
  // ══════════════════════════════════════════
  loadExercises(): void {
    this.exerciseService.getMyExercises()
      .pipe(takeUntil(this.destroy$), retry(2))
      .subscribe({
        next: (exs: ExerciseResponse[]) => {
          this.exercises.set(exs);
          if (exs.length === 0) {
            this.errorMsg.set(
              'Aucun exercice disponible.'
              + ' Contactez votre kinésithérapeute.');
          }
        },
        error: (err: { message?: string }) => {
          this.errorMsg.set(
            err.message
            || 'Impossible de charger vos exercices.');
        }
      });
  }

  selectExercise(ex: ExerciseResponse): void {
    this.selectedEx.set(ex);
  }

  getInstructions(): string[] {
    const ex = this.selectedEx();
    if (!ex) return [];

    const base: Record<string, string[]> = {
      'GENOU': [
        'Gardez le dos bien droit',
        'Contractez les abdominaux',
        'Fléchissez le genou lentement',
        'Revenez à la position initiale'
      ],
      'EPAULE': [
        'Gardez le dos droit',
        'Contractez les abdominaux',
        'Montez le bras à hauteur de l\'épaule',
        'Redescendez lentement'
      ],
      'DOS': [
        'Pieds écartés largeur d\'épaules',
        'Contractez les abdominaux',
        'Redressez le dos progressivement',
        'Maintenez 2 secondes en haut'
      ],
      'HANCHE': [
        'Gardez l\'équilibre',
        'Mouvements lents et contrôlés',
        'Ne dépassez pas la douleur',
        'Respirez régulièrement'
      ],
      'COUDE': [
        'Gardez le coude proche du corps',
        'Mouvements lents et précis',
        'Contractez le biceps',
        'Descendez lentement'
      ]
    };

    return base[ex.bodyZone] ?? [
      'Positionnez-vous correctement',
      'Mouvements lents et contrôlés',
      'Respirez régulièrement',
      'Arrêtez si douleur'
    ];
  }

  // ══════════════════════════════════════════
  // NAVIGATION INSTRUCTIONS
  // ══════════════════════════════════════════
  nextStep(): void {
    if (this.currentStep() < this.steps.length - 1) {
      this.currentStep.update(s => s + 1);
    } else {
      this.phase.set('CALIBRATION');
      this.feedback.set(
        'Placez votre corps dans le cadre');
      this.initMediaPipe();
    }
  }

  prevStep(): void {
    if (this.currentStep() > 0) {
      this.currentStep.update(s => s - 1);
    }
  }

  private loadActivePlanId(): void {
    this.rehabPlanService.getMyActivePlan()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (plan) =>
          this.activePlanId.set(plan.id),
        error: () =>
          this.activePlanId.set(null)
      });
  }

  private loadCurrentLevel(): void {
    this.patientService.getMyProfile()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (profile) => {
          this.patientLevelSignal.set(
            profile.level || 'Inconnu');
        },
        error: () => {}
      });
  }

  // ══════════════════════════════════════════
  // DÉMARRER SÉANCE
  // ══════════════════════════════════════════
  startSession(): void {
    const ex = this.selectedEx();
    if (!ex) return;

    if (!this.isValidExerciseData(ex)) {
      this.errorMsg.set(
        'Données d\'exercice invalides.'
        + ' Contactez l\'administrateur.');
      return;
    }

    if (!this.isValidJointConfig(ex)) {
      this.errorMsg.set(
        'Configuration MediaPipe invalide.');
      return;
    }

    this.sessionService.start({
      exerciseId: ex.id,
      planId: this.activePlanId()
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (session: SessionResponse) => {
        this.currentSession.set(session);
        this.currentStep.set(0);
        this.phase.set('INSTRUCTIONS');
      },
      error: (err: { message: string }) => {
        this.errorMsg.set(
          err.message || 'Erreur démarrage séance');
      }
    });
  }

  // ✅ Validation données exercice — protège NaN
  private isValidExerciseData(
    ex: ExerciseResponse
  ): boolean {
    const angle = ex.targetAngle;
    const tol   = ex.toleranceDeg;
    if (angle === undefined || angle === null
        || isNaN(angle)
        || angle < 0 || angle > 180) return false;
    if (tol === undefined || tol === null
        || isNaN(tol)
        || tol <= 0 || tol > 45) return false;
    if (!ex.repsTarget || ex.repsTarget <= 0)
      return false;
    return true;
  }

  getToleranceLabel(
    toleranceDeg: number | undefined
  ): string {
    if (!toleranceDeg) return 'Précision modérée';
    if (toleranceDeg >= 15)
      return 'Mouvement souple accepté';
    if (toleranceDeg >= 8)
      return 'Précision modérée requise';
    return 'Mouvement précis demandé';
  }

  private isValidJointConfig(
    ex: ExerciseResponse
  ): boolean {
    if (!ex.mediapipeJoints) return false;
    const names = ex.mediapipeJoints
      .split(',').map(j => j.trim());
    if (names.length !== 3) return false;
    return names.every(
      n => this.LANDMARK_INDEX[n] !== undefined);
  }

  // ══════════════════════════════════════════
  // MEDIAPIPE — INITIALISATION
  // ══════════════════════════════════════════
  private async initMediaPipe(): Promise<void> {
    try {
      this.loadingScripts.set(true);
      await this.loadMediaPipeScripts();
      this.loadingScripts.set(false);
      await this.setupCamera();
      this.setupPose();
    } catch (err: any) {
      this.loadingScripts.set(false);

      if (err?.name === 'NotAllowedError') {
        this.errorMsg.set(
          'Accès à la caméra refusé.'
          + ' Autorisez l\'accès dans votre'
          + ' navigateur.');
      } else if (err?.name === 'NotFoundError') {
        this.errorMsg.set(
          'Aucune caméra détectée.');
      } else if (
          err?.message?.includes('timeout')) {
        this.errorMsg.set(
          'Chargement MediaPipe trop lent.'
          + ' Vérifiez votre connexion.');
      } else {
        this.errorMsg.set(
          'Impossible d\'accéder à la webcam.');
      }
      this.phase.set('ERROR');
    }
  }

  // ✅ Timeout 10s sur le chargement des scripts
  private loadMediaPipeScripts(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.scriptLoaded) {
        resolve();
        return;
      }

      const timeoutId = setTimeout(() => {
        reject(new Error('timeout'));
      }, 10000);

      const pose = document.createElement('script');
      pose.src =
        'https://cdn.jsdelivr.net/npm/'
        + '@mediapipe/pose/pose.js';
      pose.crossOrigin = 'anonymous';

      const cam = document.createElement('script');
      cam.src =
        'https://cdn.jsdelivr.net/npm/'
        + '@mediapipe/camera_utils/camera_utils.js';
      cam.crossOrigin = 'anonymous';

      pose.onload = () => {
        document.body.appendChild(cam);
        cam.onload = () => {
          clearTimeout(timeoutId);
          this.scriptLoaded = true;
          resolve();
        };
        cam.onerror = () => {
          clearTimeout(timeoutId);
          reject(new Error('Camera utils failed'));
        };
      };
      pose.onerror = () => {
        clearTimeout(timeoutId);
        reject(new Error('Pose script failed'));
      };
      document.body.appendChild(pose);
    });
  }

  private async setupCamera(): Promise<void> {
    const stream = await navigator.mediaDevices
      .getUserMedia({
        video: { width: 640, height: 480 }
      });
    this.videoRef.nativeElement.srcObject = stream;
    await this.videoRef.nativeElement.play();
  }

  private setupPose(): void {
    const PoseClass = (window as any).Pose;
    if (!PoseClass) return;

    this.pose = new PoseClass({
      locateFile: (file: string) =>
        'https://cdn.jsdelivr.net/npm/'
        + `@mediapipe/pose/${file}`
    });

    this.pose.setOptions({
      modelComplexity: 1,
      smoothLandmarks: true,
      minDetectionConfidence: 0.6,
      minTrackingConfidence: 0.6
    });

    this.pose.onResults((results: any) => {
      this.ngZone.run(() => {
        this.processResults(results);
      });
    });

    const CameraClass = (window as any).Camera;
    if (!CameraClass) return;

    this.camera = new CameraClass(
      this.videoRef.nativeElement,
      {
        onFrame: async () => {
          await this.pose.send({
            image: this.videoRef.nativeElement
          });
        },
        width: 640,
        height: 480
      }
    );
    this.camera.start();
  }

  // ══════════════════════════════════════════
  // MAPPING ARTICULATION DYNAMIQUE
  // ══════════════════════════════════════════
  private getJointIndices(): number[] | null {
    const ex = this.selectedEx();
    if (!ex?.mediapipeJoints) return null;

    const jointNames = ex.mediapipeJoints
      .split(',').map(j => j.trim());

    if (jointNames.length !== 3) return null;

    const indices = jointNames.map(
      name => this.LANDMARK_INDEX[name]);

    if (indices.some(idx => idx === undefined)) {
      return null;
    }
    return indices;
  }

  private getMissingBodyPart(
    landmarks: any[],
    jointIndices: number[]
  ): string {
    for (const idx of jointIndices) {
      const lm = landmarks[idx];
      if (!lm || (lm.visibility ?? 0) < 0.6) {
        return this.JOINT_NAMES_FR[idx]
          || 'une partie de votre corps';
      }
    }
    return 'votre corps';
  }

  // ══════════════════════════════════════════
  // TRAITEMENT RÉSULTATS MEDIAPIPE
  // ══════════════════════════════════════════
  private processResults(results: any): void {
    if (!results.poseLandmarks) return;

    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(
      0, 0, canvas.width, canvas.height);
    ctx.drawImage(
      results.image, 0, 0,
      canvas.width, canvas.height);

    const ex = this.selectedEx();
    if (!ex) return;

    const indices = this.getJointIndices();
    if (!indices) return;

    this.drawSkeleton(
      ctx, results.poseLandmarks,
      canvas, indices);

    // ── Phase CALIBRATION ──────────────────
    if (this.phase() === 'CALIBRATION') {
      const bodyVisible =
        this.checkBodyVisibility(
          results.poseLandmarks);
      this.drawCalibrationGuide(ctx, canvas);
      this.handleCalibration(bodyVisible);
      return;
    }

    // ── Phase SESSION ──────────────────────
    if (this.phase() === 'SESSION') {

      const jointsVisible =
        this.areTrackedJointsVisible(
          results.poseLandmarks, indices);

      if (!jointsVisible) {
        const missingPart =
          this.getMissingBodyPart(
            results.poseLandmarks, indices);

        // ✅ Message visuel STABLE — ne clignote pas
        // Reste affiché jusqu'au repositionnement
        this.feedback.set('⚠️ Repositionnez-vous');
        this.isConformant.set(false);
        this.drawFeedback(ctx, canvas, false);

        this.repPhase = 'NEUTRAL';
        this.angleHistory = [];
        this.holdStartTime = 0;
        this.holdAnnounced = false;

        // ✅ Vocal avec interrupt (urgence) — 4s
        const now = Date.now();
        if (now - this.lastVisibilityWarning
            > 4000) {
          this.speak(
            `Je ne vois plus votre ${missingPart}`,
            true);
          this.lastVisibilityWarning = now;
        }
        return;
      }

      const angle = this.calculateAngle(
        results.poseLandmarks, indices);
      const diff =
        Math.abs(angle - ex.targetAngle);
      const conformant =
        diff <= ex.toleranceDeg;

      this.frameAngles = { main_angle: angle };
      this.isConformant.set(conformant);

      // ✅ Seulement le canvas change à chaque
      // frame — PAS le signal feedback
      this.handleSession(angle, conformant, ex);
      this.drawFeedback(ctx, canvas, conformant);
    }
  }

  // ══════════════════════════════════════════
  // VÉRIFICATION VISIBILITÉ
  // ══════════════════════════════════════════
  private checkBodyVisibility(
    landmarks: any[]
  ): boolean {
    const required = [11, 12, 23, 24, 25, 26];
    return required.every(idx => {
      const lm = landmarks[idx];
      return lm && (lm.visibility ?? 1) > 0.5;
    });
  }

  private areTrackedJointsVisible(
    landmarks: any[],
    jointIndices: number[]
  ): boolean {
    return jointIndices.every(idx => {
      const lm = landmarks[idx];
      return lm
        && (lm.visibility ?? 0) >= 0.6;
    });
  }

  // ══════════════════════════════════════════
  // DESSIN SQUELETTE
  // ══════════════════════════════════════════
  private drawSkeleton(
    ctx: CanvasRenderingContext2D,
    landmarks: any[],
    canvas: HTMLCanvasElement,
    jointIndices: number[]
  ): void {
    const connections = [
      [11, 12], [11, 23], [12, 24], [23, 24],
      [11, 13], [13, 15],
      [12, 14], [14, 16],
      [23, 25], [25, 27],
      [24, 26], [26, 28]
    ];

    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    connections.forEach(([s, e]) => {
      const start = landmarks[s];
      const end   = landmarks[e];
      if (!start || !end) return;
      ctx.strokeStyle =
        'rgba(59, 130, 246, 0.7)';
      ctx.beginPath();
      ctx.moveTo(
        start.x * canvas.width,
        start.y * canvas.height);
      ctx.lineTo(
        end.x * canvas.width,
        end.y * canvas.height);
      ctx.stroke();
    });

    [11, 12, 13, 14, 15, 16,
     23, 24, 25, 26, 27, 28]
    .forEach(idx => {
      const lm = landmarks[idx];
      if (!lm) return;
      const x = lm.x * canvas.width;
      const y = lm.y * canvas.height;
      ctx.fillStyle   = 'rgba(255,255,255,0.9)';
      ctx.strokeStyle = 'rgba(59,130,246,1)';
      ctx.lineWidth   = 2;
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, 2 * Math.PI);
      ctx.fill();
      ctx.stroke();
    });

    // Articulation cible — couleur dynamique
    const targetIdx = jointIndices[1];
    const target = landmarks[targetIdx];
    if (target) {
      const x = target.x * canvas.width;
      const y = target.y * canvas.height;
      ctx.strokeStyle = this.isConformant()
        ? 'rgba(34,197,94,0.5)'
        : 'rgba(239,68,68,0.5)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(x, y, 18, 0, 2 * Math.PI);
      ctx.stroke();
      ctx.fillStyle = this.isConformant()
        ? 'rgba(34,197,94,0.9)'
        : 'rgba(239,68,68,0.9)';
      ctx.beginPath();
      ctx.arc(x, y, 10, 0, 2 * Math.PI);
      ctx.fill();
    }
  }

  private drawCalibrationGuide(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement
  ): void {
    if (this.calibStable > 25) return;

    const cx = canvas.width  / 2;
    const cy = canvas.height / 2;

    ctx.save();
    ctx.strokeStyle = 'rgba(59,130,246,0.5)';
    ctx.fillStyle   = 'rgba(59,130,246,0.08)';
    ctx.lineWidth   = 2;
    ctx.setLineDash([6, 4]);

    ctx.beginPath();
    ctx.arc(cx, cy - 115, 32, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.rect(cx - 38, cy - 80, 76, 115);
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(cx - 28, cy + 35);
    ctx.lineTo(cx - 28, cy + 130);
    ctx.moveTo(cx + 28, cy + 35);
    ctx.lineTo(cx + 28, cy + 130);
    ctx.stroke();

    ctx.setLineDash([]);
    ctx.fillStyle    = 'rgba(59,130,246,0.95)';
    ctx.font         = 'bold 13px sans-serif';
    ctx.textAlign    = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(
      'Placez votre corps dans le cadre',
      cx, cy + 158);
    ctx.restore();
  }

  private drawFeedback(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    conformant: boolean
  ): void {
    ctx.strokeStyle = conformant
      ? 'rgba(34,197,94,0.9)'
      : 'rgba(239,68,68,0.9)';
    ctx.lineWidth = 5;
    ctx.strokeRect(2, 2,
      canvas.width - 4, canvas.height - 4);
  }

  // ══════════════════════════════════════════
  // CALIBRATION
  // ══════════════════════════════════════════
  private handleCalibration(
    bodyVisible: boolean
  ): void {
    if (bodyVisible) {
      this.calibStable++;
      const countdown = Math.max(0,
        2 - Math.floor(this.calibStable / 25));
      this.calibCountdown.set(countdown);

      if (this.calibStable >= 50) {
        this.resetSessionState();
        this.phase.set('SESSION');
        this.startSessionTimer();
        this.startMetricsInterval();
        // ✅ Message visuel stable — ne changera
        // plus jusqu'à un événement réel
        this.feedback.set('Séance en cours');
        this.speak(
          'Calibration réussie.'
          + ' L\'exercice commence.',
          true);
      } else {
        // ✅ Message de calibration stable
        this.feedback.set(
          `Restez visible... ${countdown}s`);
      }
    } else {
      this.calibStable = Math.max(0,
        this.calibStable - 3);
      this.calibCountdown.set(2);
      this.feedback.set(
        'Placez votre corps dans le cadre');
    }
  }

  // ══════════════════════════════════════════
  // RÉINITIALISATION COMPLÈTE AVANT SESSION
  // ══════════════════════════════════════════
  private resetSessionState(): void {
    this.repPhase = 'NEUTRAL';
    this.angleHistory = [];
    this.lastAngle = 0;
    this.allScores = [];
    this.frameAngles = {};
    this.autoCompleting = false;
    this.holdStartTime = 0;
    this.holdAnnounced = false;
    this.lastFrameTime = 0;
    this.lastVisibilityWarning = 0;
    this.lastSpokenMsg = '';
    this.lastSpeakTime = 0;
    this.lastSentConformityPct = -1;
    this.lastSentReps = -1;
    this.repsCompleted.set(0);
    this.conformityPct.set(0);
    this.isConformant.set(false);
    this.sessionTime.set(0);
  }

  // ══════════════════════════════════════════
  // SÉANCE ACTIVE
  // ✅ Règles feedback :
  //   - Vocal uniquement pour les répétitions
  //     et événements importants
  //   - AUCUN message visuel qui change à chaque
  //     frame — seulement le canvas (vert/rouge)
  // ══════════════════════════════════════════
  private handleSession(
    angle: number,
    conformant: boolean,
    ex: ExerciseResponse
  ): void {

    // Protection NaN
    if (isNaN(angle) || isNaN(ex.targetAngle))
      return;

    const diff = Math.abs(angle - ex.targetAngle);
    const score = conformant
      ? 100
      : Math.max(0,
          100 - (diff - ex.toleranceDeg) * 2);

    this.allScores.push(score);
    const avg = this.allScores
      .reduce((a, b) => a + b, 0)
      / this.allScores.length;
    this.conformityPct.set(Math.round(avg));

    this.lastFrameTime = Date.now();

    const repDone = this.detectRepetition(
      angle, ex.targetAngle, ex.toleranceDeg);

    if (repDone) {
      const newReps = this.repsCompleted() + 1;
      this.repsCompleted.set(newReps);

      const remaining =
        (ex.repsTarget ?? 10) - newReps;

      // ✅ Règle 1 — vocal uniquement pour les reps
      // Message court, clair, jamais coupé
      if (remaining <= 0) {
        // Géré plus bas par autoCompleting
      } else if (remaining === 1) {
        this.speak('Dernière répétition');
      } else if (remaining % 5 === 0) {
        this.speak(
          `Plus que ${remaining} répétitions`);
      } else {
        this.speak(`Répétition ${newReps}`);
      }

      // ✅ Vibration guardée
      if (typeof navigator !== 'undefined'
          && navigator.vibrate) {
        navigator.vibrate(50);
      }

      this.holdStartTime = 0;
      this.holdAnnounced = false;
    }

    // ✅ Règle 2 & 3 — AUCUN message visuel d'angle
    // Le contour vert/rouge du canvas suffit
    // comme indicateur de conformité

    // ✅ Règle 4 — objectif atteint
    // ✅ Objectif atteint — message adapté au score
const target = ex.repsTarget ?? 10;
if (this.repsCompleted() >= target
    && !this.autoCompleting) {
  this.autoCompleting = true;

  if (this.timerInterval) {
    clearInterval(this.timerInterval);
    this.timerInterval = null;
  }
  if (this.metricsInterval) {
    clearInterval(this.metricsInterval);
    this.metricsInterval = null;
  }

  // ✅ Message adapté selon le score réel
  const score = this.conformityPct();
  let vocalMessage: string;
  let visualMessage: string;

  if (score >= 80) {
    visualMessage =
      '🎉 Excellent ! Objectif atteint !';
    vocalMessage =
      'Objectif atteint. Excellent travail !';
  } else if (score >= 60) {
    visualMessage =
      '✅ Objectif atteint. Continuez vos efforts !';
    vocalMessage =
      'Objectif atteint.'
      + ' Continuez vos efforts, vous progressez.';
  } else {
    visualMessage =
      '✅ Séance terminée. À améliorer.';
    vocalMessage =
      'Séance terminée.'
      + ' Essayez d\'aller plus loin'
      + ' la prochaine fois.';
  }

  this.feedback.set(visualMessage);

  // ✅ completeSession() appelé UNIQUEMENT après
  // la fin complète du message vocal — plus de
  // coupure possible
  this.speak(vocalMessage, true, () => {
    setTimeout(
      () => this.completeSession(), 500);
  });
}
  }

  // ══════════════════════════════════════════
  // ALGORITHME RÉPÉTITIONS PAR PHASES
  // ══════════════════════════════════════════
  private detectRepetition(
    angle: number,
    targetAngle: number,
    toleranceDeg: number
  ): boolean {
    this.angleHistory.push(angle);
    if (this.angleHistory.length
        > this.HISTORY_SIZE) {
      this.angleHistory.shift();
    }
    const smooth = this.angleHistory
      .reduce((a, b) => a + b, 0)
      / this.angleHistory.length;

    const inZone = Math.abs(
      smooth - targetAngle) <= toleranceDeg;
    const descending =
      smooth < this.lastAngle - 2;
    const ascending =
      smooth > this.lastAngle + 2;

    let repCompleted = false;

    switch (this.repPhase) {
      case 'NEUTRAL':
        if (descending)
          this.repPhase = 'GOING_DOWN';
        break;
      case 'GOING_DOWN':
        if (inZone)
          this.repPhase = 'AT_BOTTOM';
        break;
      case 'AT_BOTTOM':
        if (ascending)
          this.repPhase = 'GOING_UP';
        break;
      case 'GOING_UP':
        if (!inZone && ascending) {
          repCompleted = true;
          this.repPhase = 'NEUTRAL';
        }
        break;
    }

    this.lastAngle = smooth;
    return repCompleted;
  }

  // ══════════════════════════════════════════
  // CALCUL ANGLE ARTICULAIRE
  // ══════════════════════════════════════════
  private calculateAngle(
    landmarks: any[],
    jointIndices: number[]
  ): number {
    const [aIdx, bIdx, cIdx] = jointIndices;
    const a = landmarks[aIdx];
    const b = landmarks[bIdx];
    const c = landmarks[cIdx];
    if (!a || !b || !c) return 0;

    return this.computeAngle(
      [a.x, a.y], [b.x, b.y], [c.x, c.y]);
  }

  private computeAngle(
    a: number[], b: number[], c: number[]
  ): number {
    const rad =
      Math.atan2(c[1] - b[1], c[0] - b[0])
      - Math.atan2(a[1] - b[1], a[0] - b[0]);
    let angle = Math.abs(rad * 180 / Math.PI);
    if (angle > 180) angle = 360 - angle;
    return Math.round(angle);
  }

  // ══════════════════════════════════════════
  // TIMERS ET MÉTRIQUES
  // ══════════════════════════════════════════
  private startSessionTimer(): void {
    this.sessionTime.set(0);
    this.timerInterval = setInterval(() => {
      this.ngZone.run(() => {
        this.sessionTime.update(t => t + 1);
      });
    }, 1000);
  }

  private startMetricsInterval(): void {
    this.metricsInterval = setInterval(() => {
      const session = this.currentSession();
      if (!session) return;

      const currentConformity =
        this.conformityPct();
      const currentReps = this.repsCompleted();

      // ✅ N'envoie que si les valeurs changent
      if (currentConformity ===
            this.lastSentConformityPct
          && currentReps === this.lastSentReps) {
        return;
      }

      this.lastSentConformityPct = currentConformity;
      this.lastSentReps = currentReps;

      this.sessionService.saveMetrics(
        session.id,
        {
          jointAngles: JSON.stringify(
            this.frameAngles),
          conformityPct: currentConformity,
          repsAtMoment: currentReps
        }
      )
      .pipe(takeUntil(this.destroy$), retry(2))
      .subscribe({
        error: (err) => {
          console.warn(
            'Métriques non sauvegardées :', err);
        }
      });
    }, 5000);
  }

  // ══════════════════════════════════════════
// FEEDBACK VOCAL — retourne l'utterance
// pour pouvoir écouter l'événement onend
// ══════════════════════════════════════════
private speak(
  message: string,
  interrupt: boolean = false,
  onEnd?: () => void
): void {
  if (!this.voiceEnabled() || !this.speech) {
    // Si vocal désactivé, exécute onEnd
    // immédiatement pour ne pas bloquer
    onEnd?.();
    return;
  }

  const now = Date.now();
  if (message === this.lastSpokenMsg
      && now - this.lastSpeakTime < 5000
      && !interrupt) {
    onEnd?.();
    return;
  }

  if (!interrupt && this.speech.speaking) {
    // Attend 100ms et réessaie
    setTimeout(() => {
      this.speak(message, interrupt, onEnd);
    }, 100);
    return;
  }

  if (interrupt) {
    this.speech.cancel();
  }

  const utterance =
    new SpeechSynthesisUtterance(message);
  utterance.lang   = 'fr-FR';
  utterance.rate   = 0.85;
  utterance.pitch  = 1.0;
  utterance.volume = 1.0;

  // ✅ onEnd — callback déclenché APRÈS la fin
  // du message, jamais avant
  if (onEnd) {
    utterance.onend = () => onEnd();
    // Sécurité — si onend ne se déclenche pas
    // (bug navigateur), on exécute quand même
    // le callback après un délai raisonnable
    const wordCount = message.split(' ').length;
    const estimatedDuration =
      (wordCount / 2.5) * 1000 + 500; // ~2.5 mots/s
    setTimeout(() => {
      onEnd();
    }, estimatedDuration);
  }

  this.speech.speak(utterance);
  this.lastSpokenMsg = message;
  this.lastSpeakTime = now;
}
  toggleVoice(): void {
    this.voiceEnabled.update(v => !v);
    if (!this.voiceEnabled()) {
      this.speech?.cancel();
    }
  }

  // ══════════════════════════════════════════
  // COMPLÉTION / INTERRUPTION
  // ══════════════════════════════════════════
  completeSession(): void {
    const session = this.currentSession();
    if (!session) return;

    this.stopAll();

    this.sessionService.complete(session.id, {
      finalScore: this.conformityPct(),
      repsCompleted: this.repsCompleted(),
      jointAngles: JSON.stringify(this.frameAngles)
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (result: SessionResponse) => {
        this.completedSession.set(result);
        this.xpGained.set(result.xpEarned ?? 0);
        this.phase.set('COMPLETED');
      },
      error: (err: { message: string }) => {
        this.errorMsg.set(
          err.message
          || 'Erreur complétion séance');
      }
    });
  }

  interruptSession(): void {
    const session = this.currentSession();
    this.stopAll();

    if (session) {
      this.sessionService
        .interrupt(session.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => this.router.navigate(
            ['/patient/dashboard']),
          error: () => this.router.navigate(
            ['/patient/dashboard'])
        });
    } else {
      this.router.navigate(
        ['/patient/dashboard']);
    }
  }

  // ══════════════════════════════════════════
  // HELPERS TEMPLATE
  // ══════════════════════════════════════════
  getBadgeLabel(type: string): string {
    const labels: Record<string, string> = {
      'FIRST_SESSION':    'Première séance',
      'SEVEN_DAYS':       '7 jours consécutifs',
      'FIFTY_REPS':       '50 répétitions',
      'PERFECT_SCORE':    'Score parfait',
      'WEEK_GOAL':        'Objectif semaine',
      'PROGRAM_COMPLETE': 'Programme complet'
    };
    return labels[type] || type;
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60)
      .toString().padStart(2, '0');
    const s = (seconds % 60)
      .toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  getLevelLabel(level: string): string {
    const labels: Record<string, string> = {
      'DEBUTANT':      'Débutant',
      'INTERMEDIAIRE': 'Intermédiaire',
      'AVANCE':        'Avancé'
    };
    return labels[level] || level;
  }

  getZoneLabel(zone: string): string {
    const labels: Record<string, string> = {
      'GENOU':  'Genou',
      'EPAULE': 'Épaule',
      'DOS':    'Dos',
      'HANCHE': 'Hanche',
      'COUDE':  'Coude'
    };
    return labels[zone] || zone;
  }

  goToDashboard(): void {
    this.router.navigate(['/patient/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  // ══════════════════════════════════════════
  // NETTOYAGE
  // ══════════════════════════════════════════
  private stopAll(): void {
    if (this.metricsInterval) {
      clearInterval(this.metricsInterval);
      this.metricsInterval = null;
    }
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }

    try { this.camera?.stop(); } catch {}
    try { this.pose?.close(); } catch {}
    try { this.speech?.cancel(); } catch {}

    const video = this.videoRef?.nativeElement;
    if (video?.srcObject) {
      (video.srcObject as MediaStream)
        .getTracks()
        .forEach(t => t.stop());
    }
  }
}