package reservahoteles.reservahoteles.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TarifaResponse {
    private Integer idTarifa;
    private String hotel;
    private String tipoHabitacion;
    private String temporada;
    private BigDecimal precioBaseNoche;
    private BigDecimal precioPersonaAdicional;
}
