CREATE DATABASE IF NOT EXISTS clinica_veterinaria
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE clinica_veterinaria;

-- 1. Tabla secundaria: mascotas (5 campos)
CREATE TABLE IF NOT EXISTS mascotas (
    id_mascota INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    especie VARCHAR(40) NOT NULL,
    raza VARCHAR(60) NOT NULL,
    propietario VARCHAR(100) NOT NULL
);

-- 2. Tabla principal: citas (5 campos, con llave foránea hacia mascotas)
CREATE TABLE IF NOT EXISTS citas (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    id_mascota INT NOT NULL,
    motivo VARCHAR(150) NOT NULL,
    fecha_cita DATETIME NOT NULL,
    estado ENUM('Programada', 'Atendida', 'Cancelada') NOT NULL DEFAULT 'Programada',
    CONSTRAINT fk_citas_mascota
        FOREIGN KEY (id_mascota) REFERENCES mascotas(id_mascota)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Datos iniciales de prueba para mascotas
INSERT INTO mascotas (nombre, especie, raza, propietario) VALUES
('Max', 'Perro', 'Golden Retriever', 'Carlos Mendoza'),
('Luna', 'Gato', 'Siames', 'Lucía Paredes'),
('Rocky', 'Perro', 'Pastor Alemán', 'Miguel Quispe'),
('Milo', 'Conejo', 'Holland Lop', 'Sofía Rojas');