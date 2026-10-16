package com.ecom.products.models;

import java.io.Serializable;
import java.util.Date;

public class BaseModel implements Serializable {
    private Long id;
    private String name;
    private int userId;
    private Date created;
    private Date updated;
    private boolean deleted;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getUserId() {
        return this.userId;
    }

    public Date getCreated() {
        return this.created;
    }

    public Date getUpdated() {
        return this.updated;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
