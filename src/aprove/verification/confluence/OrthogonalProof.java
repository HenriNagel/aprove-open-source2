package aprove.verification.confluence;

import aprove.prooftree.Export.Utility.*;
import aprove.prooftree.Proofs.Proof.*;
import aprove.verification.oldframework.Utility.*;

public class OrthogonalProof extends DefaultProof {
    
    boolean weak;
    
    public OrthogonalProof(boolean weak) {
        this.weak = weak;
    }

    @Override
    public String export(Export_Util o, VerbosityLevel level) {
        StringBuilder s = new StringBuilder();
        s.append(o.export("The TRS is left-linear and "));
        if (this.weak) {
            s.append(o.export("all critical pairs are trivial."));
            s.append(o.cond_linebreak());
            s.append(o.export("Therefore the TRS is weakly"));
        }
        else {
            s.append(o.export("non-overlapping."));
            s.append(o.cond_linebreak());
            s.append(o.export("Therefore the TRS is"));
        }
        s.append(o.export(" orthogonal."));
        s.append(o.cond_linebreak());
        s.append(o.cond_linebreak());
        s.append(o.export("This is sufficient for confluence. "));
        s.append(o.cite(Citation.BN98));
        return s.toString();
    }
    
}