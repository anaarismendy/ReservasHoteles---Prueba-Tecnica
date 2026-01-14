package reservahoteles.reservahoteles.Repository;


import reservahoteles.reservahoteles.DTO.CalculoPrecioResponse;
import reservahoteles.reservahoteles.DTO.CrearReservaResponse;
import reservahoteles.reservahoteles.DTO.DisponibilidadResponse;
import reservahoteles.reservahoteles.DTO.TarifaResponse;
import reservahoteles.reservahoteles.DTO.CalculoPrecioRequest;
import reservahoteles.reservahoteles.DTO.CrearReservaRequest;

import java.time.LocalDate;
import java.util.List;

public interface ReservaCustomRepository {
    
    List<DisponibilidadResponse> verificarDisponibilidad(Integer idHotel, Integer idTipo, 
                                                         LocalDate fechaInicio, LocalDate fechaFin);
    
    List<TarifaResponse> obtenerTarifas(Integer idHotel, Integer idTipo, LocalDate fechaInicio);
    
    CalculoPrecioResponse calcularPrecio(CalculoPrecioRequest request);
    
    CrearReservaResponse crearReserva(CrearReservaRequest request);
}