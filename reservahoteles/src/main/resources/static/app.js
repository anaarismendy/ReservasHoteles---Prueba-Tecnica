const API_BASE_URL = 'http://localhost:8080/api/reservas';

// Elementos del DOM
const searchForm = document.getElementById('searchForm');
const searchBtn = document.getElementById('searchBtn');
const loader = document.getElementById('loader');
const resultsSection = document.getElementById('resultsSection');
const resultsContainer = document.getElementById('resultsContainer');
const errorSection = document.getElementById('errorSection');
const errorMessage = document.getElementById('errorMessage');

// Establecer fecha mínima como hoy
const today = new Date().toISOString().split('T')[0];
document.getElementById('fechaInicio').setAttribute('min', today);
document.getElementById('fechaFin').setAttribute('min', today);

// Validar que fecha fin sea mayor que fecha inicio
document.getElementById('fechaInicio').addEventListener('change', function() {
    const fechaInicio = this.value;
    const fechaFinInput = document.getElementById('fechaFin');
    if (fechaInicio) {
        fechaFinInput.setAttribute('min', fechaInicio);
        if (fechaFinInput.value && fechaFinInput.value <= fechaInicio) {
            fechaFinInput.value = '';
        }
    }
});

// Manejar envío del formulario
searchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData(searchForm);
    const idHotel = parseInt(formData.get('idHotel'));
    const idTipo = parseInt(formData.get('idTipo'));
    const fechaInicio = formData.get('fechaInicio');
    const fechaFin = formData.get('fechaFin');

    // Validaciones
    if (!idHotel || !idTipo || !fechaInicio || !fechaFin) {
        showError('Por favor, complete todos los campos del formulario.');
        return;
    }

    if (fechaFin <= fechaInicio) {
        showError('La fecha de fin debe ser posterior a la fecha de inicio.');
        return;
    }

    // Ocultar errores y resultados anteriores
    hideError();
    hideResults();

    // Mostrar loading
    setLoading(true);

    try {
        const url = `${API_BASE_URL}/disponibilidad?idHotel=${idHotel}&idTipo=${idTipo}&fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
        
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            let errorMessage = `Error ${response.status}: ${response.statusText}`;
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorData.error || errorMessage;
            } catch (e) {
                const errorText = await response.text().catch(() => '');
                if (errorText) {
                    errorMessage = errorText.length > 200 ? errorText.substring(0, 200) + '...' : errorText;
                }
            }
            throw new Error(errorMessage);
        }

        const disponibilidadData = await response.json();
        
        // Obtener tarifas simultáneamente
        let tarifasData = [];
        try {
            const tarifasUrl = `${API_BASE_URL}/tarifas?idHotel=${idHotel}&idTipo=${idTipo}&fechaInicio=${fechaInicio}`;
            const tarifasResponse = await fetch(tarifasUrl, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });
            if (tarifasResponse.ok) {
                tarifasData = await tarifasResponse.json();
            }
        } catch (error) {
            console.warn('No se pudieron obtener las tarifas:', error);
        }
        
        if (disponibilidadData && disponibilidadData.length > 0) {
            displayResults(disponibilidadData, tarifasData, idHotel, idTipo, fechaInicio, fechaFin);
        } else {
            showNoResults();
        }
    } catch (error) {
        console.error('Error al buscar disponibilidad:', error);
        let errorMsg = error.message || 'Error desconocido';
        
        // Mensajes más amigables según el tipo de error
        if (errorMsg.includes('Failed to fetch') || errorMsg.includes('NetworkError')) {
            errorMsg = 'No se pudo conectar con el servidor. Por favor, verifique que el servidor esté ejecutándose en http://localhost:8080';
        } else if (errorMsg.includes('500')) {
            errorMsg = 'Error interno del servidor. Por favor, verifique que los stored procedures existan en la base de datos y que los parámetros sean correctos.';
        } else if (errorMsg.includes('404')) {
            errorMsg = 'Endpoint no encontrado. Por favor, verifique la configuración del servidor.';
        }
        
        showError(`Error al consultar la disponibilidad: ${errorMsg}`);
    } finally {
        setLoading(false);
    }
});

function setLoading(isLoading) {
    if (isLoading) {
        searchBtn.disabled = true;
        loader.classList.add('active');
        searchBtn.querySelector('.btn-text').textContent = 'Buscando...';
    } else {
        searchBtn.disabled = false;
        loader.classList.remove('active');
        searchBtn.querySelector('.btn-text').textContent = 'Buscar Disponibilidad';
    }
}

function displayResults(results, tarifas, idHotel, idTipo, fechaInicio, fechaFin) {
    resultsContainer.innerHTML = '';
    
    results.forEach((result, index) => {
        // Buscar tarifa correspondiente
        const tarifa = tarifas.find(t => t.tipoHabitacion === result.tipoHabitacion) || tarifas[0] || null;
        const card = createResultCard(result, tarifa, index, idHotel, idTipo, fechaInicio, fechaFin);
        resultsContainer.appendChild(card);
    });
    
    resultsSection.style.display = 'block';
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function createResultCard(result, tarifa, index, idHotel, idTipo, fechaInicio, fechaFin) {
    const card = document.createElement('div');
    card.className = 'result-card';
    
    const disponibilidadPorcentaje = result.cantidadTotal > 0 
        ? (result.cantidadDisponible / result.cantidadTotal) * 100 
        : 0;
    
    let availabilityClass = 'availability-low';
    let availabilityText = 'Baja';
    
    if (disponibilidadPorcentaje >= 50) {
        availabilityClass = 'availability-high';
        availabilityText = 'Alta';
    } else if (disponibilidadPorcentaje >= 25) {
        availabilityClass = 'availability-medium';
        availabilityText = 'Media';
    }
    
    card.innerHTML = `
        <div class="result-header">
            <h3 class="result-title">${result.tipoHabitacion || 'Tipo de Habitación'}</h3>
            <span class="result-badge">
                <span class="availability-indicator ${availabilityClass}"></span>
                ${availabilityText}
            </span>
        </div>
        <div class="result-info">
            <div class="info-item">
                <span class="info-label">Capacidad de Personas</span>
                <span class="info-value">${result.capacidadPersonas || 'N/A'}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Cantidad Total</span>
                <span class="info-value">${result.cantidadTotal || 0}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Habitaciones Disponibles</span>
                <span class="info-value" style="color: ${result.cantidadDisponible > 0 ? 'var(--success)' : 'var(--error)'}">
                    ${result.cantidadDisponible || 0}
                </span>
            </div>
            <div class="info-item">
                <span class="info-label">Habitaciones Ocupadas</span>
                <span class="info-value">${(result.cantidadTotal || 0) - (result.cantidadDisponible || 0)}</span>
            </div>
            ${tarifa ? `
            <div class="tarifa-info">
                <div class="info-item tarifa-item">
                    <span class="info-label">Temporada</span>
                    <span class="info-value">${tarifa.temporada || 'N/A'}</span>
                </div>
                <div class="info-item tarifa-item">
                    <span class="info-label">Precio por Noche</span>
                    <span class="info-value precio-value">$${parseFloat(tarifa.precioBaseNoche || 0).toFixed(2)}</span>
                </div>
                <div class="info-item tarifa-item">
                    <span class="info-label">Precio Persona Adicional</span>
                    <span class="info-value precio-value">$${parseFloat(tarifa.precioPersonaAdicional || 0).toFixed(2)}</span>
                </div>
            </div>
            ` : ''}
        </div>
        <div class="result-actions">
            <button class="btn btn-reservar" onclick="abrirModalReserva(${idHotel}, ${idTipo}, '${fechaInicio}', '${fechaFin}', ${result.cantidadDisponible || 0}, ${result.capacidadPersonas || 0})">
                Reservar
            </button>
        </div>
    `;
    
    return card;
}

function showNoResults() {
    resultsContainer.innerHTML = `
        <div class="no-results">
            <div class="no-results-icon">∅</div>
            <h3 style="color: var(--text-dark); margin-bottom: 10px;">No se encontraron resultados</h3>
            <p style="color: var(--text-light);">No hay habitaciones disponibles para los criterios de búsqueda seleccionados.</p>
            <p style="color: var(--text-light); margin-top: 10px; font-size: 0.9rem;">Intente con otras fechas o parámetros diferentes.</p>
        </div>
    `;
    resultsSection.style.display = 'block';
}

function showError(message) {
    errorMessage.textContent = message;
    errorSection.style.display = 'block';
    errorSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideError() {
    errorSection.style.display = 'none';
}

function hideResults() {
    resultsSection.style.display = 'none';
}

// Datos de prueba para demostración (se pueden usar cuando el backend no esté disponible)
function loadSampleData() {
    const sampleData = [
        {
            tipoHabitacion: 'Habitación Estándar',
            cantidadTotal: 20,
            cantidadDisponible: 15,
            capacidadPersonas: 2
        },
        {
            tipoHabitacion: 'Suite Deluxe',
            cantidadTotal: 10,
            cantidadDisponible: 3,
            capacidadPersonas: 4
        },
        {
            tipoHabitacion: 'Habitación Familiar',
            cantidadTotal: 15,
            cantidadDisponible: 12,
            capacidadPersonas: 6
        }
    ];
    
    // Comentar esta línea cuando el backend esté disponible
    // displayResults(sampleData);
}

// Funcionalidad del modal de reserva
const reservaModal = document.getElementById('reservaModal');
const closeModal = document.getElementById('closeModal');
const calcularPrecioBtn = document.getElementById('calcularPrecioBtn');
const confirmarReservaBtn = document.getElementById('confirmarReservaBtn');
const precioResult = document.getElementById('precioResult');
const precioDetails = document.getElementById('precioDetails');

// Cerrar modal
closeModal.addEventListener('click', () => {
    reservaModal.style.display = 'none';
    precioResult.style.display = 'none';
    document.getElementById('reservaForm').reset();
});

window.addEventListener('click', (e) => {
    if (e.target === reservaModal) {
        reservaModal.style.display = 'none';
        precioResult.style.display = 'none';
        document.getElementById('reservaForm').reset();
    }
});

// Abrir modal de reserva
window.abrirModalReserva = function(idHotel, idTipo, fechaInicio, fechaFin, cantidadDisponible, capacidadPersonas) {
    document.getElementById('modalIdHotel').value = idHotel;
    document.getElementById('modalIdTipo').value = idTipo;
    document.getElementById('modalFechaInicio').value = fechaInicio;
    document.getElementById('modalFechaFin').value = fechaFin;
    document.getElementById('cantidadHabitaciones').setAttribute('max', cantidadDisponible);
    document.getElementById('cantidadHabitaciones').setAttribute('placeholder', `Máximo: ${cantidadDisponible}`);
    document.getElementById('numeroPersonas').setAttribute('placeholder', `Capacidad base: ${capacidadPersonas} por habitación`);
    reservaModal.style.display = 'block';
    precioResult.style.display = 'none';
};

// Calcular precio
calcularPrecioBtn.addEventListener('click', async () => {
    const formData = new FormData(document.getElementById('reservaForm'));
    const numeroPersonas = parseInt(formData.get('numeroPersonas'));
    const cantidadHabitaciones = parseInt(formData.get('cantidadHabitaciones'));
    
    if (!numeroPersonas || !cantidadHabitaciones) {
        alert('Por favor, complete todos los campos');
        return;
    }
    
    const request = {
        idHotel: parseInt(document.getElementById('modalIdHotel').value),
        idTipo: parseInt(document.getElementById('modalIdTipo').value),
        fechaInicio: document.getElementById('modalFechaInicio').value,
        fechaFin: document.getElementById('modalFechaFin').value,
        numeroPersonas: numeroPersonas,
        cantidadHabitaciones: cantidadHabitaciones
    };
    
    try {
        calcularPrecioBtn.disabled = true;
        calcularPrecioBtn.textContent = 'Calculando...';
        
        const response = await fetch(`${API_BASE_URL}/calcular-precio`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        
        if (!response.ok) {
            throw new Error('Error al calcular el precio');
        }
        
        const precioData = await response.json();
        mostrarPrecio(precioData);
        
    } catch (error) {
        console.error('Error al calcular precio:', error);
        alert('Error al calcular el precio. Por favor, intente nuevamente.');
    } finally {
        calcularPrecioBtn.disabled = false;
        calcularPrecioBtn.textContent = 'Calcular Precio';
    }
});

function mostrarPrecio(precioData) {
    let desglose = {};
    try {
        desglose = JSON.parse(precioData.desglose || '{}');
    } catch (e) {
        desglose = {};
    }
    
    precioDetails.innerHTML = `
        <div class="precio-summary">
            <div class="precio-item">
                <span class="precio-label">Precio por Noche:</span>
                <span class="precio-amount">$${parseFloat(precioData.precioPorNoche || 0).toFixed(2)}</span>
            </div>
            <div class="precio-item">
                <span class="precio-label">Número de Noches:</span>
                <span class="precio-amount">${precioData.numeroNoches || 0}</span>
            </div>
            <div class="precio-item">
                <span class="precio-label">Temporada:</span>
                <span class="precio-amount">${precioData.temporada || 'N/A'}</span>
            </div>
            ${desglose.cantidadHabitaciones ? `
            <div class="precio-item">
                <span class="precio-label">Cantidad de Habitaciones:</span>
                <span class="precio-amount">${desglose.cantidadHabitaciones}</span>
            </div>
            <div class="precio-item">
                <span class="precio-label">Subtotal Habitaciones:</span>
                <span class="precio-amount">$${parseFloat(desglose.subtotalHabitaciones || 0).toFixed(2)}</span>
            </div>
            ` : ''}
            ${desglose.personasExtra > 0 ? `
            <div class="precio-item">
                <span class="precio-label">Personas Extra:</span>
                <span class="precio-amount">${desglose.personasExtra}</span>
            </div>
            <div class="precio-item">
                <span class="precio-label">Precio Persona Adicional:</span>
                <span class="precio-amount">$${parseFloat(desglose.precioPersonaAdicional || 0).toFixed(2)}</span>
            </div>
            <div class="precio-item">
                <span class="precio-label">Subtotal Personas Extra:</span>
                <span class="precio-amount">$${parseFloat(desglose.subtotalPersonasExtra || 0).toFixed(2)}</span>
            </div>
            ` : ''}
            <div class="precio-total">
                <span class="precio-label-total">TOTAL:</span>
                <span class="precio-amount-total">$${parseFloat(precioData.precioTotal || 0).toFixed(2)}</span>
            </div>
        </div>
    `;
    
    precioResult.style.display = 'block';
}

// Confirmar reserva
confirmarReservaBtn.addEventListener('click', async () => {
    const formData = new FormData(document.getElementById('reservaForm'));
    
    const request = {
        idHotel: parseInt(document.getElementById('modalIdHotel').value),
        idTipo: parseInt(document.getElementById('modalIdTipo').value),
        fechaInicio: document.getElementById('modalFechaInicio').value,
        fechaFin: document.getElementById('modalFechaFin').value,
        numeroPersonas: parseInt(formData.get('numeroPersonas')),
        cantidadHabitaciones: parseInt(formData.get('cantidadHabitaciones'))
    };
    
    try {
        confirmarReservaBtn.disabled = true;
        confirmarReservaBtn.textContent = 'Procesando...';
        
        const response = await fetch(`${API_BASE_URL}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        
        if (!response.ok) {
            throw new Error('Error al crear la reserva');
        }
        
        const reservaData = await response.json();
        
        if (reservaData.exito) {
            alert(`Reserva creada exitosamente! ID: ${reservaData.idReserva}\nTotal: $${parseFloat(reservaData.totalCalculado || 0).toFixed(2)}`);
            reservaModal.style.display = 'none';
            precioResult.style.display = 'none';
            document.getElementById('reservaForm').reset();
        } else {
            alert(`Error: ${reservaData.mensaje || 'No se pudo crear la reserva'}`);
        }
        
    } catch (error) {
        console.error('Error al crear reserva:', error);
        alert('Error al crear la reserva. Por favor, intente nuevamente.');
    } finally {
        confirmarReservaBtn.disabled = false;
        confirmarReservaBtn.textContent = 'Confirmar Reserva';
    }
});

// Cargar datos de muestra al iniciar (opcional, para pruebas)
// loadSampleData();
