package org.joget.marketplace;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowAssignment;

public class PaymentProcessorTool extends DefaultApplicationPlugin implements PluginWebSupport {

    private final static String MESSAGE_PATH = "messages/PaymentProcessorTool";
    private static final String METHOD_PAYPAL = "PAYPAL";
    private static final String METHOD_STRIPE = "STRIPE";
    private static final String METHOD_ALL = "ALL";

    @Override
    public Object execute(Map properties) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        String recordId;
        AppDefinition appDef = (AppDefinition) properties.get("appDef");
        WorkflowAssignment wfAssignment = (WorkflowAssignment) properties.get("workflowAssignment");

        if (wfAssignment != null) {
            recordId = appService.getOriginProcessId(wfAssignment.getProcessId());
        } else {
            recordId = (String) properties.get("recordId");
        }
        if (recordId != null && !recordId.isEmpty()) {
            String paymentMethod = (String) properties.get("paymentMethod");
            if (METHOD_PAYPAL.equalsIgnoreCase(paymentMethod)) {
                PayPalPaymentProcessor payPalPaymentProcessor = new PayPalPaymentProcessor();
                payPalPaymentProcessor.generatePaymentLink(properties, appDef, recordId);
            } else if (METHOD_STRIPE.equalsIgnoreCase(paymentMethod)) {
                StripePaymentProcessor stripePaymentProcessor = new StripePaymentProcessor();
                stripePaymentProcessor.generatePaymentLink(properties, appDef, recordId);
            } else if (METHOD_ALL.equalsIgnoreCase(paymentMethod)) {
                PayPalPaymentProcessor payPalPaymentProcessor = new PayPalPaymentProcessor();
                payPalPaymentProcessor.generatePaymentLink(properties, appDef, recordId);

                StripePaymentProcessor stripePaymentProcessor = new StripePaymentProcessor();
                stripePaymentProcessor.generatePaymentLink(properties, appDef, recordId);
            }
        }

        return null;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String provider = request.getParameter("provider");
        String id = request.getParameter("id");
        String formDefId = request.getParameter("formDefId");
        String token = request.getParameter("token");

        if (provider != null && !provider.isEmpty() && "paypal".equalsIgnoreCase(provider)) {
            PayPalPaymentProcessor payPalPaymentProcessor = new PayPalPaymentProcessor();
            if (action != null && !action.isEmpty() && "generateLink".equalsIgnoreCase(action)) {
                payPalPaymentProcessor.createPayPalLink(request, response, id, formDefId);
            } else if (action != null && !action.isEmpty() && "created".equalsIgnoreCase(action)) {
                payPalPaymentProcessor.captureOrder(response, id, formDefId, token);
            } else if (action != null && !action.isEmpty() && "cancelled".equalsIgnoreCase(action)) {
                payPalPaymentProcessor.processCancel(response, id, formDefId);
            }
        } else if (provider != null && !provider.isEmpty() && "stripe".equalsIgnoreCase(provider)) {
            StripePaymentProcessor stripePaymentProcessor = new StripePaymentProcessor();
            if (action != null && !action.isEmpty() && "generateLink".equalsIgnoreCase(action)) {
                stripePaymentProcessor.createStripeLink(request, response, id, formDefId);
            } else if (action != null && !action.isEmpty() && "success".equalsIgnoreCase(action)) {
                stripePaymentProcessor.processPayment(request, response, id, formDefId);
            }
        }
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("processtool.paymentprocessortool.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("processtool.paymentprocessortool.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("processtool.paymentprocessortool.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/paymentProcessorTool.json", null, true, MESSAGE_PATH);
    }

}
