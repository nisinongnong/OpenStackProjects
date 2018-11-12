package com.jzby.util;

import java.io.Serializable;

/**
 * Created by gordan on 2017/10/25.
 */

public class JZInputDevice implements Serializable
{
    private static final long serialVersionUID = -1803647563555210822L;
    private int id;

    private int productId;

    private int vendorId;

    private String name;

    private String descriptor;

    private boolean redirectFlag;

    public JZInputDevice()
    {

    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public boolean isRedirectFlag() {
        return redirectFlag;
    }

    public void setRedirectFlag(boolean redirectFlag) {
        this.redirectFlag = redirectFlag;
    }
}
