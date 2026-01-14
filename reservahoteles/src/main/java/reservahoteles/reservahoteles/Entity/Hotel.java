package reservahoteles.reservahoteles.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "hoteles")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hotel")
    private Integer idHotel;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(length = 200)
    private String ubicacion;
    
    @Column(name = "cupo_maximo_personas", nullable = false)
    private Integer cupoMaximoPersonas;
    
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;
}