package uk.ac.ucl.protecs.objects.diseases;

import sim.engine.SimState;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;
import uk.ac.ucl.swise.behaviours.BehaviourNode;

public class Malaria extends Disease{

	public Malaria(Person p, WorldBankCovid19Sim sim, BehaviourNode initNode, int time) {
		this.host = p;
		this.source = p;
		this.infectedAtLocation = p.getLocation();
		this.currentBehaviourNode = initNode;
		this.time_infected = time;
		this.myWorld = sim;
		this.myWorld.human_infections.add(this);
		this.host.addDisease(this);
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
		return null;
	}

	@Override
	public String getDiseaseName() {
		return null;
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