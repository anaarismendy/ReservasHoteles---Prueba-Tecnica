package reservahoteles.reservahoteles.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculoPrecioResponse {
    private BigDecimal precioTotal;
    private BigDecimal precioPorNoche;
    private Integer numeroNoches;
    private String temporada;
    private String desglose;
}
