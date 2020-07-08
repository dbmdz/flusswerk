package com.github.dbmdz.flusswerk.framework.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionSupplierTest {

  @Test
  void shouldSupplyExceptionWithOnlyMessage() {
    var supplier = new ExceptionSupplier<>(RetryProcessingException.class, "Hallo Welt", null);
    assertThat(supplier.get())
        .isInstanceOf(RetryProcessingException.class)
        .hasMessage("Hallo Welt");
  }

  @Test
  void shouldSupplyStopProcessingExceptionWithOnlyMessage() {
    var supplier = new ExceptionSupplier<>(StopProcessingException.class, "Hallo Welt", null);
    assertThat(supplier.get()).isInstanceOf(StopProcessingException.class).hasMessage("Hallo Welt");
  }

  @Test
  void shouldSupplyExceptionWithMessageAndCause() {
    var supplier =
        new ExceptionSupplier<>(RetryProcessingException.class, "Hallo Welt", null)
            .causedBy(new RuntimeException("THE CAUSE"));
    assertThat(supplier.get())
        .isInstanceOf(RetryProcessingException.class)
        .hasMessage("Hallo Welt");
  }

  @Test
  void shouldSupplyStopProcessingExceptionWithMessageAndCause() {
    var supplier =
        new ExceptionSupplier<>(StopProcessingException.class, "Hallo Welt", null)
            .causedBy(new RuntimeException("THE CAUSE"));
    assertThat(supplier.get()).isInstanceOf(StopProcessingException.class).hasMessage("Hallo Welt");
  }
}
