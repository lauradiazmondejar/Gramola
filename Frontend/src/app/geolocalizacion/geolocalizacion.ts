import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-geolocalizacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './geolocalizacion.html',
  styleUrl: './geolocalizacion.css'
})
// Demo de geolocalizacion: ciudad y clima desde servicios externos.
export class Geolocalizacion {

  coordenadas?: GeolocationPosition;
  temperaturaMAX?: number;
  temperaturaMIN?: number;
  ciudad?: string;

  constructor() {
    // Al iniciar el componente, pedimos geolocalizacion y lanzamos las peticiones auxiliares.
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.coordenadas = position;
          console.log('Latitud: ' + position.coords.latitude);
          console.log('Longitud: ' + position.coords.longitude);
          
          // Llamamos a los metodos para rellenar los datos.
          this.obtenerCiudad();
          this.obtenerClima();
        },
        (error) => {
          console.error('Error al obtener la geolocalización: ', error);
        },
        {
          enableHighAccuracy: true,
          timeout: 5000,
          maximumAge: 0
        }
      );
    } else { 
      console.error('La geolocalización no es soportada por este navegador.');
    } 
  }

  private obtenerCiudad() {
    // Invoca Nominatim para traducir lat/lon a ciudad.
    if (this.coordenadas) {
      const lat = this.coordenadas.coords.latitude;
      const lon = this.coordenadas.coords.longitude;
      const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}`;
      
      fetch(url)
        .then(response => response.json())
        .then(data => {
          // Guardamos el valor en la variable de la clase 'this.ciudad'.
          this.ciudad = data.address.city || data.address.town || data.address.village;
          console.log('Ciudad: ' + this.ciudad);
        })
        .catch(error => {
          console.error('Error al obtener la ciudad: ', error);
        });
    }
  }

  private obtenerClima() {
    // Consulta el servicio de clima usando las coordenadas actuales.
    if (this.coordenadas) {
      // Definimos lat y lon antes de usarlas.
      const lat = this.coordenadas.coords.latitude;
      const lon = this.coordenadas.coords.longitude;
      
      let apiKey = 'MFJABWDHQPXK3ADVFF36XZ65X'; 
      let url = `https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/${lat},${lon}?unitGroup=metric&key=${apiKey}&contentType=json`;
      
      // Usamos fetch en lugar de XMLHttpRequest para evitar problemas con 'this'.
      fetch(url)
        .then(response => response.json())
        .then(data => {
          this.temperaturaMAX = data.days[0].tempmax;
          this.temperaturaMIN = data.days[0].tempmin;
          // Usamos this.ciudad (puede que aun no haya cargado porque es asincrono).
          console.log("Temperaturas: Max " + this.temperaturaMAX + " Min " + this.temperaturaMIN);
        })
        .catch(error => {
            console.error("Error al obtener clima", error);
        });
    }
  }
}


