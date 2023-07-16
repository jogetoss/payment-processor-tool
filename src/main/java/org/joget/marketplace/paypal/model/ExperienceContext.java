package org.joget.marketplace.paypal.model;

import com.google.gson.annotations.SerializedName;

public class ExperienceContext {

    @SerializedName("payment_method_preference")
    private String paymentMethodPreference;
    
    @SerializedName("payment_method_selected")
    private String paymentMethodSelected;
    
    @SerializedName("user_action")
    private String userAction;
    
    @SerializedName("return_url")
    private String returnUrl;
    
    @SerializedName("cancel_url")
    private String cancelUrl;

    public String getPaymentMethodPreference() {
        return paymentMethodPreference;
    }

    public void setPaymentMethodPreference(String paymentMethodPreference) {
        this.paymentMethodPreference = paymentMethodPreference;
    }

    public String getPaymentMethodSelected() {
        return paymentMethodSelected;
    }

    public void setPaymentMethodSelected(String paymentMethodSelected) {
        this.paymentMethodSelected = paymentMethodSelected;
    }

    public String getUserAction() {
        return userAction;
    }

    public void setUserAction(String userAction) {
        this.userAction = userAction;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

}
