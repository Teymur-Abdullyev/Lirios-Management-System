package com.liriosbeauty.Entity;

public enum OrderStatus {
    PENDING,     // Gözləyir (istifadə edilmir hal-hazırda)
    COMPLETED,   // Tamamlanıb
    CANCELLED,   // Ləğv edilib (mallar göndərilməyib)
    REFUNDED     // Geri qaytarılıb (mallar göndərilib, sonra qaytarılıb)
}