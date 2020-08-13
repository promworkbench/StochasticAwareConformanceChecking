package org.processmining.stochasticawareconformancechecking.cli;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.RelativeEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticPetriNet2StochasticDeterministicFiniteAutomaton2;
import org.processmining.stochasticawareconformancechecking.plugins.StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin;
import org.processmining.xeslite.plugin.OpenLogFileLiteImplPlugin;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class CLI {
	private static final String version = "ProM 6.10";

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		Options options;

		options = new Options();

		// auxiliary
		Option helpOption = Option.builder("h").longOpt("help").numberOfArgs(0).required(false)
				.desc("print help message").hasArg(false).build();
		Option versionOption = Option.builder("v").longOpt("version").numberOfArgs(0).required(false)
				.desc("get version of this tool").hasArg(false).build();
		Option logOption = Option.builder("l").longOpt("log").numberOfArgs(1).required(true)
				.desc("the path to the event log").hasArg(true).build();
		Option modelOption = Option.builder("m").longOpt("model").numberOfArgs(1).required(true)
				.desc("the path to the event log").hasArg(true).build();

		// create groups
		OptionGroup cmdGroup = new OptionGroup();
		cmdGroup.addOption(helpOption);
		cmdGroup.addOption(versionOption);
		options.addOptionGroup(cmdGroup);

		options.addOption(logOption);
		options.addOption(modelOption);

		// parse the command line arguments
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h") || cmd.getOptions().length == 0) { // handle help
			showHelp(options);
			return;
		} else if (cmd.hasOption("v")) { // handle version
			System.out.println(CLI.version);
			return;
		}

		PluginContext context = new FakeContext();

		//read log
		XLog log;
		if (!cmd.hasOption("l")) {
			System.out.println("Please provide a log file.");
			return;
		} else {
			File file = new File(cmd.getOptionValue("l"));
			if (file.isDirectory() || !file.canRead() || !file.isFile()) {
				System.out.println("Cannot read log file.");
				return;
			}
			log = (XLog) new OpenLogFileLiteImplPlugin().importFile(context, file);
		}

		//read model
		StochasticNet model;
		if (!cmd.hasOption("m")) {
			System.out.println("Please provide a model file.");
			return;
		} else {
			File file = new File(cmd.getOptionValue("m"));
			if (file.isDirectory() || !file.canRead() || !file.isFile()) {
				System.out.println("Cannot read model file.");
				return;
			}
			Serializer serializer = new Persister();
			PNMLRoot pnml = serializer.read(PNMLRoot.class, file);

			StochasticNetDeserializer converter = new StochasticNetDeserializer();
			Object[] objs = converter.convertToNet(context, pnml, file.getName(), true);
			model = (StochasticNet) objs[0];
		}

		StochasticDeterministicFiniteAutomatonMapped automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, MiningParameters.getDefaultClassifier(), new ProMCanceller() {
					public boolean isCancelled() {
						return context.getProgress().isCancelled();
					}
				});

		Marking initialMarking = StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin
				.guessInitialMarking(model);
		StochasticDeterministicFiniteAutomatonMapped automatonB = StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
				.convert(model, initialMarking);
		final Pair<Double, Double> p = RelativeEntropy.relativeEntropyHalf(automatonA, automatonB);

		System.out.println("stochastic recall: " + p.getA());
		System.out.println("stochastic precision: " + p.getB());
	}

	private static void showHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		showHeader();
		formatter.printHelp(120, String.format("java -jar jbpt-pm.jar <options>", CLI.version),
				String.format("", CLI.version), options,
				"================================================================================\n");
	}

	private static void showHeader() {
		System.out.println(String.format(
				"================================================================================\n"
						+ "Tool to compute quality measures for Process Mining and Process Querying ver. %s.\n"
						+ "For support, please contact us at jbpt.project@gmail.com.\n"
						+ "================================================================================\n"
						+ "PNML format:	http://www.pnml.org/\n" + "XES format:	https://xes-standard.org/\n"
						+ "================================================================================\n",
				CLI.version));

	}
}
