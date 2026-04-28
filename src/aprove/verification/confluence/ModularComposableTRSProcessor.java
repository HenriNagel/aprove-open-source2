package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Obligations.*;
import aprove.strategies.Abortions.*;
import aprove.strategies.ExecutableStrategies.*;
import aprove.verification.confluence.Utility.*;
import aprove.verification.dpframework.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Logic.*;
import immutables.*;


public class ModularComposableTRSProcessor implements Processor {

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        ConfTRSProblem conftrs = (ConfTRSProblem) obl;
        
        Collection<FunctionSymbol> constructors = conftrs.getSharedLayerPreservingConstructorSymbols();
        
        if (constructors.isEmpty()) {
            return ResultFactory.unsuccessful();
        }
        
        Collection<ImmutableSet<Rule>> rules = conftrs.calculateCompositableRules();
        
        if(1 <= rules.size()) {
            return ResultFactory.unsuccessful();
        }
        
        HereditaryProperties mp = conftrs.getHereditaryProperties();
        
        Collection<Rule> sharedRules = ProcessorUtil.findNonUniqueElements(rules);
        
        Collection<ConfTRSProblem> subconftrss = rules.stream()
                .map(R -> ConfTRSProblem.create(R, mp))
                .toList();
               
                
        return ResultFactory.provedAnd(subconftrss, YNMImplication.EQUIVALENT, new CompositableModularityProof(sharedRules, constructors));
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        if (obl instanceof ConfTRSProblem conftrs) {
            return !conftrs.isNotLayerPreserving();
        }
        return false;
    }
}

