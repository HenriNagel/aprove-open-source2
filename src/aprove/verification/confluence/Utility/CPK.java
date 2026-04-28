package aprove.verification.confluence.Utility;

import aprove.verification.oldframework.Logic.*;

public enum CPK {

                 MAYBE,

                 NOTWCR,

                 NOTCR,

                 WCR,

                 WCR_DONE;

    public boolean isAnyAnswer() {
        return this != CPK.MAYBE;
    }
    
    public boolean isDefinitiveAnswer() {
        return this.isAnyAnswer() && this != WCR;
    }

    public YNM intoYNM() {
        if (this == CPK.MAYBE)
            return YNM.MAYBE;
        if (this == CPK.NOTCR || this == CPK.NOTWCR)
            return YNM.NO;
        return YNM.YES;
    }

}
