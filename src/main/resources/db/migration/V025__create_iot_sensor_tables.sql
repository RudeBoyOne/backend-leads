-- Vínculo dispositivo <-> imóvel. 1:1 por definição: o device_id é único
-- (um dispositivo pertence a no maximo um imóvel) e property_id também
-- (um imóvel expoe no maximo um sensor na rota de detalhe).
CREATE TABLE property_sensor (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID        NOT NULL REFERENCES property(id) ON DELETE CASCADE,
    device_id   VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_property_sensor_device   UNIQUE (device_id),
    CONSTRAINT uq_property_sensor_property UNIQUE (property_id)
);

CREATE INDEX idx_property_sensor_property_id ON property_sensor(property_id);

-- Última leitura conhecida de cada dispositivo. Substituída a cada mensagem.
-- DOUBLE PRECISION (e nao NUMERIC) porque a entidade mapeia Double, e o Hibernate
-- valida a coluna como float(53) — NUMERIC reprovaria no ddl-auto: validate.
CREATE TABLE sensor_metric (
    device_id   VARCHAR(64)  NOT NULL PRIMARY KEY,
    temperature DOUBLE PRECISION NOT NULL,
    humidity    DOUBLE PRECISION NOT NULL,
    luminosity  DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ   NOT NULL
);

-- Agregação incremental por hora UTC. A média é derivada de sum/sample_count,
-- então o bucket é acumulável sem recalcular a partir do histórico.
CREATE TABLE sensor_metric_hourly (
    device_id        VARCHAR(64)  NOT NULL,
    bucket_start     TIMESTAMPTZ  NOT NULL,
    sample_count     INT          NOT NULL DEFAULT 0,
    temperature_sum  DOUBLE PRECISION NOT NULL DEFAULT 0,
    temperature_min  DOUBLE PRECISION  NOT NULL,
    temperature_max  DOUBLE PRECISION  NOT NULL,
    humidity_sum     DOUBLE PRECISION NOT NULL DEFAULT 0,
    humidity_min     DOUBLE PRECISION  NOT NULL,
    humidity_max     DOUBLE PRECISION  NOT NULL,
    luminosity_sum   DOUBLE PRECISION NOT NULL DEFAULT 0,
    luminosity_min   DOUBLE PRECISION NOT NULL,
    luminosity_max   DOUBLE PRECISION NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (device_id, bucket_start)
);

CREATE INDEX idx_sensor_metric_hourly_device_id ON sensor_metric_hourly(device_id);
