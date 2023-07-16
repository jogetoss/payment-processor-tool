
package org.joget.marketplace.paypal.model;

import com.google.gson.annotations.SerializedName;

public class PurchaseUnit {

    @SerializedName("reference_id")
    private String referenceId;
    
    @SerializedName("invoice_id")
    private String invoiceId;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("amount")
    private Amount amount;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

}
