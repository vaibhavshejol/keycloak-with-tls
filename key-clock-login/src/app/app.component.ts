import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { firstValueFrom } from 'rxjs';  

@Component({
  selector: 'app-root',
  template: `
    <h1> APP 1 </h1>
    <button (click)="callBackend()">Call Secured Backend</button>
    <button (click)="callAdmin()">Call Admin Endpoint</button>
    <p>{{ response }}</p>
  `,
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'My Angular App 1';
  response: string = '';

  constructor(private http: HttpClient, private keycloak: KeycloakService) { }

  private async makeRequest(endpoint: string) {
    try {
      const token = await this.keycloak.getToken();
      const data = await firstValueFrom(this.http.get(endpoint, {
        headers: {
          Authorization: `Bearer ${token}`
        },
        responseType: 'text'
      }));
      return data;
    } catch (err) {
      if (err && typeof err === 'object') {
        const message = (err as any).message || (err as any).statusText || err.toString();
        return `Error: ${message}`;
      }
      return `Error: ${err}`;
    }
  }

  async callBackend() {
    this.response = await this.makeRequest('http://localhost:8081/auth/check');
  }

  async callAdmin() {
    this.response = await this.makeRequest('http://localhost:8081/auth/admin');
  }
}
