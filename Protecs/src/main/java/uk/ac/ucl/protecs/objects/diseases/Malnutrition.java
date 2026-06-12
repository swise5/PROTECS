package uk.ac.ucl.protecs.objects.diseases;

import sim.engine.SimState;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;
import uk.ac.ucl.swise.behaviours.BehaviourNode;

public class Malnutrition extends Disease{
	
	public Malnutrition(Person myHost, Person mySource, BehaviourNode initNode, WorldBankCovid19Sim sim){
		this(myHost, mySource, initNode, sim, (int) sim.schedule.getTime());
	}

	public Malnutrition(Person myHost, Person mySource, BehaviourNode initNode, WorldBankCovid19Sim sim, int time) {
		host = myHost;
		
		source = mySource;
		
		host.addDisease(this);
		
		this.diseaseStage = DISEASESTAGE.ASYMPTOMATIC;
			
		// store the time when it is infected!
		time_infected = time;		
		infectedAtLocation = myHost.getLocation();
		currentBehaviourNode = initNode;
		myWorld = sim;
		myWorld.human_infections.add(this);	}

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
		return disease.equals(DISEASE.MALNUTRITION);
	}

	@Override
	public DISEASE getDiseaseType() {
		return DISEASE.MALNUTRITION;
	}

	@Override
	public String getDiseaseName() {
		return DISEASE.MALNUTRITION.name();
	}

	@Override
	public String writeOut() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean inATestingAdminZone() {
		return false;
	}}