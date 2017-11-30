package de.digitalcollections.workflow.bdd;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.After;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty"}, features = {"classpath:features/deadlettering.feature", "classpath:features/successful_processing.feature"})
public class RunCucumberTest {

  private Orchestration orchestration = Orchestration.getInstance();

  @After
  public void tearDown() {
    orchestration.stopEngine();
  }

}
