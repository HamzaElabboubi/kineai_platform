import { Component, inject, input } from '@angular/core';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-mobile-header',
  standalone: true,
  templateUrl: './mobile-header.component.html'
})
export class MobileHeaderComponent {
  sidebar = inject(SidebarService);
  title = input<string>('KINEAI');
}
