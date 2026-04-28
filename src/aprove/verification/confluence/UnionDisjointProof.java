package aprove.verification.confluence;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.oldframework.Utility.*;


public class UnionDisjointProof extends DefaultProof {
    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        return "The Union of TRS, which have disjoint signatures, are fully modular. " + o.cite(Citation.TOY87);
    }

}
