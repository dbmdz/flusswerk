package com.github.dbmdz.flusswerk.framework.engine;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TaskQueue {

  private final PriorityBlockingQueue<Task> queue;
  private final Semaphore semaphore;

  public TaskQueue(int workers) {
    queue = new PriorityBlockingQueue<>();
    semaphore = new Semaphore(workers);
  }

  /**
   * Adds a new task to the queue and blocks if all workers are busy.
   *
   * @param task The task to add to the queue
   * @throws InterruptedException If the thread is interrupted while blocking
   */
  public void put(Task task) throws InterruptedException {
    semaphore.acquire(); // make sure there is space for a new task
    queue.put(task);
  }

  /**
   * @return The task with the highest priority (or first in instead).
   * @throws InterruptedException If the thread is interrupted while releasing the semaphore
   */
  public Task get() throws InterruptedException {
    semaphore.release(); // free up space for the next task
    return queue.take();
  }

  /**
   * Polls the queue, waiting for a certain amount of time. If there is a task, then the semaphore
   * will be released to make space for new tasks.
   *
   * @param timeout
   * @param timeUnit
   * @return
   * @throws InterruptedException
   */
  public Task poll(int timeout, TimeUnit timeUnit) throws InterruptedException {
    Task task = queue.poll(timeout, timeUnit);
    if (task != null) {
      semaphore.release();
    }
    return task;
  }
}
