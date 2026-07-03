import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../src/environments/environment';

export interface AlertResponse {
  id: string;
  patientId: string;
  patientName: string;
  type: 'INACTIVITY' | 'SCORE' | string;
  message: string;
  sentAt: string;
  resolved: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {

 
 private readonly API = `${environment.apiUrl}/api/v1/alerts`;


  private http = inject(HttpClient);

  // ── Mes alertes en attente ────────────────
  getMyAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(
      `${this.API}/my`);
  }

  // ── Résoudre une alerte ────────────────────
  resolve(alertId: string): Observable<AlertResponse> {
    return this.http.put<AlertResponse>(
      `${this.API}/${alertId}/resolve`, {});
  }

  // ── Compteur alertes en attente ────────────
  countPending(): Observable<number> {
    return this.http.get<number>(
      `${this.API}/pending/count`);
  }

  getMyAlertsAsPatient(): Observable<AlertResponse[]> {
  return this.http.get<AlertResponse[]>(
    `${this.API}/my-patient`);
}

// ── Toutes mes alertes — historique complet ────
getAllMyAlerts(): Observable<AlertResponse[]> {
  return this.http.get<AlertResponse[]>(
    `${this.API}/my/all`);
}
}