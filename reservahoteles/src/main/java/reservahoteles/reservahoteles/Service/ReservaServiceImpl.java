package reservahoteles.reservahoteles.Service;

import reservahoteles.reservahoteles.DTO.CalculoPrecioRequest;
import reservahoteles.reservahoteles.DTO.CrearReservaRequest;
import reservahoteles.reservahoteles.DTO.CalculoPrecioResponse;
import reservahoteles.reservahoteles.DTO.CrearReservaResponse;
import reservahoteles.reservahoteles.DTO.DisponibilidadResponse;
import reservahoteles.reservahoteles.DTO.TarifaResponse;
import reservahoteles.reservahoteles.Repository.ReservaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> consultarDisponibilidad(Integer idHotel, Integer idTipo, 
                                                                 LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Consultando disponibilidad pool para hotel: {}, tipo: {}", idHotel, idTipo);
        return reservaRepository.verificarDisponibilidad(idHotel, idTipo, fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaResponse> obtenerTarifas(Integer idHotel, Integer idTipo, LocalDate fechaInicio) {
        log.info("Obteniendo tarifas para hotel: {}, tipo: {}, fecha: {}", idHotel, idTipo, fechaInicio);
        return reservaRepository.obtenerTarifas(idHotel, idTipo, fechaInicio);
    }

    @Override
    @Transactional(readOnly = true)
    public CalculoPrecioResponse calcularPrecio(CalculoPrecioRequest request) {
        log.info("Calculando precio para reserva: {}", request);
        return reservaRepository.calcularPrecio(request);
    }

    @Override
    @Transactional
    public CrearReservaResponse crearReserva(CrearReservaRequest request) {
        log.info("Creando reserva: {}", request);
        return reservaRepository.crearReserva(request);
    }
}
