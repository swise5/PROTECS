package uk.ac.ucl.protecs.sim;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;

import uk.ac.ucl.protecs.helperFunctions.*;
import uk.ac.ucl.protecs.helperFunctions.HelperFunctions.birthsOrDeaths;

import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import uk.ac.ucl.protecs.objects.hosts.Person;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class PregnancyTest extends TestWatcherSetup{
	@Override
	protected String getParams() {
		return "params_demography.txt";
	}
	@Override
	protected String getOutputFileName() {
		return "pregnancy-test-seeds.log";
	}
	
	@Test
	public void testPregnancyIsReset() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
		sim.start();
		// turn off deaths
		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);
		// Increase the birth rate to ensure births take place
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.05);
		
		// Run the simulation for one day to set up the births
		int numDays = 1; 		
		HelperFunctions.runSimulation(sim, numDays);
		// Get the original set of pregnant women
		List<Person> currently_pregnant = get_pregnant(sim).get(true).get(true);
		
		int original_number_of_pregnant_women = currently_pregnant.size();
		// keep running the simulation for 8 months so that some pregnancies will have resolved
		numDays = 8 * 30;
		HelperFunctions.runSimulation(sim, numDays);
		// check the number of people currently pregnant after this year's births are done
		currently_pregnant = get_pregnant(sim).get(true).get(true);
		int final_number_of_pregnant_women = 0;
		try {
			final_number_of_pregnant_women = currently_pregnant.size();
		}
		catch (Exception e) {
			// no pregnant woman
		}		
		// As some of the initially set up pregnancies should have resolved, the final number of pregnant people should be less than the initial number of 
		// pregnant people
		Assert.assertTrue(original_number_of_pregnant_women > final_number_of_pregnant_women);
		
	}
	
	@Test
	public void testPregnancyLastsNineMonths() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
		sim.start();
		// turn off deaths
		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);
		// Increase the birth rate to ensure births take place
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.05);
		// Run the simulation for one day to set up the births
		int numDays = 1; 		
		HelperFunctions.runSimulation(sim, numDays);
		// Get the original set of pregnant women
		List<Person> currently_pregnant = get_pregnant(sim).get(true).get(true);
		
		// keep running the simulation for 10 months so that the births scheduled for the initial 9 months and those starting during the first month
		// of simulation time will have resolved
		numDays = 10 * 30;
		HelperFunctions.runSimulation(sim, numDays);
		// iterate over the set of original pregnant woman and check that none are still pregnant
		for (Person p: currently_pregnant) {
			Assert.assertFalse(p.isPregnant());

		}
	}
	
	@Test
	public void pregnantPeopleDoNotImmediatelyGetPregnantAgain() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
		sim.start();
		// turn off deaths
		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);
		// Increase the birth rate to ensure births take place
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.05);
		// Run the simulation for one day to set up the births
		int numDays = 1; 		
		HelperFunctions.runSimulation(sim, numDays);
		// Get the original set of pregnant women
		List<Person> currently_pregnant = get_pregnant(sim).get(true).get(true);
		
		// keep running the simulation for one year (the minimum time that those originally pregnant will need to have the option for births again)
		numDays = 365;
		HelperFunctions.runSimulation(sim, numDays);
		// iterate over the set of original pregnant woman and check that none are still pregnant
		for (Person p: currently_pregnant) {
			Assert.assertFalse(p.isPregnant());

		}
	}
	
	@Test
	public void whenWeTurnTheBirthRateOffThereAreNoBirths() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
		sim.start();
		int initialNumberOfPeople = HelperFunctions.GetNumberAlive(sim);
		// turn off deaths
		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);

		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.00);
		// Run the simulation for one day to set up the births
		int numDays = 365; 		
		HelperFunctions.runSimulation(sim, numDays);
		int finalNumberOfPeople = HelperFunctions.GetNumberAlive(sim);

		Assert.assertTrue(initialNumberOfPeople == finalNumberOfPeople);

		
	}
	
	@Test
	public void testPreTermBirthsOccur() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.5);
		int numDays = 9 * 30; 		
		// run for 9 months so that the initial pregnancies will have caused ptb
		HelperFunctions.runSimulation(sim, numDays);
		ArrayList <Person> babiesBornPreTerm = new ArrayList<Person>();
		ArrayList <Person> mothersGaveBirthPreTerm = new ArrayList<Person>();

		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				babiesBornPreTerm.add(p);
			}
			if (p.hasPriorPreTerm()) {
				mothersGaveBirthPreTerm.add(p);
			}
		}
		
		Assert.assertTrue(babiesBornPreTerm.size() > 0);
		Assert.assertTrue(mothersGaveBirthPreTerm.size() > 0);

	}
	
	@Test
	public void testTwinsAreBeingBorn() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		int initial_n_people = sim.agents.size();
		// Run sim with no twins being born
		sim.demographyFramework.setProb_multiple_pregnancy(0);
		int numDays = 9 * 30; 		
		HelperFunctions.runSimulation(sim, numDays);
		int no_twins_n_people = sim.agents.size();
		int no_twin_n_babies = no_twins_n_people - initial_n_people;
		
		WorldBankCovid19Sim sim_w_twins = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_twins.start();
		// Run sim with every birth being a twin
		sim_w_twins.demographyFramework.setProb_multiple_pregnancy(1);
		HelperFunctions.runSimulation(sim_w_twins, numDays);
		int with_twins_n_people = sim_w_twins.agents.size();
		int with_twins_n_babies = with_twins_n_people - initial_n_people;
		Assert.assertTrue(with_twins_n_people > no_twins_n_people);
		Assert.assertTrue(with_twins_n_babies == 2 * no_twin_n_babies);



	}
	
	// ================================================ Helper functions =======================================================================

	// get those who are currently pregnant
	public static Map<Boolean, Map<Boolean, List<Person>>> get_pregnant(WorldBankCovid19Sim world) {
		// create a function to group the population by who is alive and pregnant
		Map<Boolean, Map<Boolean, List<Person>>> pregnant = world.agents.stream().collect(
				Collectors.groupingBy(
						Person::isAlive,
						Collectors.groupingBy(
								Person::isPregnant
								)
						)
				);
		return pregnant;
	}
}
