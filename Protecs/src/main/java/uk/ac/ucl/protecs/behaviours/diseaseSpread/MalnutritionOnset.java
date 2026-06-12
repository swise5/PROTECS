package uk.ac.ucl.protecs.behaviours.diseaseSpread;

import sim.engine.SimState;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;

public class MalnutritionOnset extends NCDOnset{

	MalnutritionOnset(WorldBankCovid19Sim world, int ticks_between_checks) {
		this.myWorld = world;
	}

	@Override
	public void step(SimState arg0) {
		determineDevelopNCD(arg0, myWorld);
		
	}

	@Override
	public void determineDevelopNCD(SimState arg0, WorldBankCovid19Sim myWorld) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createNCD(WorldBankCovid19Sim myWorld, Person personToDevelopNCD) {
		// TODO Auto-generated method stub
		
	}}