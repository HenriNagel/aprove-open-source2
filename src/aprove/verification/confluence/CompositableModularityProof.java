package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.dpframework.BasicStructures.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Utility.*;


public class CompositableModularityProof extends DefaultProof {
    
    private Collection<Rule> sharedRules;
    private Collection<FunctionSymbol> constructors;
    
    public CompositableModularityProof(Collection<Rule> sharedRules, Collection<FunctionSymbol> constructors) {
        this.sharedRules = sharedRules;
        this.constructors = constructors;
    }

    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        StringBuilder s = new StringBuilder();
        s.append(o.export("Compositable TRS are modular under confluence, if they are layer-preserving."));
        s.append(o.cite(Citation.OHL94A));
        s.append(o.cond_linebreak());
        s.append(o.export("These Rules are shared between different SubTRS:"));
        s.append(o.cond_linebreak());
        s.append(o.set(sharedRules, Export_Util.RULES));
        s.append(o.export("These Constructors can be shared between TRS:"));
        s.append(o.cond_linebreak());
        s.append(o.set(constructors, Export_Util.SIMPLESET));
        s.append(o.cond_linebreak());
        return s.toString();
    }

}
