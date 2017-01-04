/*
 * This file is part of JGAP.
 *
 * JGAP offers a dual license model containing the LGPL as well as the MPL.
 *
 * For licensing information please see the file license.txt included with JGAP
 * or have a look at the top of class org.jgap.Chromosome which representatively
 * includes the JGAP license policy applicable for any file delivered with JGAP.
 */
package grant.wu.ga;

import grant.wu.util.PrintUtil;

import java.util.ArrayDeque;
import java.util.Date;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.IGeneConstraintChecker;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.WeightedRouletteSelector;

public class DcEnergy {
  /** String containing the CVS revision. Read out via reflection!*/
  private final  String CVS_REVISION = "$Revision: 1.10 $";

  /**
   * The total number of times we'll let the population evolve.
   */
  private  final int MAX_ALLOWED_EVOLUTIONS = 400*100;
  
  private double rouletteCrossoverRate=0.5;
  private int mutationRate = 10;
  
   private Date startTime = new Date();
   
   private boolean bPrintAssignment = false;
  
   DcEnergyFitnessFunction myFunc = null;
   
   int popuSize = 0;

	public void optimizeEnergyNetwork(int scale, int capacityIndex)
			throws Exception {
		startTime = new Date();
		// Start with a DefaultConfiguration, which comes setup with the
		// most common settings.
		// -------------------------------------------------------------
		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		conf.setPreservFittestIndividual(true);
		conf.setKeepPopulationSizeConstant(true);
		// Set the fitness function we want to use, which is our
		// MinimizingMakeChangeFitnessFunction. We construct it with
		// the target amount of change passed in to this method.
		// ---------------------------------------------------------
		myFunc = new DcEnergyFitnessFunction(scale, capacityIndex);
		conf.setFitnessFunction(myFunc);
		conf.setAlwaysCaculateFitness(false);
		// conf.setBulkFitnessFunction(new BulkFitnessOffsetRemover(myFunc));
		// Now we need to tell the Configuration object how we want our
		// Chromosomes to be setup. We do that by actually creating a
		// sample Chromosome and then setting it on the Configuration
		// object. As mentioned earlier, we want our Chromosomes to each
		// have four genes, one for each of the coin types. We want the
		// values (alleles) of those genes to be integers, which represent
		// how many coins of that type we have. We therefore use the
		// IntegerGene class to represent each of the genes. That class
		// also lets us specify a lower and upper bound, which we set
		// to sensible values for each coin type.
		// --------------------------------------------------------------
		Gene[] sampleGenes = new Gene[scale];

		// Initialize energys of Gene's. Each Gene represents a coin with a
		// specific value, and each coin with different value has a specific
		// weight. Not necessarily a higher weight for higher coin values!
		// (as in real life!).
		Date beforeFirstFit = new Date();
		@SuppressWarnings("-access")		
		int vmAssign[] = myFunc.firstFit();
		println("ffd time:"+ ((new Date()).getTime() - beforeFirstFit.getTime())/1000.0);

		for (int i = 0; i < vmAssign.length; i++) {
			IntegerGene gene = new IntegerGene(conf, 0, scale / capacityIndex
					- 1);
			gene.setConstraintChecker(new EnergyGeneConstraintChecker());
			sampleGenes[i] = gene; // Quarters
			sampleGenes[i].setEnergy(1.0d);
			gene.setAllele(new Integer(vmAssign[i]));
		}

		IChromosome sampleChromosome = new Chromosome(conf, sampleGenes);
		conf.setSampleChromosome(sampleChromosome);
		conf.getGeneticOperators().clear();
		
		conf.addGeneticOperator(new WeightedRouletteCrossoverOperator(conf, rouletteCrossoverRate));
		
		conf.addGeneticOperator(new MutationOperator(conf, mutationRate ));

		
		WeightedRouletteSelector selector = new WeightedRouletteSelector(conf);		

		conf.addNaturalSelector(selector, false);

		

		// Finally, we need to tell the Configuration object how many
		// Chromosomes we want in our population. The more Chromosomes,
		// the larger number of potential solutions (which is good for
		// finding the answer), but the longer it will take to evolve
		// the population (which could be seen as bad).
		// ------------------------------------------------------------
		popuSize = 200;//scale / capacityIndex;
		conf.setPopulationSize(popuSize);
		
		queue = new ArrayDeque<Double>(popuSize);
		
		conf.setSelectFromPrevGen(0.5);
		// Create random initial population of Chromosomes.
		// ------------------------------------------------
		Genotype population = Genotype.randomInitialGenotype(conf);

		// Evolve the population. Since we don't know what the best answer
		// is going to be, we just evolve the max number of times.
		// ---------------------------------------------------------------
		int i = 0;
		for (i = 0; i < MAX_ALLOWED_EVOLUTIONS * scale / capacityIndex; i++) {

			population.evolve();
			
			bPrintAssignment = (i==0)?true:false;
			
			if ( i % 50 == 1) 
			{				
				printSolution(population, i);
			}
			if (evolutionOver(population, i))
				break;
		}
		// Display the best solution we found.
		// -----------------------------------
		bPrintAssignment = true;
		printSolution(population, i);
	}
  
	private ArrayDeque<Double> queue = null;
	private double fitnessSum = 0;
	private double fitnessBest = 0;
	private boolean evolutionOver(Genotype population, int generation) {
		boolean over = false;
		Double ftail = population.getFittestChromosome().getFitnessValue();
		if (ftail>fitnessBest) fitnessBest = ftail;
		fitnessSum += ftail;
		queue.push(ftail);
		int length = (popuSize > 200? popuSize : 200)/1;
		if (generation > length) {
			Double f0 = queue.removeLast();
			fitnessSum -= f0;
			double fitnessAvg = fitnessSum/(length+1);
			if ( ( fitnessBest - fitnessAvg)/fitnessBest <= 0.000000001)
				over = true;
			else if (generation >1000*1000)
				over = true;
			else {
				Date now = new Date();
				long duration = now.getTime() - startTime.getTime();
				if (duration > 3600*1000) 
					over = true;
			}
		}
		return over;
	}
	
	private void printSolution(Genotype population, int generation) {
		IChromosome bestSolutionSoFar = population.getFittestChromosome();
		Date now = new Date();
		long duration = now.getTime() - startTime.getTime();
		if (bPrintAssignment){
			println("ticking time: " + String.format("%.1f", duration / 1000.0)
					+ " The best solution has a fitness value of "
					+ bestSolutionSoFar.getFitnessValue());
			println("It contains the following: ");
		}

		println((bPrintAssignment?myFunc.printResult(bestSolutionSoFar) + "\n":"")				
				+ "time:"
				+ String.format("%.1f", duration / 1000.0)
				+ " generation:"
				+ generation
				+ String.format(" fitness value: %.4f",
						bestSolutionSoFar.getFitnessValue()) + " total energy:"
				+ myFunc.getTotalWeightStr(bestSolutionSoFar) + "\n");
	}

	public void println(String s) {
		PrintUtil.print(s);
	}
	
	public void setLogFolder(String path){
		PrintUtil.setLogFolder(path);
	}

	
	public static void main(String[] args) throws Exception {		
		for (int j =30; j < 31; j++) {
			int scales[] = { 100,200,300,400,500};
			for (int i = 0; i < scales.length; i++) {
				PrintUtil.setLogName(scales[i] + "-" + j);
				DcEnergy dc = new DcEnergy();
				if (args.length != 2) {
					int scale = scales[i];
					int capacityIndex = 5;
					dc.println(new Date()
							+ String.format(
									" start\nSyntax: DcEnergy <scale=%d> <capacityIndex=%d>",
									scale, capacityIndex));
					dc.optimizeEnergyNetwork(scale, capacityIndex);
				} else {
					int amount = dc.getValue(args, 0);
					int weight = dc.getValue(args, 1);
					dc.optimizeEnergyNetwork(amount, weight);
				}
				dc.println(new Date() + "End");
			}
		}
	}

	protected int getValue(String[] args, int index) {
		int value;
		try {
			value = Integer.parseInt(args[index]);
			return value;
		} catch (NumberFormatException e) {
			println("The " + (index + 1)
					+ ". argument must be a valid integer value");
			System.exit(1);
			return -1; // does not matter
		}
	}

	public class EnergyGeneConstraintChecker implements IGeneConstraintChecker {
	
		public boolean verify(Gene a_gene, final Object a_alleleValue,
				final IChromosome a_chrom, final int a_geneIndex)
				throws RuntimeException {
				
			return true;
		}
	}
	
	  public  void setRouletteCrossoverRate(double v){
		  rouletteCrossoverRate=v;
	  }
	  public void  setMutationRate(int v){
		  mutationRate = v;
	  }
}
