package org.durapix.sms.controller;

import java.util.*;
import hms.kite.samples.api.StatusCodes;
import hms.kite.samples.api.caas.ChargingRequestSender;
import hms.kite.samples.api.caas.messages.DirectDebitRequest;
import hms.kite.samples.api.caas.messages.DirectDebitResponse;
import hms.kite.samples.api.sms.MoSmsListener;
import hms.kite.samples.api.sms.SmsRequestSender;
import hms.kite.samples.api.sms.messages.MoSmsReq;
import hms.kite.samples.api.sms.messages.MtSmsReq;
import hms.kite.samples.api.sms.messages.MtSmsResp;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public class SmsHandler implements MoSmsListener {

    private String app_url = "http://localhost:7000/sms/send";
    private String CAAS_URL_directDebit = "http://localhost:7000/caas/direct/debit";
    private String app_id = "APP_000001";
    private String app_password = "password";
    private String app_version;

    private final int MAX_CHARACTERS = 160;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final static Logger LOGGER = Logger.getLogger(SmsHandler.class.getName());

    private MtSmsReq mtSmsReq;
    private SmsRequestSender smsMtSender;
    private String deliveryReq;

    @Override
    public void init() {
        try {
            LOGGER.info("\n\nSystem Starting....");
            smsMtSender = new SmsRequestSender(new URL(app_url));

        } catch (Exception e) {
            LOGGER.info("Url format is wrong, check the url again.");
            e.printStackTrace();
        }
    }

    @Override
    public void onReceivedSms(MoSmsReq moSmsReq) {

        init();

        app_version = moSmsReq.getVersion();
        deliveryReq = moSmsReq.getDeliveryStatusRequest();

        String phoneNumber = moSmsReq.getSourceAddress();
        String message = moSmsReq.getMessage().trim();

        Calendar cal = Calendar.getInstance();
        String getTime = dateFormat.format(cal.getTime());
        LOGGER.info("Server Time : " + getTime
                + "\n\n******** New Message Received >> SMS: " + message + " | From: " + phoneNumber + "\n");

        String parts[] = message.split(" ");

        // Syntax: KEY DEBIT <ServiceCharge>

        if (parts[1].equalsIgnoreCase("debit")) {
            try {
                DirectDebitResponse directDebitResponse = directDebit(phoneNumber, parts[2]);
                String statusCode = directDebitResponse.getStatusCode();
                String statusDetails = directDebitResponse.getStatusDetail();

                if (statusCode.equals("S1000")) {
                    String infoMsg = "Charging Process completed successfully.";
                    LOGGER.info(infoMsg);
                    sendReply(infoMsg, phoneNumber);
                } else {
                    String infoMsg = "Charging Process failed with status code [" + statusCode + "] " + statusDetails;
                    LOGGER.info(infoMsg);
                    sendReply(infoMsg, phoneNumber);
                }

                return;
            } catch (Exception ee) {
                ee.printStackTrace();
                LOGGER.info("Error : " + ee.getMessage());
            }
        }
        else{
            String infoMsg = "Hello!";
            LOGGER.info(infoMsg);
            sendReply(infoMsg, phoneNumber);
        }
    }

    public void sendReply(String reply, String address) {

        LOGGER.info("Start sending sms message[" + reply + "] to [" + address + "]");
        if (reply.length() > MAX_CHARACTERS) {
            int lastIndex = reply.substring(0, MAX_CHARACTERS).lastIndexOf(" ");
            mtSmsReq = createSimpleMtSms(reply.substring(0, lastIndex), address);
            sendReply(reply.substring(lastIndex + 1, reply.length()), address);
        } else {
            mtSmsReq = createSimpleMtSms(reply, address);
        }

        mtSmsReq.setApplicationId(app_id);
        mtSmsReq.setPassword(app_password);
        mtSmsReq.setVersion(app_version);

        try{
            if (deliveryReq != null) {
                if (deliveryReq.equals("1")) {
                    mtSmsReq.setDeliveryStatusRequest("1");
                }
            } else {
                mtSmsReq.setDeliveryStatusRequest("0");
            }

            MtSmsResp mtSmsResp = smsMtSender.sendSmsRequest(mtSmsReq);
            String statusCode = mtSmsResp.getStatusCode();
            String statusDetails = mtSmsResp.getStatusDetail();
            if (StatusCodes.SuccessK.equals(statusCode)) {
                LOGGER.info("MT SMS message successfully sent");
            } else {
                LOGGER.info("MT SMS message sending failed with status code [" + statusCode + "] "+statusDetails);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private MtSmsReq createSimpleMtSms(String reply, String address) {
        MtSmsReq mtSmsReq = new MtSmsReq();

        mtSmsReq.setMessage(reply);
        List<String> addressList = new ArrayList<String>();
        addressList.add(address);
        mtSmsReq.setDestinationAddresses(addressList);

        return mtSmsReq;
    }

    protected DirectDebitResponse directDebit(String phoneNumber, String serviceCharges) {
        DirectDebitResponse directDebitResponse = new DirectDebitResponse();
        try {
            Integer Min = 1000000;
            Integer Max = 2000000;
            Integer rand = Min + (int)(Math.random() * ((Max - Min) + 1));

            DirectDebitRequest directDebitRequest = new DirectDebitRequest();
            directDebitRequest.setApplicationId(app_id);
            directDebitRequest.setPassword(app_password);
            directDebitRequest.setExternalTrxId(String.valueOf(rand));
            directDebitRequest.setSubscriberId(phoneNumber);
            directDebitRequest.setAmount(serviceCharges);

            ChargingRequestSender chargingRequestSender = new ChargingRequestSender(new URL(CAAS_URL_directDebit));
            directDebitResponse = chargingRequestSender.sendDirectDebitRequest(directDebitRequest);

        } catch (Exception ex) {
            LOGGER.info(ex.getMessage());
            ex.printStackTrace();
        }
        return directDebitResponse;
    }
}

