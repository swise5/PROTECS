package uk.ac.ucl.protecs.treatments;

import sim.engine.Steppable;
import uk.ac.ucl.protecs.objects.hosts.Host;
import uk.ac.ucl.protecs.objects.locations.Location;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;

public abstract class medicalIntervention implements Steppable{
	
	public Host patient;
	WorldBankCovid19Sim myWorld;
	public Location placeOfTreatment;
	boolean treatmentAvailable;
	boolean treatmentCompleted;

	public double time_start_treatment = Double.MAX_VALUE;
	public double time_end_treatment = Double.MAX_VALUE;


	public abstract void applyIntervention();
	public abstract void leaveIntervention();

	
	public Location getPlaceOfTreatment() {return placeOfTreatment;}
	public void setPlaceOfTreatment(Location placeOfTreatment) {this.placeOfTreatment = placeOfTreatment;}
	
	public boolean isTreatmentAvailable() {return treatmentAvailable;}
	public void setTreatmentAvailable(boolean treatmentAvailable) {this.treatmentAvailable = treatmentAvailable;}
	
	public boolean isTreatmentCompleted() {return treatmentCompleted;}
	public void setTreatmentCompleted(boolean treatmentCompleted) {this.treatmentCompleted = treatmentCompleted;}
}