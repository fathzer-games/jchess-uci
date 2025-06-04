package com.fathzer.jchess.uci.parameters;

/** The parameters of the <i>test</i> UCI command.
 * <br>Parameters are a superset of the <i>perft</i> command ones (see {@link PerfTParameters}).
 * <br>Additional parameters:
 * <ul>
 * <li><b>cut</b>: The maximum time (in seconds) to spend on the test. By default there is no time limit</li>
 * </ul>
 */
public class PerfStatsParameters extends PerfTParameters {
	/** A {@link Parser} for the <i>test</i> command. */
	public static final Parser<PerfStatsParameters> PARSER;
	
	static {
		PARSER = new PerfTLikeParser<>();
		PARSER.add(new ParameterDefinition<>((p,tok) -> p.cutTime=Parser.positiveInt(tok), "cut"));
	}

	private int cutTime = Integer.MAX_VALUE;
	
	/** Gets the <i>cutTime</i> option.
	 * @return Integer.MAX_VALUE if the option is not set
	 */
	public int getCutTime() {
		return cutTime;
	}
}
