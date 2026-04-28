package aprove.verification.confluence;

import java.util.*;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.oldframework.BasicStructures.*;
import aprove.verification.oldframework.Utility.*;


public class ConstructorSharingModularityProof extends DefaultProof {
    final Set<FunctionSymbol> constructors;
    final boolean leftLinear;
    public ConstructorSharingModularityProof(final boolean leftLinear, final Set<FunctionSymbol> constructors) {
        this.constructors = constructors;
        this.leftLinear = leftLinear;
    }

    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        StringBuilder s = new StringBuilder();
        if (leftLinear) {
            s.append(o.export("The Union of left-linear, constructor sharing TRS is modular. "));
            s.append(o.cite(Citation.RV80));
        }
        else {
            s.append(o.export("The Union of layer-preserving, constructor sharing TRS is modular. "));
            s.append(o.cite(Citation.OHL94B));
        }
        s.append(o.cond_linebreak());
        s.append(o.export("These Constructor Symbols can be shared between TRS:"));
        s.append(o.cond_linebreak());
        s.append(o.set(constructors, Export_Util.SIMPLESET));
        return s.toString();
    }

}
