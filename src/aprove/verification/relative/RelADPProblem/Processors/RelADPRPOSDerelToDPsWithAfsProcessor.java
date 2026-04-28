package aprove.verification.relative.RelADPProblem.Processors;

import java.util.*;
import java.util.Map.*;

import aprove.prooftree.Export.Utility.*;
import aprove.solver.*;
import aprove.solver.Engines.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.Annotations.*;
import aprove.verification.dpframework.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.dpframework.DPProblem.*;
import aprove.verification.dpframework.Orders.*;
import aprove.verification.dpframework.TRSProblem.*;
import aprove.verification.dpframework.TRSProblem.Processors.FromITRS.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Logic.*;
import aprove.verification.oldframework.Utility.*;
import aprove.verification.oldframework.Utility.GenericStructures.*;
import aprove.verification.oldframework.Utility.Graph.*;
import aprove.verification.relative.RelADPProblem.*;
import immutables.*;

/**
 * Derelatifying Processor as described in [IJCAR24] but in addition we direclty
 * apply a clever argument filtering on the resulting DP problem.
 * 
 * @author Jan-Christoph Kassing
 * @version $Id$
 */
public class RelADPRPOSDerelToDPsWithAfsProcessor extends RelADPProblemProcessor {

    // ================================================================================
    // Constructors and Creators
    // ================================================================================

    @Override
    public boolean isRelADPPApplicable(RelADPProblem reladpp) {
        return reladpp.isNonRelative();
    }

    @Override
    protected Result processRelADPProblem(RelADPProblem origreladpp, Abortion aborter) throws AbortionException {

        RPOSFactory.Arguments args = new RPOSFactory.Arguments();

        args.afsType = AFSType.FULLAFS;
        args.quasi = true;
        MINISATEngine.Arguments argsMini = new MINISATEngine.Arguments();
        argsMini.version = 2;
        argsMini.simp = false;
        args.engine = new MINISATEngine(argsMini);

        SolverFactory factory = new RPOSFactory(args);

        Set<Rule> depPairs = new HashSet<Rule>();
        for (Rule adp : origreladpp.getPAbs()) {
            TRSFunctionApplication lhs = adp.getLeft();
            lhs = lhs.renameAtMap(Position.EPSILON, origreladpp.getAnnotator());

            for (TRSFunctionApplication subterm : adp.getRight().subAnnoTerms(origreladpp.getDeannotator())) {
                depPairs.add(Rule.create(lhs, subterm));
            }
        }

        Set<Rule> R = ImmutableCreator.create(getKindaUsableRules(origreladpp, depPairs));

        final QActiveSolver solver = factory.getQActiveSolver();
        final Map<Rule, QActiveCondition> heuristicActive = new LinkedHashMap<Rule, QActiveCondition>();
        for (final Rule p : R) {
            heuristicActive.put(p, QActiveCondition.TRUE);
        }
        aborter.checkAbortion();
        final AfsOrder solvingOrder = (AfsOrder) solver.solveQActive(depPairs, heuristicActive, false, false, aborter);

        if (solvingOrder != null) {
            /* find weak monotonic arguments */
            Set<FunctionSymbol> signature = new HashSet<>(origreladpp.getSignature());
            CollectionMap<FunctionSymbol, Integer> removedPositions = new CollectionMap<FunctionSymbol, Integer>();
            for (FunctionSymbol sym : signature) {
                Set<Integer> positionSet = new HashSet<>();
                for (int i = 0; i < sym.getArity(); i++) {
                    if (solvingOrder.getAfs().getFunctionSymbols().contains(sym) && !solvingOrder.getAfs().getFiltering(sym).x[i].equals(YNM.YES)) {
                        positionSet.add(i);
                    }
                }
                removedPositions.add(sym, positionSet);
            }

            Pair<Pair<Set<Rule>, Set<Rule>>, Map<FunctionSymbol, FunctionSymbol>> pair = null;
            try {
                pair = this.getResultingRules(depPairs, origreladpp.getQ().getR(), removedPositions, signature);
            } catch (IllegalArgumentFilterException e) {
                return ResultFactory.unsuccessful();
            }

            QTRSProblem rWithQ = QTRSProblem.create(ImmutableCreator.create(pair.x.y));

            QDPProblem qdpp = QDPProblem.create(pair.x.x, rWithQ, false);

            RelADPCleverAfsProof RPPproof = new RelADPCleverAfsProof(solvingOrder,
                origreladpp,
                removedPositions);

            return ResultFactory.proved(qdpp, YNMImplication.SOUND, RPPproof);
        }
        return ResultFactory.unsuccessful();
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

}
