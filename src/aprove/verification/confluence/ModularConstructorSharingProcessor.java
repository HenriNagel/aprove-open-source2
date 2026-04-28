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


public class ModularConstructorSharingProcessor implements Processor {

    @Override
    public Result process(BasicObligation obl,
                          BasicObligationNode oblNode,
                          Abortion aborter,
                          RuntimeInformation rti) throws AbortionException {
        ConfTRSProblem conftrs = (ConfTRSProblem) obl;
        
        Collection<ImmutableSet<Rule>> rules;
        boolean leftLinear = conftrs.isLeftLinear();
        
        Set<FunctionSymbol> constructors = leftLinear ? conftrs.getSharedConstructorSymbols() : conftrs.getSharedLayerPreservingConstructorSymbols();
        
        if(constructors.isEmpty()) {
            return ResultFactory.unsuccessful();
        }
        
        rules = conftrs.getConstructorSharingRules(!leftLinear);
        

        if(1 == rules.size()) {
            return ResultFactory.unsuccessful();
        }
       
        if(0 == rules.size()) {
            return ResultFactory.error("Split lead to empty List");
        }
        
        HereditaryProperties mp = conftrs.getHereditaryProperties();
        
        Collection<ConfTRSProblem> subconftrss = rules.stream()
                .map(R -> ConfTRSProblem.create(R, mp))
                .toList();
        
        
        
               
                
        return ResultFactory.provedAnd(subconftrss, YNMImplication.EQUIVALENT, new ConstructorSharingModularityProof(leftLinear, constructors));
    }

    @Override
    public boolean isApplicable(BasicObligation obl) {
        if (obl instanceof ConfTRSProblem conftrs) {
            return conftrs.isLeftLinear() || !conftrs.isNotLayerPreserving();
        }
        return false;
    }

}
