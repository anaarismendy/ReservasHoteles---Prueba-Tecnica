package reservahoteles.reservahoteles.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CalculoPrecioRequest {
    @NotNull(message = "El ID del hotel es requerido")
    private Integer idHotel;
    
    @NotNull(message = "El tipo de habitación es requerido")
    private Integer idTipo;
    
    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;
    
    @NotNull(message = "El número de personas es requerido")
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    private Integer numeroPersonas;
    
    @NotNull(message = "La cantidad de habitaciones es requerida")
    @Min(value = 1, message = "Debe reservar al menos 1 habitación")
    private Integer cantidadHabitaciones;
}
