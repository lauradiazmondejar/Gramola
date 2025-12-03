import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-geolocalizacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './geolocalizacion.html',
  styleUrl: './geolocalizacion.css'
})
export class Geolocalizacion {

  coordenadas?: GeolocationPosition;
  temperaturaMAX?: number;
  temperaturaMIN?: number;
  ciudad?: string;

  constructor() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.coordenadas = position;
          console.log('Latitud: ' + position.coords.latitude);
          console.log('Longitud: ' + position.coords.longitude);
          
          // Llamamos a los métodos para rellenar los datos
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
    if (this.coordenadas) {
      const lat = this.coordenadas.coords.latitude;
      const lon = this.coordenadas.coords.longitude;
      const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}`;
      
      fetch(url)
        .then(response => response.json())
        .then(data => {
          // CORREGIDO: Guardamos el valor en la variable de la clase 'this.ciudad'
          this.ciudad = data.address.city || data.address.town || data.address.village;
          console.log('Ciudad: ' + this.ciudad);
        })
        .catch(error => {
          console.error('Error al obtener la ciudad: ', error);
        });
    }
  }

  private obtenerClima() {
    if (this.coordenadas) {
      // CORREGIDO: Definimos lat y lon antes de usarlas
      const lat = this.coordenadas.coords.latitude;
      const lon = this.coordenadas.coords.longitude;
      
      let apiKey = 'MFJABWDHQPXK3ADVFF36XZ65X'; 
      let url = `https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/${lat},${lon}?unitGroup=metric&key=${apiKey}&contentType=json`;
      
      // CORREGIDO: Usamos fetch en lugar de XMLHttpRequest para evitar problemas con 'this'
      fetch(url)
        .then(response => response.json())
        .then(data => {
          this.temperaturaMAX = data.days[0].tempmax;
          this.temperaturaMIN = data.days[0].tempmin;
          // Usamos this.ciudad (puede que aún no haya cargado porque es asíncrono, pero no dará error de compilación)
          console.log("Temperaturas: Max " + this.temperaturaMAX + " Min " + this.temperaturaMIN);
        })
        .catch(error => {
            console.error("Error al obtener clima", error);
        });
    }
  }
}