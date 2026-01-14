package reservahoteles.reservahoteles.Controller;

import reservahoteles.reservahoteles.DTO.CalculoPrecioRequest;
import reservahoteles.reservahoteles.DTO.CrearReservaRequest;
import reservahoteles.reservahoteles.DTO.CalculoPrecioResponse;
import reservahoteles.reservahoteles.DTO.CrearReservaResponse;
import reservahoteles.reservahoteles.DTO.DisponibilidadResponse;
import reservahoteles.reservahoteles.DTO.TarifaResponse;
import reservahoteles.reservahoteles.Service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<DisponibilidadResponse>> consultarDisponibilidad(
            @RequestParam Integer idHotel,
            @RequestParam Integer idTipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        List<DisponibilidadResponse> disponibilidad = 
            reservaService.consultarDisponibilidad(idHotel, idTipo, fechaInicio, fechaFin);
        return ResponseEntity.ok(disponibilidad);
    }

    @GetMapping("/tarifas")
    public ResponseEntity<List<TarifaResponse>> obtenerTarifas(
            @RequestParam Integer idHotel,
            @RequestParam Integer idTipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio
    ) {
        List<TarifaResponse> tarifas = 
            reservaService.obtenerTarifas(idHotel, idTipo, fechaInicio);
        return ResponseEntity.ok(tarifas);
    }

    @PostMapping("/calcular-precio")
    public ResponseEntity<CalculoPrecioResponse> calcularPrecio(
            @Valid @RequestBody CalculoPrecioRequest request
    ) {
        CalculoPrecioResponse precio = reservaService.calcularPrecio(request);
        return ResponseEntity.ok(precio);
    }

    @PostMapping
    public ResponseEntity<CrearReservaResponse> crearReserva(
            @Valid @RequestBody CrearReservaRequest request
    ) {
        CrearReservaResponse response = reservaService.crearReserva(request);
        return ResponseEntity.ok(response);
    }
}