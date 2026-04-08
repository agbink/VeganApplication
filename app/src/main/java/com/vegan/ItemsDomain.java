package com.vegan;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemsDomain implements Serializable {
    private String pimg;
    private String pname;
    private String pbname;
    private int pprice;
    private String pdetailimg;
    private int pid;
    private int stock;
    private int category;
    private String psay;
    private long timestamp;
    private int salescount;


    public int getSalescount() {
        return salescount;
    }

    public void setSalescount(int salescount) {
        this.salescount = salescount;
    }

    public String getPsay() {
        return psay;
    }

    public void setPsay(String psay) {
        this.psay = psay;
    }

    public ItemsDomain(){}

    public String getPimg() {
        return pimg;
    }

    public void setPimg(String pimg) {
        this.pimg = pimg;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public int getPprice() {
        return pprice;
    }

    public void setPprice(int pprice) {
        this.pprice = pprice;
    }

    public String getPdetailimg() {
        return pdetailimg;
    }

    public void setPdetailimg(String pdetailimg) {
        this.pdetailimg = pdetailimg;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getPbname() {
        return pbname;
    }

    public void setPbname(String pbname) {
        this.pbname = pbname;
    }
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}