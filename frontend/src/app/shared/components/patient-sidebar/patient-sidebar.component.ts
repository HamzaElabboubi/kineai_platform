import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-patient-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './patient-sidebar.component.html'
})
export class PatientSidebarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  sidebar = inject(SidebarService);

  logout(): void {
    this.authService.logout();
  }
}