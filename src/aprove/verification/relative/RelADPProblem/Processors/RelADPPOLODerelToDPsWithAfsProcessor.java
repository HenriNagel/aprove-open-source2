package aprove.verification.relative.RelADPProblem.Processors;

import java.util.*;
import java.util.Map.*;

import aprove.prooftree.Export.Utility.*;
import aprove.solver.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.Annotations.*;
import aprove.verification.dpframework.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.dpframework.DPProblem.*;
import aprove.verification.dpframework.Orders.*;
import aprove.verification.dpframework.Orders.Solvers.*;
import aprove.verification.dpframework.Orders.Utility.POLO.*;
import aprove.verification.dpframework.TRSProblem.*;
import aprove.verification.dpframework.TRSProblem.Processors.FromITRS.*;
import aprove.verification.oldframework.Algebra.Polynomials.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Logic.*;
import aprove.verification.oldframework.PropositionalLogic.*;
import aprove.verification.oldframework.PropositionalLogic.Formulae.*;
import aprove.verification.oldframework.PropositionalLogic.TheoryPropositions.*;
import aprove.verification.oldframework.Utility.*;
import aprove.verification.oldframework.Utility.GenericStructures.*;
import aprove.verification.oldframework.Utility.Graph.*;
import aprove.verification.relative.RelADPProblem.*;
import aprove.verification.relative.RelADPProblem.Processors.RelADPRPOSDerelToDPsWithAfsProcessor.*;
import aprove.verification.relative.RelADPProblem.Processors.RelADPReductionPairProcessor.*;
import immutables.*;

/**
 * Derelatifying Processor as described in [IJCAR24] but in addition we direclty
 * apply a clever argument filtering on the resulting DP problem.
 * 
 * @author Jan-Christoph Kassing
 * @version $Id$
 */
public class RelADPPOLODerelToDPsWithAfsProcessor extends RelADPProblemProcessor {

    // ================================================================================
    // Properties
    // ================================================================================

    private final SolverFactory order;

    // ================================================================================
    // Constructors and Creators
    // ================================================================================

    @ParamsViaArgumentObject
    public RelADPPOLODerelToDPsWithAfsProcessor(final Arguments arguments) {
        this.order = arguments.order;
    }

    @Override
    public boolean isRelADPPApplicable(RelADPProblem reladpp) {
        return reladpp.isNonRelative();
    }

    @Override
    protected Result processRelADPProblem(RelADPProblem origreladpp, Abortion aborter) throws AbortionException {

        //Find a weakly monotonic Polo order that orients at least one ADP with annotations strictly.
        final POLO order = this.findOrdering(origreladpp, false, aborter);

        if (order == null) {
            return ResultFactory.unsuccessful("Could not find a suitable poly interpretation");
        }

        /* find weak monotonic arguments */
        Set<FunctionSymbol> signature = new HashSet<>(origreladpp.getSignature());
        CollectionMap<FunctionSymbol, Integer> removedPositions = new CollectionMap<FunctionSymbol, Integer>();
        for (FunctionSymbol sym : signature) {
            Set<Integer> positionSet = new HashSet<>();
            for (int i = 0; i < sym.getArity(); i++) {
                if (!order.getInterpretation().isMonotonicIn(sym, i)) {
                    positionSet.add(i);
                }
            }
            removedPositions.add(sym, positionSet);
        }

        Set<Rule> depPairs = new HashSet<Rule>();
        for (Rule adp : origreladpp.getPAbs()) {
            TRSFunctionApplication lhs = adp.getLeft();
            lhs = lhs.renameAtMap(Position.EPSILON, origreladpp.getAnnotator());

            for (TRSFunctionApplication subterm : adp.getRight().subAnnoTerms(origreladpp.getDeannotator())) {
                depPairs.add(Rule.create(lhs, subterm));
            }
        }

        Pair<Pair<Set<Rule>, Set<Rule>>, Map<FunctionSymbol, FunctionSymbol>> pair;
        try {
            pair = this.getResultingRules(depPairs, origreladpp.getQ().getR(), removedPositions, signature);
        } catch (IllegalArgumentFilterException e) {
            return ResultFactory.unsuccessful();
        }

        QTRSProblem rWithQ = QTRSProblem.create(ImmutableCreator.create(pair.x.y));

        QDPProblem qdpp = QDPProblem.create(pair.x.x, rWithQ, false);

        RelADPCleverAfsProof RPPproof = new RelADPCleverAfsProof(order,
            origreladpp,
            removedPositions);

        return ResultFactory.proved(qdpp, YNMImplication.SOUND, RPPproof);

    }

    private Set<Rule> getKindaUsableRules(RelADPProblem pqdp, Set<Rule> depPairs) {
        // Add all usable rules from the DPs...
        Set<Rule> Rhelp = pqdp.getQ().getQUsableRulesCalculator().getUsableRules(depPairs);
        // ... And add all usable rules from duplicating rules

        Graph<Rule, ?> g;
        int n = pqdp.getQ().getR().size();
        Set<Node<Rule>> nodes = new LinkedHashSet<Node<Rule>>(n);
        for (Rule rule : pqdp.getQ().getR()) {
            nodes.add(new Node<Rule>(rule));
        }
        g = new Graph<Rule, Object>(nodes);

        // iterate through all possible edges
        Node<Rule>[] nodeArr = new Node[n];
        nodeArr = nodes.toArray(nodeArr);

        // first do crude approximation on root symbols
        // (only check nodes in sccs in more detail!)
        for (int i = 0; i < n; i++) {
            Node<Rule> fromDP = nodeArr[i];
            Rule fromDPRule = fromDP.getObject();
            for (int j = i + 1; j < n; j++) {
                Node<Rule> toDP = nodeArr[j];
                Rule toDPRule = toDP.getObject();
                // standard direction
                if (this.calculateFastConnection(fromDPRule, toDPRule)) {
                    g.addEdge(fromDP, toDP);
                }
                // reverse direction
                if (this.calculateFastConnection(toDPRule, fromDPRule)) {
                    g.addEdge(toDP, fromDP);
                }
            }
            // and self-cycle
            if (this.calculateFastConnection(fromDPRule, fromDPRule)) {
                g.addEdge(fromDP, fromDP);
            }
        }
        Set<Node<Rule>> duplicatingRules = new HashSet();
        for (Rule rule : pqdp.getQ().getR()) {
            if (rule.isDuplicating())
                duplicatingRules.add(g.getNodeFromObject(rule));
        }
        for (Node<Rule> dupnode : g.determineReachableNodes(duplicatingRules)) {
            Rhelp.add(dupnode.getObject());
        }
        return Rhelp;
    }

    private boolean calculateFastConnection(Rule from, Rule to) {
        Set<TRSTerm> tSet = from.getRight().getSubTerms();
        for (TRSTerm t : tSet) {
            if (t instanceof TRSFunctionApplication tfun) {
                final FunctionSymbol f = tfun.getRootSymbol();
                final FunctionSymbol g = to.getRootSymbol();
                if (f.equals(g))
                    return true;
            }
        }
        return false;
    }

    /**
     * The SMT-Solving part of the processor.
     * This includes the creation of an interpretation, the formula about diophantine constraints
     * and the search for a satisfying interpretation via some solver.
     * 
     * @param pqdpProblem - the original PQDPProblem problem
     * @param allstrict - boolean whether we want to strict all of the rules strictly
     * @param aborter - Aborter to check the timer
     * @return the satisfying polynomial interpretation
     * @throws AbortionException
     */
    private POLO findOrdering(final RelADPProblem pqdpProblem, final boolean allstrict, final Abortion aborter)
        throws AbortionException {

        POLOSolver solver = this.order.getPOLOSolver(pqdpProblem.getSignature(), aborter);
        solver.setAllowWeakMonotonicity(true);

        final Formula<Diophantine> fml = this.createFormula(pqdpProblem, solver.getInterpretation(), aborter);

        return solver.solveDioFormula(fml, aborter);
    }

    /**
     * Create the formula that we need to satisfy
     * (All expected values are non increasing, at least one element in the support of the rhs is strictly decreasing)
     * 
     * @param pqdpProblem - the original PQDPProblem problem
     * @param interpretation - the polynomial interpretation with variables as coefficients
     * @param aborter - Aborter to check the timer
     * @return The complete formula for the probabilistic RPP
     */
    private Formula<Diophantine> createFormula(
        final RelADPProblem pqdpProblem,
        Interpretation interpretation,
        final Abortion aborter) {
        final FormulaFactory<Diophantine> ff = NonCountingCircuitFactory.create(SplitMode.FLATTEN, SplitMode.LEFT_COMB); //TODO: Check these parameters

        Set<Rule> depPairs = new HashSet<Rule>();
        for (Rule adp : pqdpProblem.getPAbs()) {
            TRSFunctionApplication lhs = adp.getLeft();
            lhs = lhs.renameAtMap(Position.EPSILON, pqdpProblem.getAnnotator());

            for (TRSFunctionApplication subterm : adp.getRight().subAnnoTerms(pqdpProblem.getDeannotator())) {
                depPairs.add(Rule.create(lhs, subterm));
            }
        }

        Set<Rule> R = getKindaUsableRules(pqdpProblem, depPairs);

        // Create the Formula of Constraints that we want to solve:
        // 1) for all l->r in R: flat(l) >= flat(r)
        List<Formula<Diophantine>> nonHashedConstraintList = new ArrayList<>(R.size());  // strict upper bound
        for (Rule rule : R) {
            nonHashedConstraintList.add(createRuleFormulaExpectation(ff, rule, interpretation, aborter));
        }

        // 2) for all l->r in PAbs u PRel: depterm(l) >= depterm(t)
        List<Formula<Diophantine>> nonStrictConstraintList = new ArrayList<>(depPairs.size());
        for (Rule rule : depPairs) {
            nonStrictConstraintList.add(createADPFormulaNonStrict(ff, rule, interpretation, aborter, pqdpProblem));
        }

        // 3) for all l->r in PAbs u PRel: depterm(l) > tepterm(t) (if posible)
        List<Formula<Diophantine>> strictConstraintList = new ArrayList<>(depPairs.size());
        for (Rule rule : depPairs) {
            strictConstraintList.add(createADPFormulaStrict(ff, rule, interpretation, aborter, pqdpProblem));
        }

        Formula<Diophantine> nonHashedConstraintFormula = ff.buildAnd(nonHashedConstraintList);
        Formula<Diophantine> nonStrictConstraintFormula;
        Formula<Diophantine> strictConstraintFormula;

        strictConstraintFormula = ff.buildOr(strictConstraintList);
        nonStrictConstraintFormula = ff.buildAnd(nonStrictConstraintList);

        List<Formula<Diophantine>> finalConstraintsList = new ArrayList<>();
        finalConstraintsList.add(nonHashedConstraintFormula);
        finalConstraintsList.add(strictConstraintFormula);
        finalConstraintsList.add(nonStrictConstraintFormula);
        Formula<Diophantine> finalFormula = ff.buildAnd(finalConstraintsList);

        return finalFormula;
    }

    /**
     * @param ff - FormulaFactory for formulas about Diophantine constraints
     * @param rule - the probabilistic rewrite rule for this formula
     * @param interpretation - the polynomial interpretation with variables as coefficients
     * @param aborter - Aborter to check the timer
     * @return formula to encode "expectation non-increasing"
     */
    private Formula<Diophantine> createRuleFormulaExpectation(
        FormulaFactory<Diophantine> ff,
        Rule rule,
        Interpretation interpretation,
        Abortion aborter) {
        VarPolynomial rhsExpectedPoly = VarPolynomial.ZERO;

        TRSTerm term = rule.getRight();

        rhsExpectedPoly = rhsExpectedPoly.plus(interpretation.interpretTerm(term, aborter));

        VarPolynomial lhsPoly = (interpretation.interpretTerm(rule.getLeft(), aborter));
        VarPolynomial constraint = lhsPoly.minus(rhsExpectedPoly);
        Set<SimplePolyConstraint> simplePolyConstraintSet = new VarPolyConstraint(constraint, ConstraintType.GE)
            .createCoefficientConstraints();
        List<Formula<Diophantine>> ruleFormulaList = new ArrayList<>();
        for (SimplePolyConstraint spc : simplePolyConstraintSet) {
            Formula<Diophantine> helpFormula = ff.buildTheoryAtom(Diophantine.create(spc));
            ruleFormulaList.add(helpFormula);
        }
        return ff.buildAnd(ruleFormulaList);
    }

    /**
     * @param ff - FormulaFactory for formulas about Diophantine constraints
     * @param rule - the probabilistic dependency tuple for this formula
     * @param interpretation - the polynomial interpretation with variables as coefficients
     * @param aborter - Aborter to check the timer
     * @return formula to encode "expectation non-increasing"
     */
    private Formula<Diophantine> createADPFormulaNonStrict(
        FormulaFactory<Diophantine> ff,
        Rule rule,
        Interpretation interpretation,
        Abortion aborter,
        RelADPProblem problem) {
        TRSTerm lhs_anno = rule.getLeft().renameAtMap(Position.EPSILON, problem.getAnnotator());
        TRSTerm rhs = rule.getRight();

        VarPolynomial annoTermPoly = VarPolynomial.ZERO;

        for (TRSFunctionApplication subterm : rhs.subAnnoTerms(problem.getDeannotator())) {
            annoTermPoly = annoTermPoly.plus(interpretation.interpretTerm(subterm, aborter));
        }

        VarPolynomial lhsPoly = (interpretation.interpretTerm(lhs_anno, aborter));
        VarPolynomial constraint = lhsPoly.minus(annoTermPoly);
        Set<SimplePolyConstraint> simplePolyConstraintSet = new VarPolyConstraint(constraint, ConstraintType.GE)
            .createCoefficientConstraints();
        List<Formula<Diophantine>> ruleFormulaList = new ArrayList<>();
        for (SimplePolyConstraint spc : simplePolyConstraintSet) {
            Formula<Diophantine> helpFormula = ff.buildTheoryAtom(Diophantine.create(spc));
            ruleFormulaList.add(helpFormula);
        }
        return ff.buildAnd(ruleFormulaList);
    }

    /**
     * @param ff - FormulaFactory for formulas about Diophantine constraints
     * @param rule - the probabilistic rewrite rule for this formula
     * @param interpretation - the polynomial interpretation with variables as coefficients
     * @param aborter - Aborter to check the timer
     * @return formula to encode "one element in the support of rhs is strictly decreasing"
     */
    private Formula<Diophantine> createADPFormulaStrict(
        FormulaFactory<Diophantine> ff,
        Rule rule,
        Interpretation interpretation,
        Abortion aborter,
        RelADPProblem problem) {
        TRSTerm lhs_anno = rule.getLeft().renameAtMap(Position.EPSILON, problem.getAnnotator());
        TRSTerm rhs = rule.getRight();

        VarPolynomial annoTermPoly = VarPolynomial.ZERO;

        for (TRSFunctionApplication subterm : rhs.subAnnoTerms(problem.getDeannotator())) {
            annoTermPoly = annoTermPoly.plus(interpretation.interpretTerm(subterm, aborter));
        }

        VarPolynomial lhsPoly = (interpretation.interpretTerm(lhs_anno, aborter));
        VarPolynomial constraint = lhsPoly.minus(annoTermPoly);
        Set<SimplePolyConstraint> simplePolyConstraintSet = new VarPolyConstraint(constraint, ConstraintType.GT)
            .createCoefficientConstraints();
        List<Formula<Diophantine>> ruleFormulaList = new ArrayList<>();
        for (SimplePolyConstraint spc : simplePolyConstraintSet) {
            Formula<Diophantine> helpFormula = ff.buildTheoryAtom(Diophantine.create(spc));
            ruleFormulaList.add(helpFormula);
        }
        return ff.buildAnd(ruleFormulaList);
    }

    /**
     * Remove the given arguments of all terms, construct a new rule set and
     * return it. In addition, information about renamed symbols is returned.
     * @param rules the rule set
     * @param removedPositions information about arguments that can be removed
     * @param takenSymbols the function symbols that are already in use and may
     * not be used again
     * @return a result with a new set of rules, a map from old to new function symbols (with smaller arity)
     */
    private Pair<Pair<Set<Rule>, Set<Rule>>, Map<FunctionSymbol, FunctionSymbol>> getResultingRules(
        final Set<Rule> DPs,
        final Set<Rule> rulesRel,
        final CollectionMap<FunctionSymbol, Integer> removedPositions,
        final Collection<FunctionSymbol> takenSymbols)
        throws IllegalArgumentFilterException {

        final Set<Rule> newRules = new LinkedHashSet<>(DPs.size());

        // helper for name generation
        final Map<FunctionSymbol, FunctionSymbol> names = new LinkedHashMap<>();

        // for uninteresting symbols do not change the name
        final Collection<FunctionSymbol> symbols = new LinkedHashSet<>();
        for (final Rule rule : DPs) {
            symbols.addAll(rule.getLeft().getFunctionSymbols());
            symbols.addAll(rule.getRight().getFunctionSymbols());
        }
        for (final Rule rule : rulesRel) {
            symbols.addAll(rule.getLeft().getFunctionSymbols());
            symbols.addAll(rule.getRight().getFunctionSymbols());
        }
        symbols.removeAll(removedPositions.keySet());
        for (final FunctionSymbol fs : symbols) {
            final boolean added = takenSymbols.add(fs);
            assert (added);
            names.put(fs, fs);
        }

        for (final Rule rule : DPs) {
            final TRSFunctionApplication lhs = rule.getLeft();
            final TRSFunctionApplication newLhs =
                (TRSFunctionApplication) HelperClass.remove(lhs, removedPositions, names, takenSymbols);
            final TRSTerm rhs = rule.getRight();
            TRSTerm newRhs;
            if (!rhs.isVariable()) {
                newRhs = HelperClass.remove(rhs, removedPositions, names, takenSymbols);
            } else {
                newRhs = rhs;
            }
            if (!Rule.checkProperLandR(newLhs, newRhs)) {
                throw new IllegalArgumentFilterException();
            }
            final Rule newRule = Rule.create(newLhs, newRhs);
            newRules.add(newRule);
        }

        final Set<Rule> newRules2 = new LinkedHashSet<>(DPs.size());

        for (final Rule rule : rulesRel) {
            final TRSFunctionApplication lhs = rule.getLeft();
            final TRSFunctionApplication newLhs =
                (TRSFunctionApplication) HelperClass.remove(lhs, removedPositions, names, takenSymbols);
            final TRSTerm rhs = rule.getRight();
            TRSTerm newRhs;
            if (!rhs.isVariable()) {
                newRhs = HelperClass.remove(rhs, removedPositions, names, takenSymbols);
            } else {
                newRhs = rhs;
            }
            if (!Rule.checkProperLandR(newLhs, newRhs)) {
                throw new IllegalArgumentFilterException();
            }
            final Rule newRule = Rule.create(newLhs, newRhs);
            newRules2.add(newRule);
        }

        return new Pair<>(new Pair<>(newRules, newRules2), names);
    }

    public class IllegalArgumentFilterException extends RuntimeException {

    }

    // ================================================================================
    // Proof
    // ================================================================================

    public static class RelADPCleverAfsProof extends RelADPProof {

        private final ExportableOrder<TRSTerm> order;
        private final RelADPProblem origreladpp;
        private final CollectionMap<FunctionSymbol, Integer> filtering;

        RelADPCleverAfsProof(
            final ExportableOrder<TRSTerm> order,
            final RelADPProblem origreladpp,
            CollectionMap<FunctionSymbol, Integer> removedPositions) {
            this.order = order;
            this.origreladpp = origreladpp;
            this.filtering = removedPositions;
        }

        @Override
        public String export(final Export_Util o, final VerbosityLevel level) {
            StringBuilder result;
            result = new StringBuilder();
            result.append("We use the first derelatifying processor " + o.cite(Citation.IJCAR24) + ".");
            result.append(o.linebreak());
            result.append("There are no annotations in relative ADPs, so the relative ADP problem can be transformed into a non-relative DP problem.");

            result.append(o.paragraph());
            result.append("Furthermore, We use an argument filter " + o.cite(Citation.LPAR04) + ".");
            result.append(o.linebreak());
            result.append("Filtering:");
            result.append(filtering.toString());
            result.append(o.cond_linebreak());
            result.append("Found this filtering by looking at the following order that orders at least one DP strictly:");
            result.append(this.order.export(o));

            return result.toString();
        }
    }

    // ================================================================================
    // Arguments Class
    // ================================================================================

    public static class Arguments {

        public SolverFactory order;
    }

}
