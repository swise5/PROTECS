package uk.ac.ucl.protecs.behaviours.diseaseSpread;

import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;

public abstract class NCDOnset implements Steppable{
	WorldBankCovid19Sim myWorld;
	public abstract void determineDevelopNCD(SimState arg0, WorldBankCovid19Sim myWorld);
	public abstract void createNCD(WorldBankCovid19Sim myWorld, Person personToDevelopNCD);
}