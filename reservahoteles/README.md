# Sistema de Reservas de Hoteles

Buenos días, tardes o noches a quien le pueda interesar.

Este aplicativo fue construido con **Java 17**, **Spring Boot**, **HTML**, **CSS** y **Bootstrap** para el frontend, utilizando una base de datos **PostgreSQL** alojada en **Supabase**. La conexión a la base de datos está configurada en el archivo `application.properties` y se encuentra correctamente conectada.

## Funcionalidades

### Consulta de Disponibilidad
Permite consultar la disponibilidad de habitaciones ingresando:
- ID del hotel
- ID del tipo de habitación deseada
- Fechas de reserva (inicio y fin)

**Nota:** Para seleccionar el hotel y el tipo de habitación, debes ingresar el ID correspondiente. A continuación se muestran los IDs disponibles:

#### Hoteles Disponibles
| ID | Nombre | Ubicación |
|----|--------|-----------|
| 1 | Hotel Barranquilla | Barranquilla, Atlántico, Colombia |
| 2 | Hotel Cali | Cali, Valle del Cauca, Colombia |
| 3 | Hotel Cartagena | Cartagena, Bolívar, Colombia |
| 4 | Hotel Bogotá | Bogotá D.C., Colombia |

#### Tipos de Habitación Disponibles
| ID | Tipo | Capacidad de Personas | Descripción |
|----|------|----------------------|-------------|
| 1 | Estándar | 4 | Habitación básica con servicios estándar |
| 2 | Premium | 6 | Habitación mejorada con servicios adicionales |
| 3 | VIP | 8 | Suite de lujo con todos los servicios premium |

El sistema muestra:
- Cantidad total de habitaciones
- Cantidad de habitaciones reservadas
- Cantidad de habitaciones disponibles
- Precios según temporada (alta o baja)

### Cálculo de Precio y Reserva
Si se desea realizar una reserva, ingresando el número de personas y habitaciones requeridas, el sistema calcula el precio total. Una vez confirmada, la reserva se refleja en la base de datos y se actualizan automáticamente los números de disponibilidad.

## Arquitectura Técnica

La mayoría de los cálculos son realizados mediante **funciones almacenadas en PostgreSQL**, excepto la consulta de tarifas que se implementó directamente como query en el repositorio debido a problemas de compatibilidad durante la implementación.

### Patrones de Diseño Utilizados
- **Repository Pattern**: Para la abstracción de acceso a datos
- **Service Layer**: Para la lógica de negocio
- **DTO Pattern**: Para la transferencia de datos entre capas
- **MVC**: Separación de responsabilidades en el frontend

## Frontend

La interfaz fue desarrollada con **Bootstrap** e inteligencia artificial para garantizar su entrega y verificar el correcto funcionamiento del backend. El código JavaScript fue implementado manualmente, y aunque no desarrollé todo el HTML y CSS desde cero, entiendo perfectamente lo que fue utilizado y cómo funciona.

## Ejecución

Tanto el backend como el frontend se ejecutan en el mismo puerto **8080**. Al correr el proyecto de Java con Spring Boot, el aplicativo estará listo para ser usado en `http://localhost:8080`.

## Configuración

La conexión a la base de datos Supabase está configurada en `src/main/resources/application.properties`. 
