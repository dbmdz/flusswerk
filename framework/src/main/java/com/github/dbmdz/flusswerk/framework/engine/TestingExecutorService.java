package com.github.dbmdz.flusswerk.framework.engine;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

class TestingExecutorService extends AbstractExecutorService implements ExecutorService {

  private final LinkedList<Runnable> tasks;
  private boolean isShutdown;

  public TestingExecutorService() {
    tasks = new LinkedList<>();
    isShutdown = false;
  }

  @Override
  public void shutdown() {
    isShutdown = true;
  }

  @Override
  public @NotNull List<Runnable> shutdownNow() {
    shutdown();
    return tasks;
  }

  @Override
  public boolean isShutdown() {
    return isShutdown;
  }

  @Override
  public boolean isTerminated() {
    return isShutdown && tasks.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
    tasks.clear();
    return true;
  }

  @Override
  public void execute(@NotNull Runnable command) {
    if (isShutdown) {
      throw new RuntimeException("TestingExecutorService has been shut down");
    }
    command.run();
    tasks.add(command);
  }
}
