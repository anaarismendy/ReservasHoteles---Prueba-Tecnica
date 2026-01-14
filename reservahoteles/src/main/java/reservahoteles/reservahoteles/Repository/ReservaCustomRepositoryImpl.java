package reservahoteles.reservahoteles.Repository;

import reservahoteles.reservahoteles.DTO.CalculoPrecioRequest;
import reservahoteles.reservahoteles.DTO.CrearReservaRequest;
import reservahoteles.reservahoteles.DTO.CalculoPrecioResponse;
import reservahoteles.reservahoteles.DTO.CrearReservaResponse;
import reservahoteles.reservahoteles.DTO.DisponibilidadResponse;
import reservahoteles.reservahoteles.DTO.TarifaResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ReservaCustomRepositoryImpl implements ReservaCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Verifica la disponibilidad de habitaciones en un hotel para un tipo específico
     * dentro de un rango de fechas determinado.
     * 
     * Este método invoca la función almacenada de PostgreSQL 'verificar_disponibilidad_pool'
     * que calcula la disponibilidad basándose en el inventario total de habitaciones
     * y las reservas confirmadas existentes.
     * 
     * @param idHotel Identificador único del hotel
     * @param idTipo Identificador del tipo de habitación a consultar
     * @param fechaInicio Fecha de inicio del período de consulta 
     * @param fechaFin Fecha de fin del período de consulta 
     * @return Lista de respuestas con información de disponibilidad, incluyendo:
     *         - Tipo de habitación
     *         - Cantidad total de habitaciones
     *         - Cantidad de habitaciones disponibles
     *         - Capacidad máxima de personas
     */
    @Override
    public List<DisponibilidadResponse> verificarDisponibilidad(Integer idHotel, Integer idTipo, 
                                                                  LocalDate fechaInicio, LocalDate fechaFin) {
        String sql = "SELECT * FROM verificar_disponibilidad_pool(?, ?, ?::date, ?::date)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, idHotel)
            .setParameter(2, idTipo)
            .setParameter(3, fechaInicio.toString())
            .setParameter(4, fechaFin.toString());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<DisponibilidadResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            DisponibilidadResponse response = new DisponibilidadResponse(
                row[0] != null ? row[0].toString() : "Desconocido",
                row[1] != null ? ((Number) row[1]).intValue() : 0,
                row[2] != null ? ((Number) row[2]).intValue() : 0,
                row[3] != null ? ((Number) row[3]).intValue() : 0
            );
            responses.add(response);
        }
        
        return responses;
    }

    /**
     * Obtiene las tarifas aplicables para un hotel y tipo de habitación específicos
     * en una fecha determinada.
     * 
     * Este método consulta las tarifas activas que corresponden a la temporada
     * en la que se encuentra la fecha proporcionada. Realiza un JOIN entre las tablas
     * de tarifas, hoteles, tipos de habitación y temporadas para obtener información
     * completa incluyendo precios base y precios por persona adicional.
     * 
     * @param idHotel Identificador único del hotel
     * @param idTipo Identificador del tipo de habitación (puede ser null para obtener todas)
     * @param fechaInicio Fecha para determinar la temporada aplicable
     * @return Lista de tarifas con información detallada:
     *         - Identificador de tarifa
     *         - Nombre del hotel
     *         - Tipo de habitación
     *         - Temporada aplicable
     *         - Precio base por noche
     *         - Precio por persona adicional
     */
    @Override
    public List<TarifaResponse> obtenerTarifas(Integer idHotel, Integer idTipo, LocalDate fechaInicio) {
        String sql = "SELECT " +
                    "t.id_tarifa, " +
                    "hot.nombre as hotel, " +
                    "th.nombre as tipo_habitacion, " +
                    "temp.nombre as temporada, " +
                    "t.precio_base_noche, " +
                    "t.precio_persona_adicional " +
                    "FROM tarifas t " +
                    "INNER JOIN hoteles hot ON t.id_hotel = hot.id_hotel " +
                    "INNER JOIN tipos_habitacion th ON t.id_tipo = th.id_tipo " +
                    "INNER JOIN temporadas temp ON t.id_temporada = temp.id_temporada " +
                    "WHERE t.id_hotel = ? " +
                    "AND (? IS NULL OR t.id_tipo = ?) " +
                    "AND ?::date BETWEEN temp.fecha_inicio::date AND temp.fecha_fin::date " +
                    "ORDER BY th.nombre";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, idHotel)
            .setParameter(2, idTipo)
            .setParameter(3, idTipo)
            .setParameter(4, fechaInicio.toString());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<TarifaResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            TarifaResponse response = new TarifaResponse(
                row[0] != null ? ((Number) row[0]).intValue() : null,
                row[1] != null ? row[1].toString() : null,
                row[2] != null ? row[2].toString() : null,
                row[3] != null ? row[3].toString() : null,
                row[4] != null ? new BigDecimal(row[4].toString()) : null,
                row[5] != null ? new BigDecimal(row[5].toString()) : null
            );
            responses.add(response);
        }
        
        return responses;
    }

    /**
     * Calcula el precio total de una reserva basándose en los parámetros proporcionados.
     * 
     * Este método invoca la función almacenada 'calcular_precio_reserva' que realiza
     * el cálculo considerando:
     * - El número de noches entre las fechas de inicio y fin
     * - La cantidad de habitaciones solicitadas
     * - El número de personas (aplicando cargos adicionales si excede la capacidad base)
     * - La temporada aplicable según las fechas
     * - Los precios base y por persona adicional de la tarifa vigente
     * 
     * El resultado incluye un desglose detallado en formato JSON que se convierte
     * a texto para evitar problemas de mapeo con tipos JSONB de PostgreSQL.
     * 
     * @param request Objeto con los parámetros de cálculo:
     *                - ID del hotel
     *                - ID del tipo de habitación
     *                - Fecha de inicio de la reserva
     *                - Fecha de fin de la reserva
     *                - Cantidad de habitaciones
     *                - Número de personas
     * @return Respuesta con el cálculo de precio incluyendo:
     *         - Precio total de la reserva
     *         - Precio por noche
     *         - Número de noches
     *         - Temporada aplicable
     *         - Desglose detallado en formato JSON (como texto)
     * @return null si no se puede calcular el precio
     */
    @Override
    public CalculoPrecioResponse calcularPrecio(CalculoPrecioRequest request) {
        String sql = "SELECT " +
                    "precio_total, " +
                    "precio_por_noche, " +
                    "numero_noches, " +
                    "temporada, " +
                    "desglose::text " +
                    "FROM calcular_precio_reserva(?, ?, ?::date, ?::date, ?, ?)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, request.getIdHotel())
            .setParameter(2, request.getIdTipo())
            .setParameter(3, request.getFechaInicio().toString())
            .setParameter(4, request.getFechaFin().toString())
            .setParameter(5, request.getCantidadHabitaciones())
            .setParameter(6, request.getNumeroPersonas());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            return new CalculoPrecioResponse(
                row[0] != null ? new BigDecimal(row[0].toString()) : null,
                row[1] != null ? new BigDecimal(row[1].toString()) : null,
                row[2] != null ? ((Number) row[2]).intValue() : null,
                row[3] != null ? row[3].toString() : null,
                row[4] != null ? row[4].toString() : null
            );
        }
        
        return null;
    }

    /**
     * Crea una nueva reserva en el sistema.
     * 
     * Este método invoca la función almacenada 'crear_reserva' que:
     * - Valida la disponibilidad de habitaciones para las fechas solicitadas
     * - Calcula el precio total de la reserva
     * - Inserta el registro en la tabla de reservas
     * - Retorna el resultado de la operación con el ID de la reserva creada
     * 
     * La función maneja internamente la lógica de negocio y validaciones,
     * asegurando la integridad de los datos y la consistencia transaccional.
     * 
     * @param request Objeto con los datos de la reserva a crear:
     *                - ID del hotel
     *                - ID del tipo de habitación
     *                - Fecha de inicio de la reserva
     *                - Fecha de fin de la reserva
     *                - Número de personas
     *                - Cantidad de habitaciones
     * @return Respuesta con el resultado de la creación:
     *         - ID de la reserva creada (si fue exitosa)
     *         - Indicador de éxito o fallo
     *         - Mensaje descriptivo del resultado
     *         - Total calculado de la reserva
     * @return Respuesta con error si no se puede crear la reserva
     */
    @Override
    public CrearReservaResponse crearReserva(CrearReservaRequest request) {
        String sql = "SELECT * FROM crear_reserva(?, ?, ?::date, ?::date, ?, ?)";
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter(1, request.getIdHotel())
            .setParameter(2, request.getIdTipo())
            .setParameter(3, request.getFechaInicio().toString())
            .setParameter(4, request.getFechaFin().toString())
            .setParameter(5, request.getNumeroPersonas())
            .setParameter(6, request.getCantidadHabitaciones());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            return new CrearReservaResponse(
                (Integer) row[0],
                (Boolean) row[1],
                (String) row[2],
                (BigDecimal) row[3]
            );
        }
        
        return new CrearReservaResponse(null, false, "Error al crear reserva", BigDecimal.ZERO);
    }
}
