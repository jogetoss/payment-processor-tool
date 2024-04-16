package org.joget.marketplace;

import com.google.gson.Gson;
import com.razorpay.RazorpayException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.paypal.util.PaymentUtil;
import org.joget.marketplace.razorpay.model.PluginProperties;
import org.joget.marketplace.razorpay.util.RazorPayUtil;
import org.joget.workflow.util.WorkflowUtil;

public class RazorpayPaymentProcessor {

    public void generatePaymentLink(Map properties, AppDefinition appDef, String recordId) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String formDefId = (String) properties.get("formDefId");
        String apiKey = (String) properties.get("razorApiKey");
        String apiSecret = (String) properties.get("apiSecret");
        String productName = (String) properties.get("productName");
        String currency = (String) properties.get("currency");
        String totalAmount = (String) properties.get("totalAmount");
        String redirectUserviewMenu = (String) properties.get("redirectUserviewMenu");
        String redirectUserviewMenuFormID = (String) properties.get("redirectUserviewMenuFormID");
        String serverUrl = PaymentUtil.getServerUrl();
        String razorpayPaymentLink = serverUrl + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion()
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=generateLink&provider=razorpay"
                + "&id=" + recordId + "&formDefId=" + formDefId;

        org.joget.marketplace.razorpay.model.PluginProperties pp = new org.joget.marketplace.razorpay.model.PluginProperties();
        pp.setApiKey(apiKey);
        pp.setApiSecret(apiSecret); // This line is added to set the API secret
        pp.setProductName(productName);
        pp.setCurrency(currency);
        pp.setTotalAmount(totalAmount);
        pp.setRedirectUserviewMenu(redirectUserviewMenu);
        pp.setRedirectUserviewMenuFormID(redirectUserviewMenuFormID);

        Gson gson = new Gson();
        String jsonPp = gson.toJson(pp);

        FormRowSet set = new FormRowSet();
        FormRow formRow = new FormRow();
        formRow.put("razorpay_payment_link", razorpayPaymentLink);
        formRow.put("razorpay_plugin_properties", jsonPp);
        formRow.put("payment_status", "PAYER_ACTION_REQUIRED");
        formRow.put("id", recordId);
        set.add(formRow);
        set.add(0, formRow);
        appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, set, recordId);
    }

    public void createRazorpayLink(HttpServletRequest request, HttpServletResponse response, String id, String formDefId, String recordId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = appDef.getAppId();
        String appVersion = String.valueOf(appDef.getVersion());

        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);
        if (rowSet != null && !rowSet.isEmpty()) {
            FormRow row = rowSet.get(0);
            Gson gson = new Gson();
            // Logging the JSON string to be deserialized
            String razorPluginPropertiesJson = row.getProperty("razorpay_plugin_properties");
            PluginProperties pp = gson.fromJson(row.getProperty("razorpay_plugin_properties"), PluginProperties.class);
            // Logging to check if pp is null after deserialization
            if (pp == null) {
                LogUtil.error(getClass().getName(), null, "Deserialized PluginProperties object (pp) is null.");
                response.sendRedirect("/error?message=Invalid plugin properties configuration");
                return; // Stop further execution since pp is null
            }

            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + pp.getRedirectUserviewMenu() + "/_/" + pp.getRedirectUserviewMenuFormID() + "?id=" + id;
            String paymentStatus = row.getProperty("payment_status");

            if (paymentStatus != null && !paymentStatus.isEmpty() && "COMPLETED".equalsIgnoreCase(paymentStatus) || "success".equalsIgnoreCase(paymentStatus)) {
                response.sendRedirect(redirectURL + "&src=stored");
            } else {
                try {
                    RazorPayUtil razorPayUtil = new RazorPayUtil(pp.getApiKey(), pp.getApiSecret());

                    // Convert total amount to the smallest currency unit (e.g., paisa for INR)
                    long totalAmountInSmallestUnit = Long.parseLong(pp.getTotalAmount()) * 100;
                    Map<String, Object> orderDetails = new HashMap<>();
                    orderDetails.put("amount", totalAmountInSmallestUnit);
                    orderDetails.put("currency", pp.getCurrency());
                    orderDetails.put("receipt", "order_rcptid_" + id);
                    orderDetails.put("payment_capture", 1);

                    // Create Razorpay Order
                    String orderId = razorPayUtil.createRazorpayOrder(orderDetails);

                    // Create Payment Link
                    Map<String, Object> linkDetails = new HashMap<>();
                    linkDetails.put("amount", totalAmountInSmallestUnit);
                    linkDetails.put("currency", pp.getCurrency());
                    linkDetails.put("description", "Payment for " + pp.getProductName());
                    String paymentLink = razorPayUtil.createRazorpayPaymentLink(linkDetails, orderId, formDefId, recordId);
                    
                    // Redirect user to the payment link
                    response.sendRedirect(paymentLink);
                } catch (RazorpayException e) {
                    LogUtil.error(getClass().getName(), e, "Razorpay API call failed: " + e.getMessage());
                    response.sendRedirect(redirectURL + "&src=error");
                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Failed to create Razorpay payment link");
                    response.sendRedirect(redirectURL + "&src=error");
                }
            }
        } else {
            response.sendRedirect("/error?message=no_form_data_found");
        }
    }

    public void processRazorpayPayment(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        
        // Log the full request URL
        String fullRequestUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            fullRequestUrl += "?" + request.getQueryString();
        }
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String paymentId = request.getParameter("razorpay_payment_id");
        String signature = request.getParameter("razorpay_signature");
        String paymentLinkId = request.getParameter("razorpay_payment_link_id");
        String paymentLinkRefId = request.getParameter("razorpay_payment_link_reference_id");
        String paymentLinkStatus = request.getParameter("razorpay_payment_link_status");

        

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();

        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);

        if (rowSet != null && !rowSet.isEmpty()) {
            FormRow row = rowSet.get(0);
            Gson gson = new Gson();
            PluginProperties pp = gson.fromJson(row.getProperty("razorpay_plugin_properties"), PluginProperties.class);

            String redirectUserviewMenu = pp.getRedirectUserviewMenu();
            String redirectUserviewMenuFormID = pp.getRedirectUserviewMenuFormID();
            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuFormID + "?id=" + id;

            if (verifyRazorpaySignature(paymentLinkId, paymentLinkRefId, paymentLinkStatus, paymentId, signature, pp.apiSecret)) {
                // Save Razorpay payment information to the database
                FormRowSet set = new FormRowSet();
                FormRow r1 = new FormRow();

                r1.put("razorpay_payment_id", paymentId); // Save the Razorpay payment ID
                r1.put("razorpay_payment_link_id", paymentLinkId); // Save the Razorpay payment link ID
                r1.put("razorpay_payment_link_reference_id", paymentLinkRefId); // Save the Razorpay payment link reference ID
                r1.put("razorpay_payment_link_status", paymentLinkStatus); // Save the Razorpay payment link status
                r1.put("payment_status", "success"); // Save the payment status
                set.add(r1);
                set.add(0, r1);
                appService.storeFormData(appId, appVersion, formDefId, set, id);

                // Process the post-payment form
                FormData formData = new FormData();
                String primaryKey = appService.getOriginProcessId(id);
                formData.setPrimaryKeyValue(primaryKey);
                Form loadForm = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), formDefId, null, null, null, formData, null, null);
                formData.addRequestParameterValues(FormUtil.FORM_META_ORIGINAL_ID, new String[]{id});

                if (loadForm != null) {
                    Map<String, Object> propertiesMap = loadForm.getProperties();
                    String postProcessorRunOn = (String) propertiesMap.get("postProcessorRunOn");
                    if ("update".equalsIgnoreCase(postProcessorRunOn)) {
                        appService.submitForm(loadForm, formData, true);
                    }

                }

                // Redirect to the success page
                response.sendRedirect(redirectURL + "&src=gateway");
            } else {

                // Handle verification failure
                response.sendRedirect(redirectURL + "&src=error");
            }
        }
    }

    private boolean verifyRazorpaySignature(String paymentLinkId, String paymentLinkRefId, String paymentLinkStatus, String paymentId, String signature, String apiSecret) {
        try {
            String payload = paymentLinkId + "|" + paymentLinkRefId + "|" + paymentLinkStatus + "|" + paymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
           
            SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            String hash = Hex.encodeHexString(sha256_HMAC.doFinal(payload.getBytes()));
           
            boolean isSignatureValid = hash.equals(signature);

            return isSignatureValid;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LogUtil.error(getClass().getName(), e, "Error verifying Razorpay signature");
            return false;
        }
    }

}
