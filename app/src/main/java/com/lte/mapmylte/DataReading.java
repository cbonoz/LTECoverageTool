/*
 * This software was developed by employees of the National Institute of Standards and Technology (NIST), an agency of the Federal Government
 * and is being made available as a public service. Pursuant to title 17 United States Code Section 105, works of NIST employees are not
 * subject to copyright protection in the United States.  This software may be subject to foreign copyright.  Permission in the United States
 * and in foreign countries, to the extent that NIST may hold copyright, to use, copy, modify, create derivative works, and distribute
 * this software and its documentation without fee is hereby granted on a non-exclusive basis, provided that this notice and disclaimer of
 * warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO,
 * ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL
 * BE ERROR FREE.  IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL
 * DAMAGES, ARISING OUT OF, RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY, CONTRACT, TORT, OR OTHERWISE,
 * WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT OF THE RESULTS OF,
 * OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package com.lte.mapmylte;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

class DataReading implements Serializable {

    static final int UNAVAILABLE = 2147483647;
    static final int LOW_RSRP = -141;
    static final int LOW_RSRQ = -20;
    static final int PCI_NA = -1;
    static final int EXECELLENT_RSRP_THRESHOLD = -95;
    static final int GOOD_RSRP_THRESHOLD = -110;
    static final int POOR_RSRP_THRESHOLD = -140;

    private Date timestamp;
    private int rsrp;
    private int rsrq;
    private int pci;
    private double lat;
    private double lng;
    private double elevation;
    private double acc;
    private int floor;
    private boolean usesensor;
    private double sensorvalue;
    private String sensortype;
    private String sensorMetric;

    DataReading() {
        this.floor = -1;
        this.timestamp = new Date();
        this.rsrp = UNAVAILABLE;
        this.rsrq = UNAVAILABLE;
        this.pci = PCI_NA;
        this.acc = -1;
        this.lat = -1;
        this.lng = -1;
        this.elevation = -1;
        this.usesensor = false;
        this.sensorvalue = -1;
        this.sensortype = "Carbon";
        this.sensorMetric = "ppm";
    }

    DataReading(DataReading dataReading) {
        this.timestamp = new Date();
        this.rsrp = dataReading.rsrp;
        this.rsrq = dataReading.rsrq;
        this.pci = dataReading.pci;
        this.lng = dataReading.lng;
        this.lat = dataReading.lat;
        this.elevation = dataReading.elevation;
        this.floor = dataReading.floor;
        this.usesensor = dataReading.usesensor;
        this.sensorvalue = dataReading.sensorvalue;
        this.sensortype = dataReading.sensortype;
        this.sensorMetric = dataReading.sensorMetric;
    }

    Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    int getRsrp() {
        return rsrp;
    }

    void setRsrp(int rsrp) {
        this.rsrp = rsrp;
    }

    public int getRsrq() {
        return rsrq;
    }

    public void setRsrq(int rsrq) {
        this.rsrq = rsrq;
    }

    public int getPci() {
        return pci;
    }

    public void setPci(int pci) {
        this.pci = pci;
    }

    public void setLat(double lastLat) {
        this.lat = lastLat;
    }

    public double getLat() {
        return lat;
    }

    public void setLng(double lastLng) {
        this.lng = lastLng;
    }

    public double getLng() {
        return lng;
    }

    public void setAcc(double lastAcc) {
        this.acc = lastAcc;
    }

    public double getAcc() {
        return acc;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getElevation() {
        return elevation;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }



    public boolean isUsesensor() {
        return usesensor;
    }

    public void setUsesensor(boolean usesensor) {
        this.usesensor = usesensor;
    }

    public double getSensorvalue() {
        return sensorvalue;
    }

    public void setSensorvalue(double sensorvalue) {
        this.sensorvalue = sensorvalue;
    }


    public String getSensortype() {
        return sensortype;
    }

    public void setSensortype(String sensortype) {
        this.sensortype = sensortype;
    }


    public String getSensorMetric() {
        return sensorMetric;
    }

    public void setSensorMetric(String sensorMetric) {
        this.sensorMetric = sensorMetric;
    }

    public String getCsvString() {
        return String.format(Locale.US, "\"%s\",\"%d\",\"%d\",\"%f\",\"%f\",\"%f\",\"%f\",\"%s\",\"%s\"%n,\"%b\",\"%f\",%s,%s",
                DateFormat.getDateTimeInstance().format(getTimestamp()),
                getRsrp(),
                getRsrq(),
                getLat(),
                getLng(),
                getElevation(),
                getAcc(),
                getPci() == -1 ? "N/A" : getPci() + "",
                getFloor() == -1 ? "N/A" : getFloor() + "",
                isUsesensor(),
                getSensorvalue(),
                getSensortype(),
                getSensorMetric());
    }
}
