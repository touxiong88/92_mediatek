package com.mediatek.op.policy;

import com.mediatek.common.policy.IKeyguardPLMNCapitalize;

public class DefaultKeyguardPLMNCapitalize implements IKeyguardPLMNCapitalize {

	@Override
	public String changedPlmnToCapitalize(String plmn) {
        if (plmn != null ){
            return plmn.toString().toUpperCase();	
        }
        return null;
    }
}
