package uk.ac.ucl.protecs.sim;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import uk.ac.ucl.protecs.behaviours.diseaseProgression.HIVDiseaseProgressionFramework;
import uk.ac.ucl.protecs.behaviours.diseaseProgression.MalariaDiseaseProgressionFramework;
import uk.ac.ucl.protecs.objects.diseases.HIV;
import uk.ac.ucl.protecs.objects.diseases.Malaria;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.objects.hosts.Person.BMIStatus;
import uk.ac.ucl.protecs.objects.hosts.Person.SEX;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;

import java.util.stream.Collectors;


public class loadEndemicConditions{
	

	public static List<Person> get_demographic(WorldBankCovid19Sim world, int[] age_range, String sex) {
		if (sex.equals("both")) {
			return world.agents.stream()
		            .filter(p -> p.inAgeRange(age_range))
		            .collect(Collectors.toList());
		}
		else {
		    return world.agents.stream()
		            .filter(p -> p.inAgeRange(age_range))
		            .filter(p -> p.isOfSex(SEX.getValue(sex)))
		            .collect(Collectors.toList());
	    }
	}
	
	static void seed_endemic_conditions(WorldBankCovid19Sim world) {

	    for (Entry<DISEASE, HashMap<String, HashMap<String, Double>>> diseaseEntry : world.params.prevalenceLineList.entrySet()) {
	        DISEASE disease = diseaseEntry.getKey();
	        HashMap<String, HashMap<String, Double>> sexMap = diseaseEntry.getValue();

	        for (Entry<String, HashMap<String, Double>> sexEntry : sexMap.entrySet()) {
	        	String sex = sexEntry.getKey();
	            HashMap<String, Double> ageMap = sexEntry.getValue();

	            for (Entry<String, Double> ageEntry : ageMap.entrySet()) {
	                String age_range = ageEntry.getKey();
	                double prevalence = ageEntry.getValue();
	                // convert this percentage (between 0 and 100) to decimal
	                prevalence /= 100;

	                int[] bounds = convert_GBD_boundary_to_int(age_range);
	                List<Person> eligible = get_demographic(world, bounds, sex);

	                for (Person p : eligible) {
	                    if (world.random.nextDouble() < prevalence) {

	                        switch (disease) {
	                            case HIV: {
	        						if (world.hivFramework == null) {
	        							world.hivFramework = new HIVDiseaseProgressionFramework(world);
	        							}
	                                HIV inf = new HIV(p, null, world.hivFramework.getEntryPoint(), world, 0);
	                                world.schedule.scheduleOnce(inf, world.param_schedule_infecting);
	                                break;
	                            }
	                            case MALARIA:{
	                            	if (world.malariaFramework == null) {
	        							world.malariaFramework = new MalariaDiseaseProgressionFramework(world);
	        							}
	                            	Malaria inf = new Malaria(p, null, world.malariaFramework.getEntryPoint(), world, 0);
	                                world.schedule.scheduleOnce(inf, world.param_schedule_infecting);
	                                break;
	                            }
	                            case ANEMIA:{
	                            	p.setDietary_iron_deficiency(true);
	                            	break;
	                            }
	                            default: {
	                                // no-op for now
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	}

	
	static void seed_BMI_status(WorldBankCovid19Sim world) {
		// Iterate over the age boundaries of the holder
	    for (Entry<String, HashMap<String, HashMap<BMIStatus, Double>>> ageEntry: world.params.prevalenceBMIStatus.entrySet()) {
	    	// get the age range of this entry
	        String ageRange = ageEntry.getKey();
	        // convert this into a format that we can use
	        int[] bounds = convert_GBD_boundary_to_int(ageRange);
	        // get the sex bmi hashmap
	        HashMap<String, HashMap<BMIStatus, Double>> sexMap = ageEntry.getValue();
	        // iterate over it
	        for (Entry<String, HashMap<BMIStatus, Double>> sexEntry : sexMap.entrySet()) {
	        	// get the sex of the entry
	            String sex = sexEntry.getKey();
	            // get the hashmap with bmi status and prevalence
	            HashMap<BMIStatus, Double> bmiMap = sexEntry.getValue();
	            // get the eligible people for assigning a bmi status
	            List<Person> eligible = get_demographic(world, bounds, sex);
	            // shuffle the list of people so that bmi status assignment is random
	            Collections.shuffle(eligible);
	            // get the number of people in the age-sex category
	            int n = eligible.size();
	            // get the number of people who will be underweight, health and overweight
	            int underweightCount = (int) Math.round(
	                    n * bmiMap.getOrDefault(BMIStatus.UNDERWEIGHT, 0.0));

	            int healthyCount = (int) Math.round(
	                    n * bmiMap.getOrDefault(BMIStatus.HEALTHYWEIGHT, 0.0));

	            int overweightCount = (int) Math.round(
	                    n * bmiMap.getOrDefault(BMIStatus.OVERWEIGHT, 0.0));
	            // there are four bmi categories, considered here. We will calculate the obese population reductively, i.e. they are the population who
	            // are not under/over or a healthy weight
	            int index = 0;
	            // assign those who are underweight
	            for (; index < underweightCount; index++) {
	                eligible.get(index).setBMIStatus(BMIStatus.UNDERWEIGHT);
	            }
	            // assign those who are a healthy weight

	            for (; index < underweightCount + healthyCount; index++) {
	                eligible.get(index).setBMIStatus(BMIStatus.HEALTHYWEIGHT);
	            }
	            // assign those who are overweight

	            for (; index < underweightCount + healthyCount + overweightCount; index++) {
	                eligible.get(index).setBMIStatus(BMIStatus.OVERWEIGHT);
	            }
	            // make the remaining people in the age sex category obese

	            for (; index < n; index++) {
	                eligible.get(index).setBMIStatus(BMIStatus.OBESE);
	            }
	        }
	    }
	}
	
	private static int[] convert_GBD_boundary_to_int(String ageRange) {
	    ageRange = ageRange.trim();

	    // Case: "<5 years"
	    if (ageRange.startsWith("<")) {
	        int upper = Integer.parseInt(ageRange.replaceAll("[^0-9]", ""));
	        return new int[]{0, upper - 1};
	    }

	    // Case: "95+ years"
	    if (ageRange.contains("+")) {
	        int lower = Integer.parseInt(ageRange.replaceAll("[^0-9]", ""));
	        return new int[]{lower, 120}; // or Integer.MAX_VALUE if you prefer
	    }
	    if (ageRange.equals("All ages")) {
	        return new int[]{0, 120}; // or Integer.MAX_VALUE if you prefer

	    }
	    // Case: "X-Y years"
	    String[] parts = ageRange.replace(" years", "").split("-");
	    int lower = Integer.parseInt(parts[0]);
	    int upper = Integer.parseInt(parts[1]);

	    return new int[]{lower, upper};
	}
}