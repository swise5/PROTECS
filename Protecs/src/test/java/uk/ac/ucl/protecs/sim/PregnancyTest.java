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
import uk.ac.ucl.protecs.objects.hosts.Person.BMIStatus;

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
		// turn off preterm births 
		sim.demographyFramework.setPtb_base_rate(0);
		// adjust the median birth interval so that short birth periods are an exception
		sim.demographyFramework.setMedian_birth_interval(1000);

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
//	Test no longer in use as randomness in birth interval is now included
//	@Test
//	public void pregnantPeopleDoNotImmediatelyGetPregnantAgain() {
//		int seed = (int) this.seed;		
//
//		// set up the simulation
//		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
//		sim.start();
//		// turn off deaths
//		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);
//		// Increase the birth rate to ensure births take place
//		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.05);
//		// Run the simulation for one day to set up the births
//		int numDays = 1; 		
//		HelperFunctions.runSimulation(sim, numDays);
//		// Get the original set of pregnant women
//		List<Person> currently_pregnant = get_pregnant(sim).get(true).get(true);
//		
//		// keep running the simulation for one year (the minimum time that those originally pregnant will need to have the option for births again)
//		numDays = 365;
//		HelperFunctions.runSimulation(sim, numDays);
//		// iterate over the set of original pregnant woman and check that none are still pregnant
//		for (Person p: currently_pregnant) {
//			Assert.assertFalse(p.isPregnant());
//
//		}
//	}
	
	@Test
	public void whenWeTurnTheBirthRateOffThereAreNoBirths() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_demography.txt");
		sim.start();
		int initialNumberOfPeople = HelperFunctions.GetNumberAlive(sim);
		// turn off deaths
		HelperFunctions.turnOffBirthsOrDeaths(sim, birthsOrDeaths.deaths);
		// Turn off birth rates
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
		// create holders for testing whether ptb has happened
		ArrayList <Person> babiesBornPreTerm = new ArrayList<Person>();
		ArrayList <Person> mothersGaveBirthPreTerm = new ArrayList<Person>();
		// add ptb occurrences and updates to baby and mother properties
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
		// set up a simulation with twins
		WorldBankCovid19Sim sim_w_twins = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_twins.start();
		// Run sim with every birth being a twin
		sim_w_twins.demographyFramework.setProb_multiple_pregnancy(1);
		HelperFunctions.runSimulation(sim_w_twins, numDays);
		// calculate the new number of babies
		int with_twins_n_people = sim_w_twins.agents.size();
		int with_twins_n_babies = with_twins_n_people - initial_n_people;
		// test whether twins have been born
		Assert.assertTrue(with_twins_n_people > no_twins_n_people);
		Assert.assertTrue(with_twins_n_babies == 2 * no_twin_n_babies);
	}
	
	@Test
	public void testMultipleBirthRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// make every birth be twins
		sim.demographyFramework.setProb_multiple_pregnancy(1);

		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
 		// find out how many babies are born pre term without risk factors
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		// set up a sim with twins
		WorldBankCovid19Sim sim_w_twins = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_twins.start();
		sim_w_twins.demographyFramework.setProb_multiple_pregnancy(1);

		// set dummy values for ptb
		sim_w_twins.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors apart from multiple births
		remove_ptb_risk_factors(sim_w_twins);
		sim_w_twins.demographyFramework.setPtb_AOR_multiple_pregnancy(3.08);
		
		HelperFunctions.runSimulation(sim_w_twins, numDays);
 		// find out how many babies are born pre term with risk factors

		ArrayList <Person> multipleBirthAffectedRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim_w_twins.agents) {
			if (p.isBornPreTerm()) {
				multipleBirthAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// check that when we account for the risk factor associated with twins, more babies are born preterm
		Assert.assertTrue(multipleBirthAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testPreviousPTBRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.1);

		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		
		for (Person p: sim.agents) {
			p.setPriorPreTerm(true);
		}
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
 		// find out how many babies are born pre term without risk factors

		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_previous_ptb = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_previous_ptb.start();
		HelperFunctions.setParameterListsToValue(sim_w_previous_ptb, sim_w_previous_ptb.demographyFramework.getProb_birth_by_age(), 0.1);
		// make everyone have a prior pre term birth

		for (Person p: sim_w_previous_ptb.agents) {
			
			p.setPriorPreTerm(true);
			
		}

		// set dummy values for ptb
		sim_w_previous_ptb.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors apart from prior ptb
		remove_ptb_risk_factors(sim_w_previous_ptb);
		// Inflate the risk of previous ptb, normally 3.45
		sim_w_previous_ptb.demographyFramework.setPtb_AOR_previous_ptb(3.45);
		
		HelperFunctions.runSimulation(sim_w_previous_ptb, numDays);
		ArrayList <Person> previousPTBAffectedRateBabiesBornPreTerm = new ArrayList<Person>();
 		// find out how many babies are born pre term with risk factors

		for (Person p: sim_w_previous_ptb.agents) {
			if (p.isBornPreTerm()) {
				previousPTBAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// check that when we account for the risk factor associated with prior ptb, more babies are born preterm

		Assert.assertTrue(previousPTBAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testUnderTwentyRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// increase birth rates
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.5);

		// make population under 20
		for (Person p: sim.agents) {
				p.setAgeForTesting(18);

		}
		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_age = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_age.start();
		HelperFunctions.setParameterListsToValue(sim_w_age, sim_w_age.demographyFramework.getProb_birth_by_age(), 0.5);

		// make the population under 20
		for (Person p: sim_w_age.agents) {
				p.setAgeForTesting(18);

		}
		
		// set dummy values for ptb
		sim_w_age.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors apart from age
		remove_ptb_risk_factors(sim_w_age);
		sim_w_age.demographyFramework.setPtb_AOR_age_less_than_20_years(1.76);
		
		HelperFunctions.runSimulation(sim_w_age, numDays);
		ArrayList <Person> ageAffectedRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim_w_age.agents) {
			if (p.isBornPreTerm()) {
				ageAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// make sure there are more ptb occurrences when we account for the risk factor
		Assert.assertTrue(ageAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testHIVRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.1);

		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
 		// count the babies born preterm without the risk factor
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_hiv = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_hiv.start();
		
		HelperFunctions.setParameterListsToValue(sim_w_hiv, sim_w_hiv.demographyFramework.getProb_birth_by_age(), 0.1);

		// set dummy values for ptb
		sim_w_hiv.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors apart from HIV
		remove_ptb_risk_factors(sim_w_hiv);
		sim_w_hiv.demographyFramework.setPtb_AOR_hiv(2.59);
		HelperFunctions.runSimulation(sim_w_hiv, numDays);
		// count the babies born preterm with the reisk factor of HIV being accounted for
		ArrayList <Person> hivAffectedRateBabiesBornPreTerm = new ArrayList<Person>();

		for (Person p: sim_w_hiv.agents) {
			if (p.isBornPreTerm()) {
				hivAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// test that there have been more babies born preterm with HIV as a risk factor than without
		Assert.assertTrue(hivAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testAnemiaRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// Make the population have anemia
		for (Person p: sim.agents) {
					
			p.setDietary_iron_deficiency(true);
					
		}
		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the babies born preterm without the risk factor associated with anemia
		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_anemia = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_anemia.start();
		// Make the population have anemia
		for (Person p: sim_w_anemia.agents) {
			
			p.setDietary_iron_deficiency(true);
			
		}
		// set dummy values for ptb
		sim_w_anemia.demographyFramework.setPtb_base_rate(0.1);
		remove_ptb_risk_factors(sim_w_anemia);

		// remove the effects of risk factors apart from Anemia
		sim_w_anemia.demographyFramework.setPtb_AOR_anemia(4.58);
		
		HelperFunctions.runSimulation(sim_w_anemia, numDays);
		ArrayList <Person> anemiaAffectedRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the babies born preterm with the risk factor from anemia being considered
		for (Person p: sim_w_anemia.agents) {
			if (p.isBornPreTerm()) {
				anemiaAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// test that the number of babies born preterm with anemia being accounted for is greater than when it isn't
		Assert.assertTrue(anemiaAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testShortBirthIntervalRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// increase the birth rate to make sure that multiple pregnancies will happen this simulation
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 1);

		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		// shorten birth interval to a day to ensure short birth intervals occur
		sim.demographyFramework.setMedian_birth_interval(1.0);
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the number of babies born preterm when we don't account for the risk of short birth interval
		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_short_birth_duration = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_short_birth_duration.start();
		// increase the birth rate
		HelperFunctions.setParameterListsToValue(sim_w_short_birth_duration, sim_w_short_birth_duration.demographyFramework.getProb_birth_by_age(), 1);
		// set dummy values for ptb
		sim_w_short_birth_duration.demographyFramework.setPtb_base_rate(0.1);

		// remove the effects of risk factors apart from short birth interval
		remove_ptb_risk_factors(sim_w_short_birth_duration);
		// increase the effect of short birth interval
		sim_w_short_birth_duration.demographyFramework.setPtb_AOR_short_birth_interval(2.03);
		// shorten birth interval to a day to ensure short birth intervals occur
		sim_w_short_birth_duration.demographyFramework.setMedian_birth_interval(1.0);
		HelperFunctions.runSimulation(sim_w_short_birth_duration, numDays);
		ArrayList <Person> shortBirthDurationAffectedRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the number of babies born preterm when we account for the risk of short birth interval

		for (Person p: sim_w_short_birth_duration.agents) {
			if (p.isBornPreTerm()) {
				shortBirthDurationAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// test that when we account for the risk factor associated with short birth interval, the number of preterm births is greater than when we don't
		Assert.assertTrue(shortBirthDurationAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	@Test
	public void testUnderweightRiskFactorIncreasesPTB() {
		int seed = (int) this.seed;		

		// set up the simulation
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim.start();
		// increase the birth rate
		HelperFunctions.setParameterListsToValue(sim, sim.demographyFramework.getProb_birth_by_age(), 0.05);

		// set dummy values for ptb
		sim.demographyFramework.setPtb_base_rate(0.1);
		// remove the effects of risk factors
		remove_ptb_risk_factors(sim);
		// make the population underweight
		for (Person p: sim.agents) {
			p.setBmistatus(BMIStatus.UNDERWEIGHT);
		}
		
		int numDays = 365; 		
 		HelperFunctions.runSimulation(sim, numDays);
		ArrayList <Person> baseRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the number of babies born preterm when we don't account for the risk factor associated with being underweight
		for (Person p: sim.agents) {
			if (p.isBornPreTerm()) {
				baseRateBabiesBornPreTerm.add(p);
			}
		}
		
		WorldBankCovid19Sim sim_w_underweight = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_ptb.txt");
		sim_w_underweight.start();
		// increase the birth rate
		HelperFunctions.setParameterListsToValue(sim_w_underweight, sim_w_underweight.demographyFramework.getProb_birth_by_age(), 0.05);
		// set dummy values for ptb
		sim_w_underweight.demographyFramework.setPtb_base_rate(0.1);

		// remove the effects of risk factors
		remove_ptb_risk_factors(sim_w_underweight);
		// make the population underweight
		for (Person p: sim_w_underweight.agents) {
			p.setBmistatus(BMIStatus.UNDERWEIGHT);
		}
				
		// Set the risk factor for being underweight
		sim_w_underweight.demographyFramework.setPtb_AOR_underweight(4.52);
		HelperFunctions.runSimulation(sim_w_underweight, numDays);
		ArrayList <Person> underweightAffectedRateBabiesBornPreTerm = new ArrayList<Person>();
		// count the number of babies born preterm when we account for the risk factor associated with being underweight

		for (Person p: sim_w_underweight.agents) {
			if (p.isBornPreTerm()) {
				underweightAffectedRateBabiesBornPreTerm.add(p);
			}
		}
		// test whether accounting for the risks of being underweight in pregnancy increases the number preterm births compared to when we don't
		Assert.assertTrue(underweightAffectedRateBabiesBornPreTerm.size() > baseRateBabiesBornPreTerm.size());

	}
	
	// ================================================ Helper functions =======================================================================

	private void remove_ptb_risk_factors(WorldBankCovid19Sim sim) {
		sim.demographyFramework.setPtb_AOR_age_less_than_20_years(1);
		sim.demographyFramework.setPtb_AOR_short_birth_interval(1);
		sim.demographyFramework.setPtb_AOR_previous_ptb(1);
		sim.demographyFramework.setPtb_AOR_anemia(1);
		sim.demographyFramework.setPtb_AOR_underweight(1);
		sim.demographyFramework.setPtb_AOR_hiv(1);
		sim.demographyFramework.setPtb_AOR_malaria(1);
		sim.demographyFramework.setPtb_AOR_multiple_pregnancy(1);
	}
	
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
