package de.digitalcollections.workflow.bdd;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty"}, features = {"classpath:features/deadlettering.feature", "classpath:features/successful_processing.feature"})
public class RunCucumberTest {

  @ClassRule
  public static final GenericContainer RABBIT_MQ = Orchestration.getInstance().getRabbitMQ();

  private Orchestration orchestration = Orchestration.getInstance();

  @After
  public void tearDown() {
    orchestration.stopEngine();
  }

}
