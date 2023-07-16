
package org.joget.marketplace.paypal.model;

import com.google.gson.annotations.SerializedName;

public class Paypal {

    @SerializedName("experience_context")
    private ExperienceContext experienceContext;

    public ExperienceContext getExperienceContext() {
        return experienceContext;
    }

    public void setExperienceContext(ExperienceContext experienceContext) {
        this.experienceContext = experienceContext;
    }

}
