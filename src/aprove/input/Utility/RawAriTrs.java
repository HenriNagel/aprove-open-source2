package aprove.input.Utility;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.Map.*;

import org.apache.commons.math3.fraction.*;

import aprove.runtime.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Utility.GenericStructures.*;
import aprove.verification.probabilistic.BasicStructures.*;
import immutables.*;

public class RawAriTrs {
    
    //general
    private InputFormat inputFormat;

    private final Set<TRSVariable> variables = new LinkedHashSet<TRSVariable>();
    private final Set<FunctionSymbol> functionsymbols = new LinkedHashSet<FunctionSymbol>();

    private boolean complexity = false;
    private boolean constructorbased = false;
    private boolean innermost = false;
    private boolean outermost = false;
    private boolean parallelInnermost = false;
    
    private final Set<Rule> rules = new LinkedHashSet<Rule>();;
    private final Set<GeneralizedRule> allRules = new LinkedHashSet<GeneralizedRule>();
    private Map<String, Integer> arityMap = new LinkedHashMap<String, Integer>();

    //relative
    private Set<Rule> relativeRules = new LinkedHashSet<Rule>();
    private final Set<Pair<TRSTerm,TRSTerm>> lhsVariableRules = new LinkedHashSet<Pair<TRSTerm,TRSTerm>>();

    //equational
    private Set<FunctionSymbol> associativeNames = new LinkedHashSet<FunctionSymbol>();
    private Set<FunctionSymbol> commutativeNames = new LinkedHashSet<FunctionSymbol>();
    private Set<FunctionSymbol> associativeAndCommutativeNames = new LinkedHashSet<FunctionSymbol>();
    
    //context sensitive
    private HashMap<FunctionSymbol, Set<Integer>> replacementMap = new HashMap<FunctionSymbol, Set<Integer>>();
    
    //conditional
    private final Set<ConditionalRule> condrules = new LinkedHashSet<ConditionalRule>();
    
    //probabilistic
    private boolean termination = false;
    private boolean ast = false;
    private boolean past = false;
    private boolean sast = false;
    private boolean basic = false;
    private RewriteStrategy strat;
    private Set<ProbabilisticRule> probabilisticRules =  new HashSet<ProbabilisticRule>();

    //confluence
    private boolean confluence = false;
    private boolean infeasibility = false;
    private Set<Condition> infeasibilityQuery = new LinkedHashSet<Condition>();
    
    public void setInputFormat(InputFormat inputFormat) {
        this.inputFormat = inputFormat;
    }
    
    public InputFormat getInputFormat() {
        return inputFormat;
    }

    public Set<TRSVariable> getVariables() {
        return this.variables;
    }

    public boolean isVariable(String sym) {
        return this.variables.stream().map(x -> x.getName()).collect(toSet()).contains(sym);
    }

    public boolean isFunctionSymbol(String sym, int ar) {
        FunctionSymbol f = FunctionSymbol.create(sym, ar);
        boolean b = false;
        for(FunctionSymbol fold : this.functionsymbols) {
            b |= fold.equals(f);
        }
        return this.functionsymbols.stream().collect(toSet()).contains(f);
    }

    public Set<FunctionSymbol> getFunctionSymbols() {
        return this.functionsymbols;
    }

    public boolean isTermination() {
		return termination;
	}

	public boolean isAst() {
		return ast;
	}

	public boolean isPast() {
		return past;
	}

    public boolean isSast() {
        return sast;
    }
	
	public boolean isBasic() {
	    return basic;
	}

    public Set<ProbabilisticRule> getProbabilisticRules() {
        return this.probabilisticRules;
    }

    public boolean isInnermost() {
        return this.strat == RewriteStrategy.INNERMOST;
    }

    public boolean isOutermost() {
        return this.strat == RewriteStrategy.OUTERMOST;
    }
    
    public void addFunctionSymbol(FunctionSymbol f) {
        this.functionsymbols.add(f);
    }

    public void setTermination(boolean termination) {
		this.termination = termination;
	}

	public void setAst(boolean ast) {
		this.ast = ast;
	}

	public void setPast(boolean past) {
		this.past = past;
	}

    public void setSast(boolean sast) {
        this.sast = sast;
    }
	
	public void setBasic(boolean basic) {
	    this.basic = basic;
	}

    public void setFullRewriting() {
        this.strat = RewriteStrategy.FULL;
    }

    public void setInnermost() {
        this.strat = RewriteStrategy.INNERMOST;
    }

    public void setOutermost() {
        this.strat = RewriteStrategy.OUTERMOST;
    }

    public void setRewriteStrategy(RewriteStrategy strat) {
        this.strat = strat;
    }

    public void addProbabilisticRule(TRSTerm l, MultiDistribution<TRSTerm> r) {
        Set<TRSVariable> lhsvars = l.getVariables();
        Set<TRSVariable> rhsvars = new HashSet<>();
        for (Entry<Pair<TRSTerm, BigFraction>, Integer> entry : r.getProbabilityMapping().entrySet()) {
            final TRSTerm term = entry.getKey().getKey();
            rhsvars.addAll(term.getVariables());
        }
        if(l instanceof TRSFunctionApplication) {
            if (lhsvars.containsAll(rhsvars)) {
                if (this.probabilisticRules == null) {
                    this.probabilisticRules = new HashSet<>();
                }
                this.probabilisticRules.add(ProbabilisticRule.create((TRSFunctionApplication)l, r));
            } else {
                throw new UnhandledConstructException("probabilistic rule with free variable in the right hand side");
            }
        } else {
            throw new UnhandledConstructException("probabilistic rule with variable as left hand side");
        }
    }

    public boolean isComplexity() {
        return this.complexity;
    }

    public boolean isConstructorbased() {
        return this.constructorbased;
    }

    public Set<Rule> getSimpleRules() {
        return this.rules;
    }

    public Set<GeneralizedRule> getAllRules() {
        return this.allRules;
    }

    public Set<ConditionalRule> getConditionalRules() {
        return this.condrules;
    }
    
    public Set<Pair<TRSTerm,TRSTerm>> getAllLHSVariableRules() {
        return this.lhsVariableRules;
    }

    public Set<TRSFunctionApplication> getInitialTerms() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<Equation> getEquations() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<Rule> getRelativeRules() {
        return this.relativeRules;
    }

    public HashMap<FunctionSymbol, Set<Integer>> getContextSensitiveRules() {
        if (this.replacementMap.isEmpty()) {
            return null;
        }
        return this.replacementMap;
    }

    public boolean getInnermost() {
        return this.innermost;
    }

    public boolean getOutermost() {
        return this.outermost;
    }

    public boolean getParellelInnermost() {
        return this.parallelInnermost;
    }

    public void setComplexity(boolean complexity) {
        this.complexity = complexity;
    }

    public void setConstructorbased(boolean constructorbased) {
        this.constructorbased = constructorbased;
    }

    public Set<FunctionSymbol> getAssociativeFunctionSymbols() {
        return this.associativeNames;
    }

    public Set<FunctionSymbol> getCommutativeFunctionSymbols() {
        return this.commutativeNames;
    }

    public Set<FunctionSymbol> getAssAndCommFunctionSymbols() {
        return this.associativeAndCommutativeNames;
    }

    public void addVariable(TRSVariable x) {
        this.variables.add(x);
    }

    public void addSimpleRule(Rule r) {
        this.rules.add(r);
        this.allRules.add(r);
    }

    public void addGeneralizedRule(GeneralizedRule r) {
        this.allRules.add(r);
    }
    
    public void addLHSVariableRule(TRSTerm l, TRSTerm r) {
        this.lhsVariableRules.add(new Pair<TRSTerm,TRSTerm>(l,r));
    }

    public void setOutermost(boolean b) {
        this.outermost = b;
    }

    public void setInnermost(boolean b) {
        this.innermost = b;
    }

    public void setParallelInnermost(boolean b) {
        this.parallelInnermost = b;
    }

    public void setReplacementMap(HashMap<FunctionSymbol, Set<Integer>> replacementMap) {
        this.replacementMap = replacementMap;
    }

    public void addAssociativeFunctionSymbol(FunctionSymbol f) {
        this.associativeNames.add(f);
    }

    public void addCommutativeFunctionSymbol(FunctionSymbol f) {
        this.commutativeNames.add(f);
    }

    public void addAssociativeAndCommutativeFunctionSymbol(FunctionSymbol f) {
        this.associativeAndCommutativeNames.add(f);
    }

    public void addRelativeRule(Rule r) {
        this.relativeRules.add(r);
    }

    public void addConditionalRule(ConditionalRule condrule) {
        this.condrules.add(condrule);
    }

    public void addReplacementMapEntry(FunctionSymbol f, Set<Integer> replacements) {
        this.replacementMap.put(f, replacements);
    }

    public void addArityMapEntry(String name, int arity) {
        this.arityMap.put(name, arity);
    }

    public void addAbstractRule(TRSTerm lhs,
            TRSTerm rhs,
            boolean relative,
            List<Condition> conditions) {
        
        Set<TRSVariable> lhsvars = lhs.getVariables();
        Set<TRSVariable> rhsvars = rhs.getVariables();
        
        if (conditions.isEmpty()) {
            if (!lhs.isVariable()) {
                if (lhsvars.containsAll(rhsvars)) {
                    Rule r = Rule.create((TRSFunctionApplication) lhs, rhs);
                    if (relative) {
                        this.addRelativeRule(r);
                    } else {
                        this.addSimpleRule(r);
                    }
                } else {
                    if (relative) {
                        throw new UnhandledConstructException(
                            "relative rules/pairs that violate the variable condition");
                    } else {
                        GeneralizedRule r = GeneralizedRule.create((TRSFunctionApplication) lhs, rhs);
                        this.addGeneralizedRule(r);
                    }
                }
            } else {
                if (relative) {
                    if (lhsvars.containsAll(rhsvars) && Options.certifier.isNone()) {
                        this.addLHSVariableRule(lhs, rhs);
                    } else {
                        throw new UnhandledConstructException(
                            "rules that violate the variable condition");
                    }
                } else {
                    throw new UnhandledConstructException(
                        "(non-relative) rule with variables as lhs");
                }
            }
        } else {
            if (relative) {
                throw new UnhandledConstructException(
                    "conditional relative rules");
            }
            GeneralizedRule r = GeneralizedRule.create((TRSFunctionApplication) lhs, rhs);
            ImmutableList<Condition> conds = ImmutableCreator.create(conditions);
            ConditionalRule condrule = ConditionalRule.create(r, conds);
            this.addConditionalRule(condrule);
        }
    }
    
    public boolean isConfluence() {
        return this.confluence;
    }
    
    public void setConfluence(boolean confluence) {
        this.confluence = confluence;
    }
    
    public boolean isInfeasibility() {
        return this.infeasibility;
    }

    public void setInfeasibility(boolean infeasibility) {
        this.infeasibility = infeasibility;
    }
    
    public void setInfeasibilityQuery(List<Condition> queryList) {
        this.infeasibilityQuery = new LinkedHashSet<Condition>(queryList);
    }

    public Set<Condition> getInfeasibilityQuery() {
        return this.infeasibilityQuery;
    }
}
