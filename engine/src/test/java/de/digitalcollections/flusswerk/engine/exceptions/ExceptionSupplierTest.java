package de.digitalcollections.flusswerk.engine.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionSupplierTest {

  @Test
  void shoudlSupplyExceptionWithOnlyMessage() {
    var args = new String[]{};
    var supplier = new ExceptionSupplier<>(RetryProcessingException.class, "Hallo Welt", null);
    assertThat(supplier.get())
        .isInstanceOf(RetryProcessingException.class)
        .hasMessage("Hallo Welt");
  }

  @Test
  void shoudlSupplyStopProcessingExceptionWithOnlyMessage() {
    var args = new String[]{};
    var supplier = new ExceptionSupplier<>(StopProcessingException.class, "Hallo Welt", null);
    assertThat(supplier.get())
        .isInstanceOf(StopProcessingException.class)
        .hasMessage("Hallo Welt");
  }

  @Test
  void shoudlSupplyExceptionWithMessageAndCause() {
    var args = new String[]{};
    var supplier = new ExceptionSupplier<>(RetryProcessingException.class, "Hallo Welt", null).causedBy(new RuntimeException("THE CAUSE"));
    assertThat(supplier.get())
        .isInstanceOf(RetryProcessingException.class)
        .hasMessage("Hallo Welt");
  }

  @Test
  void shoudlSupplyStopProcessingExceptionWithMessageAndCause() {
    var args = new String[]{};
    var supplier = new ExceptionSupplier<>(StopProcessingException.class, "Hallo Welt", null).causedBy(new RuntimeException("THE CAUSE"));;
    assertThat(supplier.get())
        .isInstanceOf(StopProcessingException.class)
        .hasMessage("Hallo Welt");
  }

}