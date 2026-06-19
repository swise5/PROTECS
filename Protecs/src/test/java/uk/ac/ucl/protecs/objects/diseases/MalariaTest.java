package uk.ac.ucl.protecs.objects.diseases;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Assert;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;
import uk.ac.ucl.protecs.helperFunctions.*;
import uk.ac.ucl.protecs.objects.hosts.Person.SEX;


public class MalariaTest extends TestWatcherSetup {
	// ==================================== Testing ==================================================================	
	@Override
	protected String getParams() {
		return "params_malaria.txt";
	}
	
	@Override
	protected String getOutputFileName() {
		return "malaria-test-seeds.log";
	}
	
	@Test
	public void prevalenceSeedingCreatesCases() {
		int seed = (int) this.seed;		

		// create a simulation and start
		WorldBankCovid19Sim sim = HelperFunctions.CreateDummySimWithSeed(seed, PARAMS_DIR + "params_malaria.txt");
		// using GBD SSA Malaria/HIV data as an example
		for (Entry<DISEASE, HashMap<String, HashMap<String, Double>>> diseaseEntry : sim.params.prevalenceLineList.entrySet()) {
	        DISEASE disease = diseaseEntry.getKey();
	        HashMap<String, HashMap<String, Double>> sexMap = diseaseEntry.getValue();

	        for (Entry<String, HashMap<String, Double>> sexEntry : sexMap.entrySet()) {
	        	String sex = sexEntry.getKey();
	            HashMap<String, Double> ageMap = sexEntry.getValue();

	            for (Entry<String, Double> ageEntry : ageMap.entrySet()) {
	                double prevalence = ageEntry.getValue();
	                prevalence *= 100;
	                ageEntry.setValue(prevalence);

	            	}
	            }
		}
		sim.start();
		Assert.assertTrue(sim.human_infections.size() > 0);
	}
	
	
}