package reservahoteles.reservahoteles.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearReservaResponse {
    private Integer idReserva;
    private Boolean exito;
    private String mensaje;
    private BigDecimal totalCalculado;
}