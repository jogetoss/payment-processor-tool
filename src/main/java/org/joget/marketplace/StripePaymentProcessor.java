package org.joget.marketplace;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.paypal.util.PaymentUtil;
import org.joget.marketplace.stripe.model.PluginProperties;
import org.joget.workflow.util.WorkflowUtil;

public class StripePaymentProcessor {

    public void generatePaymentLink(Map properties, AppDefinition appDef, String recordId) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String formDefId = (String) properties.get("formDefId");
        String apiKey = (String) properties.get("apiKey");
        String currency = (String) properties.get("currency");
        String totalAmount = (String) properties.get("totalAmount");
        String redirectUserviewMenu = (String) properties.get("redirectUserviewMenu");
        String redirectUserviewMenuFormID = (String) properties.get("redirectUserviewMenuFormID");
        String serverUrl = PaymentUtil.getServerUrl();
        String stripePaymentLink = serverUrl + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion()
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=generateLink&provider=stripe" 
                + "&id=" + recordId + "&formDefId=" + formDefId;

        org.joget.marketplace.stripe.model.PluginProperties pp = new org.joget.marketplace.stripe.model.PluginProperties();
        pp.setApiKey(apiKey);
        pp.setCurrency(currency);
        pp.setTotalAmount(totalAmount);
        pp.setRedirectUserviewMenu(redirectUserviewMenu);
        pp.setRedirectUserviewMenuFormID(redirectUserviewMenuFormID);

        Gson gson = new Gson();
        String jsonPp = gson.toJson(pp);

        FormRowSet set = new FormRowSet();
        FormRow formRow = new FormRow();
        formRow.put("stripe_payment_link", stripePaymentLink);
        formRow.put("stripe_plugin_properties", jsonPp);
        formRow.put("payment_status", "PAYER_ACTION_REQUIRED");
        set.add(formRow);
        set.add(0, formRow);
        appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, set, recordId);
    }

    public void createStripeLink(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();
        PaymentUtil util = new PaymentUtil();

        FormRow row = new FormRow();
        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);
        if (rowSet != null && !rowSet.isEmpty()) {
            row = rowSet.get(0);
            Gson gson = new Gson();
            PluginProperties pp = gson.fromJson(row.getProperty("stripe_plugin_properties"), PluginProperties.class);
            Stripe.apiKey = pp.getApiKey();
            String redirectUserviewMenu = pp.getRedirectUserviewMenu();
            String redirectUserviewMenuFormID = pp.getRedirectUserviewMenuFormID();
            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuFormID + "?id=" + id;

            String paymentStatus = row.getProperty("payment_status");

            if (paymentStatus != null && !paymentStatus.isEmpty() && "COMPLETED".equalsIgnoreCase(paymentStatus) || "succeeded".equalsIgnoreCase(paymentStatus)) {
                response.sendRedirect(redirectURL + "&src=stored");
            } else {
                String currency = pp.getCurrency();
                String totalAmount = pp.getTotalAmount();

                try {
                    String productId = util.stripeCreateProducts();
                    Long totalAmountLong = (long) (Double.parseDouble(totalAmount) * 100);
                    String priceId = util.stripeCreatePrices(currency, totalAmountLong, productId);
                    String paymentUrl = util.stripeCreatePaymentLinks(priceId, formDefId, id, appId, appVersion);
                    response.sendRedirect(paymentUrl);

                } catch (StripeException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                    response.sendRedirect(redirectURL + "&src=error");
                    return;
                }
            }
        }
    }

    public void processPayment(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        String sessionId = request.getParameter("session_id");

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();

        FormRow row = new FormRow();
        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);

        if (rowSet != null && !rowSet.isEmpty()) {
            row = rowSet.get(0);
            Gson gson = new Gson();
            PluginProperties pp = gson.fromJson(row.getProperty("stripe_plugin_properties"), PluginProperties.class);
            
            Stripe.apiKey = pp.getApiKey();
            
            String redirectUserviewMenu = pp.getRedirectUserviewMenu();
            String redirectUserviewMenuFormID = pp.getRedirectUserviewMenuFormID();
            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuFormID + "?id=" + id;

            try {
                Session session = Session.retrieve(sessionId);
                String paymentIntentId = session.getPaymentIntent();
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                String paymentStatus = paymentIntent.getStatus();

                String payload = gson.toJson(paymentIntent);
                
                // save to db
                FormRowSet set = new FormRowSet();
                FormRow r1 = new FormRow();
                r1.put("stripe_payment_intent_id", paymentIntentId);
                r1.put("stripe_payment_intent", payload);
                r1.put("payment_status", paymentStatus);
                set.add(r1);
                set.add(0, r1);
                appService.storeFormData(appId, appVersion, formDefId, set, id);

                response.sendRedirect(redirectURL + "&src=gateway");
            } catch (StripeException ex) {
                LogUtil.error(getClassName(), ex, ex.getMessage());
                response.sendRedirect(redirectURL + "&src=error");
                return;
            }
        }
    }

    public String getClassName() {
        return this.getClass().getName();
    }

}
