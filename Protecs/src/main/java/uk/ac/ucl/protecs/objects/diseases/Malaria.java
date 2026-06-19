package uk.ac.ucl.protecs.objects.diseases;

import sim.engine.SimState;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;
import uk.ac.ucl.swise.behaviours.BehaviourNode;

public class Malaria extends Disease{

	public Malaria(Person myHost, Person mySource, BehaviourNode initNode, WorldBankCovid19Sim sim){
		this(myHost, mySource, initNode, sim, (int) sim.schedule.getTime());
	}

	public Malaria(Person myHost, Person mySource, BehaviourNode initNode, WorldBankCovid19Sim sim, int time){
		
		host = myHost;
		myHost.addDisease(this);
		source = mySource;
		
		//	epidemic_state = Params.state_susceptible;
		//	infected_symptomatic_status = Params.symptom_none;
		//	clinical_state = Params.clinical_not_hospitalized;
			
		// store the time when it is infected!
		time_infected = time;		
		infectedAtLocation = myHost.getLocation();
		
		time_died = Double.MAX_VALUE;
		currentBehaviourNode = initNode;
		myWorld = sim;
		myWorld.human_infections.add(this);
	}
	
	@Override
	public void step(SimState arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInfectious() {
		return false;
	}

	@Override
	public boolean isWaterborne() {
		return false;
	}

	@Override
	public void horizontalTransmission() {		
	}

	@Override
	public void verticalTransmission(Person baby) {		
	}

	@Override
	public boolean isOfType(DISEASE disease) {
		return false;
	}

	@Override
	public DISEASE getDiseaseType() {
		return DISEASE.MALARIA;
	}

	@Override
	public String getDiseaseName() {
		return DISEASE.MALARIA.key;
	}

	@Override
	public String writeOut() {
		return null;
	}

	@Override
	public boolean inATestingAdminZone() {
		return false;
	}
	
}