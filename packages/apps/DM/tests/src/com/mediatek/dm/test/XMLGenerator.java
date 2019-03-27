/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.test;

import com.mediatek.xlog.Xlog;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLGenerator implements ConfigInfoGenerator {
    private static String TAG = "SmsReg/XMLGenerator";
    private Document dom = null;
    private List<SmsInfoUnit> smsInfoList = null;
    private String operatorName = null;
    private String manufacturerName = null;
    private String smsNumber = null;
    private String smsPort = null;
    private String srcPort = null;
    private String[] networkNumber = null;
    private static ConfigInfoGenerator xmlGenerator = null;

    public static ConfigInfoGenerator getInstance(String configName) {
        if (xmlGenerator == null) {
            File smsRegConfig = new File(configName);
            if (!smsRegConfig.exists()) {
                Xlog.e(TAG, "create XMLGenerator failed! config file"
                        + configName + " is not exist!");
            } else {
                xmlGenerator = new XMLGenerator(configName);
            }
        }
        return xmlGenerator;
    }

    private XMLGenerator(String uri) {
        parse(uri);
        getOperatorName();
    }

    boolean parse(String filename) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            File configFile = new File(filename);
            if (!configFile.exists()) {
                Xlog.e(TAG, "config file is not exist!");
            }
            dom = builder.parse(configFile);
        } catch (ParserConfigurationException e) {
            Xlog.e(TAG, "Create document builder failed!" + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (SAXException e) {
            Xlog.e(TAG, "Create dom failed! SAXException:" + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Xlog.e(TAG, "Create dom failed! IOException:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getValByTagName(Document dom, String tagname) {
        Node childnode = null;
        if (dom == null)
            return null;
        NodeList nodelist = dom.getElementsByTagName(tagname);
        Node node = nodelist.item(0);
        if (node != null)
            childnode = node.getFirstChild();
        else
            return null;

        if (childnode != null)
            return childnode.getNodeValue();
        else
            return null;
    }

    // private void getValArrayFromDom(Node node, String NodeName,
    // String[] strArray, int[] recordCount) {
    // if (node == null) {
    // NodeList nodeList = dom.getElementsByTagName("operator");
    // if(nodeList == null){
    // Xlog.e(TAG, "Node <operator> is not exist in the xml file.");
    // return;
    // }
    // for (int i = 0; i < nodeList.getLength(); i++) {
    // if (nodeList.item(i).getAttributes().getNamedItem("xml:id")
    // .getNodeValue().equals(operatorName)) {
    // node = nodeList.item(i);
    // break;
    // }
    // }
    // }
    //
    // if(node == null){
    // Xlog.e(TAG, "Node to read is null");
    // return;
    // }
    //
    // if (node.getNodeType() == Node.TEXT_NODE) { //Text node is the leaf
    // Xlog.w(TAG, "Node to read is a leaf.");
    // return;
    // }
    // String nodeName = node.getNodeName();
    // Xlog.i(TAG, "Node to read: " + nodeName);
    // if (nodeName.equalsIgnoreCase(NodeName)) {
    // for (; node != null;) {
    // Node siblingNode = node.getFirstChild();
    // while (siblingNode != null) {
    // Xlog.i(TAG, "sibling node: " + siblingNode.getNodeName());
    // if (siblingNode.getNodeType() != Node.TEXT_NODE) {
    // if (siblingNode.getFirstChild() == null) {
    // strArray[recordCount[0]] = siblingNode.getNodeValue();
    // Xlog.i(TAG, "found NodeValue: "
    // + siblingNode.getNodeValue());
    // } else {
    // strArray[recordCount[0]] = siblingNode
    // .getFirstChild().getNodeValue();
    // Xlog.i(TAG, "found NodeValue: "
    // + siblingNode.getFirstChild()
    // .getNodeValue());
    // }
    // recordCount[0]++;
    //
    // }
    // try {
    // siblingNode = siblingNode.getNextSibling();
    // if (siblingNode != null)
    // Xlog.i(TAG, "siblingNode NodeName: "
    // + siblingNode.getNodeName());
    // } catch (IndexOutOfBoundsException e) {
    // siblingNode = null;
    // }
    // }
    // try {
    // node = node.getNextSibling();
    // if (node != null)
    // Xlog.i(TAG, "node NodeName:-- " + node.getNodeName());
    // } catch (IndexOutOfBoundsException e) {
    // node = null;
    // }
    // }
    // return;
    // } else {
    // if (node.hasChildNodes()) {
    // getValArrayFromDom(node.getFirstChild(), NodeName, strArray,
    // recordCount);
    // }
    // Node sibling = null;
    // try {
    // sibling = node.getNextSibling();
    // } catch (IndexOutOfBoundsException e) {
    // sibling = null;
    // }
    // if (sibling != null) {
    // getValArrayFromDom(sibling, NodeName, strArray, recordCount);
    // }
    // return;
    // }
    // }

    private void getValArrayFromDom(Node node, String NodeName,
            ArrayList<String> strArray) {
        if (node == null) {
            NodeList nodeList = dom.getElementsByTagName("operator");
            if (nodeList == null) {
                Xlog.e(TAG, "Node <operator> is not exist in the xml file.");
                return;
            }
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getAttributes().getNamedItem("xml:id")
                        .getNodeValue().equals(operatorName)) {
                    node = nodeList.item(i);
                    break;
                }
            }
        }

        if (node == null) {
            Xlog.e(TAG, "Node to read is null");
            return;
        }

        if (node.getNodeType() == Node.TEXT_NODE) { // Text node is the leaf
            Xlog.w(TAG, "Node to read is a leaf.");
            return;
        }
        String nodeName = node.getNodeName();
        Xlog.i(TAG, "Node to read: " + nodeName);
        if (nodeName.equalsIgnoreCase(NodeName)) {
            for (; node != null;) {
                Node siblingNode = node.getFirstChild();
                while (siblingNode != null) {
                    Xlog.i(TAG, "sibling node: " + siblingNode.getNodeName());
                    if (siblingNode.getNodeType() != Node.TEXT_NODE) {
                        if (siblingNode.getFirstChild() == null) {
                            strArray.add(siblingNode.getNodeValue());
                            Xlog.i(TAG,
                                    "found NodeValue: "
                                            + siblingNode.getNodeValue());
                        } else {
                            String temp = siblingNode.getFirstChild()
                                    .getNodeValue();
                            if (temp != null) {
                                strArray.add(temp);
                            }
                            Xlog.i(TAG, "found NodeValue: " + temp);
                        }
                        // recordCount[0]++;
                    }
                    try {
                        siblingNode = siblingNode.getNextSibling();
                        if (siblingNode != null)
                            Xlog.i(TAG,
                                    "siblingNode NodeName: "
                                            + siblingNode.getNodeName());
                    } catch (IndexOutOfBoundsException e) {
                        siblingNode = null;
                    }
                }
                try {
                    node = node.getNextSibling();
                    if (node != null)
                        Xlog.i(TAG, "node NodeName:-- " + node.getNodeName());
                } catch (IndexOutOfBoundsException e) {
                    node = null;
                }
            }
            return;
        } else {
            if (node.hasChildNodes()) {
                getValArrayFromDom(node.getFirstChild(), NodeName, strArray);
            }
            Node sibling = null;
            try {
                sibling = node.getNextSibling();
            } catch (IndexOutOfBoundsException e) {
                sibling = null;
            }
            if (sibling != null) {
                getValArrayFromDom(sibling, NodeName, strArray);
            }
            return;
        }
    }

    public boolean getCustomizedStatus() {
        return "on".equalsIgnoreCase(getValByTagName(dom, "customized"));
    }

    public String getManufacturerName() {
        manufacturerName = getValByTagName(dom, "manufacturer");
        return manufacturerName;
    }

    public String getOperatorName() {
        if (operatorName == null) {
            operatorName = getValByTagName(dom, "operatorcustomized");
        }
        return operatorName;
    }

    public String getOemName() {
        if (operatorName == null) {
            operatorName = getValByTagName(dom, "oemname");
        }
        return operatorName;
    }

    public String getSmsNumber() {
        if (smsNumber == null) {
            smsNumber = getValByTagName(dom, "smsnumber");
        }
        return smsNumber;
    }

    public Short getSmsPort() {
        if (smsPort == null) {
            smsPort = getValByTagName(dom, "smsport");
        }
        return Short.parseShort(smsPort);
    }

    public Short getSrcPort() {
        if (srcPort == null) {
            srcPort = getValByTagName(dom, "srcport");
        }
        return Short.parseShort(srcPort);
    }

    public String[] getNetworkNumber() {
        if (networkNumber == null) {
            // int[] intArray = { 0 };
            // networkNumber = new String[10];
            ArrayList<String> valueArray = new ArrayList<String>();
            getValArrayFromDom(null, "networkNumber", valueArray);
            networkNumber = new String[valueArray.size()];
            valueArray.toArray(networkNumber);
        }
        return networkNumber;
    }

    public List<SmsInfoUnit> getSmsInfoList() {
        if (smsInfoList == null) {
            // int[] intArray = { 0 };
            // String[] smsArray = new String[32];
            ArrayList<String> valueArray = new ArrayList<String>();
            getValArrayFromDom(null, "segment", valueArray);
            String[] smsArray = new String[valueArray.size()];
            valueArray.toArray(smsArray);
            smsInfoList = new ArrayList<SmsInfoUnit>();
            for (int i = 0; (3 * i + 1) < smsArray.length
                    && smsArray[3 * i + 1] != null; i++) {
                SmsInfoUnit smsUnit = new SmsInfoUnit();
                if (3 * i < smsArray.length) {
                    smsUnit.setPrefix(smsArray[3 * i]);
                }
                if ((3 * i + 1) < smsArray.length) {
                    smsUnit.setContent(smsArray[3 * i + 1]);
                }
                if ((3 * i + 2) < smsArray.length) {
                    smsUnit.setPostfix(smsArray[3 * i + 2]);
                }
                smsInfoList.add(smsUnit);
            }
        }
        return smsInfoList;
    }
}
