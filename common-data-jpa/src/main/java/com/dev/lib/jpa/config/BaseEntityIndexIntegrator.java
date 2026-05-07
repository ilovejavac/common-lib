package com.dev.lib.jpa.config;

import org.hibernate.integrator.spi.Integrator;

/**
 * Backward-compatible Hibernate SPI hook.
 * <p>
 * Earlier builds published a service entry for this integrator. Keeping a
 * no-op implementation here prevents startup failures when older descriptors
 * are still present on the classpath during incremental builds or mixed
 * reactor/local-repository resolution.
 */
public class BaseEntityIndexIntegrator implements Integrator {
}
