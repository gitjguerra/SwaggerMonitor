# Informe de Impacto / Dependencias — Microservicio `osba` (restservices.xml)

Fuente de datos: `%BM_HOME%\appl` (entorno de pruebas, 2 dominios + osba).

- `api-pagos/config/services.xml` → dominio **Pagos** (10 servicios)
- `api-persona/config/services.xml` → dominio **Persona** (54 servicios)
- `osba/config/restservices.xml` → microservicio analizado (52 servicios REST expuestos/consumidos por osba)
- `osba/config/services.xml` → **excluido** del análisis de dominios (es la integración legacy SOAP/CICS propia de osba, no un canal de negocio)

Metodología de cruce: **coincidencia exacta por atributo `name`** (decisión explícita del solicitante, fiel al método original aunque arroja baja cobertura — ver advertencia metodológica).

---

## 1. Resumen ejecutivo

- **Dominios analizados:** 2 (Pagos, Persona). El entorno productivo real tiene más dominios (Tarjeta, Créditos, Inversiones, etc.); en este entorno de pruebas solo se cargaron Pagos y Persona más osba, por tamaño.
- **Servicios en `restservices.xml`:** 52.
- **Servicios con coincidencia exacta de `name` en algún dominio:** 7 de 52 (**13,5 %**).
- **Servicios sin ninguna coincidencia de `name`:** 45 de 52 (**86,5 %**).
- De los 7 servicios con coincidencia, **todos** caen en el dominio **Persona**; ninguno coincide con Pagos por nombre.
- Como solo hay 2 dominios cargados, el porcentaje máximo posible de afectación por servicio es 50 % (1 de 2 dominios) → los 7 servicios con coincidencia quedan clasificados como **Alta** criticidad (41-60 %). No hay servicios "Críticos" (61-100 %) en esta metodología, porque ningún `name` coincide simultáneamente en los dos dominios.
- **Advertencia metodológica (importante):** el cruce estricto por `name` subestima fuertemente la dependencia real. Al revisar el último segmento del `endpoint` (que normalmente identifica el programa/transacción backend real), aparecen **al menos 5 coincidencias adicionales de alta confianza** donde el `name` difiere entre `restservices.xml` (nomenclatura REST en inglés) y el `services.xml` de dominio (nomenclatura interna en español), pero apuntan al mismo programa backend. Es decir, bajo el método solicitado (solo `name`), el informe es fiel a la instrucción, pero **no debe interpretarse como el mapa real de dependencias** de osba. Ver sección 7.

---

## 2. Tabla consolidada (coincidencia exacta por `name`)

| Servicio (`restservices.xml`) | Dominios afectados | Cantidad | % Afectación | Criticidad |
|---|---|---|---|---|
| consultar-telefonos-detallado | Persona | 1 | 50% | Alta |
| consultar-localizacion-nacional-compactada | Persona | 1 | 50% | Alta |
| ingresar-datos-sociodemograficos | Persona | 1 | 50% | Alta |
| ingresar-localizacion-nacional-compactada | Persona | 1 | 50% | Alta |
| ingresar-telefonos-cliente | Persona | 1 | 50% | Alta |
| ingresar-autorizaciones-cliente | Persona | 1 | 50% | Alta |
| actualizar-telefono-cliente | Persona | 1 | 50% | Alta |
| *(los 45 servicios restantes)* | — | 0 | 0% | Baja |

---

## 3. `afectacion.xml` (generado)

Ver archivo adjunto: [`afectacion.xml`](./afectacion.xml) — contiene únicamente los 7 servicios con coincidencia (los de 0% se omiten del XML por convención, igual que en el ejemplo del prompt original; decir si se prefiere listar también los de 0%).

## 4. JSON equivalente

Ver archivo adjunto: [`afectacion.json`](./afectacion.json) — incluye también metadatos (`totalDominiosAnalizados`, `totalServiciosRestservices`, etc.) y el listado completo de los 52 servicios (incluidos los de 0%, con `domains: []`), útil como fuente directa para el backend de la pestaña "Impacto / Dependencias".

---

## 5. Visualización tipo grafo (texto)

```
consultar-telefonos-detallado
     └── Persona

consultar-localizacion-nacional-compactada
     └── Persona

ingresar-datos-sociodemograficos
     └── Persona

ingresar-localizacion-nacional-compactada
     └── Persona

ingresar-telefonos-cliente
     └── Persona

ingresar-autorizaciones-cliente
     └── Persona

actualizar-telefono-cliente
     └── Persona

(45 servicios restantes de restservices.xml)
     └── (sin dominio coincidente por nombre)
```

---

## 6. Hallazgos

- **Servicios más críticos (por este método):** los 7 listados arriba, todos empatados en 50% / Alta — no hay diferenciación adicional posible con solo 2 dominios cargados.
- **Servicios compartidos por más dominios:** ninguno comparte más de 1 dominio (máximo posible = 2, no se da ningún caso de doble coincidencia).
- **Dominio con mayor dependencia declarada desde osba:** **Persona** (7 coincidencias) vs. **Pagos** (0 coincidencias por nombre).
- **Posibles SPOF (Single Point of Failure):** con la metodología estricta, no se puede concluir SPOF real porque la cobertura de nombres es muy baja (86,5% de servicios sin trazabilidad). El SPOF más probable en la práctica —según la evidencia de endpoint de la sección 7— es el programa backend `gipw106x` (`consultarTelefonosDetallado`), referenciado tanto por Persona como por osba vía dos nombres distintos.
- **Top 10 de mayor impacto potencial:** no aplica estrictamente (solo 7 servicios tienen impacto distinto de cero, y todos están empatados). Se listan los 7:
  1. consultar-telefonos-detallado
  2. consultar-localizacion-nacional-compactada
  3. ingresar-datos-sociodemograficos
  4. ingresar-localizacion-nacional-compactada
  5. ingresar-telefonos-cliente
  6. ingresar-autorizaciones-cliente
  7. actualizar-telefono-cliente

---

## 7. Posibles inconsistencias de nomenclatura (evidencia secundaria, NO usada para el cálculo oficial)

Cruzando por el **último segmento del `endpoint`** (el identificador real de programa/transacción, p. ej. `...-gipw106x`), se detectan correspondencias de alta confianza que el cruce por `name` no ve, porque `osba/restservices.xml` usa nombres REST en inglés y los `services.xml` de dominio usan nombres internos en español:

| `restservices.xml` (name) | services.xml dominio (name) | Dominio | Evidencia (segmento final de endpoint) |
|---|---|---|---|
| validate-prospect | validar-aceptacion-prospecto | Persona | `validarAceptacionProspecto-gipw148x` |
| ingresar-data-sociodemograficos | ingresar-datos-sociodemograficos | Persona | `ingresarDatosSociodemograficos-gipw022x` (coincide también por name) |
| inclusion-account-gip-pr | ingresar-producto | Persona | `ingresarProducto-gipw052x` |
| consult-detailed-phone | consultar-telefonos-detallado | Persona | `consultarTelefonosDetallado-gipw106x` (coincide también por name) |
| consult-locations-thundernet | consultar-thundernet-sedes | Pagos | `companies` |

Adicionalmente, `consult-pay-thundernet`, `register-pay-thundernet` y `reverse-pay-thundernet` comparten el segmento genérico `payment` con 3 servicios de Pagos (`consultar-thundernet-pago`, `registrar-thundernet-pago`, `reversar-thundernet-pago`) — **esta coincidencia es demasiado genérica para ser evidencia confiable** (falso positivo probable) y se reporta solo para visibilidad, no como hallazgo validado.

**Recomendación:** si se quiere un mapa de dependencias confiable, el cruce no debería hacerse solo por `name`; conviene mantener en cada servicio de `restservices.xml` una referencia explícita (tag o convención) al `name` real del `services.xml` de dominio que invoca, o cruzar por el identificador de programa backend (sufijo del endpoint), excluyendo segmentos genéricos.

---

## 8. Estadísticas generales del ecosistema

- Total de dominios cargados: 2
- Total de servicios de dominio: 64 (Pagos 10 + Persona 54)
- Total de servicios del microservicio analizado: 52
- Cobertura de trazabilidad por `name`: 13,5%
- Cobertura de trazabilidad por sufijo de endpoint (heurística): ~17,3% (9 servicios, descontando los 3 ambiguos de "payment")
- Duplicados de `name` dentro de un mismo archivo: 0 (en los 3 archivos analizados)
- Nombres duplicados entre dominios distintos (Pagos vs. Persona): 0

---

## 9. Propuestas de visualización empresarial

(detalladas también en la propuesta de pestaña "Impacto / Dependencias" — ver `propuesta-pestana-impacto.md`)

- **Heatmap** servicio × dominio, celdas en verde/amarillo/rojo por % de afectación.
- **Grafo de dependencias** interactivo (nodo servicio en el centro, nodos de dominio alrededor; clic en nodo → panel de detalle).
- **Dashboard ejecutivo** con contadores (dominios, servicios, dependencias, críticos, SPOF) + ranking ordenable.
- **Matriz de impacto** tabular con scroll, filtro por dominio y por nivel de criticidad.
