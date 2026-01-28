package com.example.laterealmenu;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Planta implements Serializable {
    private String id;
    private String tituloRegistro;
    private String nombreComun;
    private String nombreCientifico;
    private String descripcion;
    private String categoria;
    private String prioridad;
    private String fechaCreacion;
    private String imagenBase64;
    private String usuarioId;
    private int diasRiego;
    private int diasFertilizante;
    private float probabilidadIdentificacion;
    private boolean notificacionesActivadas;
    private boolean agregadoManual;
    private String estadoSeguimiento;
    private int progreso;
    private boolean requiereSeguimiento;


    private Object ultimoRiego;
    private Object proximoRiego;
    private Object proximaFertilizacion;
    private Object fechaRegistro;

    private String notasCuidados;

    // Campos de progreso
    private String estadoCrecimiento;
    private Object fechaUltimoProgreso;
    private String observacionesProgreso;
    private List<String> historialProgreso;
    private int salud;
    private boolean necesitaAtencion;

    // Campos para progreso diario
    private boolean regadoHoy;
    private boolean fertilizadoHoy;
    private Object fechaUltimaFertilizacion;
    private int totalRiegos;
    private int totalFertilizaciones;
    private String notasAdicionales;

    // Constructores
    public Planta() {}


    public Long getUltimoRiegoLong() {
        return convertToLong(ultimoRiego);
    }

    public Long getProximoRiegoLong() {
        return convertToLong(proximoRiego);
    }

    public Long getProximaFertilizacionLong() {
        return convertToLong(proximaFertilizacion);
    }

    public Long getFechaRegistroLong() {
        return convertToLong(fechaRegistro);
    }

    public Long getFechaUltimaFertilizacionLong() {
        return convertToLong(fechaUltimaFertilizacion);
    }

    public Long getFechaUltimoProgresoLong() {
        return convertToLong(fechaUltimoProgreso);
    }


    private Long convertToLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof com.google.firebase.Timestamp) {
            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) obj;
            return timestamp.getSeconds();
        }
        return null;
    }


    public Date getUltimoRiegoAsDate() {
        Long seconds = getUltimoRiegoLong();
        return seconds != null ? new Date(seconds * 1000) : new Date();
    }

    public Date getProximoRiegoAsDate() {
        try {
            Long seconds = getProximoRiegoLong();
            return seconds != null ? new Date(seconds * 1000) : new Date();
        } catch (Exception e) {
            return new Date(); // Retorna fecha actual si hay error
        }
    }

    public Date getFechaRegistroAsDate() {
        try {
            Long seconds = getFechaRegistroLong();
            return seconds != null ? new Date(seconds * 1000) : new Date();
        } catch (Exception e) {
            return new Date(); // Retorna fecha actual si hay error
        }
    }

    public Date getProximaFertilizacionAsDate() {
        try {
            Long seconds = getProximaFertilizacionLong();
            return seconds != null ? new Date(seconds * 1000) : new Date();
        } catch (Exception e) {
            return new Date(); // Retorna fecha actual si hay error
        }
    }
    public Date getFechaUltimaFertilizacionAsDate() {
        Long seconds = getFechaUltimaFertilizacionLong();
        return seconds != null ? new Date(seconds * 1000) : new Date();
    }

    // Getters y Setters para Object (necesarios para Firestore)
    public Object getUltimoRiego() { return ultimoRiego; }
    public void setUltimoRiego(Object ultimoRiego) { this.ultimoRiego = ultimoRiego; }

    public Object getProximoRiego() { return proximoRiego; }
    public void setProximoRiego(Object proximoRiego) { this.proximoRiego = proximoRiego; }

    public Object getProximaFertilizacion() { return proximaFertilizacion; }
    public void setProximaFertilizacion(Object proximaFertilizacion) { this.proximaFertilizacion = proximaFertilizacion; }

    public Object getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Object fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Object getFechaUltimaFertilizacion() { return fechaUltimaFertilizacion; }
    public void setFechaUltimaFertilizacion(Object fechaUltimaFertilizacion) { this.fechaUltimaFertilizacion = fechaUltimaFertilizacion; }

    public Object getFechaUltimoProgreso() { return fechaUltimoProgreso; }
    public void setFechaUltimoProgreso(Object fechaUltimoProgreso) { this.fechaUltimoProgreso = fechaUltimoProgreso; }

    // ... (el resto de tus getters y setters permanecen igual)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTituloRegistro() { return tituloRegistro; }
    public void setTituloRegistro(String tituloRegistro) { this.tituloRegistro = tituloRegistro; }

    public String getNombreComun() { return nombreComun; }
    public void setNombreComun(String nombreComun) { this.nombreComun = nombreComun; }

    public String getNombreCientifico() { return nombreCientifico; }
    public void setNombreCientifico(String nombreCientifico) { this.nombreCientifico = nombreCientifico; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public int getDiasRiego() { return diasRiego; }
    public void setDiasRiego(int diasRiego) { this.diasRiego = diasRiego; }

    public int getDiasFertilizante() { return diasFertilizante; }
    public void setDiasFertilizante(int diasFertilizante) { this.diasFertilizante = diasFertilizante; }

    public float getProbabilidadIdentificacion() { return probabilidadIdentificacion; }
    public void setProbabilidadIdentificacion(float probabilidadIdentificacion) { this.probabilidadIdentificacion = probabilidadIdentificacion; }

    public boolean isNotificacionesActivadas() { return notificacionesActivadas; }
    public void setNotificacionesActivadas(boolean notificacionesActivadas) { this.notificacionesActivadas = notificacionesActivadas; }

    public boolean isAgregadoManual() { return agregadoManual; }
    public void setAgregadoManual(boolean agregadoManual) { this.agregadoManual = agregadoManual; }

    public String getEstadoSeguimiento() { return estadoSeguimiento; }
    public void setEstadoSeguimiento(String estadoSeguimiento) { this.estadoSeguimiento = estadoSeguimiento; }

    public int getProgreso() { return progreso; }
    public void setProgreso(int progreso) { this.progreso = progreso; }

    public boolean isRequiereSeguimiento() { return requiereSeguimiento; }
    public void setRequiereSeguimiento(boolean requiereSeguimiento) { this.requiereSeguimiento = requiereSeguimiento; }

    public String getNotasCuidados() { return notasCuidados; }
    public void setNotasCuidados(String notasCuidados) { this.notasCuidados = notasCuidados; }

    public String getEstadoCrecimiento() { return estadoCrecimiento; }
    public void setEstadoCrecimiento(String estadoCrecimiento) { this.estadoCrecimiento = estadoCrecimiento; }

    public String getObservacionesProgreso() { return observacionesProgreso; }
    public void setObservacionesProgreso(String observacionesProgreso) { this.observacionesProgreso = observacionesProgreso; }

    public List<String> getHistorialProgreso() { return historialProgreso; }
    public void setHistorialProgreso(List<String> historialProgreso) { this.historialProgreso = historialProgreso; }

    public int getSalud() { return salud; }
    public void setSalud(int salud) { this.salud = salud; }

    public boolean isNecesitaAtencion() { return necesitaAtencion; }
    public void setNecesitaAtencion(boolean necesitaAtencion) { this.necesitaAtencion = necesitaAtencion; }

    public boolean isRegadoHoy() { return regadoHoy; }
    public void setRegadoHoy(boolean regadoHoy) { this.regadoHoy = regadoHoy; }

    public boolean isFertilizadoHoy() { return fertilizadoHoy; }
    public void setFertilizadoHoy(boolean fertilizadoHoy) { this.fertilizadoHoy = fertilizadoHoy; }

    public int getTotalRiegos() { return totalRiegos; }
    public void setTotalRiegos(int totalRiegos) { this.totalRiegos = totalRiegos; }

    public int getTotalFertilizaciones() { return totalFertilizaciones; }
    public void setTotalFertilizaciones(int totalFertilizaciones) { this.totalFertilizaciones = totalFertilizaciones; }

    public String getNotasAdicionales() { return notasAdicionales; }
    public void setNotasAdicionales(String notasAdicionales) { this.notasAdicionales = notasAdicionales; }
}