package uk.ac.ucl.protecs.objects.diseases;

import org.junit.Test;

import org.junit.Assert;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;
import uk.ac.ucl.protecs.helperFunctions.*;


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
		
		sim.start();
		boolean malaria_has_been_seeded = false;
		for (Disease d: sim.human_infections) {
			if (d.getDiseaseType().equals(DISEASE.MALARIA)) {
				malaria_has_been_seeded = true;
				break;
			}
		}
		Assert.assertTrue(malaria_has_been_seeded);
	}
	
	
}