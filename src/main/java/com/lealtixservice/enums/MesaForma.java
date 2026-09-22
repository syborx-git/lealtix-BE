package com.lealtixservice.enums;

/**
 * Forma visual de la mesa en el plano interactivo.
 * Se almacenan en BD como texto en minúsculas (redonda|cuadrada|rectangular)
 * para que coincidan con los valores usados por el frontend.
 */
public enum MesaForma {
    redonda,
    cuadrada,
    rectangular
}