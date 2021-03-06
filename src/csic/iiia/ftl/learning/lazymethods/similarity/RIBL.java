/**
 * Copyright (c) 2013, Santiago Ontañón All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * the IIIA-CSIC nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
  
 package csic.iiia.ftl.learning.lazymethods.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import java.util.Collection;

import csic.iiia.ftl.base.bridges.Clause;
import csic.iiia.ftl.base.bridges.HornClauses;
import csic.iiia.ftl.base.core.FTKBase;
import csic.iiia.ftl.base.core.FTRefinement;
import csic.iiia.ftl.base.core.FeatureTerm;
import csic.iiia.ftl.base.core.FloatFeatureTerm;
import csic.iiia.ftl.base.core.IntegerFeatureTerm;
import csic.iiia.ftl.base.core.Ontology;
import csic.iiia.ftl.base.core.SetFeatureTerm;
import csic.iiia.ftl.base.core.Sort;
import csic.iiia.ftl.base.core.Symbol;
import csic.iiia.ftl.base.core.TermFeatureTerm;
import csic.iiia.ftl.base.utils.FeatureTermException;
import csic.iiia.ftl.base.utils.Pair;

// TODO: Auto-generated Javadoc
/**
 * The Class RIBL.
 */
public class RIBL extends Distance {

	/** The m_numeric feature ranges. */
	HashMap<String, Pair<Double, Double>> m_numericFeatureRanges = new HashMap<String, Pair<Double, Double>>();

	/**
	 * @return the m_numericFeatureRanges
	 */
	public HashMap<String, Pair<Double, Double>> getM_numericFeatureRanges() {
		return m_numericFeatureRanges;
	}

	/** The max_depth. */
	int max_depth = 3;

	/** The debug. */
	boolean debug = false;

	/**
	 * Instantiates a new rIBL.
	 * 
	 * @param cases
	 *            the cases
	 * @param md
	 *            the md
	 * @throws FeatureTermException
	 *             the feature term exception
	 */
	public RIBL(Collection<FeatureTerm> cases, int md) throws FeatureTermException {
		max_depth = md;
		for (FeatureTerm c : cases) {
			List<FeatureTerm> v = FTRefinement.variables(c);

			for (FeatureTerm ft : v) {
				if (ft instanceof TermFeatureTerm) {
					Sort s = ft.getSort();
					for (Symbol feature : s.getFeatures()) {
						String name = feature.get();
						FeatureTerm value = ft.featureValue(feature);
						Double dvalue = null;

						if (value instanceof IntegerFeatureTerm) {
							Integer ivalue = ((IntegerFeatureTerm) value).getValue();
							if (ivalue != null) {
								dvalue = (double) ((int) ivalue);
							}
						} else if (value instanceof FloatFeatureTerm) {
							Float fvalue = ((FloatFeatureTerm) value).getValue();
							if (fvalue != null) {
								dvalue = (double) ((float) fvalue);
							}
						}

						if (dvalue != null) {
							Pair<Double, Double> range = m_numericFeatureRanges.get(name);
							if (range == null) {
								range = new Pair<Double, Double>(dvalue, dvalue);
								m_numericFeatureRanges.put(name, range);
							} else {
								range.mA = Math.min(range.mA, dvalue);
								range.mB = Math.max(range.mB, dvalue);
							}
						}
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see csic.iiia.ftl.learning.lazymethods.similarity.Distance#distance(csic.iiia.ftl.base.core.FeatureTerm,
	 * csic.iiia.ftl.base.core.FeatureTerm, csic.iiia.ftl.base.core.Ontology, csic.iiia.ftl.base.core.FTKBase)
	 */
	public double distance(FeatureTerm f1, FeatureTerm f2, Ontology o, FTKBase dm) throws FeatureTermException {

		List<Clause> clauses_f1 = HornClauses.toClauses(f1, dm, "X");
		List<Clause> clauses_f2 = HornClauses.toClauses(f2, dm, "X");

		// System.out.println("Clauses for f1:\n" + f1.toStringNOOS(dm));
		// for(Clause c:clauses_f1) System.out.println(c);
		// System.out.println("Clauses for f2:\n" + f2.toStringNOOS(dm));
		// for(Clause c:clauses_f2) System.out.println(c);

		// System.out.println("RIBL.distance start");

		Clause clause_f1 = clauses_f1.get(0);
		Clause clause_f2 = clauses_f2.get(0);
		List<Pair<Integer, List<Clause>>> descriptors_f1 = buildDescriptors(clauses_f1, max_depth);
		List<Pair<Integer, List<Clause>>> descriptors_f2 = buildDescriptors(clauses_f2, max_depth);

		HashMap<String, List<Pair<Clause, Integer>>> cache_f1 = new HashMap<String, List<Pair<Clause, Integer>>>();
		HashMap<String, List<Pair<Clause, Integer>>> cache_f2 = new HashMap<String, List<Pair<Clause, Integer>>>();

		if (debug) {
			System.out.println("Descriptor of f1:");
			for (Pair<Integer, List<Clause>> d : descriptors_f1) {
				System.out.print("Depth " + d.mA + " < ");
				for (Clause c : d.mB) {
					System.out.print(c + " ");
				}
				System.out.println(">");
			}
			System.out.println("Descriptor of f2:");
			for (Pair<Integer, List<Clause>> d : descriptors_f2) {
				System.out.print("Depth " + d.mA + " < ");
				for (Clause c : d.mB) {
					System.out.print(c + " ");
				}
				System.out.println(">");
			}
		}

		double distance = 1.0 - SimE(clause_f1, clause_f2, descriptors_f1, descriptors_f2, cache_f1, cache_f2, 0);

		if (debug) {
			System.out.println("RIBL distance: " + distance);
		}

		return distance;
	}

	/**
	 * Sim e.
	 * 
	 * @param clause_f1
	 *            the clause_f1
	 * @param clause_f2
	 *            the clause_f2
	 * @param descriptors_f1
	 *            the descriptors_f1
	 * @param descriptors_f2
	 *            the descriptors_f2
	 * @param cache_f1
	 *            the cache_f1
	 * @param cache_f2
	 *            the cache_f2
	 * @param depth
	 *            the depth
	 * @return the double
	 */
	private double SimE(Clause clause_f1, Clause clause_f2, List<Pair<Integer, List<Clause>>> descriptors_f1, List<Pair<Integer, List<Clause>>> descriptors_f2,
			HashMap<String, List<Pair<Clause, Integer>>> cache_f1, HashMap<String, List<Pair<Clause, Integer>>> cache_f2, int depth) {

		double similarity = 0.0;

		for (int i = 0; i < clause_f1.getNumberParameters(); i++) {
			switch (clause_f1.getParameterType(i)) {
			case Clause.TYPE_ID:
				similarity += SimA_ID(clause_f1.getParameterValue(i), clause_f2.getParameterValue(i), descriptors_f1, descriptors_f2, cache_f1, cache_f2, depth);
				break;
			case Clause.TYPE_SYMBOL:
				similarity += SimA_Symbol(clause_f1.getParameterValue(i), clause_f2.getParameterValue(i));
				break;
			case Clause.TYPE_INTEGER:
				similarity += SimA_Integer(clause_f1.getHead(), clause_f1.getParameterValue(i), clause_f2.getParameterValue(i));
				break;
			case Clause.TYPE_FLOAT:
				similarity += SimA_Float(clause_f1.getHead(), clause_f1.getParameterValue(i), clause_f2.getParameterValue(i));
				break;
			}
		}

		if (debug) {
			System.out.println("RIBL SimE: " + similarity + " / " + clause_f1.getNumberParameters());
		}

		similarity /= clause_f1.getNumberParameters();

		return similarity;
	}

	/**
	 * Sim a_ id.
	 * 
	 * @param parameterValue1
	 *            the parameter value1
	 * @param parameterValue2
	 *            the parameter value2
	 * @param descriptors_f1
	 *            the descriptors_f1
	 * @param descriptors_f2
	 *            the descriptors_f2
	 * @param cache_f1
	 *            the cache_f1
	 * @param cache_f2
	 *            the cache_f2
	 * @param depth
	 *            the depth
	 * @return the double
	 */
	private double SimA_ID(String parameterValue1, String parameterValue2, List<Pair<Integer, List<Clause>>> descriptors_f1,
			List<Pair<Integer, List<Clause>>> descriptors_f2, HashMap<String, List<Pair<Clause, Integer>>> cache_f1,
			HashMap<String, List<Pair<Clause, Integer>>> cache_f2, int depth) {

		List<Pair<Clause, Integer>> L_f1 = findClausesOfDepth(parameterValue1, depth, descriptors_f1, cache_f1);
		List<Pair<Clause, Integer>> L_f2 = findClausesOfDepth(parameterValue2, depth, descriptors_f2, cache_f2);

		double similarity = 0.0;
		int card = Math.max(L_f1.size(), L_f2.size());

		if (card == 0) {
			if (parameterValue1.equals(parameterValue2))
				return 1.0;
			return 0;
		}

		for (Pair<Clause, Integer> p : L_f1) {
			for (Pair<Clause, Integer> p2 : L_f2) {
				if (p.mA.getHead().equals(p2.mA.getHead()) && p.mB.equals(p2.mB)) {

					List<Clause> L1 = new LinkedList<Clause>();
					List<Clause> L2 = new LinkedList<Clause>();

					for (Pair<Clause, Integer> tmp_p1 : L_f1) {
						if (tmp_p1.mA.getHead().equals(p.mA.getHead()) && tmp_p1.mB.equals(p.mB)) {
							L1.add(tmp_p1.mA);
						}
					}
					for (Pair<Clause, Integer> tmp_p2 : L_f2) {
						if (tmp_p2.mA.getHead().equals(p.mA.getHead()) && tmp_p2.mB.equals(p.mB)) {
							L2.add(tmp_p2.mA);
						}
					}

					double s = SimLS(parameterValue1, parameterValue2, depth, p.mA.getHead(), p.mB, L1, L2, descriptors_f1, descriptors_f2, cache_f1,
							cache_f2);
					similarity += s;
					break;
				}
			}
		}

		if (card != 0) {
			similarity /= card;
		}

		if (similarity > 1 || similarity < 0) {
			System.err.println("SimA_ID: " + similarity + " between " + parameterValue1 + " and " + parameterValue2);
		}

		return similarity;
	}

	/**
	 * Sim ls.
	 * 
	 * @param A
	 *            the a
	 * @param B
	 *            the b
	 * @param depth
	 *            the depth
	 * @param P
	 *            the p
	 * @param pos
	 *            the pos
	 * @param LA
	 *            the lA
	 * @param LB
	 *            the lB
	 * @param descriptors_f1
	 *            the descriptors_f1
	 * @param descriptors_f2
	 *            the descriptors_f2
	 * @param cache_f1
	 *            the cache_f1
	 * @param cache_f2
	 *            the cache_f2
	 * @return the double
	 */
	private double SimLS(String A, String B, int depth, String P, Integer pos, List<Clause> LA, List<Clause> LB,
			List<Pair<Integer, List<Clause>>> descriptors_f1, List<Pair<Integer, List<Clause>>> descriptors_f2,
			HashMap<String, List<Pair<Clause, Integer>>> cache_f1, HashMap<String, List<Pair<Clause, Integer>>> cache_f2) {
		double similarity = 0.0;

		if (LA.size() < LB.size()) {
			for (Clause Lx : LA) {
				boolean first = true;
				double max = 1.0;
				for (Clause Ly : LB) {
					double sim = SimL(A, B, depth, P, pos, Lx, Ly, descriptors_f1, descriptors_f2, cache_f1, cache_f2);
					if (first || sim > max) {
						first = false;
						max = sim;
					}
				}
				similarity += max;
			}
			if (LB.size() > 0)
				similarity /= LB.size();
		} else {
			for (Clause Lx : LB) {
				boolean first = true;
				double max = 1.0;
				for (Clause Ly : LA) {
					double sim = SimL(A, B, depth, P, pos, Ly, Lx, descriptors_f1, descriptors_f2, cache_f1, cache_f2);
					if (first || sim > max) {
						first = false;
						max = sim;
					}
				}
				similarity += max;
			}

			if (LA.size() > 0)
				similarity /= LA.size();
		}

		if (debug) {
			System.out.println("RIBL SimLS: (" + A + "," + B + ") " + similarity);
		}

		return similarity;
	}

	/**
	 * Sim l.
	 * 
	 * @param O1
	 *            the o1
	 * @param O2
	 *            the o2
	 * @param depth
	 *            the depth
	 * @param p
	 *            the p
	 * @param pos
	 *            the pos
	 * @param P1
	 *            the p1
	 * @param P2
	 *            the p2
	 * @param descriptors_f1
	 *            the descriptors_f1
	 * @param descriptors_f2
	 *            the descriptors_f2
	 * @param cache_f1
	 *            the cache_f1
	 * @param cache_f2
	 *            the cache_f2
	 * @return the double
	 */
	private double SimL(String O1, String O2, int depth, String p, Integer pos, Clause P1, Clause P2, List<Pair<Integer, List<Clause>>> descriptors_f1,
			List<Pair<Integer, List<Clause>>> descriptors_f2, HashMap<String, List<Pair<Clause, Integer>>> cache_f1,
			HashMap<String, List<Pair<Clause, Integer>>> cache_f2) {
		double similarity = 0.0;
		int count = 0;

		for (int i = 0; i < P1.getNumberParameters(); i++) {
			String Ai = P1.getParameterValue(i);
			String Bi = P2.getParameterValue(i);

			if (P1.getParameterType(i) != Clause.TYPE_ID || !Ai.equals(O1) || P2.getParameterType(i) != Clause.TYPE_ID || !Bi.equals(O2)) {

				// System.out.println(Ai + " - " + Bi + " -> " + P1.getParameterType(i));

				if (depth <= max_depth) {
					switch (P1.getParameterType(i)) {
					case Clause.TYPE_ID:
						similarity += SimA_ID(Ai, Bi, descriptors_f1, descriptors_f2, cache_f1, cache_f2, depth + 1);
						break;
					case Clause.TYPE_SYMBOL:
						similarity += SimA_Symbol(Ai, Bi);
						break;
					case Clause.TYPE_INTEGER:
						similarity += SimA_Integer(p, Ai, Bi);
						break;
					case Clause.TYPE_FLOAT:
						similarity += SimA_Float(p, Ai, Bi);
						break;
					}

				} else {
					System.err.println("MAX DEPTH reached!!!!!");
					// ...
				}
				count++;
			}
		}

		if (debug) {
			System.out.println("RIBL SimL: (" + O1 + "," + O2 + ") " + " (" + P1 + "," + P2 + ") " + similarity + " / " + count);
		}

		if (count != 0) {
			similarity /= count;
		} else {
			return 1;
		}

		return similarity;
	}

	/**
	 * Find clauses of depth.
	 * 
	 * @param parameterValue1
	 *            the parameter value1
	 * @param depth
	 *            the depth
	 * @param clauses_f1
	 *            the clauses_f1
	 * @param cache_f1
	 *            the cache_f1
	 * @return the list
	 */
	private List<Pair<Clause, Integer>> findClausesOfDepth(String parameterValue1, int depth, List<Pair<Integer, List<Clause>>> clauses_f1,
			HashMap<String, List<Pair<Clause, Integer>>> cache_f1) {

		String code = depth + "," + parameterValue1;
		List<Pair<Clause, Integer>> l = cache_f1.get(code);

		if (l != null)
			return l;

		l = new LinkedList<Pair<Clause, Integer>>();
		cache_f1.put(code, l);

		for (Pair<Integer, List<Clause>> p : clauses_f1) {
			if (p.mA == depth) {
				for (Clause c : p.mB) {
					int len = c.getNumberParameters();
					for (int i = 0; i < len; i++) {
						if (c.getParameterType(i) == Clause.TYPE_ID && c.getParameterValue(i).equals(parameterValue1)) {
							l.add(new Pair<Clause, Integer>(c, i));
						}
					}
				}
			}
		}

		return l;
	}

	/**
	 * Sim a_ symbol.
	 * 
	 * @param parameterValue
	 *            the parameter value
	 * @param parameterValue2
	 *            the parameter value2
	 * @return the double
	 */
	private double SimA_Symbol(String parameterValue, String parameterValue2) {
		if (parameterValue.equals(parameterValue2)) {
			return 1.0;
		}
		return 0.0;
	}

	/**
	 * Sim a_ integer.
	 * 
	 * @param rangeName
	 *            the range name
	 * @param parameterValue
	 *            the parameter value
	 * @param parameterValue2
	 *            the parameter value2
	 * @return the double
	 */
	private double SimA_Integer(String rangeName, String parameterValue, String parameterValue2) {
		Pair<Double, Double> range = m_numericFeatureRanges.get(rangeName);

		if (range == null) {
			if (parameterValue.equals(parameterValue2)) {
				return 1.0;
			}
			return 0.0;
		} else {
			double v1 = Integer.parseInt(parameterValue);
			double v2 = Integer.parseInt(parameterValue2);
			if (range.mA == range.mA)
				return 1.0;

			return 1.0 - (Math.abs(v1 - v2)) / (range.mB - range.mA);
		}
	}

	/**
	 * Sim a_ float.
	 * 
	 * @param rangeName
	 *            the range name
	 * @param parameterValue
	 *            the parameter value
	 * @param parameterValue2
	 *            the parameter value2
	 * @return the double
	 */
	private double SimA_Float(String rangeName, String parameterValue, String parameterValue2) {
		Pair<Double, Double> range = m_numericFeatureRanges.get(rangeName);

		if (debug)
			System.out.println("SimA_Float: " + parameterValue + " - " + parameterValue2 + " in " + range);

		if (range == null) {
			if (parameterValue.equals(parameterValue2)) {
				return 1.0;
			}
			return 0.0;
		} else {
			double v1 = Double.parseDouble(parameterValue);
			double v2 = Double.parseDouble(parameterValue2);
			if (range.mA == range.mB)
				return 1.0;

			double r = 1.0 - (Math.abs(v1 - v2)) / (range.mB - range.mA);
			if (debug)
				System.out.println("SimA_Float: " + r);
			return r;
		}
	}

	/**
	 * Builds the descriptors.
	 * 
	 * @param clauses_f
	 *            the clauses_f
	 * @param max_depth2
	 *            the max_depth2
	 * @return the list
	 */
	private List<Pair<Integer, List<Clause>>> buildDescriptors(List<Clause> clauses_f, int max_depth2) {
		List<Pair<Integer, List<Clause>>> descriptors = new LinkedList<Pair<Integer, List<Clause>>>();
		Clause clause_f = clauses_f.get(0);
		HashSet<Clause> alreadyAdded = new HashSet<Clause>();

		alreadyAdded.add(clause_f);

		for (int i = 0; i < max_depth; i++) {
			List<Clause> newClauses = new LinkedList<Clause>();

			for (Clause c : alreadyAdded) {
				for (int p = 0; p < c.getNumberParameters(); p++) {
					if (c.getParameterType(p) == Clause.TYPE_ID) {
						for (Clause c2 : clauses_f) {
							if (!alreadyAdded.contains(c2) && !newClauses.contains(c2)) {
								for (int p2 = 0; p2 < c2.getNumberParameters(); p2++) {
									if (c2.getParameterType(p2) == Clause.TYPE_ID && c2.getParameterValue(p2).equals(c.getParameterValue(p))) {
										newClauses.add(c2);
									}
								}
							}
						}
					}
				}
			}

			if (newClauses.size() > 0) {
				descriptors.add(new Pair<Integer, List<Clause>>(i, newClauses));
				alreadyAdded.addAll(newClauses);

				// System.out.println("At level " + i + " added " + newClauses.size() + " clauses");
			}
		}

		return descriptors;
	}

}
