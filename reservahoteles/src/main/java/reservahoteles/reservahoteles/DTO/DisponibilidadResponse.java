package reservahoteles.reservahoteles.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadResponse {
    private String tipoHabitacion;
    private Integer cantidadTotal;
    private Integer cantidadDisponible;
    private Integer capacidadPersonas;
}