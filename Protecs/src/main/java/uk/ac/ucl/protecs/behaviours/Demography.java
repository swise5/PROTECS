package uk.ac.ucl.protecs.behaviours;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import sim.engine.SimState;
import sim.engine.Steppable;
import uk.ac.ucl.protecs.objects.diseases.Disease;
import uk.ac.ucl.protecs.objects.hosts.Person;
import uk.ac.ucl.protecs.objects.hosts.Person.BMIStatus;
import uk.ac.ucl.protecs.objects.hosts.Person.OCCUPATION;
import uk.ac.ucl.protecs.objects.hosts.Person.SEX;
import uk.ac.ucl.protecs.objects.locations.Household;
import uk.ac.ucl.protecs.objects.locations.Workplace;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim;
import uk.ac.ucl.protecs.sim.WorldBankCovid19Sim.DISEASE;

public class Demography {
	WorldBankCovid19Sim myWorld;
	// all cause mortality parameters
	public ArrayList <Integer> all_cause_death_age_params;
	public ArrayList <Double> prob_death_by_age_male;
	public ArrayList <Double> prob_death_by_age_female;
	public ArrayList <Integer> birth_age_params;
	public ArrayList <Double> prob_birth_by_age;
	
	// PTB risk factor parameters, taken from https://link.springer.com/article/10.1186/s13052-020-0772-1 meta review in east Africa
	public double ptb_base_rate = 0;
	public double ptb_AOR_age_less_than_20_years = 1.76;
	public double ptb_AOR_short_birth_interval = 2.03;
	public double ptb_AOR_previous_ptb = 3.45;
	public double ptb_AOR_anemia = 4.58;
	public double ptb_AOR_hiv = 2.59;
	public double ptb_AOR_malaria = 3.08;
	public double ptb_AOR_multiple_pregnancy = 3.08;
	public double ptb_AOR_underweight = 4.52;

	public double prob_multiple_pregnancy = 0.0174; // https://www.cambridge.org/core/journals/twin-research-and-human-genetics/article/twin-births-in-42-subsaharan-african-countries-from-1986-to-2016-frequency-trends-and-factors-of-variation/39A88B150744A794DDBF816FFA7F5950

	public double median_birth_interval = 32; // median birth interval in SSA is 32 months https://link.springer.com/article/10.1186/s40834-026-00448-w


	public double q1_birth_interval = 23; // q1 birth interval in SSA is 23 months https://link.springer.com/article/10.1186/s40834-026-00448-w
	public double q3_birth_interval = 46;// q3 birth interval in SSA is 46 months https://link.springer.com/article/10.1186/s40834-026-00448-w
	
	// generate a distribution for the weeks born preterm
	public double ptb_prob_born_before_28_weeks = 0.042; // global estimates from https://www.thelancet.com/journals/lancet/article/PIIS0140-6736(23)00878-4/fulltext?uuid=uuid%3A1db73a17-556f-469a-b546-6bcb66dee6f5#supplementary-material
	public double ptb_prob_born_28_32_weeks = 0.104;// https://www.thelancet.com/journals/lancet/article/PIIS0140-6736(23)00878-4/fulltext?uuid=uuid%3A1db73a17-556f-469a-b546-6bcb66dee6f5#supplementary-material
	public double ptb_prob_born_32_39_weeks = 0.854;// https://www.thelancet.com/journals/lancet/article/PIIS0140-6736(23)00878-4/fulltext?uuid=uuid%3A1db73a17-556f-469a-b546-6bcb66dee6f5#supplementary-material
	
	// we want to split the categories of PTB to match the more standard definitions with moderate PTB 32–33 weeks and late PTB: 34–36 weeks, assume uniform distribution
	
	double ptb_prob_born_32_33_weeks = ptb_prob_born_32_39_weeks * (2.0 / 5.0);
	double ptb_prob_born_34_36_weeks = ptb_prob_born_32_39_weeks * (3.0 / 5.0);
	
	List<Double> ptbDistributionOfWeeksBornEarly = Arrays.asList(
		    ptb_prob_born_before_28_weeks,
		    ptb_prob_born_before_28_weeks + ptb_prob_born_28_32_weeks,
		    ptb_prob_born_before_28_weeks + ptb_prob_born_28_32_weeks + ptb_prob_born_32_33_weeks,
		    ptb_prob_born_before_28_weeks + ptb_prob_born_28_32_weeks + ptb_prob_born_32_33_weeks + ptb_prob_born_34_36_weeks
		);

	// PTB neonatal mortality (death in first 28 days) rates w.r.t born weeks early
	public double ptb_neonatal_mortality_prob_less_than_28_weeks = 0.2732; // https://pmc.ncbi.nlm.nih.gov/articles/PMC12678064/?utm_source=chatgpt.com#bjo17506-sec-0015
	public double ptb_neonatal_mortality_prob_28_to_31_weeks = 0.0324; // https://pmc.ncbi.nlm.nih.gov/articles/PMC12678064/?utm_source=chatgpt.com#bjo17506-sec-0015
	public double ptb_neonatal_mortality_prob_32_to_33_weeks = 0.0136; // https://pmc.ncbi.nlm.nih.gov/articles/PMC12678064/?utm_source=chatgpt.com#bjo17506-sec-0015
	public double ptb_neonatal_mortality_prob_34_to_36_weeks = 0.043; // https://pmc.ncbi.nlm.nih.gov/articles/PMC12678064/?utm_source=chatgpt.com#bjo17506-sec-0015

	
	enum MortalitySteps {
		DEATH,
		NO_DEATH
	}
	
	enum BirthSteps {
		INITIALISED_PREGNANT,
		BIRTH,
		PREGNANCY,
		SCHEDULE_PREGNANCY,
		NO_PREGNANCY
	}
	public Demography(WorldBankCovid19Sim myWorld) {
		this.myWorld = myWorld;
		load_all_cause_mortality_params(myWorld.params.dataDir + myWorld.params.all_cause_mortality_filename);
		load_all_birthrate_params(myWorld.params.dataDir + myWorld.params.birth_rate_filename);
	}
	
	public class Aging implements Steppable {

		Person target;
		int ticksUntilNextBirthday = 365;
		
		public Aging(Person p, WorldBankCovid19Sim myWorld) {
			this.target = p;
			this.ticksUntilNextBirthday = myWorld.params.ticks_per_year;
		}
		
		@Override
		public void step(SimState arg0) {
			target.updateAge();
			arg0.schedule.scheduleOnce(arg0.schedule.getTime() + ticksUntilNextBirthday, this);
		}
		
	}
	
	public class Mortality implements Steppable{
		
		Person target;
		int ticksUntilNextMortalityCheck = 0;
		int tickToCauseMortality = Integer.MAX_VALUE;
		WorldBankCovid19Sim world;
		public Mortality( Person p, WorldBankCovid19Sim myWorld ) {
			this.target = p;
			this.ticksUntilNextMortalityCheck = myWorld.params.ticks_per_year;
			this.world = myWorld;
		} 
		// steps taken
		@Override
		public void step(SimState arg0) {
			// This step performs the determining/causing mortality. Each year, a person will have a chance to have a date of
			// death selected. Initially everyone goes through the determineMortality function. Which creates the date of death/reschedules
			// itself for the start of next year if no date is selected.
			if(this.target.isAlive()) {
			if (this.tickToCauseMortality < Integer.MAX_VALUE) {
				causeDeath();
			}
			else {
				
				determineMortality(arg0);
			}		
			}
		}
		// functions used
		private void causeDeath() {
			if (target.isAlive()) {
				target.die("<default>");
				}
		}

		private void determineMortality(SimState arg0) {
			double myMortalityLikelihood = 0.0;
			// do switch operation on the person's sex to determine likelihood of mortality
			switch (target.getSex()) {
			// ------------------------------------------------------------------------------------------------------------
			// get probability of dying this year if male
			case MALE: {
				myMortalityLikelihood = world.params.getLikelihoodByAge(
					prob_death_by_age_male, all_cause_death_age_params, target.getAge());
				break;
			}
			// ------------------------------------------------------------------------------------------------------------
			// get probability of dying this year if female
			case FEMALE: {
				myMortalityLikelihood = world.params.getLikelihoodByAge(
						prob_death_by_age_female, all_cause_death_age_params, target.getAge());	
				break;
			}
			// ------------------------------------------------------------------------------------------------------------
			default: System.out.println("Sex/age based mortality not found");
			}
				
			// check if this person is going to die in the next year. Set up default choice here.
			MortalitySteps nextStep = MortalitySteps.NO_DEATH;
			
			if (arg0.random.nextDouble() <= myMortalityLikelihood){
				nextStep = MortalitySteps.DEATH;
				}
			// act on next step
			switch (nextStep) {
			// ------------------------------------------------------------------------------------------------------------
			case DEATH:{
				// choose a day to die this year, then schedule this death to take place
				this.tickToCauseMortality = arg0.random.nextInt(365) * world.params.ticks_per_day;
				arg0.schedule.scheduleOnce(arg0.schedule.getTime() + this.tickToCauseMortality, this);
				break;
			}
			// ------------------------------------------------------------------------------------------------------------
			case NO_DEATH:{
				// reschedule this whole mortality deciding process to begin next year.
				arg0.schedule.scheduleOnce(arg0.schedule.getTime() + this.ticksUntilNextMortalityCheck, this);
				break;
			}
			// ------------------------------------------------------------------------------------------------------------
			default: {System.out.println("Mortality not determined");
			}
			}
			
		}
		
	}
	
	public class Births implements Steppable{
		
		Person target;
		int ticksUntilNextBirthCheck = 0;
		int tickToCauseBirth = Integer.MAX_VALUE;
		int ticksToUpdatePregnancy = Integer.MAX_VALUE;
		int daysToRescheduleNextBirth = Integer.MAX_VALUE;
		boolean ptb = false;
		boolean multiplePregnancy = false;
		boolean shortBirthInterval = false;
		boolean initialSetUp = true;
		double gestationalAge = 0;
		WorldBankCovid19Sim world;
		public Births( Person p, WorldBankCovid19Sim myWorld ) {
			this.target = p;
			this.ticksUntilNextBirthCheck = myWorld.params.ticks_per_month;
			this.world = myWorld;
		} 
		@Override
		public void step(SimState arg0) {
			
			determineGivingBirth(arg0, target, initialSetUp);
		}
		
		private void postBirthRescheduling(SimState arg0, boolean isAlive) {
			if (isAlive) {
				// reset tickToCauseBirth so this pathway can be used again
				this.tickToCauseBirth = Integer.MAX_VALUE;
				this.ticksToUpdatePregnancy = Integer.MAX_VALUE;
				// reset the preterm birth characteristic
				this.ptb = false;
				// reschedule the check to occur next year
				int currentTime = (int) arg0.schedule.getTime();
				int currentDay = (int) currentTime / world.params.ticks_per_day;
				// draw the birth interval randomly using a log-normal distribution
				double mu = Math.log(median_birth_interval);
				double sigma = (Math.log(q3_birth_interval) - Math.log(q1_birth_interval)) / 1.349;
				double months_between_births = Math.exp(mu + sigma * arg0.random.nextGaussian());
				// if months between births is greater than nine, minus nine months from it to represent the pregnancy duration
				if (months_between_births > 9) months_between_births -= 9;
				int days_between_births = (int) months_between_births * 30;
				// offset by days between births plus one day in case the model randomly generates a near immediate potential birth interval
				this.ticksUntilNextBirthCheck = (days_between_births + 1 + currentDay) * world.params.ticks_per_day;
				arg0.schedule.scheduleOnce((days_between_births + 1 + currentDay) * world.params.ticks_per_day, this);
			}
		}
		private void determineGivingBirth(SimState myWorld, Person target, Boolean initialSetUp) {
			// We first need to determine if this person will give birth this year. As some people will have become pregnant before the start of the simulation, we will need to
			// update pregnancy properties when births are scheduled in the first nine months of simulation time
			boolean isAlive = target.isAlive();
			int currentTime = (int) myWorld.schedule.getTime();
			int currentDay = (int) myWorld.schedule.getTime() / world.params.ticks_per_day;
			
			
			// handle the first time this is set up here (we want to start with some people being pregnant, therefore we need to set up the pregnancies that would
			// have happened before the simulation started.
			if (isAlive) {
				if (initialSetUp) {
					
					// assuming that in the last nine months on average 9/12 of the population would be a year younger we may need to adjust this person's age
					// to accurately represent their likelihood of being pregnant in the last 9 months
					boolean needToAdjustAge = world.random.nextDouble() < (9 / 12);
					int ageForCheck = target.getAge();
					if ((needToAdjustAge) && (target.getAge() > 1)) {
						ageForCheck -= 1;
					}
					double myPregnancyLikelihood = world.params.getLikelihoodByAge(
							prob_birth_by_age, birth_age_params, ageForCheck);
					// increase this likelihood nine-fold to determine if they got pregnant in the last nine months
					double prob_not_pregnant = Math.pow(1 - myPregnancyLikelihood, 9);
					double adjustedPregnancyLikelihood = 1 - prob_not_pregnant;
					BirthSteps nextStep = BirthSteps.NO_PREGNANCY;
					if (myWorld.random.nextDouble() <= adjustedPregnancyLikelihood) {
						// this person is pregnant
						nextStep = BirthSteps.INITIALISED_PREGNANT;
					}
					// If they aren't initialised as pregnant do a check again to see if they will be born at some point in the tenth month
					if ((nextStep != BirthSteps.INITIALISED_PREGNANT) && (myWorld.random.nextDouble() <= myPregnancyLikelihood)) {
						nextStep = BirthSteps.SCHEDULE_PREGNANCY;
					}
					
					// In the initial set up, we will have some women who will be pregnant before the simulation starts who will need to start the 
					// simulation pregnant. We will also have some women in the first month (before this event runs again in month 2) become pregnant
					
					switch (nextStep) {
					case INITIALISED_PREGNANT:{
						// Here we schedule pregnancy for women in the simulation who would be pregnant in the first months
						target.setPregnant(true);
						// Some of these will be twins
						multiplePregnancy = (myWorld.random.nextDouble() < prob_multiple_pregnancy);

						this.tickToCauseBirth = (determine_pregnancy_duration_in_days(this) + currentDay) * world.params.ticks_per_day;
//						System.out.println("First nine months scheduled to give birth on " + (currentDay + dayToCauseBirth));
						// schedule this to rerun on the birth date
						myWorld.schedule.scheduleOnce(this.tickToCauseBirth, this);	
						break;
						
					}
					// ------------------------------------------------------------------------------------------------------------
					case SCHEDULE_PREGNANCY:{
						// schedule day for the pregnancy
						int dayToCausePregnancy = myWorld.random.nextInt(30);
						// determine in this pregnancy will be twins (assume only twins)
						multiplePregnancy = (myWorld.random.nextDouble() < prob_multiple_pregnancy);
						// check if prior pregnancy has occurred and if it is too short of a duration
						shortBirthInterval = (currentDay + dayToCausePregnancy - target.getDateGaveBirth() < 24 * 30);

						// create a corresponding start of pregnancy
						this.ticksToUpdatePregnancy = (currentDay + dayToCausePregnancy) * world.params.ticks_per_day;
//						System.out.println("Starting pregnancy on " + (currentDay + dayToCausePregnancy));

						// schedule this event again on the day to cause pregnancy
						myWorld.schedule.scheduleOnce(currentTime + this.ticksToUpdatePregnancy, this);
						break;
					}
					// ------------------------------------------------------------------------------------------------------------
					case NO_PREGNANCY:{
						// schedule a check for birth next month
						myWorld.schedule.scheduleOnce(myWorld.schedule.getTime() + this.ticksUntilNextBirthCheck, this);
						this.ticksUntilNextBirthCheck += world.params.ticks_per_month;
						break;
						}
					// ------------------------------------------------------------------------------------------------------------
					default: {
						System.out.println("Giving birth not determined");
						}
					}
					
					// after initial set up is done, make sure this pathway isn't followed again
					this.initialSetUp = false;
					return;
				}
			
				// handle this months checks of starting pregnancy in this section
				double myPregnancyLikelihood = world.params.getLikelihoodByAge(
						prob_birth_by_age, birth_age_params, target.getAge());
				BirthSteps nextStep = BirthSteps.NO_PREGNANCY;
				if (myWorld.random.nextDouble() <= myPregnancyLikelihood) {
					nextStep = BirthSteps.SCHEDULE_PREGNANCY;
				}
				if (this.ticksToUpdatePregnancy < Integer.MAX_VALUE) {
					nextStep = BirthSteps.PREGNANCY;
				}
				if (this.tickToCauseBirth < Integer.MAX_VALUE) {
					nextStep = BirthSteps.BIRTH;
				}
				
			
				switch (nextStep) {
					case BIRTH:{
						// create a birth
						createBirth(myWorld, target.isAlive(), this.ptb, this.gestationalAge);
						// store this as a previous birth date
						target.addBirthDate(currentDay);
						// if they have twins, track it here
						if (this.multiplePregnancy) {
							createBirth(myWorld, target.isAlive(), this.ptb, this.gestationalAge);
						}
						postBirthRescheduling(myWorld, target.isAlive());
						break;
					}
					
					case PREGNANCY:{
						target.setPregnant(true);
						// set a date for the birth
						this.tickToCauseBirth = (determine_pregnancy_duration_in_days(this) + currentDay) * world.params.ticks_per_day;
						// schedule this to rerun on the birth date
						myWorld.schedule.scheduleOnce(this.tickToCauseBirth, this);	
						break;
					
					}
					// ------------------------------------------------------------------------------------------------------------
					case SCHEDULE_PREGNANCY:{
						// schedule day for the pregnancy
						int dayToCausePregnancy = myWorld.random.nextInt(30);
						multiplePregnancy = (myWorld.random.nextDouble() < prob_multiple_pregnancy);
						shortBirthInterval = (currentDay + dayToCausePregnancy - target.getDateGaveBirth() < 24 * 30);

						this.ticksToUpdatePregnancy = (currentDay + dayToCausePregnancy) * world.params.ticks_per_day;
//						System.out.println("Starting pregnancy on " + (currentDay + dayToCausePregnancy));

						// schedule this event again on the day to cause pregnancy
						myWorld.schedule.scheduleOnce(currentTime + this.ticksToUpdatePregnancy, this);
						break;
								
					}
					// ------------------------------------------------------------------------------------------------------------
					case NO_PREGNANCY:{
						// schedule a check for birth next month
						myWorld.schedule.scheduleOnce(this.ticksUntilNextBirthCheck, this);
						this.ticksUntilNextBirthCheck += world.params.ticks_per_month;
						break;
						}
					// ------------------------------------------------------------------------------------------------------------
					default: {
						System.out.println("Giving birth not determined");
						}
				}
			}
		}
		
		private void createBirth(SimState arg0, boolean isAlive, boolean isPreTerm, double weeksEarly) {
			if (isAlive) {
				int time = (int) (arg0.schedule.getTime() / world.params.ticks_per_day);
//				System.out.println(target.getID() + " giving birth on " + (time));

				target.gaveBirth(time);
				// create attributed for the newborn, id, age, sex, occupation status (lol), their 
				// household (assume this is the mothers), where the baby is, that it's not going to school
				// and a copy of the simulation, then create the person
				int new_id = world.agents.size() + 1;
				int baby_age = 0;
				// although we use an enum for biological sex, upon creation of a person a string is passed to choose sex. This is because
				List<SEX> sexList = Arrays.asList(SEX.MALE, SEX.FEMALE);
				OCCUPATION babiesJob = OCCUPATION.UNEMPLOYED;
				SEX sexAssigned = sexList.get(world.random.nextInt(sexList.size()));
				Household babyHousehold = target.getHouseholdAsType();
				Workplace babyWorkplace = null;
				boolean babySchooling = false;
				int birthday = time;
				Person baby = new Person(new_id, // ID 
						baby_age, // age
						birthday, // date of birth
						sexAssigned, // sex
						babiesJob, // lower case all of the job titles
						babySchooling,
						babyHousehold, // household
						babyWorkplace,
						world
						);				
				// update the household and location to include the baby
				babyHousehold.addHost(baby);
				// the baby has decided to go home
				baby.setActivityNode(world.movementFramework.getEntryPoint());
				// store the baby in the newBirths array
				world.agents.add(baby);
				// Add the person to the admin zone
				baby.transferTo((Household) babyHousehold);
				// This is a new birth that hasn't been recorded
				target.removeBirthLogged();
				// call on vertical transmission functions for any infections
				for (Disease d: target.getDiseaseSet().values()) {
					d.verticalTransmission(baby);
				}
				// if baby is born pre-term, set this property
				if (this.ptb) {
					baby.setBornPreTerm(true);
					target.setPriorPreTerm(isPreTerm);
					baby.setGestationalAge(weeksEarly);
				}
			}
		// reset if they are pregnant or not
		target.setPregnant(false);
		}
		
	}
	public ArrayList<Integer> getAll_cause_death_age_params() {
		return all_cause_death_age_params;
	}

	public int determine_pregnancy_duration_in_days(Births BirthChecker) {
		int birthdate = 0;
		if (BirthChecker.initialSetUp) {
			// first step, those pregnant before the start of the simulation, some will be nine months pregnant up to nine months before the start of simulation
			// draw a number randomly for births in the first nine months
			birthdate = myWorld.random.nextInt(9 * 30);
			}
		else {
			birthdate = 39 * 7; // full term
			}
		// get the baseline odds for ptb
		double baseline_odds = Math.log(ptb_base_rate / (1 - ptb_base_rate));
		double logit = baseline_odds;
		// adjust the odds of ptb with respect to risk factors
		if (BirthChecker.target.getAge() < 20) logit += Math.log(ptb_AOR_age_less_than_20_years);
		if (BirthChecker.shortBirthInterval) logit += Math.log(ptb_AOR_short_birth_interval);
		if (BirthChecker.target.hasPriorPreTerm()) logit += Math.log(ptb_AOR_previous_ptb);
		if (BirthChecker.target.hasDietary_iron_deficiency()) logit += Math.log(ptb_AOR_anemia);
		if (BirthChecker.target.getBMIStatus().equals(BMIStatus.UNDERWEIGHT)) logit += Math.log(ptb_AOR_underweight); // TODO create bmi status prevalence
		if (BirthChecker.target.getDiseaseSet().containsKey(DISEASE.HIV.key)) logit += Math.log(ptb_AOR_hiv);
		if (BirthChecker.target.getDiseaseSet().containsKey("MALARIA")) logit += Math.log(ptb_AOR_malaria);
		if (BirthChecker.multiplePregnancy) logit += Math.log(ptb_AOR_multiple_pregnancy);
		// convert logit to probability
		double prob_ptb = 1.0 / (1.0 + Math.exp(-logit));
		// check if this person will give birth pre term
		BirthChecker.ptb = BirthChecker.world.random.nextDouble() < prob_ptb;
		
		if (BirthChecker.ptb) {
			double gestational_age = determine_gestational_age(BirthChecker);
			// set this for further use
			BirthChecker.gestationalAge = gestational_age;
			// calculate the difference between full term and the number of weeks they will be bron
			double difference = 39 - gestational_age;
			// reduce their birthdate (set to full term by default) by the difference
			birthdate -= difference * 7;
		}
		// Finally, if this is the inital set up births and the birth is scheduled before the start of the sim, 
		// just set the birth date to 0
		if (birthdate < 0) birthdate = 0;
		
		return birthdate;
	}

	
	private double determine_gestational_age(Births BirthChecker) {
		// generate random number for decision on how early
	    double rand = BirthChecker.world.random.nextDouble();
	    // set up variable
	    int category = 0;
	    // determine how early PTB will be
	    for (double p : ptbDistributionOfWeeksBornEarly) {
	        if (rand < p) {
	            break;
	        }
	        category++;
	    }

	    double gestationalAge;
	    // generate the number of weeks the baby will be born at using nextDouble times the weeks in the category to act as a 
	    // uniforn distribution with aid of mapping doubles to integers
	    switch (category) {
	        case 0: // Extremely preterm: 22–27 weeks
	            gestationalAge = 22 + BirthChecker.world.random.nextDouble() * 6;
	            break;

	        case 1: // Very preterm: 28–31 weeks
	            gestationalAge = 28 + BirthChecker.world.random.nextDouble() * 4;
	            break;

	        case 2: // Moderate preterm: 32–33 weeks
	            gestationalAge = 32 + BirthChecker.world.random.nextDouble() * 2;
	            break;

	        default: // Late preterm: 34–36 weeks
	            gestationalAge = 34 + BirthChecker.world.random.nextDouble() * 3;
	            break;
	    }
	    // return 
	    return gestationalAge;
	}
	

	public void setAll_cause_death_age_params(ArrayList<Integer> all_cause_death_age_params) {
		this.all_cause_death_age_params = all_cause_death_age_params;
	}

	public ArrayList<Double> getProb_death_by_age_male() {
		return prob_death_by_age_male;
	}

	public void setProb_death_by_age_male(ArrayList<Double> prob_death_by_age_male) {
		this.prob_death_by_age_male = prob_death_by_age_male;
	}

	public ArrayList<Double> getProb_death_by_age_female() {
		return prob_death_by_age_female;
	}

	public void setProb_death_by_age_female(ArrayList<Double> prob_death_by_age_female) {
		this.prob_death_by_age_female = prob_death_by_age_female;
	}

	public ArrayList<Integer> getBirth_age_params() {
		return birth_age_params;
	}

	public void setBirth_age_params(ArrayList<Integer> birth_age_params) {
		this.birth_age_params = birth_age_params;
	}

	public ArrayList<Double> getProb_birth_by_age() {
		return prob_birth_by_age;
	}

	public void setProb_birth_by_age(ArrayList<Double> prob_birth_by_age) {
		this.prob_birth_by_age = prob_birth_by_age;
	}
	public void load_all_cause_mortality_params(String filename) {
		try {
			
			if(myWorld.params.verbose)
				System.out.println("Reading in all cause mortality data from " + filename);
			
			// Open the tracts file
			FileInputStream fstream = new FileInputStream(filename);

			// Convert our input stream to a BufferedReader
			BufferedReader lineListDataFile = new BufferedReader(new InputStreamReader(fstream));
			String s;

			// extract the header
			s = lineListDataFile.readLine();

			// map the header into column names relative to location
			String [] header = myWorld.params.splitRawCSVString(s);
			HashMap <String, Integer> columnNames = myWorld.params.parseHeader(header);
			
			// set up data container
			
			all_cause_death_age_params = new ArrayList<Integer> ();
			prob_death_by_age_male = new ArrayList <Double> ();
			prob_death_by_age_female = new ArrayList <Double> ();

			
			// read in the raw data
			while ((s = lineListDataFile.readLine()) != null) {
				String [] bits = myWorld.params.splitRawCSVString(s);
				
				// assemble the age data
				String [] ageRange = bits[0].split("-");
				int maxAge = Integer.MAX_VALUE;
				if(ageRange.length > 1){
					maxAge = Integer.parseInt(ageRange[1]); // take the maximum
				}
				all_cause_death_age_params.add(maxAge);
				
				double male_prob_death  = Double.parseDouble(bits[1]),
						female_prob_death = Double.parseDouble(bits[2]);
				
				// store the values
				prob_death_by_age_male.add(male_prob_death);
				prob_death_by_age_female.add(female_prob_death);

			}
			lineListDataFile.close();
			} catch (Exception e) {
		        throw new IllegalArgumentException("File input error: " + filename);

			}
	}
	
	public void load_all_birthrate_params(String filename) {
		try {
			
			if(myWorld.params.verbose)
				System.out.println("Reading in birth rate data from " + filename);
			
			// Open the tracts file
			FileInputStream fstream = new FileInputStream(filename);

			// Convert our input stream to a BufferedReader
			BufferedReader lineListDataFile = new BufferedReader(new InputStreamReader(fstream));
			String s;

			// extract the header
			s = lineListDataFile.readLine();

			// map the header into column names relative to location
			String [] header = myWorld.params.splitRawCSVString(s);
			HashMap <String, Integer> columnNames = myWorld.params.parseHeader(header);
			
			// set up data container
			
			birth_age_params = new ArrayList<Integer> ();
			prob_birth_by_age = new ArrayList <Double> ();

			
			// read in the raw data
			while ((s = lineListDataFile.readLine()) != null) {
				String [] bits = myWorld.params.splitRawCSVString(s);
				
				// assemble the age data
				String [] ageRange = bits[0].split("-");
				int maxAge = Integer.MAX_VALUE;
				if(ageRange.length > 1){
					maxAge = Integer.parseInt(ageRange[1]); // take the maximum
				}
				birth_age_params.add(maxAge);
				
				double female_prob_birth = Double.parseDouble(bits[1]);
				
				// store the values
				prob_birth_by_age.add(female_prob_birth);

			}
			lineListDataFile.close();
			} catch (Exception e) {
		        throw new IllegalArgumentException("File input error: " + filename);

			}
	}
	
	public double getPtb_base_rate() {
		return ptb_base_rate;
	}

	public void setPtb_base_rate(double ptb_base_rate) {
		this.ptb_base_rate = ptb_base_rate;
	}
	
	public double getProb_multiple_pregnancy() {
		return prob_multiple_pregnancy;
	}

	public void setProb_multiple_pregnancy(double prob_multiple_pregnancy) {
		this.prob_multiple_pregnancy = prob_multiple_pregnancy;
	}
	public double getPtb_AOR_age_less_than_20_years() {
		return ptb_AOR_age_less_than_20_years;
	}

	public void setPtb_AOR_age_less_than_20_years(double ptb_AOR_age_less_than_20_years) {
		this.ptb_AOR_age_less_than_20_years = ptb_AOR_age_less_than_20_years;
	}

	public double getPtb_AOR_short_birth_interval() {
		return ptb_AOR_short_birth_interval;
	}

	public void setPtb_AOR_short_birth_interval(double ptb_AOR_short_birth_interval) {
		this.ptb_AOR_short_birth_interval = ptb_AOR_short_birth_interval;
	}

	public double getPtb_AOR_previous_ptb() {
		return ptb_AOR_previous_ptb;
	}

	public void setPtb_AOR_previous_ptb(double ptb_AOR_previous_ptb) {
		this.ptb_AOR_previous_ptb = ptb_AOR_previous_ptb;
	}

	public double getPtb_AOR_anemia() {
		return ptb_AOR_anemia;
	}

	public void setPtb_AOR_anemia(double ptb_AOR_anemia) {
		this.ptb_AOR_anemia = ptb_AOR_anemia;
	}

	public double getPtb_AOR_hiv() {
		return ptb_AOR_hiv;
	}

	public void setPtb_AOR_hiv(double ptb_AOR_hiv) {
		this.ptb_AOR_hiv = ptb_AOR_hiv;
	}

	public double getPtb_AOR_malaria() {
		return ptb_AOR_malaria;
	}

	public void setPtb_AOR_malaria(double ptb_AOR_malaria) {
		this.ptb_AOR_malaria = ptb_AOR_malaria;
	}

	public double getPtb_AOR_multiple_pregnancy() {
		return ptb_AOR_multiple_pregnancy;
	}

	public void setPtb_AOR_multiple_pregnancy(double ptb_AOR_multiple_pregnancy) {
		this.ptb_AOR_multiple_pregnancy = ptb_AOR_multiple_pregnancy;
	}
	
	public double getPtb_AOR_underweight() {
		return ptb_AOR_underweight;
	}

	public void setPtb_AOR_underweight(double ptb_AOR_underweight) {
		this.ptb_AOR_underweight = ptb_AOR_underweight;
	}
	
	public double getMedian_birth_interval() {
		return median_birth_interval;
	}

	public void setMedian_birth_interval(double median_birth_interval) {
		this.median_birth_interval = median_birth_interval;
	}
	
}