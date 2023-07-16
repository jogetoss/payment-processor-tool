
package org.joget.marketplace.paypal.model;

import com.google.gson.annotations.SerializedName;

public class Amount {

    @SerializedName("currency_code")
    private String currencyCode;
    
    @SerializedName("value")
    private String value;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
