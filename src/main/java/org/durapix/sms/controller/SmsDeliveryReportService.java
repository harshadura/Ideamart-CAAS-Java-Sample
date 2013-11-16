package org.durapix.sms.controller;

import hms.kite.samples.api.sms.MoSmsDeliveryReportListener;
import hms.kite.samples.api.sms.messages.MoSmsDeliveryReportReq;

import java.util.logging.Logger;

public class SmsDeliveryReportService implements MoSmsDeliveryReportListener {

    private final static Logger LOGGER = Logger.getLogger(SmsDeliveryReportService.class.getName());

    @Override
    public void onReceivedDeliveryReport(MoSmsDeliveryReportReq moDeliveryReportReq) {

        LOGGER.info("\n\n==> Sms delivery report received from SDP : "+ moDeliveryReportReq);

    }
}