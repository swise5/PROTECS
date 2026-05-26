package uk.ac.ucl.protecs.behaviours.diseaseSpread;


import sim.engine.SimState;
import uk.ac.ucl.protecs.objects.diseases.DummyNonCommunicableDisease;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.objects.hosts.Person.SEX;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;


public class DummyNCDOnset extends NCDOnset{
	public DummyNCDOnset(WorldBankCovid19Sim myWorld){
		this.myWorld = myWorld;
	}

	@Override
	public void step(SimState arg0) {
		// determine if anyone will develop a dummy NCD this month
		determineDevelopNCD(arg0, myWorld);				
	}

	@Override
	public void determineDevelopNCD(SimState arg0, WorldBankCovid19Sim myWorld) {
		// Create a list to account for risk factors and protective factors against developing the dummy NCD

				for (Person p: myWorld.agents) {
					// create a risk factor for this individual
					double riskFactor = 1;
					// if they aren't alive they can't develop the NCD
					if (!p.isAlive()) {
						riskFactor = 0;
						continue;
					}
					// if they already have the NCD they can't develop the NCD
					if (p.getDiseaseSet().containsKey(DISEASE.DUMMY_NCD.key)) {
						riskFactor = 0;
						continue;
					}
					// if they are male increase the likelihood of developing the NCD 
					if (p.getSex().equals(SEX.MALE)) {
						riskFactor *= myWorld.dummyNCDFramework.getDummy_ncd_relative_risk_male();
					}
					// if they are over 50 increase the likelihood of developing the NCD 
					if (p.getAge() > 50) {
						riskFactor *= myWorld.dummyNCDFramework.getDummy_ncd_relative_risk_over_50();
					}
					// Check if they develop the NCD
					if (myWorld.random.nextDouble() < riskFactor * myWorld.dummyNCDFramework.getDummy_ncd_base_rate()) {
						createNCD(myWorld, p);
					 }
				}
			
						
	}

	@Override
	public void createNCD(WorldBankCovid19Sim myWorld, Person personToDevelopNCD) {
		DummyNonCommunicableDisease inf = new DummyNonCommunicableDisease(personToDevelopNCD, personToDevelopNCD, myWorld.dummyNCDFramework.getStandardEntryPoint(), myWorld);
		myWorld.schedule.scheduleOnce(inf);		
	}	
	

}

	
	
