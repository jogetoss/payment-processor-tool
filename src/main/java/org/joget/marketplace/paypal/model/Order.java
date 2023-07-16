
package org.joget.marketplace.paypal.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Order {

    private String intent;
    
    @SerializedName("purchase_units")
    private List<PurchaseUnit> purchaseUnits;
    
    @SerializedName("payment_source")
    private PaymentSource paymentSource;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<PurchaseUnit> getPurchaseUnits() {
        return purchaseUnits;
    }

    public void setPurchaseUnits(List<PurchaseUnit> purchaseUnits) {
        this.purchaseUnits = purchaseUnits;
    }

    public PaymentSource getPaymentSource() {
        return paymentSource;
    }

    public void setPaymentSource(PaymentSource paymentSource) {
        this.paymentSource = paymentSource;
    }

}
